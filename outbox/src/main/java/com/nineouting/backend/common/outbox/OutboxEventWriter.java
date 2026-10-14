package com.nineouting.backend.common.outbox;

import com.nineouting.backend.common.event.EventEnvelope;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * 서비스가 이벤트를 남길 때 쓴다. 반드시 데이터를 바꾸는 트랜잭션 안에서 불러야 한다(MANDATORY) —
 * 그래야 데이터 변경과 이벤트 기록이 함께 저장되거나 함께 취소된다.
 *
 * <pre>{@code
 * @Transactional
 * public void signup(...) {
 *     userRepository.save(user);
 *     outboxEventWriter.append(UserSignedUp.TOPIC, user.getId().toString(), UserSignedUp.TYPE, new UserSignedUp(...));
 * }
 * }</pre>
 */
public class OutboxEventWriter {

    private final OutboxEventRepository repository;
    private final JsonMapper jsonMapper;
    private final ObjectProvider<Tracer> tracer;
    private final String source;
    private final Clock clock;

    public OutboxEventWriter(OutboxEventRepository repository, JsonMapper jsonMapper, ObjectProvider<Tracer> tracer,
                             String source, Clock clock) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
        this.tracer = tracer;
        this.source = source;
        this.clock = clock;
    }

    /**
     * @param topic      Kafka 토픽 (이벤트 클래스의 TOPIC 상수)
     * @param messageKey 순서를 지켜야 하는 단위 (예: 사용자 ID). 상관없으면 null
     * @param eventType  이벤트 클래스의 TYPE 상수
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> EventEnvelope<T> append(String topic, String messageKey, String eventType, T payload) {
        EventEnvelope<T> envelope = EventEnvelope.of(eventType, source, currentTraceparent(), payload);
        repository.save(new OutboxEvent(envelope.eventId(), eventType, topic, messageKey,
                jsonMapper.writeValueAsString(envelope), LocalDateTime.now(clock)));
        return envelope;
    }

    /** W3C Trace Context 형식. 추적이 꺼져 있으면 null. */
    private String currentTraceparent() {
        Tracer current = tracer.getIfAvailable();
        Span span = current == null ? null : current.currentSpan();
        if (span == null) {
            return null;
        }
        String sampled = Boolean.TRUE.equals(span.context().sampled()) ? "01" : "00";
        return "00-" + span.context().traceId() + "-" + span.context().spanId() + "-" + sampled;
    }
}
