package com.nineouting.backend.common.event.meeting;

import java.time.LocalDateTime;
import java.util.List;

/**
 * meeting-service → minutes-service: 회의를 마무리했다 (REQ-098). minutes는 회의록(작성 중)을 만든다.
 *
 * @param startAt         한국 시간
 * @param attendeeUserIds 마무리 시점의 참석자 전원 (만든 사람·회의록 작성자 포함)
 */
public record MeetingFinished(Long meetingId, Long workspaceId, Long folderId, String title, LocalDateTime startAt,
                              Long writerUserId, List<Long> attendeeUserIds) {

    public static final String TYPE = "MeetingFinished";
    public static final String TOPIC = "meeting.finished";
}
