package com.nineouting.backend.common.outbox;

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
 * 보낼 이벤트. 서비스의 데이터 변경과 같은 DB 트랜잭션에서 저장되므로, 변경이 저장되면 이벤트도 반드시 남는다.
 * 브로커(Kafka·SNS)로 보내면 행을 지운다. 끝내 보내지 못한 이벤트(FAILED)만 원인 확인을 위해 남는다.
 * 테이블(outbox_events)은 각 서비스가 Flyway로 만든다 (README의 DDL).
 */
@Getter
@Entity
@Table(name = "outbox_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /** 이벤트 클래스의 TOPIC 상수 (Kafka 토픽, SNS는 이 이름으로 토픽 ARN을 만든다) */
    @Column(nullable = false, length = 200)
    private String destination;

    /** 같은 키는 순서가 지켜진다 (Kafka 파티션 키, SNS FIFO MessageGroupId) */
    @Column(name = "message_key", length = 100)
    private String messageKey;

    /** EventEnvelope JSON */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    OutboxEvent(String eventId, String eventType, String destination, String messageKey, String payload,
                LocalDateTime now) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.destination = destination;
        this.messageKey = messageKey;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
        this.nextAttemptAt = now;
        this.createdAt = now;
    }

    /** 실패 횟수가 늘수록 다시 보내기까지 더 오래 기다린다. 최대 횟수에 닿으면 FAILED. */
    void markFailed(String error, LocalDateTime now, int maxAttempts, Duration retryDelay) {
        this.attempts++;
        this.lastError = error == null ? null : error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
        if (attempts >= maxAttempts) {
            this.status = OutboxStatus.FAILED;
        } else {
            this.nextAttemptAt = now.plus(retryDelay);
        }
    }
}
