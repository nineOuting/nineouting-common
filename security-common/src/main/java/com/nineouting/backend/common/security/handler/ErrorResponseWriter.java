package com.nineouting.backend.common.security.handler;

import com.nineouting.backend.common.web.error.ErrorCode;
import com.nineouting.backend.common.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Security 필터 단계의 에러는 @RestControllerAdvice까지 가지 않으므로, 같은 ApiResponse 형식을 직접 쓴다.
 */
class ErrorResponseWriter {

    private final JsonMapper jsonMapper;

    ErrorResponseWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonMapper.writeValueAsString(ApiResponse.fail(errorCode)));
    }
}
