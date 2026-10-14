package com.nineouting.backend.common.web.error;

import com.nineouting.backend.common.web.response.ApiResponse;
import com.nineouting.backend.common.web.response.FieldErrorDetail;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 모든 예외를 {@link ApiResponse} 형식으로 바꾼다.
 * 인증·인가 예외(401·403)는 Spring Security 필터에서 처리되므로 security-common이 맡는다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        if (errorCode.getStatus().is5xxServerError()) {
            log.error("비즈니스 예외: {}", errorCode.getCode(), e);
        } else {
            log.info("비즈니스 예외: {} - {}", errorCode.getCode(), e.getMessage());
        }
        return respond(errorCode, ApiResponse.fail(errorCode, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<FieldErrorDetail>>> handleValidation(MethodArgumentNotValidException e) {
        List<FieldErrorDetail> details = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        return respond(CommonErrorCode.INVALID_INPUT, ApiResponse.fail(CommonErrorCode.INVALID_INPUT, details));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
        log.info("잘못된 요청: {}", e.getMessage());
        return respond(CommonErrorCode.INVALID_INPUT, ApiResponse.fail(CommonErrorCode.INVALID_INPUT));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception e) {
        return respond(CommonErrorCode.NOT_FOUND, ApiResponse.fail(CommonErrorCode.NOT_FOUND));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return respond(CommonErrorCode.METHOD_NOT_ALLOWED, ApiResponse.fail(CommonErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);
        return respond(CommonErrorCode.INTERNAL_ERROR, ApiResponse.fail(CommonErrorCode.INTERNAL_ERROR));
    }

    private static <T> ResponseEntity<ApiResponse<T>> respond(ErrorCode errorCode, ApiResponse<T> body) {
        return ResponseEntity.status(errorCode.getStatus()).body(body);
    }
}
