package com.nineouting.backend.common.web.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.nineouting.backend.common.web.error.CommonErrorCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ApiResponseTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void 성공_응답에는_code가_없다() {
        Map<String, Object> json = toMap(ApiResponse.ok(Map.of("id", 1)));

        assertThat(json).containsOnlyKeys("success", "data", "message");
        assertThat(json.get("success")).isEqualTo(true);
        assertThat(json.get("message")).isEqualTo("");
    }

    @Test
    void 데이터_없는_성공_응답은_data가_null이다() {
        Map<String, Object> json = toMap(ApiResponse.ok());

        assertThat(json).containsEntry("data", null);
    }

    @Test
    void 에러_응답에는_code와_기본_메시지가_들어간다() {
        Map<String, Object> json = toMap(ApiResponse.fail(CommonErrorCode.NOT_FOUND));

        assertThat(json).containsEntry("success", false)
                .containsEntry("data", null)
                .containsEntry("message", "요청한 대상을 찾을 수 없습니다")
                .containsEntry("code", "NOT_FOUND");
    }

    @Test
    void 에러_응답의_메시지를_바꿀_수_있다() {
        ApiResponse<Void> response = ApiResponse.fail(CommonErrorCode.CONFLICT, "이미 시작된 회의입니다");

        assertThat(response.message()).isEqualTo("이미 시작된 회의입니다");
        assertThat(response.code()).isEqualTo("CONFLICT");
    }

    @Test
    void 에러_응답에_데이터를_담을_수_있다() {
        List<FieldErrorDetail> details = List.of(new FieldErrorDetail("title", "회의명은 필수입니다"));

        ApiResponse<List<FieldErrorDetail>> response = ApiResponse.fail(CommonErrorCode.INVALID_INPUT, details);

        assertThat(response.data()).containsExactly(new FieldErrorDetail("title", "회의명은 필수입니다"));
        assertThat(response.message()).isEqualTo("입력값을 확인해 주세요");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(ApiResponse<?> response) {
        return jsonMapper.readValue(jsonMapper.writeValueAsString(response), Map.class);
    }
}
