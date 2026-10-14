package com.nineouting.backend.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nineouting.backend.common.security.error.SecurityErrorCode;
import com.nineouting.backend.common.web.error.CommonErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JwtTokenVerifierTest {

    private final TestKeys keys = new TestKeys("key-1");
    private final JwtTokenVerifier verifier =
            new JwtTokenVerifier(new JwksKeyLocator(keys::jwks), TestKeys.ISSUER);

    @Test
    void 올바른_토큰이면_사용자_ID를_꺼낸다() {
        String token = keys.token("42", Instant.now().plusSeconds(600));

        assertThat(verifier.verify(token)).isEqualTo(new AuthenticatedUser(42L));
    }

    @Test
    void 만료된_토큰은_TOKEN_EXPIRED다() {
        String token = keys.token("42", Instant.now().minusSeconds(600));

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(TokenVerificationException.class)
                .extracting("errorCode").isEqualTo(SecurityErrorCode.TOKEN_EXPIRED);
    }

    @Test
    void 다른_키로_서명한_토큰은_UNAUTHORIZED다() {
        TestKeys attacker = new TestKeys("key-1");
        String token = attacker.token("42", Instant.now().plusSeconds(600));

        assertUnauthorized(token);
    }

    @Test
    void 발급자가_다른_토큰은_UNAUTHORIZED다() {
        String token = keys.token("42", "someone-else", Instant.now().plusSeconds(600));

        assertUnauthorized(token);
    }

    @Test
    void 사용자_ID가_숫자가_아니면_UNAUTHORIZED다() {
        String token = keys.token("not-a-number", Instant.now().plusSeconds(600));

        assertUnauthorized(token);
    }

    @Test
    void 토큰_형식이_아니면_UNAUTHORIZED다() {
        assertUnauthorized("abc.def.ghi");
        assertUnauthorized("");
    }

    private void assertUnauthorized(String token) {
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(TokenVerificationException.class)
                .extracting("errorCode").isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }
}
