package com.nineouting.backend.common.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * nineouting.clients.*-service-url을 설정한 서비스의 조회 부품을 만든다.
 * RestClient.Builder는 서비스의 restclient 스타터가 만든다 (추적 헤더가 자동으로 붙는다).
 */
@AutoConfiguration(afterName = "org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration")
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(ClientProperties.class)
public class ClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "nineouting.clients", name = "user-service-url")
    UserClient userClient(RestClient.Builder builder, ClientProperties properties) {
        return new UserClient(builder.clone().baseUrl(properties.userServiceUrl()).build());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "nineouting.clients", name = "workspace-service-url")
    WorkspaceClient workspaceClient(RestClient.Builder builder, ClientProperties properties) {
        return new WorkspaceClient(builder.clone().baseUrl(properties.workspaceServiceUrl()).build());
    }
}
