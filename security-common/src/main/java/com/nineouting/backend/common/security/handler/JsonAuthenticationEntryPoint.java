package com.nineouting.backend.common.security.handler;

import com.nineouting.backend.common.security.jwt.JwtAuthenticationFilter;
import com.nineouting.backend.common.web.error.CommonErrorCode;
import com.nineouting.backend.common.web.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.json.JsonMapper;

/**
 * 로그인이 필요한 요청에 로그인 정보가 없거나 틀리면 401을 응답한다.
 * 토큰이 만료된 경우에만 code가 {@code TOKEN_EXPIRED}이고, 나머지는 {@code UNAUTHORIZED}다.
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ErrorResponseWriter writer;

    public JsonAuthenticationEntryPoint(JsonMapper jsonMapper) {
        this.writer = new ErrorResponseWriter(jsonMapper);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object error = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        ErrorCode errorCode = error instanceof ErrorCode code ? code : CommonErrorCode.UNAUTHORIZED;
        writer.write(response, errorCode);
    }
}
