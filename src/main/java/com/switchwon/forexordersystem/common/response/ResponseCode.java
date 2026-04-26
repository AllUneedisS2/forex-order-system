package com.switchwon.forexordersystem.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 공통 응답 코드
 */
@Getter
@RequiredArgsConstructor
public enum ResponseCode {

    OK("OK", "SUCCESS"),
    BAD_REQUEST("BAD_REQUEST", "잘못된 요청입니다."),
    NOT_FOUND("NOT_FOUND", "데이터를 찾을 수 없습니다."),
    EXTERNAL_API_ERROR("EXTERNAL_API_ERROR", "외부 환율 API 호출에 실패했습니다."),
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final String message;
}