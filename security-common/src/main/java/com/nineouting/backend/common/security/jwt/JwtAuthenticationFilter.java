package com.nineouting.backend.common.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code Authorization: Bearer <토큰>}을 검증해 로그인 사용자를 SecurityContext에 넣는다.
 * 토큰이 없거나 틀려도 요청을 막지 않는다 — 막을지는 서비스의 SecurityFilterChain이 정하고,
 * 막힌 경우 {@link com.nineouting.backend.common.security.handler.JsonAuthenticationEntryPoint}가
 * 여기서 남긴 실패 이유로 401을 응답한다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 토큰 검증 실패 이유(ErrorCode)를 entry point에 넘기는 요청 속성 이름 */
    public static final String AUTH_ERROR_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".AUTH_ERROR";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenVerifier verifier;

    public JwtAuthenticationFilter(JwtTokenVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            authenticate(request, header.substring(BEARER_PREFIX.length()));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            AuthenticatedUser user = verifier.verify(token);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
            SecurityContextHolder.setContext(context);
        } catch (TokenVerificationException e) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, e.getErrorCode());
        }
    }
}
