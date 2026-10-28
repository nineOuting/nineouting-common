package com.nineouting.backend.common.inbox;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/**
 * 서비스 큐(nineouting.inbox.queue)를 롱 폴링으로 받아 inbox에 넣는다.
 * inbox 저장이 끝난 메시지만 큐에서 지운다. 저장에 실패하면(DB 장애 등) 지우지 않으므로,
 * 큐의 보이지 않는 시간(visibility timeout)이 지나면 같은 메시지가 다시 온다. 여러 번 실패하면 SQS가 DLQ로 옮긴다.
 * FIFO 큐에서 한 메시지가 실패하면 같은 그룹의 뒤 메시지도 이번에는 처리하지 않는다 (순서를 지키기 위해).
 */
@Slf4j
public class SqsInboxListener implements SmartLifecycle {

    /** 보내는 쪽(outbox SnsOutboxSender)이 원래 TOPIC 이름을 담는 메시지 속성. SNS 구독이 Raw 전달이면 그대로 온다 */
    static final String TOPIC_ATTRIBUTE = "topic";
    static final String UNKNOWN_TOPIC = "unknown";

    /** SQS 롱 폴링 최대값. 메시지가 없으면 이만큼 기다렸다가 빈 응답을 준다 */
    private static final int WAIT_TIME_SECONDS = 20;
    private static final int MAX_MESSAGES = 10;
    /** SQS에 닿지 못하면 이만큼 쉬었다가 다시 받는다 */
    private static final Duration ERROR_PAUSE = Duration.ofSeconds(5);
    private static final Duration STOP_TIMEOUT = Duration.ofSeconds(WAIT_TIME_SECONDS + 5);

    private final SqsClient sqsClient;
    private final InboxReceiver receiver;
    private final String queueUrl;

    private volatile boolean running;
    private Thread poller;

    public SqsInboxListener(SqsClient sqsClient, InboxReceiver receiver, String queueUrl) {
        this.sqsClient = sqsClient;
        this.receiver = receiver;
        this.queueUrl = queueUrl;
    }

    @Override
    public void start() {
        running = true;
        poller = Thread.ofPlatform().name("inbox-sqs").daemon().start(this::pollLoop);
    }

    @Override
    public void stop() {
        running = false;
        if (poller == null) {
            return;
        }
        poller.interrupt();
        try {
            poller.join(STOP_TIMEOUT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void pollLoop() {
        while (running) {
            try {
                pollOnce();
            } catch (RuntimeException e) {
                if (!running) {
                    return;
                }
                log.warn("SQS에서 받지 못했습니다. {}초 뒤 다시 받습니다 (queue={}): {}",
                        ERROR_PAUSE.toSeconds(), queueUrl, e.toString());
                pause();
            }
        }
    }

    /** 한 번 받아 처리한다. 받은 메시지 수를 돌려준다 */
    int pollOnce() {
        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .waitTimeSeconds(WAIT_TIME_SECONDS)
                .maxNumberOfMessages(MAX_MESSAGES)
                .messageAttributeNames(TOPIC_ATTRIBUTE)
                .messageSystemAttributeNames(MessageSystemAttributeName.MESSAGE_GROUP_ID)
                .build()).messages();
        Set<String> failedGroups = new HashSet<>();
        for (Message message : messages) {
            String group = message.attributes().get(MessageSystemAttributeName.MESSAGE_GROUP_ID);
            if (group != null && failedGroups.contains(group)) {
                continue;
            }
            if (!handle(message) && group != null) {
                failedGroups.add(group);
            }
        }
        return messages.size();
    }

    private boolean handle(Message message) {
        MessageAttributeValue topic = message.messageAttributes().get(TOPIC_ATTRIBUTE);
        try {
            receiver.receive(topic == null ? UNKNOWN_TOPIC : topic.stringValue(), message.body());
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build());
            return true;
        } catch (RuntimeException e) {
            log.warn("inbox에 저장하지 못했습니다. 보이지 않는 시간이 지나면 다시 받습니다 (messageId={}): {}",
                    message.messageId(), e.toString());
            return false;
        }
    }

    private void pause() {
        try {
            Thread.sleep(ERROR_PAUSE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
