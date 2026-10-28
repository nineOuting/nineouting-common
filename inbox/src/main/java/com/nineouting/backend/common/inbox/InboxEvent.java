package com.nineouting.backend.common.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SQS로 받은 이벤트. 받자마자 저장하고(중복이면 버림) 처리는 따로 한다.
 * 처리한 이벤트도 중복 확인을 위해 일정 기간 남겨 둔다.
 * 테이블(inbox_events)은 각 서비스가 Flyway로 만든다 (README의 DDL).
 */
@Getter
@Entity
@Table(name = "inbox_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboxEvent {

    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /** EventEnvelope JSON 그대로 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    InboxEvent(String eventId, String topic, String eventType, String payload, LocalDateTime now) {
        this.eventId = eventId;
        this.topic = topic;
        this.eventType = eventType;
        this.payload = payload;
        this.status = InboxStatus.RECEIVED;
        this.attempts = 0;
        this.nextAttemptAt = now;
        this.receivedAt = now;
    }

    void markProcessed(LocalDateTime now) {
        this.status = InboxStatus.PROCESSED;
        this.processedAt = now;
        this.lastError = null;
    }

    void markFailed(String error, LocalDateTime now, int maxAttempts, Duration retryDelay) {
        this.attempts++;
        this.lastError = error == null ? null : error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
        if (attempts >= maxAttempts) {
            this.status = InboxStatus.FAILED;
        } else {
            this.nextAttemptAt = now.plus(retryDelay);
        }
    }
}
