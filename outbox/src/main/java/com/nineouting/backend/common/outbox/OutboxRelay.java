package com.nineouting.backend.common.outbox;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기록된 이벤트를 주기적으로 꺼내 보내고, 보낸 이벤트는 지운다.
 * 보낸 직후 지우기 전에 서비스가 죽으면 같은 이벤트를 한 번 더 보낼 수 있다(at-least-once).
 * 받는 쪽은 eventId로 중복을 걸러야 한다.
 */
@Slf4j
public class OutboxRelay {

    private static final Duration FIRST_RETRY_DELAY = Duration.ofSeconds(1);

    private final OutboxEventRepository repository;
    private final OutboxSender sender;
    private final OutboxProperties properties;
    private final Clock clock;

    public OutboxRelay(OutboxEventRepository repository, OutboxSender sender, OutboxProperties properties, Clock clock) {
        this.repository = repository;
        this.sender = sender;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${nineouting.outbox.relay-interval:1s}")
    @Transactional
    public void relay() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<OutboxEvent> due = repository.findDue(OutboxStatus.PENDING, now, PageRequest.of(0, properties.batchSize()));
        for (OutboxEvent event : due) {
            try {
                sender.send(event.getDestination(), event.getMessageKey(), event.getPayload());
                repository.delete(event);
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                event.markFailed(e.toString(), now, properties.maxAttempts(), retryDelay(event.getAttempts()));
                if (event.getStatus() == OutboxStatus.FAILED) {
                    log.error("이벤트 전송을 포기합니다 (eventId={}, type={}, 시도 {}회)",
                            event.getEventId(), event.getEventType(), event.getAttempts(), e);
                } else {
                    log.warn("이벤트 전송 실패, 다시 보냅니다 (eventId={}, 시도 {}회): {}",
                            event.getEventId(), event.getAttempts() + 1, e.toString());
                }
            }
        }
    }

    /** 1초, 2초, 4초 … 최대 maxRetryDelay */
    Duration retryDelay(int previousAttempts) {
        Duration delay = FIRST_RETRY_DELAY.multipliedBy(1L << Math.min(previousAttempts, 20));
        return delay.compareTo(properties.maxRetryDelay()) > 0 ? properties.maxRetryDelay() : delay;
    }
}
