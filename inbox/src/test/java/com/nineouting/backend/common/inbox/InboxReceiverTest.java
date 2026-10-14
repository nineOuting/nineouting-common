package com.nineouting.backend.common.inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class InboxReceiverTest {

    private static final String ENVELOPE = """
            {"eventId":"e-1","eventType":"MemberJoinRequested","occurredAt":"2026-10-14T00:00:00Z",
             "source":"workspace-service","traceparent":null,"payload":{"workspaceId":1}}""";

    private final InboxEventRepository repository = mock(InboxEventRepository.class);
    private final InboxReceiver receiver = new InboxReceiver(repository, JsonMapper.builder().build(), Clock.systemUTC());

    @Test
    void 처음_받은_이벤트는_저장한다() {
        receiver.receive("workspace.member-join-requested", ENVELOPE);

        ArgumentCaptor<InboxEvent> saved = ArgumentCaptor.forClass(InboxEvent.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEventId()).isEqualTo("e-1");
        assertThat(saved.getValue().getEventType()).isEqualTo("MemberJoinRequested");
        assertThat(saved.getValue().getStatus()).isEqualTo(InboxStatus.RECEIVED);
    }

    @Test
    void 이미_받은_이벤트는_다시_저장하지_않는다() {
        when(repository.existsByEventId("e-1")).thenReturn(true);

        receiver.receive("workspace.member-join-requested", ENVELOPE);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void 봉투_형식이_아닌_메시지는_버린다() {
        receiver.receive("t", "not json");
        receiver.receive("t", "{\"payload\":{}}");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void payload를_이벤트_클래스로_읽는다() {
        InboxMessage message = new InboxMessage("e-1", "MemberJoinRequested", "t", ENVELOPE);

        assertThat(message.payload(JsonMapper.builder().build(), Sample.class)).isEqualTo(new Sample(1L));
    }

    record Sample(Long workspaceId) {
    }
}
