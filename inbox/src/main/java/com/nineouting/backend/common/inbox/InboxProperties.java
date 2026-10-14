package com.nineouting.backend.common.inbox;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <pre>
 * nineouting:
 *   inbox:
 *     topics: user.email-verification-requested,workspace.member-join-requested   # 쉼표로 구분
 * </pre>
 *
 * @param topics        받을 Kafka 토픽 (쉼표로 구분)
 * @param batchSize     한 번에 처리할 최대 개수
 * @param maxAttempts   이 횟수만큼 실패하면 FAILED로 두고 더 처리하지 않는다
 * @param maxRetryDelay 실패할수록 다시 처리하는 간격이 두 배씩 늘어나는데, 그 최대값
 * @param retention     처리한 이벤트를 남겨 두는 기간 (이 기간 안에 같은 이벤트가 다시 오면 걸러낸다)
 */
@ConfigurationProperties(prefix = "nineouting.inbox")
public record InboxProperties(List<String> topics, Integer batchSize, Integer maxAttempts, Duration maxRetryDelay,
                              Duration retention) {

    public InboxProperties {
        batchSize = batchSize == null ? 100 : batchSize;
        maxAttempts = maxAttempts == null ? 10 : maxAttempts;
        maxRetryDelay = maxRetryDelay == null ? Duration.ofMinutes(5) : maxRetryDelay;
        retention = retention == null ? Duration.ofDays(7) : retention;
    }
}
