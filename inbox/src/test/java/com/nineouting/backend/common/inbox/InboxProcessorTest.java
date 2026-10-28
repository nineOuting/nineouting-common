package com.nineouting.backend.common.inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class InboxProcessorTest {

    private static final Instant NOW = Instant.parse("2026-10-14T00:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);

    private final InboxEventRepository repository = mock(InboxEventRepository.class);
    private final InboxProperties properties = new InboxProperties(List.of("t"), null, 10, 3, Duration.ofSeconds(5), null);
    private final List<String> handled = new ArrayList<>();
    private final RecordingTransactionManager transactions = new RecordingTransactionManager();

    @Test
    void 처리기에_넘기고_처리_완료로_둔다() {
        InboxEvent event = givenDue("Greeting");

        processor(new Handler("Greeting", false)).process();

        assertThat(handled).containsExactly("Greeting:event-1");
        assertThat(event.getStatus()).isEqualTo(InboxStatus.PROCESSED);
        assertThat(event.getProcessedAt()).isEqualTo(NOW_LOCAL);
    }

    @Test
    void 처리기가_없는_이벤트는_처리_완료로_둔다() {
        InboxEvent event = givenDue("Unknown");

        processor(new Handler("Greeting", false)).process();

        assertThat(handled).isEmpty();
        assertThat(event.getStatus()).isEqualTo(InboxStatus.PROCESSED);
    }

    @Test
    void 처리에_실패하면_되돌리고_기다렸다_다시_하며_정해진_횟수에_닿으면_포기한다() {
        InboxEvent event = givenDue("Greeting");
        when(repository.findById(event.getId())).thenReturn(Optional.of(event));
        InboxProcessor processor = processor(new Handler("Greeting", true));

        processor.processOne();
        assertThat(transactions.rollbacks).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(InboxStatus.RECEIVED);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW_LOCAL.plusSeconds(1));
        assertThat(event.getLastError()).contains("처리 실패");

        processor.processOne();
        processor.processOne();
        assertThat(event.getStatus()).isEqualTo(InboxStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(3);
    }

    @Test
    void 처리할_것이_없으면_멈춘다() {
        when(repository.findDue(eq(InboxStatus.RECEIVED), any(), any())).thenReturn(List.of());

        assertThat(processor(new Handler("Greeting", false)).processOne()).isFalse();
    }

    @Test
    void 다시_하는_간격은_두_배씩_늘고_최대값을_넘지_않는다() {
        InboxProcessor processor = processor(new Handler("Greeting", false));

        assertThat(processor.retryDelay(0)).isEqualTo(Duration.ofSeconds(1));
        assertThat(processor.retryDelay(2)).isEqualTo(Duration.ofSeconds(4));
        assertThat(processor.retryDelay(10)).isEqualTo(Duration.ofSeconds(5));
    }

    /** 아직 처리되지 않은(RECEIVED) 동안은 계속 처리 대상으로 돌려준다. */
    private InboxEvent givenDue(String eventType) {
        InboxEvent event = new InboxEvent("event-1", "topic", eventType, "{}", NOW_LOCAL);
        setId(event, 1L);
        when(repository.findDue(eq(InboxStatus.RECEIVED), any(), any())).thenAnswer(invocation ->
                event.getStatus() == InboxStatus.RECEIVED ? List.of(event) : List.of());
        return event;
    }

    private static void setId(InboxEvent event, Long id) {
        try {
            java.lang.reflect.Field field = InboxEvent.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(event, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private InboxProcessor processor(InboxHandler handler) {
        return new InboxProcessor(repository, List.of(handler), new TransactionTemplate(transactions), properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private final class Handler implements InboxHandler {

        private final String type;
        private final boolean fail;

        Handler(String type, boolean fail) {
            this.type = type;
            this.fail = fail;
        }

        @Override
        public boolean supports(String eventType) {
            return type.equals(eventType);
        }

        @Override
        public void handle(InboxMessage message) {
            if (fail) {
                throw new IllegalStateException("처리 실패");
            }
            handled.add(message.eventType() + ":" + message.eventId());
        }
    }

    /** 실제 DB 없이 트랜잭션 시작·되돌림만 기록한다. */
    private static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {

        int rollbacks;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbacks++;
        }
    }
}
