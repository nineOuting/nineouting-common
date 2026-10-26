package com.nineouting.backend.common.event.meeting;

import java.time.LocalDateTime;
import java.util.List;

/**
 * meeting-service → notification-service: 참석자(만든 사람 제외)에게 회의 초대 알림 (REQ-061).
 *
 * @param startAt         한국 시간
 * @param attendeeUserIds 만든 사람을 포함한 참석자 전원
 */
public record MeetingCreated(Long meetingId, Long workspaceId, Long folderId, String title, LocalDateTime startAt,
                             Long ownerUserId, List<Long> attendeeUserIds) {

    public static final String TYPE = "MeetingCreated";
    public static final String TOPIC = "meeting.created";
}
