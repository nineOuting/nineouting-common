package com.nineouting.backend.common.client;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 서비스 주소. 설정한 서비스의 조회 부품만 만든다.
 *
 * @param userServiceUrl      예: http://localhost:8081 → {@link UserClient}
 * @param workspaceServiceUrl 예: http://localhost:8082 → {@link WorkspaceClient}
 * @param connectTimeout      연결 타임아웃. 기본 2초
 * @param readTimeout         응답 타임아웃. 기본 5초
 */
@ConfigurationProperties(prefix = "nineouting.clients")
public record ClientProperties(String userServiceUrl, String workspaceServiceUrl,
                               Duration connectTimeout, Duration readTimeout) {

    public ClientProperties {
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(5) : readTimeout;
    }
}
