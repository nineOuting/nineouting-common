package com.nineouting.backend.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.nineouting.backend.common.security.error.SecurityErrorCode;
import com.nineouting.backend.common.web.error.CommonErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private final TestKeys keys = new TestKeys("key-1");
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            new JwtTokenVerifier(new JwksKeyLocator(keys::jwks), TestKeys.ISSUER));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 올바른_토큰이면_로그인_사용자를_넣는다() throws Exception {
        MockHttpServletRequest request = requestWithToken(keys.token("7", Instant.now().plusSeconds(600)));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getPrincipal()).isEqualTo(new AuthenticatedUser(7L));
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void 토큰이_없으면_로그인_없이_다음으로_넘긴다() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void 만료된_토큰이면_실패_이유를_남기고_다음으로_넘긴다() throws Exception {
        MockHttpServletRequest request = requestWithToken(keys.token("7", Instant.now().minusSeconds(600)));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE))
                .isEqualTo(SecurityErrorCode.TOKEN_EXPIRED);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void 잘못된_토큰이면_UNAUTHORIZED를_남긴다() throws Exception {
        MockHttpServletRequest request = requestWithToken("abc.def.ghi");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE))
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }

    private static MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
