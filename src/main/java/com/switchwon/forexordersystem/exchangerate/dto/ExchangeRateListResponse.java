package com.switchwon.forexordersystem.exchangerate.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 전체 통화 환율 DTO.
 * JSON 형식으로 (returnObject.exchangeRateList)
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExchangeRateListResponse {
    private List<ExchangeRateResponse> exchangeRateList;
}