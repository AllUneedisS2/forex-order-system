package com.switchwon.forexordersystem.common.exception;

import com.switchwon.forexordersystem.common.response.CommonResponse;
import com.switchwon.forexordersystem.common.response.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리 
 *
 * [우선순위]
 *  1. BusinessException               : 비즈니스 예외 → ResponseCode 
 *  2. MethodArgumentNotValidException : @Valid 검증 실패 → 400
 *  3. IllegalArgumentException        : 런타임 검증 실패 → 일단 400..
 *  4. Exception                       : 그 외 → 500
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<Void>> handleBusiness(BusinessException e) {
        log.warn("[BusinessException] code={}, message={}", e.getResponseCode(), e.getMessage());
        HttpStatus status = mapStatus(e.getResponseCode());
        return ResponseEntity.status(status)
                             .body(
                                CommonResponse.fail(e.getResponseCode(), e.getMessage())
                             );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(
                    fe -> String.format(
                        "[%s] %s", fe.getField(), fe.getDefaultMessage()
                    )
                )
                .orElse(ResponseCode.BAD_REQUEST.getMessage());
        log.warn("[Validation] {}", message);
        return ResponseEntity.badRequest()
                             .body(
                                CommonResponse.fail(ResponseCode.BAD_REQUEST, message)
                             );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonResponse<Void>> handleIllegal(IllegalArgumentException e) {
        log.warn("[IllegalArgument] {}", e.getMessage());
        return ResponseEntity.badRequest()
                             .body(
                                CommonResponse.fail(ResponseCode.BAD_REQUEST, e.getMessage())
                             );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleUnexpected(Exception e) {
        // 그 외 예외는 stack trace까지 추가
        log.error("[Unexpected]", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(
                                CommonResponse.fail(ResponseCode.INTERNAL_ERROR)
                             );
    }

    private HttpStatus mapStatus(ResponseCode code) {
        return switch (code) {
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case EXTERNAL_API_ERROR -> HttpStatus.BAD_GATEWAY;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.OK;
        };
    }
}