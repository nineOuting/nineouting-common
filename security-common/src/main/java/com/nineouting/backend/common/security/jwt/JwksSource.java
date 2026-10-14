package com.nineouting.backend.common.security.jwt;

/**
 * user-service가 공개한 JWKS(공개키 목록) JSON을 가져온다.
 */
@FunctionalInterface
public interface JwksSource {

    String fetch();
}
