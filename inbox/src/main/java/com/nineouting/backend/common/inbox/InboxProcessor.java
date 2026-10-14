package com.nineouting.backend.common.inbox;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * inbox에 쌓인 이벤트를 하나씩 꺼내 서비스의 {@link InboxHandler}에 넘긴다.
 * 이벤트마다 트랜잭션을 따로 쓴다 — 처리기가 실패하면 그 처리기가 바꾼 DB 내용만 되돌리고,
 * 실패 기록은 별도 트랜잭션으로 남긴 뒤 나중에 다시 시도한다.
 */
@Slf4j
public class InboxProcessor {

    private static final Duration FIRST_RETRY_DELAY = Duration.ofSeconds(1);

    private final InboxEventRepository repository;
    private final List<InboxHandler> handlers;
    private final TransactionTemplate transactionTemplate;
    private final InboxProperties properties;
    private final Clock clock;

    public InboxProcessor(InboxEventRepository repository, List<InboxHandler> handlers,
                          TransactionTemplate transactionTemplate, InboxProperties properties, Clock clock) {
        this.repository = repository;
        this.handlers = handlers;
        this.transactionTemplate = transactionTemplate;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${nineouting.inbox.process-interval:1s}")
    public void process() {
        for (int i = 0; i < properties.batchSize(); i++) {
            if (!processOne()) {
                return;
            }
        }
    }

    /** @return 처리할 이벤트가 있었으면 true */
    boolean processOne() {
        AtomicReference<Long> current = new AtomicReference<>();
        try {
            return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                LocalDateTime now = LocalDateTime.now(clock);
                Optional<InboxEvent> due = repository.findDue(InboxStatus.RECEIVED, now, PageRequest.of(0, 1))
                        .stream().findFirst();
                if (due.isEmpty()) {
                    return false;
                }
                InboxEvent event = due.get();
                current.set(event.getId());
                handlerFor(event.getEventType()).ifPresentOrElse(
                        handler -> handler.handle(new InboxMessage(event.getEventId(), event.getEventType(),
                                event.getTopic(), event.getPayload())),
                        () -> log.warn("처리기가 없는 이벤트입니다. 처리한 것으로 둡니다 (type={})", event.getEventType()));
                event.markProcessed(now);
                return true;
            }));
        } catch (RuntimeException e) {
            if (current.get() == null) {
                throw e;
            }
            recordFailure(current.get(), e);
            return true;
        }
    }

    private void recordFailure(Long inboxEventId, RuntimeException cause) {
        transactionTemplate.executeWithoutResult(status -> repository.findById(inboxEventId).ifPresent(event -> {
            event.markFailed(cause.toString(), LocalDateTime.now(clock), properties.maxAttempts(),
                    retryDelay(event.getAttempts()));
            if (event.getStatus() == InboxStatus.FAILED) {
                log.error("이벤트 처리를 포기합니다 (eventId={}, type={}, 시도 {}회)",
                        event.getEventId(), event.getEventType(), event.getAttempts(), cause);
            } else {
                log.warn("이벤트 처리 실패, 다시 시도합니다 (eventId={}, 시도 {}회): {}",
                        event.getEventId(), event.getAttempts(), cause.toString());
            }
        }));
    }

    private Optional<InboxHandler> handlerFor(String eventType) {
        return handlers.stream().filter(handler -> handler.supports(eventType)).findFirst();
    }

    /** 1초, 2초, 4초 … 최대 maxRetryDelay */
    Duration retryDelay(int previousAttempts) {
        Duration delay = FIRST_RETRY_DELAY.multipliedBy(1L << Math.min(previousAttempts, 20));
        return delay.compareTo(properties.maxRetryDelay()) > 0 ? properties.maxRetryDelay() : delay;
    }
}
