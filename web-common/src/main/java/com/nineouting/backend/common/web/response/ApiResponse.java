package com.nineouting.backend.common.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nineouting.backend.common.web.error.ErrorCode;

/**
 * 모든 API의 응답 형식. 성공 응답에는 {@code code}가 들어가지 않는다.
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) String code
) {

    private static final String NO_MESSAGE = "";

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, NO_MESSAGE, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, NO_MESSAGE, null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, errorCode.getMessage(), errorCode.getCode());
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, message, errorCode.getCode());
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, T data) {
        return new ApiResponse<>(false, data, errorCode.getMessage(), errorCode.getCode());
    }
}
