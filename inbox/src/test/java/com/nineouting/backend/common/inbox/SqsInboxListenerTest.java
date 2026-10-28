package com.nineouting.backend.common.inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

class SqsInboxListenerTest {

    private static final String QUEUE = "http://localhost:4566/000000000000/nineouting-dev-minutes-service.fifo";

    private final SqsClient sqsClient = mock(SqsClient.class);
    private final InboxReceiver receiver = mock(InboxReceiver.class);
    private final SqsInboxListener listener = new SqsInboxListener(sqsClient, receiver, QUEUE);

    @Test
    void 받은_메시지를_원래_토픽_이름으로_inbox에_넣고_큐에서_지운다() {
        given(message("m-1", "meeting.finished", "31", "{\"eventId\":\"e-1\"}"));

        assertThat(listener.pollOnce()).isEqualTo(1);

        verify(receiver).receive("meeting.finished", "{\"eventId\":\"e-1\"}");
        assertThat(deletedReceipts()).containsExactly("receipt-m-1");
        ArgumentCaptor<ReceiveMessageRequest> request = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(sqsClient).receiveMessage(request.capture());
        assertThat(request.getValue().waitTimeSeconds()).isEqualTo(20);
        assertThat(request.getValue().messageAttributeNames()).contains("topic");
    }

    @Test
    void 저장에_실패하면_지우지_않아_다시_오게_하고_같은_그룹의_뒤_메시지도_미룬다() {
        given(message("m-1", "meeting.updated", "31", "first"),
                message("m-2", "meeting.updated", "31", "second"),
                message("m-3", "meeting.updated", "32", "other-meeting"));
        doThrow(new DataAccessResourceFailureException("db down")).when(receiver).receive("meeting.updated", "first");

        listener.pollOnce();

        verify(receiver, never()).receive("meeting.updated", "second");
        verify(receiver).receive("meeting.updated", "other-meeting");
        assertThat(deletedReceipts()).containsExactly("receipt-m-3");
    }

    @Test
    void 토픽_속성이_없으면_unknown으로_넣는다() {
        given(Message.builder().messageId("m-1").receiptHandle("receipt-m-1").body("{}").build());

        listener.pollOnce();

        verify(receiver).receive("unknown", "{}");
    }

    private void given(Message... messages) {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(messages).build());
    }

    private static Message message(String id, String topic, String group, String body) {
        return Message.builder()
                .messageId(id)
                .receiptHandle("receipt-" + id)
                .body(body)
                .attributes(Map.of(MessageSystemAttributeName.MESSAGE_GROUP_ID, group))
                .messageAttributes(Map.of("topic",
                        MessageAttributeValue.builder().dataType("String").stringValue(topic).build()))
                .build();
    }

    private List<String> deletedReceipts() {
        ArgumentCaptor<DeleteMessageRequest> captor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(sqsClient, org.mockito.Mockito.atLeast(0)).deleteMessage(captor.capture());
        List<String> receipts = new ArrayList<>();
        captor.getAllValues().forEach(request -> receipts.add(request.receiptHandle()));
        return receipts;
    }
}
