package com.switchwon.forexordersystem.exchangerate.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.switchwon.forexordersystem.common.enums.Currency;

import lombok.Builder;
import lombok.Getter;

/**
 * 개별 통화 환율 DTO
 * JSON 형식으로
 * 엔티티의 collectedAt → 응답에서는 dateTime 으로 노출 (명세 준수)
 */
@Getter
@Builder
public class ExchangeRateResponse {

    private final Currency currency;
    private final BigDecimal buyRate;
    private final BigDecimal tradeStanRate;
    private final BigDecimal sellRate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime dateTime;

}