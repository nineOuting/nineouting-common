package com.nineouting.backend.common.web.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 비즈니스_예외는_에러_코드의_상태와_코드로_응답한다() throws Exception {
        mockMvc.perform(get("/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value("이미 시작된 회의입니다"))
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void 검증_실패는_틀린_칸_목록을_data에_담는다() throws Exception {
        mockMvc.perform(post("/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.data[0].field").value("title"))
                .andExpect(jsonPath("$.data[0].reason").value("회의명은 필수입니다"));
    }

    @Test
    void 읽을_수_없는_요청_본문은_INVALID_INPUT이다() throws Exception {
        mockMvc.perform(post("/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{잘못된 json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void 필수_파라미터_누락은_INVALID_INPUT이다() throws Exception {
        mockMvc.perform(get("/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void 파라미터_타입_오류는_INVALID_INPUT이다() throws Exception {
        mockMvc.perform(get("/param").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void 지원하지_않는_요청_방식은_METHOD_NOT_ALLOWED이다() throws Exception {
        mockMvc.perform(delete("/business"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void 예상하지_못한_예외는_내부_사정을_숨기고_INTERNAL_ERROR로_응답한다() throws Exception {
        mockMvc.perform(get("/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요"));
    }

    record CreateRequest(@NotBlank(message = "회의명은 필수입니다") String title) {
    }

    @RestController
    static class TestController {

        @GetMapping("/business")
        void business() {
            throw new BusinessException(CommonErrorCode.CONFLICT, "이미 시작된 회의입니다");
        }

        @PostMapping("/validation")
        void validation(@Valid @RequestBody CreateRequest request) {
        }

        @GetMapping("/param")
        void param(@RequestParam int page) {
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("DB 접속 정보 같은 내부 사정");
        }
    }
}
