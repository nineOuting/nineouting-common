package com.nineouting.backend.common.inbox;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;

/**
 * nineouting.inbox.topics의 메시지를 받아 inbox에 넣는다. 소비자 그룹은 서비스 이름이다.
 */
public class InboxKafkaListener {

    private final InboxReceiver receiver;

    public InboxKafkaListener(InboxReceiver receiver) {
        this.receiver = receiver;
    }

    @KafkaListener(
            topics = "#{'${nineouting.inbox.topics}'.split(',')}",
            groupId = "${nineouting.inbox.group-id:${spring.application.name}}")
    public void onMessage(ConsumerRecord<String, String> record) {
        receiver.receive(record.topic(), record.value());
    }
}
