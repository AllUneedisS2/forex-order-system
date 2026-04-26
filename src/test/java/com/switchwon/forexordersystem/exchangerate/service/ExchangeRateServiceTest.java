package com.switchwon.forexordersystem.exchangerate.service;

import com.switchwon.forexordersystem.common.enums.Currency;
import com.switchwon.forexordersystem.common.exception.BusinessException;
import com.switchwon.forexordersystem.common.response.ResponseCode;
import com.switchwon.forexordersystem.exchangerate.domain.ExchangeRateHistory;
import com.switchwon.forexordersystem.exchangerate.dto.ExchangeRateResponse;
import com.switchwon.forexordersystem.exchangerate.repository.ExchangeRateHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 환율 도메인 단위 테스트
 * 시드 환율은 2026-04-25 11:50 기준 BOOTSTRAP_SEED 사용
 * 외부 API 클라이언트 / Repository 는 Mock 처리하여
 * 스프레드 계산 / JPY 100엔 환산 / 데이터 없음 처리 룰을 검증
 */
@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    ExternalExchangeRateClient externalClient;

    @Mock
    ExchangeRateHistoryRepository repository;

    @InjectMocks
    ExchangeRateService service;

    @BeforeEach
    void setUp() {
        // @Value 는 @InjectMocks 가 주입 못하므로 수동 세팅
        ReflectionTestUtils.setField(service, "buySpread", new BigDecimal("1.05"));
        ReflectionTestUtils.setField(service, "sellSpread", new BigDecimal("0.95"));
    }

    // ------------------------------------------------------------------
    // 수집/저장
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("collectAndSave()")
    class CollectAndSave {

        @Test
        @DisplayName("USD 1477.65 매매기준율에 ×1.05 / ×0.95 스프레드를 적용하여 저장한다")
        void applies_spread_and_saves() {
            // given - USD 매매기준율 1477.65
            Map<Currency, BigDecimal> rates = new EnumMap<>(Currency.class);
            rates.put(Currency.USD, new BigDecimal("1477.65"));
            when(externalClient.fetchKrwRates()).thenReturn(Mono.just(rates));

            // when
            service.collectAndSave();

            // then
            ArgumentCaptor<List<ExchangeRateHistory>> captor = ArgumentCaptor.forClass(List.class);
            verify(repository).saveAll(captor.capture());

            List<ExchangeRateHistory> saved = captor.getValue();
            assertThat(saved).hasSize(1);

            ExchangeRateHistory usd = saved.get(0);
            assertThat(usd.getCurrency()).isEqualTo(Currency.USD);
            assertThat(usd.getTradeStanRate()).isEqualByComparingTo("1477.65");
            assertThat(usd.getBuyRate()).isEqualByComparingTo("1551.53");   // 1477.65 × 1.05
            assertThat(usd.getSellRate()).isEqualByComparingTo("1403.77");  // 1477.65 × 0.95
        }

        @Test
        @DisplayName("4개 통화 결과가 모두 저장된다")
        void saves_all_four_currencies() {
            // given - 2026-04-25 11:50 기준 BOOTSTRAP_SEED
            Map<Currency, BigDecimal> rates = new EnumMap<>(Currency.class);
            rates.put(Currency.USD, new BigDecimal("1477.65"));
            rates.put(Currency.JPY, new BigDecimal("9.26"));
            rates.put(Currency.CNY, new BigDecimal("215.81"));
            rates.put(Currency.EUR, new BigDecimal("1730.37"));
            when(externalClient.fetchKrwRates()).thenReturn(Mono.just(rates));

            // when
            service.collectAndSave();

            // then
            ArgumentCaptor<List<ExchangeRateHistory>> captor = ArgumentCaptor.forClass(List.class);
            verify(repository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(4);
        }

        @Test
        @DisplayName("외부 API 결과가 비어있으면 저장을 건너뛴다")
        void skips_save_when_empty() {
            // given
            when(externalClient.fetchKrwRates()).thenReturn(Mono.just(Map.of()));

            // when
            service.collectAndSave();

            // then
            verify(repository, never()).saveAll(any());
        }
    }

    // ------------------------------------------------------------------
    // 단건 조회 + JPY 100엔 환산
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("getLatest(Currency)")
    class GetLatest {

        @Test
        @DisplayName("USD 는 unit=1 이라 응답 환율이 저장값과 동일")
        void usd_response_equals_stored() {
            // given
            ExchangeRateHistory entity = ExchangeRateHistory.builder()
                                                            .currency(Currency.USD)
                                                            .tradeStanRate(new BigDecimal("1477.65"))
                                                            .buyRate(new BigDecimal("1551.53"))
                                                            .sellRate(new BigDecimal("1403.77"))
                                                            .collectedAt(LocalDateTime.now())
                                                            .build();
            when(repository.findFirstByCurrencyOrderByCollectedAtDesc(Currency.USD))
                    .thenReturn(Optional.of(entity));

            // when
            ExchangeRateResponse response = service.getLatest(Currency.USD);

            // then - USD 는 변환 X
            assertThat(response.getCurrency()).isEqualTo(Currency.USD);
            assertThat(response.getTradeStanRate()).isEqualByComparingTo("1477.65");
            assertThat(response.getBuyRate()).isEqualByComparingTo("1551.53");
            assertThat(response.getSellRate()).isEqualByComparingTo("1403.77");
        }

        @Test
        @DisplayName("JPY 는 응답에서 100엔 단위로 환산된다 (×100)")
        void jpy_response_multiplied_by_100() {
            // given - 1엔 기준 raw 값 저장
            ExchangeRateHistory entity = ExchangeRateHistory.builder()
                                                            .currency(Currency.JPY)
                                                            .tradeStanRate(new BigDecimal("9.26"))
                                                            .buyRate(new BigDecimal("9.72"))
                                                            .sellRate(new BigDecimal("8.80"))
                                                            .collectedAt(LocalDateTime.now())
                                                            .build();
            when(repository.findFirstByCurrencyOrderByCollectedAtDesc(Currency.JPY))
                    .thenReturn(Optional.of(entity));

            // when
            ExchangeRateResponse response = service.getLatest(Currency.JPY);

            // then - 100엔 단위로 환산
            assertThat(response.getCurrency()).isEqualTo(Currency.JPY);
            assertThat(response.getTradeStanRate()).isEqualByComparingTo("926.00");
            assertThat(response.getBuyRate()).isEqualByComparingTo("972.00");
            assertThat(response.getSellRate()).isEqualByComparingTo("880.00");
        }

        @Test
        @DisplayName("환율 데이터가 없으면 NOT_FOUND BusinessException 을 던진다")
        void throws_not_found_when_no_data() {
            // given
            when(repository.findFirstByCurrencyOrderByCollectedAtDesc(Currency.EUR))
                    .thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> service.getLatest(Currency.EUR))
                                            .isInstanceOf(BusinessException.class)
                                            .extracting(e -> ((BusinessException) e).getResponseCode())
                                            .isEqualTo(ResponseCode.NOT_FOUND);
        }
    }

    // ------------------------------------------------------------------
    // 전체 조회
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("getLatestAll()")
    class GetLatestAll {

        @Test
        @DisplayName("KRW 는 제외하고 외화 4종만 조회한다")
        void excludes_krw() {
            // given - 2026-04-25 11:50 기준
            mockEntity(Currency.USD, "1477.65");
            mockEntity(Currency.JPY, "9.26");
            mockEntity(Currency.CNY, "215.81");
            mockEntity(Currency.EUR, "1730.37");

            // when
            List<ExchangeRateResponse> all = service.getLatestAll();

            // then
            assertThat(all).hasSize(4);
            assertThat(all).extracting(ExchangeRateResponse::getCurrency)
                    .containsExactlyInAnyOrder(
                            Currency.USD, Currency.JPY, Currency.CNY, Currency.EUR);
            // KRW 는 조회 자체를 시도하지 않아야 함
            verify(repository, never())
                    .findFirstByCurrencyOrderByCollectedAtDesc(Currency.KRW);
        }

        @Test
        @DisplayName("일부 통화만 데이터가 있으면 있는 것만 반환한다 (NOT_FOUND 대신 누락)")
        void returns_only_present_currencies() {
            // given - USD 만 데이터 존재
            mockEntity(Currency.USD, "1477.65");
            when(repository.findFirstByCurrencyOrderByCollectedAtDesc(Currency.JPY))
                    .thenReturn(Optional.empty());
            when(repository.findFirstByCurrencyOrderByCollectedAtDesc(Currency.CNY))
                    .thenReturn(Optional.empty());
            when(repository.findFirstByCurrencyOrderByCollectedAtDesc(Currency.EUR))
                    .thenReturn(Optional.empty());

            // when
            List<ExchangeRateResponse> all = service.getLatestAll();

            // then
            assertThat(all).hasSize(1);
            assertThat(all.get(0).getCurrency()).isEqualTo(Currency.USD);
        }
    }

    // ------------------------------------------------------------------
    // 헬퍼
    // ------------------------------------------------------------------

    private void mockEntity(Currency currency, String stanRate) {
        ExchangeRateHistory entity = ExchangeRateHistory.builder()
                                                        .currency(currency)
                                                        .tradeStanRate(new BigDecimal(stanRate))
                                                        .buyRate(new BigDecimal(stanRate))   // 단순화 - 스프레드 무시
                                                        .sellRate(new BigDecimal(stanRate))
                                                        .collectedAt(LocalDateTime.now())
                                                        .build();
        when(repository.findFirstByCurrencyOrderByCollectedAtDesc(currency))
                .thenReturn(Optional.of(entity));
    }
}