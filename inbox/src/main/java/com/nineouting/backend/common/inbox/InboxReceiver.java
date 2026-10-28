package com.nineouting.backend.common.inbox;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * SQS로 받은 메시지를 inbox에 저장한다. 같은 eventId는 한 번만 저장한다.
 * 저장이 끝나야 SQS 메시지를 지우므로, 저장에 실패하면(DB 장애 등) 같은 메시지가 다시 온다.
 */
@Slf4j
public class InboxReceiver {

    private final InboxEventRepository repository;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public InboxReceiver(InboxEventRepository repository, JsonMapper jsonMapper, Clock clock) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Transactional
    public void receive(String topic, String envelopeJson) {
        String eventId;
        String eventType;
        try {
            JsonNode envelope = jsonMapper.readTree(envelopeJson);
            eventId = envelope.path("eventId").asString(null);
            eventType = envelope.path("eventType").asString(null);
        } catch (JacksonException e) {
            log.warn("이벤트 봉투 형식이 아니어서 버립니다 (topic={})", topic, e);
            return;
        }
        if (eventId == null || eventType == null) {
            log.warn("eventId·eventType이 없는 메시지를 버립니다 (topic={})", topic);
            return;
        }
        if (repository.existsByEventId(eventId)) {
            log.debug("이미 받은 이벤트입니다 (eventId={})", eventId);
            return;
        }
        try {
            repository.saveAndFlush(new InboxEvent(eventId, topic, eventType, envelopeJson, LocalDateTime.now(clock)));
        } catch (DataIntegrityViolationException e) {
            log.debug("동시에 받은 같은 이벤트입니다 (eventId={})", eventId);
        }
    }
}
