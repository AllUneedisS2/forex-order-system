package com.switchwon.forexordersystem.exchangerate.controller;

import com.switchwon.forexordersystem.common.enums.Currency;
import com.switchwon.forexordersystem.exchangerate.domain.ExchangeRateHistory;
import com.switchwon.forexordersystem.exchangerate.repository.ExchangeRateHistoryRepository;
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
 * 환율 조회 API 통합 테스트
 * 시드 환율은 2026-04-25 11:50 기준 BOOTSTRAP_SEED(USD) 사용
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.scheduler.enabled=false",
                "app.exchange-api.fallback-to-mock=true"
        }
)
@ActiveProfiles("dev")
class ExchangeRateControllerTest {

    @Autowired WebTestClient webTestClient;
    @Autowired ExchangeRateHistoryRepository exchangeRateRepository;

    @BeforeEach
    void seed() {
        exchangeRateRepository.deleteAll();
        exchangeRateRepository.save(ExchangeRateHistory.builder()
                              .currency(Currency.USD)
                              // 결정론적 검증을 위한 시드 데이터 (2026-04-25 11:50 기준)
                              .tradeStanRate(new BigDecimal("1477.65"))
                              .buyRate(new BigDecimal("1551.53"))
                              .sellRate(new BigDecimal("1403.77"))
                              .collectedAt(LocalDateTime.now())
                              .build());
    }

    @Test
    @DisplayName("GET /exchange-rate/latest/USD 정상 환율 반환")
    void latest_usd() {
        webTestClient.get().uri("/exchange-rate/latest/USD")
                           .exchange()
                           .expectStatus().isOk()
                           .expectBody()
                           .jsonPath("$.code").isEqualTo("OK")
                           .jsonPath("$.returnObject.currency").isEqualTo("USD")
                           .jsonPath("$.returnObject.buyRate").isEqualTo(1551.53)
                           .jsonPath("$.returnObject.sellRate").isEqualTo(1403.77);
    }

    @Test
    @DisplayName("GET /exchange-rate/latest 외화 통화 목록 반환 (KRW 제외)")
    void latest_all() {
        webTestClient.get().uri("/exchange-rate/latest")
                           .exchange()
                           .expectStatus().isOk()
                           .expectBody()
                           .jsonPath("$.code").isEqualTo("OK")
                           .jsonPath("$.returnObject.exchangeRateList").isArray()
                           .jsonPath("$.returnObject.exchangeRateList[0].currency").isEqualTo("USD");
    }

    @Test
    @DisplayName("지원하지 않는 통화 코드 400 BAD_REQUEST")
    void unsupported_currency() {
        webTestClient.get().uri("/exchange-rate/latest/XYZ")
                           .exchange()
                           .expectStatus().isBadRequest()
                           .expectBody()
                           .jsonPath("$.code").isEqualTo("BAD_REQUEST");
    }

    @Test
    @DisplayName("환율 데이터가 없는 통화 404 NOT_FOUND")
    void rate_not_collected() {
        webTestClient.get().uri("/exchange-rate/latest/EUR")
                           .exchange()
                           .expectStatus().isNotFound()
                           .expectBody()
                           .jsonPath("$.code").isEqualTo("NOT_FOUND");
    }
    
}