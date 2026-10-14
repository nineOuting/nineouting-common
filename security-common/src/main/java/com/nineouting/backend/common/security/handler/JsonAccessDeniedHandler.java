package com.nineouting.backend.common.security.handler;

import com.nineouting.backend.common.web.error.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * 로그인은 했지만 Security 설정상 접근할 수 없는 요청에 403을 응답한다.
 * 워크스페이스·회의 역할 같은 서비스 권한 검사는 서비스가 BusinessException(FORBIDDEN)으로 처리한다.
 */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ErrorResponseWriter writer;

    public JsonAccessDeniedHandler(JsonMapper jsonMapper) {
        this.writer = new ErrorResponseWriter(jsonMapper);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        writer.write(response, CommonErrorCode.FORBIDDEN);
    }
}
