package com.nineouting.backend.common.event.meeting;

import java.time.LocalDateTime;
import java.util.List;

/**
 * meeting-service → minutes-service: 회의를 마무리했다 (REQ-098). minutes는 회의록(작성 중)을 만든다.
 *
 * @param startAt         한국 시간
 * @param location        회의 장소 (회의록 아카이브 장소 필터, REQ-055). 없으면 빈 문자열
 * @param attendeeUserIds 마무리 시점의 참석자 전원 (만든 사람·회의록 작성자 포함)
 * @param templateId      회의 대기 화면에서 고른 회의록 양식 (REQ-123). 안 골랐으면 null. minutes는 이 양식으로 회의록을 만든다
 */
public record MeetingFinished(Long meetingId, Long workspaceId, Long folderId, String title, LocalDateTime startAt,
                              String location, Long writerUserId, List<Long> attendeeUserIds, Long templateId) {

    public static final String TYPE = "MeetingFinished";
    public static final String TOPIC = "meeting.finished";
}
