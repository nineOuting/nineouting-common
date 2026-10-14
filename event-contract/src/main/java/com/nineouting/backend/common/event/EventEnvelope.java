package com.nineouting.backend.common.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 모든 이벤트가 공통으로 갖는 봉투. Kafka 메시지 본문은 이 형식의 JSON이다.
 *
 * <pre>{@code
 * { "eventId": "...", "eventType": "EmailVerificationRequested", "occurredAt": "2026-10-14T05:00:00Z",
 *   "source": "user-service", "traceparent": "00-...-...-01", "payload": { ... } }
 * }</pre>
 *
 * @param eventId     중복 처리 방지용. 같은 이벤트가 두 번 와도(at-least-once) 받는 쪽은 이 값으로 한 번만 처리한다
 * @param traceparent W3C Trace Context. 이벤트를 받은 쪽의 처리도 같은 추적으로 이어진다 (없으면 null)
 */
public record EventEnvelope<T>(
        String eventId,
        String eventType,
        Instant occurredAt,
        String source,
        String traceparent,
        T payload
) {

    public EventEnvelope {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(payload, "payload");
    }

    public static <T> EventEnvelope<T> of(String eventType, String source, String traceparent, T payload) {
        return new EventEnvelope<>(UUID.randomUUID().toString(), eventType, Instant.now(), source, traceparent, payload);
    }
}
