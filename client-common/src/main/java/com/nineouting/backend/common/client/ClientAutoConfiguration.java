package com.nineouting.backend.common.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * nineouting.clients.*-service-url을 설정한 서비스의 조회 부품을 만든다.
 * RestClient.Builder는 서비스의 restclient 스타터가 만든다 (추적 헤더가 자동으로 붙는다).
 * 연결·응답 타임아웃을 두고, 연결 실패·시간 초과면 한 번 더 부른다 (조회만 하므로 다시 불러도 안전하다).
 */
@AutoConfiguration(afterName = "org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration")
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(ClientProperties.class)
public class ClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "nineouting.clients", name = "user-service-url")
    UserClient userClient(RestClient.Builder builder, ClientProperties properties) {
        return new UserClient(client(builder, properties).baseUrl(properties.userServiceUrl()).build());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "nineouting.clients", name = "workspace-service-url")
    WorkspaceClient workspaceClient(RestClient.Builder builder, ClientProperties properties) {
        return new WorkspaceClient(client(builder, properties).baseUrl(properties.workspaceServiceUrl()).build());
    }

    static RestClient.Builder client(RestClient.Builder builder, ClientProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return builder.clone().requestFactory(requestFactory).requestInterceptor(retryOnce());
    }

    private static ClientHttpRequestInterceptor retryOnce() {
        return (request, body, execution) -> {
            try {
                return execution.execute(request, body);
            } catch (IOException e) {
                return execution.execute(request, body);
            }
        };
    }
}
