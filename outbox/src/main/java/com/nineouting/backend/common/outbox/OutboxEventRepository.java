package com.nineouting.backend.common.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * 보낼 차례인 이벤트를 잠그고 가져온다. 다른 인스턴스가 잠근 행은 건너뛰어(SKIP LOCKED)
     * 서비스를 여러 개 띄워도 같은 이벤트를 동시에 보내지 않는다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select e from OutboxEvent e where e.status = :status and e.nextAttemptAt <= :now order by e.id")
    List<OutboxEvent> findDue(@Param("status") OutboxStatus status, @Param("now") LocalDateTime now, Pageable page);
}
