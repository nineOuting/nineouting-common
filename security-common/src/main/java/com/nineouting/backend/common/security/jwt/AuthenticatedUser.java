package com.nineouting.backend.common.security.jwt;

/**
 * 토큰에서 꺼낸 로그인 사용자. 역할(관리자·회의록 작성자 등)은 워크스페이스·회의마다 달라
 * 토큰에 넣지 않고 각 서비스가 조회한다.
 */
public record AuthenticatedUser(Long userId) {
}
