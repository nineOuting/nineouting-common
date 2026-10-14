package com.nineouting.backend.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class JwksKeyLocatorTest {

    private final AtomicInteger fetchCount = new AtomicInteger();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-14T00:00:00Z"));
    private TestKeys current = new TestKeys("key-1");

    private final JwksKeyLocator locator = new JwksKeyLocator(() -> {
        fetchCount.incrementAndGet();
        return current.jwks();
    }, clock);
    private final JwtTokenVerifier verifier = new JwtTokenVerifier(locator, TestKeys.ISSUER);

    @Test
    void 공개키는_한_번_받아_캐시한다() {
        verifier.verify(current.token("1", Instant.now().plusSeconds(600)));
        verifier.verify(current.token("2", Instant.now().plusSeconds(600)));

        assertThat(fetchCount).hasValue(1);
    }

    @Test
    void 키가_교체되면_처음_보는_kid로_다시_받아온다() {
        verifier.verify(current.token("1", Instant.now().plusSeconds(600)));

        current = new TestKeys("key-2");
        clock.advanceSeconds(JwksKeyLocator.MIN_REFRESH_INTERVAL.getSeconds());

        assertThat(verifier.verify(current.token("1", Instant.now().plusSeconds(600))).userId()).isEqualTo(1L);
        assertThat(fetchCount).hasValue(2);
    }

    @Test
    void 알_수_없는_kid가_반복돼도_정해진_간격_안에는_다시_받지_않는다() {
        verifier.verify(current.token("1", Instant.now().plusSeconds(600)));
        TestKeys unknown = new TestKeys("unknown");

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> verifier.verify(unknown.token("1", Instant.now().plusSeconds(600))))
                    .isInstanceOf(TokenVerificationException.class);
        }

        assertThat(fetchCount).hasValue(1);
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
