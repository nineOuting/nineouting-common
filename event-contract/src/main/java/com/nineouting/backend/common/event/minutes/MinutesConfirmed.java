package com.nineouting.backend.common.event.minutes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * minutes-service → notification·meeting-service: 회의록이 확정됐다 (REQ-110, 112).
 * notification은 참석자에게 알림과 메일을 보내고, meeting은 회의 목록에 "확정"을 표시한다.
 *
 * @param attendeeUserIds 참석자 전원 (작성자 포함)
 * @param confirmedAt     한국 시간
 */
public record MinutesConfirmed(Long minutesId, Long meetingId, Long workspaceId, Long folderId, String title,
                               List<Long> attendeeUserIds, LocalDateTime confirmedAt) {

    public static final String TYPE = "MinutesConfirmed";
    public static final String TOPIC = "minutes.confirmed";
}
