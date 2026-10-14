package com.nineouting.backend.common.event.user;

/**
 * user-service → notification-service: 비밀번호 재설정 링크 메일 발송 (REQ-009).
 */
public record PasswordResetRequested(String email, String resetLink) {

    public static final String TYPE = "PasswordResetRequested";
    public static final String TOPIC = "user.password-reset-requested";
}
