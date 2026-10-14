package com.nineouting.backend.common.web.config;

import com.nineouting.backend.common.web.error.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

/**
 * 서비스가 web-common 의존성만 추가하면 전역 예외 처리가 켜지게 한다.
 * 서비스의 기본 패키지 밖이라 컴포넌트 스캔에 잡히지 않기 때문에 자동 설정으로 등록한다.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(GlobalExceptionHandler.class)
public class WebCommonAutoConfiguration {
}
