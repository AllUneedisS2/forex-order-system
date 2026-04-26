package com.switchwon.forexordersystem.exchangerate.service;

import com.switchwon.forexordersystem.common.enums.Currency;
import com.switchwon.forexordersystem.exchangerate.dto.ExternalApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 외부 환율 API 클라이언트
 *
 * 1. 호출 성공 시 환율을 메모리에 캐싱
 * 2. 실패 + fallback 옵션이 켜져 있으면 => 마지막 성공 값을 기준으로 ±1% 변동시켜 Mock 대체
 */
@Slf4j
@Component
public class ExternalExchangeRateClient {

    // WebClientConfig 주입
    private final WebClient webClient;

    // API 실패 시 Mock fallback 사용 여부 (yml: app.exchange-api.fallback-to-mock)
    private final boolean fallbackToMock;

    // 마지막 성공 응답 캐싱 -> 다음 fallback 작동시 시드
    // 안전을 위해 Atomic 개겣 사용
    private final AtomicReference<Map<Currency, BigDecimal>> lastSuccessfulRates = new AtomicReference<>();

    // 최초 부팅 후 성공 데이터가 없을 때 사용할 비상 시드
    // 20260425 11:50 기준
    private static final Map<Currency, BigDecimal> BOOTSTRAP_SEED = new EnumMap<>(Currency.class);
    static {
        BOOTSTRAP_SEED.put(Currency.USD, new BigDecimal("1477.65"));
        BOOTSTRAP_SEED.put(Currency.JPY, new BigDecimal("9.26")); // 단위는 100
        BOOTSTRAP_SEED.put(Currency.CNY, new BigDecimal("215.81"));
        BOOTSTRAP_SEED.put(Currency.EUR, new BigDecimal("1730.37"));
    }

    public ExternalExchangeRateClient(
            @Qualifier("exchangeApiWebClient") WebClient webClient,
            @Value("${app.exchange-api.fallback-to-mock:true}") boolean fallbackToMock) {
        this.webClient = webClient;
        this.fallbackToMock = fallbackToMock;
    }

    // USD 기준 최신 환율을 조회 => KRW 기준으로 변환
    // 실패 시 fallback 옵션에 따라 Mock 데이터로 대체
    public Mono<Map<Currency, BigDecimal>> fetchKrwRates() {
        return webClient.get()
                .uri("/latest/USD")
                .retrieve()
                .bodyToMono(ExternalApiResponse.class)
                .timeout(Duration.ofSeconds(5))
                .retryWhen(Retry.backoff(1, Duration.ofMillis(300)))
                .map(this::convertToKrwBased)
                // 성공한 결과는 다음 fallback 을 위해 캐시
                .doOnNext(rates -> {
                    lastSuccessfulRates.set(rates);
                    log.debug("외부 환율 API 성공 - 캐시 갱신: {}", rates);
                })
                .onErrorResume(e -> {
                    log.warn("외부 환율 API 실패: {}", e.getMessage());
                    if (fallbackToMock) {
                        Map<Currency, BigDecimal> mock = generateMockRates();
                        log.warn("Mock 데이터로 fallback (마지막 성공값 기준 ±1% 변동): {}", mock);
                        return Mono.just(mock);
                    }
                    return Mono.error(e);
                });
    }

    // USD 기준 응답을 KRW 기준으로 변환
    private Map<Currency, BigDecimal> convertToKrwBased(ExternalApiResponse response) {
        Map<String, BigDecimal> rates = response.getRates();
        BigDecimal usdToKrw = rates.get("KRW");
        if (usdToKrw == null) {
            throw new IllegalStateException("외부 API 응답에 KRW 환율이 없습니다.");
        }

        Map<Currency, BigDecimal> result = new EnumMap<>(Currency.class);
        // USD는 그대로
        result.put(Currency.USD, scale(usdToKrw));
        // 그 외 통화는 usdToKrw / usdToCurrency = currencyToKrw
        addIfPresent(result, Currency.JPY, rates.get("JPY"), usdToKrw);
        addIfPresent(result, Currency.CNY, rates.get("CNY"), usdToKrw);
        addIfPresent(result, Currency.EUR, rates.get("EUR"), usdToKrw);
        return result;
    }

    private void addIfPresent(Map<Currency, BigDecimal> result,
                              Currency currency,
                              BigDecimal usdToTarget,
                              BigDecimal usdToKrw) {
        if (usdToTarget == null || usdToTarget.signum() == 0) return; // null, 0 체크
        // USD 기준 환율을 KRW 기준으로 환산
        // target에 대한 KRW 환율 비율 계산 => usdToKrw / usdToTarget = targetToKrw
        BigDecimal targetToKrw = usdToKrw.divide(usdToTarget, 2, RoundingMode.HALF_UP);
        result.put(currency, scale(targetToKrw));
    }

    // Mock 환율 반환
    // 1. 마지막 성공 응답이 있으면 해당 값 반환
    // 2. 성공한 적 없으면 BOOTSTRAP_SEED 반환
    private Map<Currency, BigDecimal> generateMockRates() {
        Map<Currency, BigDecimal> base = lastSuccessfulRates.get();
        return new EnumMap<>(base != null ? base : BOOTSTRAP_SEED);
    }

    // **중요** 매입/매매 기준율 소수점 2자리 반올림
    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
    
}