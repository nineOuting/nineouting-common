package com.nineouting.backend.common.client;

/**
 * user-service /internal/users 응답 항목.
 * workspaceId를 주고 조회하면 이름·사진·소속은 그 워크스페이스에서 덮어쓴 값이다 (REQ-114·115·118). 이메일은 늘 계정 값.
 */
public record UserSummary(Long userId, String name, String email, String profileImageUrl, String affiliation) {

    /** 소속이 없던 예전 모양 (테스트용 가짜 응답 등) */
    public UserSummary(Long userId, String name, String email, String profileImageUrl) {
        this(userId, name, email, profileImageUrl, null);
    }
}
