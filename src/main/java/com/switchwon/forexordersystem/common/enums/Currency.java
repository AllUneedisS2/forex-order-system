package com.switchwon.forexordersystem.common.enums;

import com.switchwon.forexordersystem.common.exception.BusinessException;
import com.switchwon.forexordersystem.common.response.ResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 취급 통화 enum
 * 기준 통화 : KRW
 */
@Getter
@RequiredArgsConstructor
public enum Currency {

    KRW(1),
    USD(1),
    JPY(100), // 일본 엔은 100엔 단위로 환산
    CNY(1),
    EUR(1);

    private final int unit; // 환산 단위

    public boolean isForeign() {
        return this != KRW;
    }

    public static Currency from(String code) {
        if (code == null || code.isBlank()) {
            // 취급 통화 이외의 잘못된 통화 코드 입력 시 비즈니스, 에러
            throw new BusinessException(ResponseCode.BAD_REQUEST, "통화 코드는 필수입니다.");
        }
        return Arrays.stream(values())
                     .filter(c -> c.name().equalsIgnoreCase(code))
                     .findFirst()
                     .orElseThrow(() -> new BusinessException(
                            ResponseCode.BAD_REQUEST, "지원하지 않는 통화 코드입니다: " + code
                     ));
    }
}