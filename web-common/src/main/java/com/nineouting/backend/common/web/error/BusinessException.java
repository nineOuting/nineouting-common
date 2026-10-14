package com.nineouting.backend.common.web.error;

import lombok.Getter;

/**
 * 서비스 로직에서 던지는 예외. {@link GlobalExceptionHandler}가 에러 코드에 맞는 응답으로 바꾼다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 에러 코드의 기본 메시지 대신 상황에 맞는 메시지를 보여 줄 때 쓴다. */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
