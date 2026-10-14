package com.nineouting.backend.common.security.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller 파라미터에 로그인 사용자를 넣는다.
 * <pre>{@code
 * @GetMapping("/api/meetings")
 * public ApiResponse<...> list(@CurrentUser AuthenticatedUser user) { ... }
 * }</pre>
 * 로그인하지 않은 요청이면 null이다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
