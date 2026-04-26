package com.switchwon.forexordersystem.order.domain;

import com.switchwon.forexordersystem.common.enums.Currency;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 외환 주문 내역 엔티티 (매수 or 매도, 주문 건 당)
 * 거래 시점의 적용 환율(tradeRate)을 함께 보관
 */
@Getter
@Builder
@Entity
@Table(
        name = "forex_order",
        indexes = {
                @Index(name = "idx_order_created_at", columnList = "createdAt DESC")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문 유형 (BUY / SELL)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private OrderType orderType;

    // 출금 통화
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency fromCurrency;

    // 출금 금액 (KRW 인 경우 절사된 정수, 외화는 소수점)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fromAmount;

    // 입금 통화
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency toCurrency;

    // 입금 금액
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal toAmount;

    // 거래 시점 적용 환율 (매수면 buyRate, 매도면 sellRate)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tradeRate;

    // 주문 시각
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
}