package com.switchwon.forexordersystem.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.switchwon.forexordersystem.common.enums.Currency;
import com.switchwon.forexordersystem.order.domain.Order;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 외환 주문 응답
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL) // id null 무시
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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    // 엔티티 → 응답 DTO 변환 (id 값 O)
    public static OrderResponse fromWithId(Order order) {
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

    // 엔티티 → 응답 DTO 변환 (id 값 X)
    public static OrderResponse fromWithoutId(Order order) {
        return OrderResponse.builder()
                            .fromAmount(order.getFromAmount())
                            .fromCurrency(order.getFromCurrency())
                            .toAmount(order.getToAmount())
                            .toCurrency(order.getToCurrency())
                            .tradeRate(order.getTradeRate())
                            .createdAt(order.getCreatedAt())
                            .build();
    }

}