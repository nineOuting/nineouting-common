package com.nineouting.backend.common.event.workspace;

/**
 * workspace-service → notification-service: 요청자에게 참여 승인 알림 (REQ-035).
 */
public record MemberJoinApproved(Long workspaceId, String workspaceName, Long userId) {

    public static final String TYPE = "MemberJoinApproved";
    public static final String TOPIC = "workspace.member-join-approved";
}
