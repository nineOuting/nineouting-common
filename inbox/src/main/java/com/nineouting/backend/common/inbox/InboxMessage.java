package com.nineouting.backend.common.inbox;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 처리기({@link InboxHandler})에 넘기는 이벤트.
 *
 * @param envelopeJson EventEnvelope JSON 전체 (eventId, eventType, occurredAt, source, traceparent, payload)
 */
public record InboxMessage(String eventId, String eventType, String topic, String envelopeJson) {

    /** 봉투의 payload를 이벤트 클래스(예: MemberJoinRequested)로 읽는다. */
    public <T> T payload(JsonMapper jsonMapper, Class<T> type) {
        JsonNode payload = jsonMapper.readTree(envelopeJson).path("payload");
        return jsonMapper.treeToValue(payload, type);
    }
}
