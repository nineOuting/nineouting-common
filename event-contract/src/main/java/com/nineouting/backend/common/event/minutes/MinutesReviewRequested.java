package com.nineouting.backend.common.event.minutes;

import java.util.List;

/**
 * minutes-service → notification·meeting-service: 회의록 작성자가 참석자에게 검토를 요청했다 (REQ-106).
 * notification은 검토할 참석자에게 알리고, meeting은 회의 목록에 "검토 중"을 표시한다.
 *
 * @param reviewerUserIds 검토할 참석자 (작성자 제외)
 */
public record MinutesReviewRequested(Long minutesId, Long meetingId, Long workspaceId, String title,
                                     Long writerUserId, List<Long> reviewerUserIds) {

    public static final String TYPE = "MinutesReviewRequested";
    public static final String TOPIC = "minutes.review-requested";
}
