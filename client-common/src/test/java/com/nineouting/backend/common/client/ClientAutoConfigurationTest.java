package com.nineouting.backend.common.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ClientAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class, ClientAutoConfiguration.class));

    @Test
    void 주소를_설정한_서비스의_조회_부품만_만든다() {
        runner.withPropertyValues("nineouting.clients.user-service-url=http://localhost:8081")
                .run(context -> {
                    assertThat(context).hasSingleBean(UserClient.class);
                    assertThat(context).doesNotHaveBean(WorkspaceClient.class);
                });
        runner.withPropertyValues("nineouting.clients.user-service-url=http://localhost:8081",
                        "nineouting.clients.workspace-service-url=http://localhost:8082")
                .run(context -> assertThat(context).hasSingleBean(WorkspaceClient.class));
        runner.run(context -> assertThat(context).doesNotHaveBean(UserClient.class));
    }
}
