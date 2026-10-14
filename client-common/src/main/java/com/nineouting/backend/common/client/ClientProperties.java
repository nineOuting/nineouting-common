package com.nineouting.backend.common.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 서비스 주소. 설정한 서비스의 조회 부품만 만든다.
 *
 * @param userServiceUrl      예: http://localhost:8081 → {@link UserClient}
 * @param workspaceServiceUrl 예: http://localhost:8082 → {@link WorkspaceClient}
 */
@ConfigurationProperties(prefix = "nineouting.clients")
public record ClientProperties(String userServiceUrl, String workspaceServiceUrl) {
}
