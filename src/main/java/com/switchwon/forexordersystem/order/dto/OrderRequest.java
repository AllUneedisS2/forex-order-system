package com.switchwon.forexordersystem.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 외환 주문 요청
 *
 * 매수: {@code fromCurrency=KRW, toCurrency=USD, forexAmount=200} (USD 200 매수)
 * 매도: {@code fromCurrency=USD, toCurrency=KRW, forexAmount=133} (USD 133 매도)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class OrderRequest {

    // 외화 금액 (매수: 사고싶은 외화량 / 매도: 팔고싶은 외화량)
    @NotNull(message = "외화 금액은 필수입니다.")
    @Positive(message = "외화 금액은 0보다 커야 합니다.")
    private BigDecimal forexAmount;

    // 출금 통화 코드 (KRW / USD / JPY / CNY / EUR)
    @NotBlank(message = "출금 통화는 필수입니다.")
    @Pattern(regexp = "^[A-Z]{3}$", message = "통화 코드는 3자리 대문자여야 합니다.")
    private String fromCurrency;

    // 입금 통화 코드
    @NotBlank(message = "입금 통화는 필수입니다.")
    @Pattern(regexp = "^[A-Z]{3}$", message = "통화 코드는 3자리 대문자여야 합니다.")
    private String toCurrency;
    
}