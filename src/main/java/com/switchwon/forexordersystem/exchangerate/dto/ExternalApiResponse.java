package com.switchwon.forexordersystem.exchangerate.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 환율 API (open.er-api.com) 응답 맵핑 DTO
 * ex) GET https://open.er-api.com/v6/latest/USD
 * 응답:
 *   {
 *     "result": "success",
 *     "base_code": "USD",
 *     "rates": { "KRW": 1477.45, "JPY": 153.21, "CNY": 7.13, "EUR": 0.92, ... },
 *     "time_last_update_unix": 1714000000
 *   }
 * 참고: 다른 통화 계산시 기준통화(base_code)의 비율로 환산
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 환율 외 데이터 무시
public class ExternalApiResponse {

    private String result;
    private String baseCode;
    private Map<String, BigDecimal> rates;
}