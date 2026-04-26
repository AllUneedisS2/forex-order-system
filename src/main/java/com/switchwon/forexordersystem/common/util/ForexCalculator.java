package com.switchwon.forexordersystem.common.util;

import com.switchwon.forexordersystem.common.enums.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 환율/주문 계산 공통 유틸
 * scale, displayUnit 변환, 현재 시각 생성을 한 곳에서 관리
 */
public final class ForexCalculator {

    private ForexCalculator() {}

    // 소수점 2자리 반올림 (환율 정밀도 표준)
    public static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    // 표시 단위 환산 - JPY는 ×100, 그 외는 그대로
    public static BigDecimal applyDisplayUnit(BigDecimal rate, Currency currency) {
        return rate.multiply(BigDecimal.valueOf(currency.getUnit()));
    }

    // 초 단위로 자른 현재 시각 (나노초 제거)
    public static LocalDateTime truncatedNow() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
