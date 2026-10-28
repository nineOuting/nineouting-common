package com.nineouting.backend.common.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <pre>
 * nineouting:
 *   outbox:
 *     source: user-service     # 이벤트를 보낸 서비스 이름 (기본값: spring.application.name)
 *     topic-prefix: arn:aws:sns:ap-northeast-2:123456789012:nineouting-dev-   # sns일 때
 * </pre>
 *
 * @param batchSize     한 번에 보낼 최대 개수
 * @param maxAttempts   이 횟수만큼 실패하면 FAILED로 두고 더 보내지 않는다
 * @param maxRetryDelay 실패할수록 다시 보내는 간격이 두 배씩 늘어나는데, 그 최대값
 * @param topicPrefix   SNS 토픽 ARN 앞부분. 뒤에 TOPIC 상수(점은 하이픈으로)가 붙는다
 * @param fifo          SNS FIFO 토픽이면 true (기본). 이름 끝에 .fifo를 붙이고 메시지 키로 순서를 지킨다
 */
@ConfigurationProperties(prefix = "nineouting.outbox")
public record OutboxProperties(String source, Integer batchSize, Integer maxAttempts, Duration maxRetryDelay,
                               String topicPrefix, Boolean fifo) {

    public OutboxProperties {
        batchSize = batchSize == null ? 100 : batchSize;
        maxAttempts = maxAttempts == null ? 10 : maxAttempts;
        maxRetryDelay = maxRetryDelay == null ? Duration.ofMinutes(5) : maxRetryDelay;
        fifo = fifo == null || fifo;
    }
}
