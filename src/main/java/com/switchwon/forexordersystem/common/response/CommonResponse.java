package com.switchwon.forexordersystem.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 컨트롤러 공통 응답 
 * {
 *   "code": "OK",
 *   "message": "SUCCESS",
 *   "returnObject": {..}
 * }
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // returnObject null 이면 직렬화 시 제외
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 생성자는 X, 팩토리 메서드 유도
public class CommonResponse<T> {

    private final String code;
    private final String message;
    private final T returnObject;

    // 성공 (데이터 O)
    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(ResponseCode.OK.getCode(), ResponseCode.OK.getMessage(), data);
    }
    
    // 성공 (데이터 X)
    public static CommonResponse<Void> success() {
        return new CommonResponse<>(ResponseCode.OK.getCode(), ResponseCode.OK.getMessage(), null);
    }

    // 실패
    public static <T> CommonResponse<T> fail(ResponseCode responseCode) {
        return new CommonResponse<>(responseCode.getCode(), responseCode.getMessage(), null);
    }

    // 실패 + 커스텀 메세지
    public static <T> CommonResponse<T> fail(ResponseCode responseCode, String customMessage) {
        return new CommonResponse<>(responseCode.getCode(), customMessage, null);
    }
}