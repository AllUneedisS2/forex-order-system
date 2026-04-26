package com.switchwon.forexordersystem.exchangerate.service;

import com.switchwon.forexordersystem.common.enums.Currency;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ExternalExchangeRateClient 단위 테스트
 * MockWebServer 로 외부 환율 API 를 가짜로 띄워, 실제 네트워크 의존 없이 검증
 * - 검증 범위
 *   1. USD 기준 응답을 KRW 기준 환율로 변환하는 로직
 *   2. 성공 응답 캐싱 후, 다음 실패 호출 시 캐시값으로 폴백
 *   3. 5xx / 응답 누락 / 연결 실패 시 BOOTSTRAP_SEED 또는 캐시로 폴백
 *   4. retryWhen(backoff(1, ...)) 가 실제로 재시도(총 2회 요청)하는지
 *   5. fallbackToMock=false 인 경우 예외 전파
 */
@DisplayName("ExternalExchangeRateClient")
class ExternalExchangeRateClientTest {

    // 가짜 외부 API 서버
    private MockWebServer mockServer;
    // 테스트 대상 (fallbackToMock=true 기본)
    private ExternalExchangeRateClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();

        // mockServer 의 baseUrl 을 사용하는 WebClient
        WebClient webClient = WebClient.builder()
                .baseUrl(mockServer.url("/").toString())
                .build();

