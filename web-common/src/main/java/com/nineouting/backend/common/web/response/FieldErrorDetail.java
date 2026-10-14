package com.nineouting.backend.common.web.response;

/**
 * 입력값 검증 실패 시 {@link ApiResponse#data()}에 담기는 항목. 프론트가 틀린 칸을 강조하는 데 쓴다.
 */
public record FieldErrorDetail(String field, String reason) {
}
