package com.nineouting.backend.common.inbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface InboxEventRepository extends JpaRepository<InboxEvent, Long> {

    boolean existsByEventId(String eventId);

    /** 처리할 차례인 이벤트를 잠그고 가져온다. 다른 인스턴스가 잠근 행은 건너뛴다(SKIP LOCKED). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select e from InboxEvent e where e.status = :status and e.nextAttemptAt <= :now order by e.id")
    List<InboxEvent> findDue(@Param("status") InboxStatus status, @Param("now") LocalDateTime now, Pageable page);

    @Modifying
    @Query("delete from InboxEvent e where e.status = :status and e.processedAt < :before")
    int deleteProcessedBefore(@Param("status") InboxStatus status, @Param("before") LocalDateTime before);
}
