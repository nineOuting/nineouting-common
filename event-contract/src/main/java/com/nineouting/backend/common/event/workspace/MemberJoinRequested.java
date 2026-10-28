package com.nineouting.backend.common.event.workspace;

/**
 * workspace-service → notification-service: 관리자에게 참여 요청 알림 (REQ-016 레거시).
 */
public record MemberJoinRequested(Long workspaceId, String workspaceName, Long joinRequestId, Long requesterUserId,
                                  Long adminUserId) {

    public static final String TYPE = "MemberJoinRequested";
    public static final String TOPIC = "workspace.member-join-requested";
}
