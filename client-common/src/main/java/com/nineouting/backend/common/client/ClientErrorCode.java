package com.nineouting.backend.common.client;

import com.nineouting.backend.common.web.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 다른 서비스에 연결할 수 없을 때의 에러. 호출한 서비스의 응답으로 그대로 나간다.
 */
@Getter
@RequiredArgsConstructor
public enum ClientErrorCode implements ErrorCode {

    USER_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "사용자 정보를 불러올 수 없습니다. 잠시 후 다시 시도해 주세요"),
    WORKSPACE_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "폴더 정보를 불러올 수 없습니다. 잠시 후 다시 시도해 주세요");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
