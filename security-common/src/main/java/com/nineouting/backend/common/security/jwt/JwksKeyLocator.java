package com.nineouting.backend.common.security.jwt;

import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.LocatorAdapter;
import io.jsonwebtoken.security.InvalidKeyException;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.PublicJwk;
import java.security.Key;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 토큰 헤더의 kid로 검증용 공개키를 찾는다. 공개키는 캐시하고, 처음 보는 kid가 오면
 * (user-service가 키를 교체한 경우) JWKS를 다시 가져온다.
 */
public class JwksKeyLocator extends LocatorAdapter<Key> {

    /** 잘못된 kid를 계속 보내 JWKS 조회를 폭주시키는 것을 막는다. */
    static final Duration MIN_REFRESH_INTERVAL = Duration.ofSeconds(30);

    private final JwksSource jwksSource;
    private final Clock clock;

    private volatile Map<String, PublicKey> keys = Map.of();
    private Instant lastRefreshedAt = Instant.MIN;

    public JwksKeyLocator(JwksSource jwksSource) {
        this(jwksSource, Clock.systemUTC());
    }

    JwksKeyLocator(JwksSource jwksSource, Clock clock) {
        this.jwksSource = jwksSource;
        this.clock = clock;
    }

    @Override
    protected Key locate(JwsHeader header) {
        String kid = header.getKeyId();
        if (kid == null) {
            throw new InvalidKeyException("토큰 헤더에 kid가 없습니다");
        }
        PublicKey key = keys.get(kid);
        if (key == null) {
            refreshIfAllowed();
            key = keys.get(kid);
        }
        if (key == null) {
            throw new InvalidKeyException("알 수 없는 kid: " + kid);
        }
        return key;
    }

    private synchronized void refreshIfAllowed() {
        Instant now = clock.instant();
        if (Duration.between(lastRefreshedAt, now).compareTo(MIN_REFRESH_INTERVAL) < 0) {
            return;
        }
        lastRefreshedAt = now;
        keys = parse(jwksSource.fetch());
    }

    private static Map<String, PublicKey> parse(String json) {
        JwkSet jwkSet = Jwks.setParser().build().parse(json);
        Map<String, PublicKey> parsed = new HashMap<>();
        for (Jwk<?> jwk : jwkSet) {
            if (jwk instanceof PublicJwk<?> publicJwk && jwk.getId() != null) {
                parsed.put(jwk.getId(), publicJwk.toKey());
            }
        }
        return Map.copyOf(parsed);
    }
}
