package com.switchwon.forexordersystem.order.controller;

import com.switchwon.forexordersystem.common.enums.Currency;
import com.switchwon.forexordersystem.exchangerate.domain.ExchangeRateHistory;
import com.switchwon.forexordersystem.exchangerate.repository.ExchangeRateHistoryRepository;
import com.switchwon.forexordersystem.order.dto.OrderRequest;
import com.switchwon.forexordersystem.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 API 통합 테스트
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.scheduler.enabled=false", // 스케줄러가 비결정적으로 데이터 덮어쓰는 것 방지
                "app.exchange-api.fallback-to-mock=true"
        }
)
@ActiveProfiles("dev")
class OrderControllerTest {

    @Autowired WebTestClient webTestClient;
    @Autowired OrderRepository orderRepository;
    @Autowired ExchangeRateHistoryRepository exchangeRateRepository;

    @BeforeEach
    void seed() {
        // 결정론적 검증을 위해 환율 데이터를 직접 주입
        orderRepository.deleteAll();
        exchangeRateRepository.deleteAll();
        // 결정론적 검증을 위한 시드 데이터 (2026-04-25 11:50 기준)
        exchangeRateRepository.save(ExchangeRateHistory.builder()
                              .currency(Currency.USD)
                              .tradeStanRate(new BigDecimal("1477.65"))
                              .buyRate(new BigDecimal("1551.53"))
                              .sellRate(new BigDecimal("1403.77"))
                              .collectedAt(LocalDateTime.now())
                              .build());
    }

    @Test
    @DisplayName("Case A: KRW → USD 200 매수 → 310306 KRW 출금")
    void place_buy_order() {
        OrderRequest req = new OrderRequest(new BigDecimal("200"), "KRW", "USD");

        webTestClient.post().uri("/order")
                            .bodyValue(req)
                            .exchange()
                            .expectStatus().isOk()
                            .expectBody()
                            .jsonPath("$.code").isEqualTo("OK")
                            .jsonPath("$.returnObject.fromAmount").isEqualTo(310306)
                            .jsonPath("$.returnObject.fromCurrency").isEqualTo("KRW")
                            .jsonPath("$.returnObject.toAmount").isEqualTo(200)
                            .jsonPath("$.returnObject.toCurrency").isEqualTo("USD")
                            .jsonPath("$.returnObject.tradeRate").isEqualTo(1551.53);
    }

    @Test
    @DisplayName("Case B: USD 133 매도 → 186701 KRW 입금")
    void place_sell_order() {
        OrderRequest req = new OrderRequest(new BigDecimal("133"), "USD", "KRW");

        webTestClient.post().uri("/order")
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.returnObject.fromAmount").isEqualTo(133)
                .jsonPath("$.returnObject.toAmount").isEqualTo(186701)
                .jsonPath("$.returnObject.tradeRate").isEqualTo(1403.77);
    }

    @Test
    @DisplayName("주문 후 GET /order/list 로 내역 조회")
    void list_orders_after_place() {
        webTestClient.post().uri("/order")
                .bodyValue(new OrderRequest(new BigDecimal("200"), "KRW", "USD"))
                .exchange()
                .expectStatus().isOk();

        webTestClient.get().uri("/order/list")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.returnObject.orderList[0].toCurrency").isEqualTo("USD")
                .jsonPath("$.returnObject.orderList[0].id").exists();
    }

    @Test
    @DisplayName("이중 통화 주문 400 BAD_REQUEST")
    void reject_dual_currency() {
        webTestClient.post().uri("/order")
                .bodyValue(new OrderRequest(new BigDecimal("10"), "USD", "JPY"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BAD_REQUEST");
    }
}