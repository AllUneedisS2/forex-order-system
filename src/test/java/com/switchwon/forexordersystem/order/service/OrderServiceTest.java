package com.switchwon.forexordersystem.order.service;

import com.switchwon.forexordersystem.common.enums.Currency;
import com.switchwon.forexordersystem.common.exception.BusinessException;
import com.switchwon.forexordersystem.common.response.ResponseCode;
import com.switchwon.forexordersystem.exchangerate.domain.ExchangeRateHistory;
import com.switchwon.forexordersystem.exchangerate.service.ExchangeRateService;
import com.switchwon.forexordersystem.order.domain.Order;
import com.switchwon.forexordersystem.order.domain.OrderType;
import com.switchwon.forexordersystem.order.dto.OrderRequest;
import com.switchwon.forexordersystem.order.dto.OrderResponse;
import com.switchwon.forexordersystem.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 주문 도메인 단위 테스트
 * 환율 조회 / Repository 는 Mock 처리하여 OrderService 비즈니스 룰만 검증
 * 시드 환율은 2026-04-25 11:50 기준, BOOTSTRAP_SEED + 5% 스프레드 적용값 사용
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    ExchangeRateService exchangeRateService;

    @Mock
    OrderRepository orderRepository;

    @InjectMocks
    OrderService orderService;

    // ------------------------------------------------------------------
    // 매수 (KRW → 외화)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("매수 (BUY)")
    class Buy {

        @Test
        @DisplayName("KRW → USD 200 매수 → 310306 KRW 출금")
        void buy_usd_200() {
            // given
            mockUsdRate();
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            OrderResponse response = orderService.placeOrder(
                    new OrderRequest(new BigDecimal("200"), "KRW", "USD"));

            // then - 200 × 1551.53 = 310306.00 → floor → 310306
            assertThat(response.getFromCurrency()).isEqualTo(Currency.KRW);
            assertThat(response.getFromAmount()).isEqualByComparingTo("310306");
            assertThat(response.getToCurrency()).isEqualTo(Currency.USD);
            assertThat(response.getToAmount()).isEqualByComparingTo("200");
            assertThat(response.getTradeRate()).isEqualByComparingTo("1551.53");

            // 저장된 엔티티의 OrderType 검증
            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(captor.capture());
            assertThat(captor.getValue().getOrderType()).isEqualTo(OrderType.BUY);
        }

        @Test
        @DisplayName("KRW → JPY 1000 매수 → tradeRate 가 100엔 단위로 저장")
        void buy_jpy_applies_100_unit_display_rate() {
            // given - JPY raw buyRate=9.72 (1엔 기준), display=972.00 (100엔)
            ExchangeRateHistory rate = jpyRate("9.72", "8.80");
            when(exchangeRateService.findLatestEntity(Currency.JPY)).thenReturn(rate);
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            OrderResponse response = orderService.placeOrder(
                    new OrderRequest(new BigDecimal("1000"), "KRW", "JPY"));

            // then - 1000 × 9.72 = 9720
            assertThat(response.getFromAmount()).isEqualByComparingTo("9720");
            assertThat(response.getToAmount()).isEqualByComparingTo("1000");
            // tradeRate 는 100엔 단위 표시값
            assertThat(response.getTradeRate()).isEqualByComparingTo("972.00");
        }

        @Test
        @DisplayName("KRW 환산 금액의 소수점은 절사된다")
        void buy_floors_krw_amount() {
            // given - 의도적으로 소수점 3자리 rate 를 사용해 절사 동작 검증
            ExchangeRateHistory rate = ExchangeRateHistory.builder()
                    .currency(Currency.USD)
                    .tradeStanRate(new BigDecimal("1000.00"))
                    .buyRate(new BigDecimal("1234.567"))
                    .sellRate(new BigDecimal("950.00"))
                    .collectedAt(LocalDateTime.now())
                    .build();
            when(exchangeRateService.findLatestEntity(Currency.USD)).thenReturn(rate);
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            // when - 1 USD 매수 시 1234.567 KRW → floor → 1234 KRW
            OrderResponse response = orderService.placeOrder(
                    new OrderRequest(BigDecimal.ONE, "KRW", "USD"));

            // then
            assertThat(response.getFromAmount()).isEqualByComparingTo("1234");
        }
    }

    // ------------------------------------------------------------------
    // 매도 (외화 → KRW)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("매도 (SELL)")
    class Sell {

        @Test
        @DisplayName("USD 133 → KRW 매도 → 186701 KRW 입금")
        void sell_usd_133() {
            // given
            mockUsdRate();
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            OrderResponse response = orderService.placeOrder(
                    new OrderRequest(new BigDecimal("133"), "USD", "KRW"));

            // then - 133 × 1403.77 = 186701.41 → floor → 186701
            assertThat(response.getFromCurrency()).isEqualTo(Currency.USD);
            assertThat(response.getFromAmount()).isEqualByComparingTo("133");
            assertThat(response.getToCurrency()).isEqualTo(Currency.KRW);
            assertThat(response.getToAmount()).isEqualByComparingTo("186701");
            assertThat(response.getTradeRate()).isEqualByComparingTo("1403.77");

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(captor.capture());
            assertThat(captor.getValue().getOrderType()).isEqualTo(OrderType.SELL);
        }
    }

    // ------------------------------------------------------------------
    // 검증 실패 케이스
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("검증 실패")
    class Validation {

        @Test
        @DisplayName("이중 통화 (USD → JPY) BAD_REQUEST")
        void reject_dual_foreign_currency() {
            assertThatThrownBy(() -> orderService.placeOrder(
                    new OrderRequest(BigDecimal.TEN, "USD", "JPY")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getResponseCode())
                    .isEqualTo(ResponseCode.BAD_REQUEST);

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("동일 통화 (USD → USD) BAD_REQUEST")
        void reject_same_currency() {
            assertThatThrownBy(() -> orderService.placeOrder(
                    new OrderRequest(BigDecimal.TEN, "USD", "USD")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getResponseCode())
                    .isEqualTo(ResponseCode.BAD_REQUEST);

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("환율이 아직 수집되지 않으면 NOT_FOUND")
        void rate_not_collected_yet() {
            // given - ExchangeRateService 가 NOT_FOUND 던지는 상황
            when(exchangeRateService.findLatestEntity(Currency.EUR))
                    .thenThrow(new BusinessException(
                            ResponseCode.NOT_FOUND, "해당 통화의 환율이 아직 수집되지 않았습니다: EUR"));

            // when / then
            assertThatThrownBy(() -> orderService.placeOrder(
                    new OrderRequest(new BigDecimal("100"), "KRW", "EUR")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getResponseCode())
                    .isEqualTo(ResponseCode.NOT_FOUND);

            verify(orderRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------
    // 헬퍼
    // ------------------------------------------------------------------

    private void mockUsdRate() {
        ExchangeRateHistory rate = ExchangeRateHistory.builder()
                                                      .currency(Currency.USD)
                                                      .tradeStanRate(new BigDecimal("1477.65"))
                                                      .buyRate(new BigDecimal("1551.53"))
                                                      .sellRate(new BigDecimal("1403.77"))
                                                      .collectedAt(LocalDateTime.now())
                                                      .build();
        when(exchangeRateService.findLatestEntity(Currency.USD)).thenReturn(rate);
    }

    private ExchangeRateHistory jpyRate(String buy, String sell) {
        return ExchangeRateHistory.builder()
                                  .currency(Currency.JPY)
                                  .tradeStanRate(new BigDecimal("9.26"))
                                  .buyRate(new BigDecimal(buy))
                                  .sellRate(new BigDecimal(sell))
                                  .collectedAt(LocalDateTime.now())
                                  .build();
    }
}