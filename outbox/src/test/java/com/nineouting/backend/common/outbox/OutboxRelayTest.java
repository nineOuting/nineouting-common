package com.nineouting.backend.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-10-14T00:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);

    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final OutboxProperties properties = new OutboxProperties("test-service", null, 3, Duration.ofSeconds(5));
    private final List<String> sent = new ArrayList<>();

    @Test
    void 보낸_이벤트는_지운다() {
        OutboxEvent event = givenDue("topic-a");
        OutboxRelay relay = relay((destination, key, payload) -> sent.add(destination + ":" + key + ":" + payload));

        relay.relay();

        assertThat(sent).containsExactly("topic-a:key-1:{}");
        verify(repository).delete(event);
    }

    @Test
    void 실패하면_기다렸다가_다시_보내고_정해진_횟수에_닿으면_포기한다() {
        OutboxEvent event = givenDue("topic-a");
        OutboxRelay relay = relay((destination, key, payload) -> {
            throw new IllegalStateException("브로커 응답 없음");
        });

        relay.relay();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW_LOCAL.plusSeconds(1));
        assertThat(event.getLastError()).contains("브로커 응답 없음");

        relay.relay();
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW_LOCAL.plusSeconds(2));

        relay.relay();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(3);
        verify(repository, never()).delete(event);
    }

    @Test
    void 하나가_실패해도_나머지는_보낸다() {
        OutboxEvent failing = new OutboxEvent("id-1", "Type", "broken", "k", "{}", NOW_LOCAL);
        OutboxEvent ok = new OutboxEvent("id-2", "Type", "topic-b", "k", "{}", NOW_LOCAL);
        when(repository.findDue(eq(OutboxStatus.PENDING), eq(NOW_LOCAL), any())).thenReturn(List.of(failing, ok));
        OutboxRelay relay = relay((destination, key, payload) -> {
            if (destination.equals("broken")) {
                throw new IllegalStateException("실패");
            }
        });

        relay.relay();

        assertThat(failing.getStatus()).isEqualTo(OutboxStatus.PENDING);
        verify(repository, never()).delete(failing);
        verify(repository).delete(ok);
    }

    @Test
    void 다시_보내는_간격은_두_배씩_늘고_최대값을_넘지_않는다() {
        OutboxRelay relay = relay((destination, key, payload) -> { });

        assertThat(relay.retryDelay(0)).isEqualTo(Duration.ofSeconds(1));
        assertThat(relay.retryDelay(1)).isEqualTo(Duration.ofSeconds(2));
        assertThat(relay.retryDelay(2)).isEqualTo(Duration.ofSeconds(4));
        assertThat(relay.retryDelay(10)).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void 오류_메시지는_컬럼_길이에_맞게_자른다() {
        OutboxEvent event = new OutboxEvent("id", "Type", "topic", null, "{}", NOW_LOCAL);

        event.markFailed("x".repeat(1000), NOW_LOCAL, 10, Duration.ofSeconds(1));

        assertThat(event.getLastError()).hasSize(500);
    }

    private OutboxEvent givenDue(String destination) {
        OutboxEvent event = new OutboxEvent("event-1", "Type", destination, "key-1", "{}", NOW_LOCAL);
        when(repository.findDue(eq(OutboxStatus.PENDING), any(), any())).thenReturn(List.of(event));
        return event;
    }

    private OutboxRelay relay(OutboxSender sender) {
        return new OutboxRelay(repository, sender, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
