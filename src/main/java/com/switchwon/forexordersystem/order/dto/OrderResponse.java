package com.switchwon.forexordersystem.order.dto;

import com.switchwon.forexordersystem.common.enums.Currency;
import com.switchwon.forexordersystem.order.domain.Order;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 외환 주문 응답
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderResponse {

    // 주문 ID
    private Long id;

    // 출금 금액
    private BigDecimal fromAmount;

    // 출금 통화
    private Currency fromCurrency;

    // 입금 금액
    private BigDecimal toAmount;

    // 입금 통화
    private Currency toCurrency;

    // 거래 시점 적용 환율
    private BigDecimal tradeRate;

    // 주문 시각
    private LocalDateTime createdAt;

    // 엔티티 → 응답 DTO 변환
    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                            .id(order.getId())
                            .fromAmount(order.getFromAmount())
                            .fromCurrency(order.getFromCurrency())
                            .toAmount(order.getToAmount())
                            .toCurrency(order.getToCurrency())
                            .tradeRate(order.getTradeRate())
                            .createdAt(order.getCreatedAt())
                            .build();
    }
}