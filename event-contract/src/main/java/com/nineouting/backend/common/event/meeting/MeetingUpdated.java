package com.nineouting.backend.common.event.meeting;

import java.time.LocalDateTime;
import java.util.List;

/**
 * meeting-service → notification-service: 회의 정보가 바뀌었다 (REQ-064).
 * 새로 추가된 사람은 초대 알림, 나머지 참석자는 변경 알림을 받는다. 빠진 사람에게는 알리지 않는다.
 *
 * @param attendeeUserIds 수정 후 참석자 전원 (만든 사람 포함)
 * @param addedUserIds    이번 수정으로 추가된 참석자
 * @param removedUserIds  이번 수정으로 빠진 참석자
 */
public record MeetingUpdated(Long meetingId, Long workspaceId, Long folderId, String title, LocalDateTime startAt,
                             Long ownerUserId, List<Long> attendeeUserIds, List<Long> addedUserIds,
                             List<Long> removedUserIds) {

    public static final String TYPE = "MeetingUpdated";
    public static final String TOPIC = "meeting.updated";
}
