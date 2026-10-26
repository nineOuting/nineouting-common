package com.nineouting.backend.common.event.meeting;

/**
 * meeting-service → minutes-service: 회의를 다른 폴더로 옮겼다 (REQ-045, 레거시).
 * minutes는 회의록의 폴더와 ai-service 챗봇 복사본의 폴더를 바꾼다.
 */
public record MeetingMoved(Long meetingId, Long workspaceId, Long fromFolderId, Long toFolderId) {

    public static final String TYPE = "MeetingMoved";
    public static final String TOPIC = "meeting.moved";
}
