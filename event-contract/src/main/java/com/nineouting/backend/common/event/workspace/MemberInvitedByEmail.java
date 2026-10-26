package com.nineouting.backend.common.event.workspace;

import java.time.LocalDateTime;

/**
 * workspace-service → notification-service: 소유자가 이메일로 멤버를 초대했다 (REQ-127).
 * notification은 초대 링크가 담긴 메일을 보낸다. 받은 사람은 링크로 승인 없이 바로 들어온다 (가입 전이면 가입 후).
 *
 * @param token     초대 토큰. 링크는 {프론트 주소}/invitations/{token}
 * @param expiresAt 한국 시간
 */
public record MemberInvitedByEmail(Long workspaceId, String workspaceName, String email, String token,
                                   Long inviterUserId, LocalDateTime expiresAt) {

    public static final String TYPE = "MemberInvitedByEmail";
    public static final String TOPIC = "workspace.member-invited";
}
