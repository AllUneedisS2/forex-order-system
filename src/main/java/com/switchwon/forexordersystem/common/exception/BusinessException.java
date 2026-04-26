package com.switchwon.forexordersystem.common.exception;

import com.switchwon.forexordersystem.common.response.ResponseCode;
import lombok.Getter;

/**
 * 비즈니스 로직에서 의도적으로 발생시키는 예외의 최상위 타입.
 *
 * - GlobalExceptionHandler 에서 "예측된 비즈니스 예외" vs "예상치 못한 시스템 예외"를
 *   구분해 다른 ResponseCode/HttpStatus 로 응답하기 위해 별도 타입을 둔다.
 * - 호출부에서 ResponseCode 를 함께 던져 응답 코드 매핑이 단순해진다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ResponseCode responseCode;

    public BusinessException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.responseCode = responseCode;
    }

    public BusinessException(ResponseCode responseCode, String customMessage) {
        super(customMessage);
        this.responseCode = responseCode;
    }
}