package com.nineouting.backend.common.inbox;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/**
 * 처리한 지 오래된 이벤트를 지운다. 이 기간이 지나면 같은 이벤트가 다시 와도 걸러낼 수 없으므로
 * Kafka의 재전송이 일어날 수 있는 기간보다 길게 둔다. FAILED는 원인 확인을 위해 남긴다.
 */
@Slf4j
public class InboxCleaner {

    private final InboxEventRepository repository;
    private final InboxProperties properties;
    private final Clock clock;

    public InboxCleaner(InboxEventRepository repository, InboxProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "${nineouting.inbox.cleanup-cron:0 30 4 * * *}")
    @Transactional
    public void deleteOldProcessedEvents() {
        int deleted = repository.deleteProcessedBefore(InboxStatus.PROCESSED,
                LocalDateTime.now(clock).minus(properties.retention()));
        if (deleted > 0) {
            log.info("처리한 지 {} 지난 이벤트 {}건을 지웠습니다", properties.retention(), deleted);
        }
    }
}
