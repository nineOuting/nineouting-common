package com.nineouting.backend.common.web.error;

import org.springframework.http.HttpStatus;

/**
 * 에러 종류. 서비스 전용 에러는 각 서비스가 이 인터페이스를 구현한 enum으로 추가한다.
 */
public interface ErrorCode {

    HttpStatus getStatus();

    /** 프론트가 에러 종류를 구분하는 값 (UPPER_SNAKE_CASE) */
    String getCode();

    /** 화면에 그대로 보여 줄 수 있는 한국어 메시지 */
    String getMessage();
}
