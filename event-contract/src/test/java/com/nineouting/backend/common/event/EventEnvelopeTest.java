package com.nineouting.backend.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nineouting.backend.common.event.user.EmailVerificationRequested;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

    @Test
    void 이벤트마다_새_ID와_발생_시각이_붙는다() {
        EmailVerificationRequested payload = new EmailVerificationRequested("a@example.com", "123456");

        EventEnvelope<EmailVerificationRequested> first =
                EventEnvelope.of(EmailVerificationRequested.TYPE, "user-service", null, payload);
        EventEnvelope<EmailVerificationRequested> second =
                EventEnvelope.of(EmailVerificationRequested.TYPE, "user-service", null, payload);

        assertThat(first.eventId()).isNotEqualTo(second.eventId());
        assertThat(first.occurredAt()).isNotNull();
        assertThat(first.payload()).isEqualTo(payload);
    }

    @Test
    void 필수_항목이_없으면_만들_수_없다() {
        assertThatThrownBy(() -> EventEnvelope.of(null, "user-service", null, "x"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> EventEnvelope.of("Type", "user-service", null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
