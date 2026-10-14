package com.nineouting.backend.common.security.jwt;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP로 JWKS를 가져온다 (예: http://user-service/.well-known/jwks.json).
 */
public class HttpJwksSource implements JwksSource {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final int HTTP_OK = 200;

    private final URI jwksUri;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public HttpJwksSource(URI jwksUri) {
        this.jwksUri = jwksUri;
    }

    @Override
    public String fetch() {
        HttpRequest request = HttpRequest.newBuilder(jwksUri).timeout(TIMEOUT).GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HTTP_OK) {
                throw new IllegalStateException("JWKS 조회 실패: " + jwksUri + " → HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new IllegalStateException("JWKS 조회 실패: " + jwksUri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("JWKS 조회 중단: " + jwksUri, e);
        }
    }
}
