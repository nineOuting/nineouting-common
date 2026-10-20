package com.nineouting.backend.common.event.user;

/**
 * user-service → notification-service: 비밀번호 찾기 인증 번호 메일 발송 (REQ-009).
 */
public record PasswordResetRequested(String email, String code) {

    public static final String TYPE = "PasswordResetRequested";
    public static final String TOPIC = "user.password-reset-requested";
}
