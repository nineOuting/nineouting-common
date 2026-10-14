package com.nineouting.backend.common.security.jwt;

import com.nineouting.backend.common.security.error.SecurityErrorCode;
import com.nineouting.backend.common.web.error.CommonErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import java.security.Key;

/**
 * user-service가 발급한 access 토큰(RS256)을 공개키로 검증한다.
 */
public class JwtTokenVerifier {

    /** 서버 간 시계 차이를 허용하는 범위 */
    private static final long ALLOWED_CLOCK_SKEW_SECONDS = 30;

    private final JwtParser parser;

    public JwtTokenVerifier(Locator<Key> keyLocator, String issuer) {
        this.parser = Jwts.parser()
                .keyLocator(keyLocator)
                .requireIssuer(issuer)
                .clockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
                .build();
    }

    public AuthenticatedUser verify(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            return new AuthenticatedUser(Long.valueOf(claims.getSubject()));
        } catch (ExpiredJwtException e) {
            throw new TokenVerificationException(SecurityErrorCode.TOKEN_EXPIRED, e);
        } catch (JwtException | IllegalArgumentException e) {
            // IllegalArgumentException: 빈 토큰, 숫자가 아닌 sub
            throw new TokenVerificationException(CommonErrorCode.UNAUTHORIZED, e);
        }
    }
}
