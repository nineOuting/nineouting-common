package com.nineouting.backend.common.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <pre>
 * nineouting:
 *   security:
 *     jwt:
 *       jwks-uri: ${JWT_JWKS_URI}            # 예: http://user-service:8080/.well-known/jwks.json
 *       issuer: nineouting-user-service       # 생략하면 기본값
 * </pre>
 */
@ConfigurationProperties(prefix = "nineouting.security.jwt")
public record JwtProperties(String jwksUri, String issuer) {

    public static final String DEFAULT_ISSUER = "nineouting-user-service";

    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = DEFAULT_ISSUER;
        }
    }
}
