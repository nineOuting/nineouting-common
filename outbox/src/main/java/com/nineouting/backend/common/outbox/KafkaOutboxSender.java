package com.nineouting.backend.common.outbox;

import java.util.concurrent.TimeUnit;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * 브로커가 받았다고 확인할 때까지 기다린다. 확인 전에 끝나면 실패로 보고 다시 보낸다.
 */
public class KafkaOutboxSender implements OutboxSender {

    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaOutboxSender(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(String destination, String messageKey, String payload) throws Exception {
        kafkaTemplate.send(destination, messageKey, payload).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
