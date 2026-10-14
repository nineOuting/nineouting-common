package com.nineouting.backend.common.event.user;

/**
 * user-service → notification-service: 회원가입 인증 메일 발송 (REQ-011).
 */
public record EmailVerificationRequested(String email, String code) {

    public static final String TYPE = "EmailVerificationRequested";
    public static final String TOPIC = "user.email-verification-requested";
}
