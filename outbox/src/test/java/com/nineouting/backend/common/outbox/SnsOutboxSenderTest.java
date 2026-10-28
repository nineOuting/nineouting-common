package com.nineouting.backend.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

class SnsOutboxSenderTest {

    private static final String PREFIX = "arn:aws:sns:ap-northeast-2:000000000000:nineouting-dev-";

    private final SnsClient snsClient = mock(SnsClient.class);

    @Test
    void FIFO_토픽으로_메시지_키를_그룹으로_보낸다() {
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(PublishResponse.builder().build());

        new SnsOutboxSender(snsClient, PREFIX, true).send("meeting.created", "31", "{\"eventId\":\"e-1\"}");

        PublishRequest request = sent();
        assertThat(request.topicArn()).isEqualTo(PREFIX + "meeting-created.fifo");
        assertThat(request.message()).isEqualTo("{\"eventId\":\"e-1\"}");
        assertThat(request.messageGroupId()).isEqualTo("31");
        assertThat(request.messageDeduplicationId()).hasSize(64);
        assertThat(request.messageAttributes().get("topic").stringValue()).isEqualTo("meeting.created");
    }

    @Test
    void 같은_봉투는_같은_중복_제거_ID로_다른_봉투는_다른_ID로_보낸다() {
        SnsOutboxSender sender = new SnsOutboxSender(snsClient, PREFIX, true);
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(PublishResponse.builder().build());

        sender.send("meeting.created", "31", "{\"eventId\":\"e-1\"}");
        String first = sent().messageDeduplicationId();
        sender.send("meeting.created", "31", "{\"eventId\":\"e-2\"}");

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient, org.mockito.Mockito.times(2)).publish(captor.capture());
        assertThat(captor.getAllValues().get(1).messageDeduplicationId()).isNotEqualTo(first);
        assertThat(first).isEqualTo(captor.getAllValues().get(0).messageDeduplicationId());
    }

    @Test
    void 메시지_키가_없으면_기본_그룹으로_보낸다() {
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(PublishResponse.builder().build());

        new SnsOutboxSender(snsClient, PREFIX, true).send("user.password-reset-requested", null, "{}");

        assertThat(sent().messageGroupId()).isEqualTo("default");
    }

    @Test
    void 표준_토픽은_그룹_없이_보낸다() {
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(PublishResponse.builder().build());

        new SnsOutboxSender(snsClient, PREFIX, false).send("meeting.created", "31", "{}");

        PublishRequest request = sent();
        assertThat(request.topicArn()).isEqualTo(PREFIX + "meeting-created");
        assertThat(request.messageGroupId()).isNull();
        assertThat(request.messageDeduplicationId()).isNull();
    }

    @Test
    void SNS가_실패하면_예외로_알려_다시_보내게_한다() {
        when(snsClient.publish(any(PublishRequest.class))).thenThrow(SnsException.builder().message("down").build());

        assertThatThrownBy(() -> new SnsOutboxSender(snsClient, PREFIX, true).send("meeting.created", "31", "{}"))
                .isInstanceOf(SnsException.class);
    }

    @Test
    void 토픽_접두사가_없으면_시작하지_않는다() {
        assertThatThrownBy(() -> new SnsOutboxSender(snsClient, " ", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("topic-prefix");
    }

    private PublishRequest sent() {
        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        return captor.getValue();
    }
}
