package com.switchwon.forexordersystem.exchangerate.domain;

import com.switchwon.forexordersystem.common.enums.Currency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 환율 수신 이력 엔티티
 */
@Getter
@Builder
@Entity
@Table(
    name = "exchange_rate_history",
    // 통화+수집일시 조합 인덱스로 최신 조회 성능 보장
    indexes = {
        @Index(
            name = "idx_currency_collected_at", 
            columnList = "currency, collectedAt DESC"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExchangeRateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    // 매매기준율 (소수점 2자리)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tradeStanRate;

    // 매입율 (매매기준율 × 1.05)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal buyRate;

    // 매도율 (매매기준율 × 0.95)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal sellRate;

    // 수집 시간
    @Column(nullable = false)
    private LocalDateTime collectedAt;

}