package com.switchwon.forexordersystem.exchangerate.controller;

import com.switchwon.forexordersystem.common.enums.Currency;
import com.switchwon.forexordersystem.common.response.CommonResponse;
import com.switchwon.forexordersystem.exchangerate.dto.ExchangeRateListResponse;
import com.switchwon.forexordersystem.exchangerate.dto.ExchangeRateResponse;
import com.switchwon.forexordersystem.exchangerate.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 환율 조회 REST API
 */
@RestController
@RequestMapping("/exchange-rate")
@RequiredArgsConstructor
public class ExchangeRateController {

    // 환율 도메인 서비스
    private final ExchangeRateService exchangeRateService;

    /**
     * 4개 통화 전체 최신 환율 조회.
     * GET /exchange-rate/latest
     */
    @GetMapping("/latest")
    public CommonResponse<ExchangeRateListResponse> getLatestAll() {
        ExchangeRateListResponse body = ExchangeRateListResponse.builder()
                .exchangeRateList(exchangeRateService.getLatestAll())
                .build();
        return CommonResponse.success(body);
    }

    /**
     * 특정 통화 최신 환율 조회.
     * GET /exchange-rate/latest/{currency}
     *
     * @param currency 통화 코드 (USD / JPY / CNY / EUR)
     */
    @GetMapping("/latest/{currency}")
    public CommonResponse<ExchangeRateResponse> getLatestByCurrency(@PathVariable("currency") String currency) {
        // 잘못된 통화 코드는 BusinessException(BAD_REQUEST)
        Currency target = Currency.from(currency);
        return CommonResponse.success(exchangeRateService.getLatest(target));
    }

}