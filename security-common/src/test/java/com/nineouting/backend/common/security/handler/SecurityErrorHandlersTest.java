package com.nineouting.backend.common.security.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.nineouting.backend.common.security.error.SecurityErrorCode;
import com.nineouting.backend.common.security.jwt.JwtAuthenticationFilter;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.json.JsonMapper;

class SecurityErrorHandlersTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void 로그인이_없으면_401_UNAUTHORIZED다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new JsonAuthenticationEntryPoint(jsonMapper).commence(new MockHttpServletRequest(), response,
                new InsufficientAuthenticationException("로그인 필요"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body(response)).containsEntry("success", false)
                .containsEntry("code", "UNAUTHORIZED")
                .containsEntry("message", "로그인이 필요합니다");
    }

    @Test
    void 토큰이_만료됐으면_401_TOKEN_EXPIRED다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE, SecurityErrorCode.TOKEN_EXPIRED);
        MockHttpServletResponse response = new MockHttpServletResponse();

        new JsonAuthenticationEntryPoint(jsonMapper).commence(request, response,
                new InsufficientAuthenticationException("만료"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body(response)).containsEntry("code", "TOKEN_EXPIRED");
    }

    @Test
    void 접근_권한이_없으면_403_FORBIDDEN이다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new JsonAccessDeniedHandler(jsonMapper).handle(new MockHttpServletRequest(), response,
                new AccessDeniedException("거부"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(body(response)).containsEntry("code", "FORBIDDEN")
                .containsEntry("message", "권한이 없습니다");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> body(MockHttpServletResponse response) throws Exception {
        return jsonMapper.readValue(response.getContentAsString(), Map.class);
    }
}
