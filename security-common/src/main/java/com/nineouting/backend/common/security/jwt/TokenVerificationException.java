package com.nineouting.backend.common.security.jwt;

import com.nineouting.backend.common.web.error.ErrorCode;
import lombok.Getter;

/**
 * 토큰 검증 실패. 만료면 {@code TOKEN_EXPIRED}, 그 밖에는 {@code UNAUTHORIZED}를 담는다.
 */
@Getter
public class TokenVerificationException extends RuntimeException {

    private final ErrorCode errorCode;

    public TokenVerificationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