        // fallback 활성 클라이언트
        client = new ExternalExchangeRateClient(webClient, true);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    // JSON 응답 헬퍼
    private MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(body);
    }

    // 성공 응답 (USD 기준): 1 USD = 1500 KRW, 1 USD = 150 JPY, 1 USD = 0.90 EUR, 1 USD = 7.0 CNY
    private static final String SUCCESS_BODY = """
            {
              "result": "success",
              "base_code": "USD",
              "rates": {
                "KRW": 1500.00,
                "JPY": 150.0,
                "EUR": 0.90,
                "CNY": 7.0,
                "GBP": 0.79
              }
            }
            """;

    @Nested
    @DisplayName("정상 응답")
    class Success {

        @Test
        @DisplayName("USD 기준 외부 응답을 KRW 기준으로 변환하여 USD/JPY/CNY/EUR 4개를 반환한다")
        void convert_to_krw_based() {
            // given - USD 기준 응답
            // 기대값:
            //   USD = 1500.00 (그대로)
            //   JPY = 1500.00 / 150.0 = 10.00
            //   EUR = 1500.00 / 0.90  = 1666.67 (HALF_UP)
            //   CNY = 1500.00 / 7.0   = 214.29 (HALF_UP)
            mockServer.enqueue(jsonResponse(SUCCESS_BODY));

            // when
            Map<Currency, BigDecimal> rates = client.fetchKrwRates().block(Duration.ofSeconds(10));

            // then
            assertThat(rates).containsOnlyKeys(Currency.USD, Currency.JPY, Currency.CNY, Currency.EUR);
            assertThat(rates.get(Currency.USD)).isEqualByComparingTo("1500.00");
            assertThat(rates.get(Currency.JPY)).isEqualByComparingTo("10.00");
            assertThat(rates.get(Currency.EUR)).isEqualByComparingTo("1666.67");
            assertThat(rates.get(Currency.CNY)).isEqualByComparingTo("214.29");
        }

        @Test
        @DisplayName("응답에 없는 통화(예: GBP)는 결과에서 제외된다")
        void ignore_unknown_currencies() {
            mockServer.enqueue(jsonResponse(SUCCESS_BODY));

            Map<Currency, BigDecimal> rates = client.fetchKrwRates().block(Duration.ofSeconds(10));

            // GBP 는 enum 에 없으므로 무시됨 → 결과 키에 GBP 없음 (애초에 Currency 에 없음)
            assertThat(rates).hasSize(4);
        }

        @Test
        @DisplayName("성공 응답이 캐시에 저장되어 다음 실패 호출 시 캐시값으로 폴백한다")
        void cache_last_success_then_use_on_failure() {
            // 1차: 성공 → 캐시 갱신
            mockServer.enqueue(jsonResponse(SUCCESS_BODY));
            Map<Currency, BigDecimal> first = client.fetchKrwRates().block(Duration.ofSeconds(10));

            // 2차: 실패 (재시도 1회 포함 → 총 2회 요청)
            mockServer.enqueue(new MockResponse().setResponseCode(500));
            mockServer.enqueue(new MockResponse().setResponseCode(500));
            Map<Currency, BigDecimal> second = client.fetchKrwRates().block(Duration.ofSeconds(10));

            // 캐시값과 동일
            assertThat(second.get(Currency.USD)).isEqualByComparingTo(first.get(Currency.USD));
            assertThat(second.get(Currency.JPY)).isEqualByComparingTo(first.get(Currency.JPY));
            assertThat(second.get(Currency.EUR)).isEqualByComparingTo(first.get(Currency.EUR));
            assertThat(second.get(Currency.CNY)).isEqualByComparingTo(first.get(Currency.CNY));
        }
        
    }

    @Nested
    @DisplayName("폴백 시나리오 (fallbackToMock=true)")
    class Fallback {

        @Test
        @DisplayName("API 5xx 에러가 재시도 후에도 실패하면 BOOTSTRAP_SEED 로 폴백한다")
        void fallback_on_server_error() {
            // 재시도 1회 → 총 2회 요청
            mockServer.enqueue(new MockResponse().setResponseCode(500));
            mockServer.enqueue(new MockResponse().setResponseCode(500));

            Map<Currency, BigDecimal> rates = client.fetchKrwRates().block(Duration.ofSeconds(10));

            // BOOTSTRAP_SEED 값 (2026-04-25 11:50 기준)
            assertThat(rates.get(Currency.USD)).isEqualByComparingTo("1477.65");
            assertThat(rates.get(Currency.JPY)).isEqualByComparingTo("9.26");
            assertThat(rates.get(Currency.CNY)).isEqualByComparingTo("215.81");
            assertThat(rates.get(Currency.EUR)).isEqualByComparingTo("1730.37");
        }

        @Test
        @DisplayName("응답에 KRW 환율이 없으면 IllegalStateException 발생 후 폴백한다")
        void fallback_when_krw_missing_in_response() {
            String missingKrw = """
                    {
                      "result": "success",
                      "rates": { "JPY": 150.0, "EUR": 0.90, "CNY": 7.0 }
                    }
                    """;
            // 재시도 1회 → 같은 응답 2번 enqueue
            mockServer.enqueue(jsonResponse(missingKrw));
            mockServer.enqueue(jsonResponse(missingKrw));

            Map<Currency, BigDecimal> rates = client.fetchKrwRates().block(Duration.ofSeconds(10));

            // BOOTSTRAP_SEED 폴백
            assertThat(rates.get(Currency.USD)).isEqualByComparingTo("1477.65");
        }

        @Test
        @DisplayName("연결 자체가 실패해도(Disconnect) 폴백한다")
        void fallback_on_connection_failure() {
            // 즉시 끊어서 빠르게 실패 (타임아웃 5초 안 기다림)
            mockServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
            mockServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

            Map<Currency, BigDecimal> rates = client.fetchKrwRates().block(Duration.ofSeconds(10));

            assertThat(rates.get(Currency.USD)).isEqualByComparingTo("1477.65");
        }

    }

    @Nested
    @DisplayName("재시도")
    class RetryLogic {
        @Test
        @DisplayName("일시적 5xx 에러 후 재시도 성공 시 정상 응답을 반환한다 (총 2회 요청)")
        void retry_then_success() {
            // 1차: 503, 2차(재시도): 200
            mockServer.enqueue(new MockResponse().setResponseCode(503));
            mockServer.enqueue(jsonResponse(SUCCESS_BODY));

            Map<Currency, BigDecimal> rates = client.fetchKrwRates().block(Duration.ofSeconds(10));

            assertThat(rates.get(Currency.USD)).isEqualByComparingTo("1500.00");
            // 정확히 2번 요청 (초기 1 + 재시도 1)
            assertThat(mockServer.getRequestCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("폴백 비활성 (fallbackToMock=false)")
    class NoFallback {
        @Test
        @DisplayName("fallbackToMock=false 인 경우 API 실패 시 예외를 전파한다")
        void propagate_error_when_fallback_disabled() {
            // fallback off 클라이언트 생성
            WebClient webClient = WebClient.builder()
                    .baseUrl(mockServer.url("/").toString())
                    .build();
            ExternalExchangeRateClient noFallbackClient =
                    new ExternalExchangeRateClient(webClient, false);

            mockServer.enqueue(new MockResponse().setResponseCode(500));
            mockServer.enqueue(new MockResponse().setResponseCode(500));

            assertThatThrownBy(() -> noFallbackClient.fetchKrwRates().block(Duration.ofSeconds(10)))
                    .isInstanceOf(Exception.class);
        }
    }

}