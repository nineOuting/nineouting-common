package com.nineouting.backend.common.event.meeting;

import java.time.LocalDateTime;
import java.util.List;

/**
 * meeting-service → notification-service: 참석자에게 회의 취소 알림 (REQ-112).
 *
 * @param attendeeUserIds 만든 사람을 포함한 참석자 전원
 */
public record MeetingCancelled(Long meetingId, Long workspaceId, Long folderId, String title, LocalDateTime startAt,
                               Long ownerUserId, List<Long> attendeeUserIds) {

    public static final String TYPE = "MeetingCancelled";
    public static final String TOPIC = "meeting.cancelled";
}
