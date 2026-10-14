package com.nineouting.backend.common.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.nineouting.backend.common.security.handler.JsonAccessDeniedHandler;
import com.nineouting.backend.common.security.handler.JsonAuthenticationEntryPoint;
import com.nineouting.backend.common.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class SecurityCommonAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityCommonAutoConfiguration.class));

    @Test
    void jwks_uri가_있으면_검증_부품이_등록된다() {
        runner.withPropertyValues("nineouting.security.jwt.jwks-uri=http://user-service/.well-known/jwks.json")
                .run(context -> {
                    assertThat(context).hasSingleBean(JwtAuthenticationFilter.class);
                    assertThat(context).hasSingleBean(JsonAuthenticationEntryPoint.class);
                    assertThat(context).hasSingleBean(JsonAccessDeniedHandler.class);
                    assertThat(context.getBean(JwtProperties.class).issuer())
                            .isEqualTo(JwtProperties.DEFAULT_ISSUER);
                });
    }

    @Test
    void jwks_uri가_없으면_아무것도_등록하지_않는다() {
        // 토큰을 직접 발급하는 user-service 같은 경우
        runner.run(context -> assertThat(context).doesNotHaveBean(JwtAuthenticationFilter.class));
    }
}
