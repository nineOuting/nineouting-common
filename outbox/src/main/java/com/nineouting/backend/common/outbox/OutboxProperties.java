package com.nineouting.backend.common.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <pre>
 * nineouting:
 *   outbox:
 *     source: user-service     # 이벤트를 보낸 서비스 이름 (기본값: spring.application.name)
 * </pre>
 *
 * @param batchSize     한 번에 보낼 최대 개수
 * @param maxAttempts   이 횟수만큼 실패하면 FAILED로 두고 더 보내지 않는다
 * @param maxRetryDelay 실패할수록 다시 보내는 간격이 두 배씩 늘어나는데, 그 최대값
 */
@ConfigurationProperties(prefix = "nineouting.outbox")
public record OutboxProperties(String source, Integer batchSize, Integer maxAttempts, Duration maxRetryDelay) {

    public OutboxProperties {
        batchSize = batchSize == null ? 100 : batchSize;
        maxAttempts = maxAttempts == null ? 10 : maxAttempts;
        maxRetryDelay = maxRetryDelay == null ? Duration.ofMinutes(5) : maxRetryDelay;
    }
}
