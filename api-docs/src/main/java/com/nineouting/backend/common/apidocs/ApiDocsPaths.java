package com.nineouting.backend.common.apidocs;

/**
 * 로그인 없이 열어 둘 API 문서 주소. 서비스의 SecurityConfig에서 permitAll에 더한다.
 * 운영에서 문서를 닫으려면 SPRINGDOC_API_DOCS_ENABLED=false, SPRINGDOC_SWAGGER_UI_ENABLED=false.
 */
public final class ApiDocsPaths {

    public static final String[] PUBLIC = {"/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"};

    private ApiDocsPaths() {
    }
}
