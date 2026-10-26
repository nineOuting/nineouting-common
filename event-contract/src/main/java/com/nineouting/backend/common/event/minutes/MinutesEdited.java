package com.nineouting.backend.common.event.minutes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * minutes-service → notification-service: 확정된 회의록을 회의록 작성자가 고쳤다 (REQ-111).
 * notification은 참석자에게 알린다 (고친 사람은 빼고).
 *
 * @param editorUserId    고친 사람 (회의록 작성자)
 * @param attendeeUserIds 참석자 전원 (작성자 포함)
 * @param editedAt        한국 시간
 */
public record MinutesEdited(Long minutesId, Long meetingId, Long workspaceId, Long folderId, String title,
                            Long editorUserId, List<Long> attendeeUserIds, LocalDateTime editedAt) {

    public static final String TYPE = "MinutesEdited";
    public static final String TOPIC = "minutes.edited";
}
