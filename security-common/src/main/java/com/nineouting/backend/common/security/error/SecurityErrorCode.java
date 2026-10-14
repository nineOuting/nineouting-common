package com.nineouting.backend.common.security.error;

import com.nineouting.backend.common.web.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SecurityErrorCode implements ErrorCode {

    /** 프론트는 이 코드를 받으면 refresh 토큰으로 access 토큰을 다시 발급받는다. */
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인해 주세요");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
