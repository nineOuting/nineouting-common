package com.nineouting.backend.common.apidocs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 서비스 공통 API 문서 설정.
 * <ul>
 *   <li>제목: {@code spring.application.name} + " API"</li>
 *   <li>JWT 입력(Authorize 버튼): 모든 API에 Bearer 토큰을 붙인다. 로그인처럼 토큰이 필요 없는 API도 붙여 보내지만 무시된다</li>
 *   <li>서버 주소: "/" (Gateway에서 모아 보면 Gateway로, 서비스에서 직접 보면 그 서비스로 요청한다)</li>
 *   <li>서비스 간 조회(/internal/**)와 공개키(/.well-known/**)는 문서에서 뺀다 (외부에 열지 않는 주소)</li>
 *   <li>{@code @CurrentUser} 파라미터는 토큰에서 채워지므로 문서에서 숨긴다</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
public class ApiDocsAutoConfiguration {

    static final String BEARER = "bearer";
    private static final List<String> HIDDEN_PREFIXES = List.of("/internal/", "/.well-known/");

    @Bean
    @ConditionalOnMissingBean
    OpenAPI nineoutingOpenApi(@Value("${spring.application.name:service}") String applicationName) {
        return new OpenAPI()
                .info(new Info().title(applicationName + " API")
                        .description("응답 형식: { success, data, message, code }. 설계: nineouting-docs/docs/services/" + applicationName + ".md"))
                .servers(List.of(new Server().url("/")))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    @Bean
    OpenApiCustomizer hideInternalPaths() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                openApi.getPaths().keySet().removeIf(path -> HIDDEN_PREFIXES.stream().anyMatch(path::startsWith));
            }
        };
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.nineouting.backend.common.security.web.CurrentUser")
    static class CurrentUserParameterConfiguration {

        static {
            SpringDocUtils.getConfig().addAnnotationsToIgnore(
                    com.nineouting.backend.common.security.web.CurrentUser.class);
        }
    }
}
