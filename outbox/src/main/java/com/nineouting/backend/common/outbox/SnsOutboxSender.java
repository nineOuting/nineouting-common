package com.nineouting.backend.common.outbox;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * 이벤트 종류마다 하나인 SNS 토픽으로 보낸다. SNS가 받았다고 답할 때까지 기다리고, 실패하면 예외로 알려 다시 보내게 한다.
 * <ul>
 *   <li>토픽 ARN = 접두사 + TOPIC 상수의 점(.)을 하이픈(-)으로 (SNS 이름에는 점을 쓸 수 없다) + FIFO면 {@code .fifo}</li>
 *   <li>FIFO: 메시지 키가 {@code MessageGroupId}(같은 키는 순서대로), 봉투의 해시가 {@code MessageDeduplicationId}
 *       (봉투마다 eventId가 달라 이벤트마다 다르다. 다시 보내도 5분 안이면 한 번만 간다)</li>
 *   <li>메시지 속성 {@code topic} = 원래 TOPIC 이름. 받는 쪽 inbox가 어느 토픽에서 왔는지 남긴다</li>
 * </ul>
 */
public class SnsOutboxSender implements OutboxSender {

    /** 받는 쪽(SqsInboxListener)도 이 이름으로 읽는다 */
    public static final String TOPIC_ATTRIBUTE = "topic";

    /** 순서가 상관없는 이벤트(메시지 키 null)의 그룹. FIFO는 그룹이 꼭 있어야 한다 */
    private static final String DEFAULT_MESSAGE_GROUP = "default";

    private final SnsClient snsClient;
    private final String topicArnPrefix;
    private final boolean fifo;

    public SnsOutboxSender(SnsClient snsClient, String topicArnPrefix, boolean fifo) {
        if (topicArnPrefix == null || topicArnPrefix.isBlank()) {
            throw new IllegalStateException("nineouting.outbox.topic-prefix(SNS 토픽 ARN 앞부분)를 설정해야 합니다");
        }
        this.snsClient = snsClient;
        this.topicArnPrefix = topicArnPrefix;
        this.fifo = fifo;
    }

    @Override
    public void send(String destination, String messageKey, String payload) {
        PublishRequest.Builder request = PublishRequest.builder()
                .topicArn(topicArn(destination))
                .message(payload)
                .messageAttributes(Map.of(TOPIC_ATTRIBUTE,
                        MessageAttributeValue.builder().dataType("String").stringValue(destination).build()));
        if (fifo) {
            request.messageGroupId(messageKey == null || messageKey.isBlank() ? DEFAULT_MESSAGE_GROUP : messageKey)
                    .messageDeduplicationId(sha256(payload));
        }
        snsClient.publish(request.build());
    }

    String topicArn(String destination) {
        return topicArnPrefix + destination.replace('.', '-') + (fifo ? ".fifo" : "");
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
