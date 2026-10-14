package com.nineouting.backend.common.outbox;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/**
 * 보낸 지 오래된 이벤트를 지워 테이블이 끝없이 커지지 않게 한다. FAILED는 원인 확인을 위해 남긴다.
 */
@Slf4j
public class OutboxCleaner {

    private final OutboxEventRepository repository;
    private final OutboxProperties properties;
    private final Clock clock;

    public OutboxCleaner(OutboxEventRepository repository, OutboxProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "${nineouting.outbox.cleanup-cron:0 0 4 * * *}")
    @Transactional
    public void deleteOldSentEvents() {
        int deleted = repository.deleteSentBefore(OutboxStatus.SENT, LocalDateTime.now(clock).minus(properties.retention()));
        if (deleted > 0) {
            log.info("보낸 지 {} 지난 이벤트 {}건을 지웠습니다", properties.retention(), deleted);
        }
    }
}
