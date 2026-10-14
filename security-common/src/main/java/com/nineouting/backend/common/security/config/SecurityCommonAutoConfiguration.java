package com.nineouting.backend.common.security.config;

import com.nineouting.backend.common.security.handler.JsonAccessDeniedHandler;
import com.nineouting.backend.common.security.handler.JsonAuthenticationEntryPoint;
import com.nineouting.backend.common.security.jwt.HttpJwksSource;
import com.nineouting.backend.common.security.jwt.JwksKeyLocator;
import com.nineouting.backend.common.security.jwt.JwtAuthenticationFilter;
import com.nineouting.backend.common.security.jwt.JwtTokenVerifier;
import com.nineouting.backend.common.security.web.CurrentUserArgumentResolver;
import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code nineouting.security.jwt.jwks-uri}가 설정된 서비스에 JWT 검증 부품을 등록한다.
 * 어떤 주소를 막을지는 서비스마다 달라서 SecurityFilterChain은 서비스가 직접 만든다 (README 참고).
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.security.web.SecurityFilterChain")
@ConditionalOnProperty(prefix = "nineouting.security.jwt", name = "jwks-uri")
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwksKeyLocator jwksKeyLocator(JwtProperties properties) {
        return new JwksKeyLocator(new HttpJwksSource(URI.create(properties.jwksUri())));
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenVerifier jwtTokenVerifier(JwksKeyLocator jwksKeyLocator, JwtProperties properties) {
        return new JwtTokenVerifier(jwksKeyLocator, properties.issuer());
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenVerifier jwtTokenVerifier) {
        return new JwtAuthenticationFilter(jwtTokenVerifier);
    }

    /**
     * Filter 빈은 Spring Boot가 서블릿 필터로도 자동 등록해 두 번 실행된다.
     * SecurityFilterChain 안에서만 실행되도록 서블릿 등록은 끈다.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint(ObjectProvider<JsonMapper> jsonMapper) {
        return new JsonAuthenticationEntryPoint(jsonMapper.getIfAvailable(() -> JsonMapper.builder().build()));
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonAccessDeniedHandler jsonAccessDeniedHandler(ObjectProvider<JsonMapper> jsonMapper) {
        return new JsonAccessDeniedHandler(jsonMapper.getIfAvailable(() -> JsonMapper.builder().build()));
    }

    @Bean
    public WebMvcConfigurer currentUserArgumentResolverConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new CurrentUserArgumentResolver());
            }
        };
    }
}
