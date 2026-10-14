package com.nineouting.backend.common.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.PublicJwk;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

/**
 * user-service 역할을 흉내 낸다: RSA 키로 토큰을 서명하고 JWKS JSON을 만든다.
 */
final class TestKeys {

    static final String ISSUER = "nineouting-user-service";

    final String kid;
    private final KeyPair keyPair = Jwts.SIG.RS256.keyPair().build();

    TestKeys(String kid) {
        this.kid = kid;
    }

    String jwks() {
        PublicJwk<?> jwk = Jwks.builder().key((RSAPublicKey) keyPair.getPublic()).id(kid).build();
        return "{\"keys\":[" + Jwks.json(jwk) + "]}";
    }

    String token(String subject, Instant expiresAt) {
        return token(subject, ISSUER, expiresAt);
    }

    String token(String subject, String issuer, Instant expiresAt) {
        return Jwts.builder()
                .header().keyId(kid).and()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(Date.from(expiresAt.minusSeconds(1800)))
                .expiration(Date.from(expiresAt))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }
}
