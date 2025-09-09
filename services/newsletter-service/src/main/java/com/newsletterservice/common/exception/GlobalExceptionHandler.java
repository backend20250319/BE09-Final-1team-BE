package com.newsletterservice.common.exception;

import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.common.exception.NewsletterException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * NewsletterException 처리
     */
    @ExceptionHandler(NewsletterException.class)
    public ResponseEntity<ApiResponse<Object>> handleNewsletterException(
            NewsletterException ex, WebRequest request) {
        
        log.warn("NewsletterException 발생: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        HttpStatus status = determineHttpStatus(ex.getErrorCode());
        ApiResponse<Object> response = ApiResponse.error(ex.getErrorCode(), ex.getMessage());
        
        if (ex.getData() != null) {
            response = ApiResponse.error(ex.getErrorCode(), ex.getMessage(), ex.getData());
        }
        
        return ResponseEntity.status(status).body(response);
    }

    /**
     * 유효성 검증 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        log.warn("유효성 검증 실패: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ApiResponse<Map<String, String>> response = ApiResponse.error(
                "VALIDATION_FAILED", 
                "입력값 검증에 실패했습니다.", 
                errors
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        
        log.warn("잘못된 인수: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.error(
                "INVALID_ARGUMENT", 
                "잘못된 요청 인수입니다: " + ex.getMessage()
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 일반 Exception 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(
            Exception ex, WebRequest request) {
        
        log.error("예상하지 못한 오류 발생", ex);
        
        ApiResponse<Object> response = ApiResponse.error(
                "INTERNAL_SERVER_ERROR", 
                "서버 내부 오류가 발생했습니다."
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 에러 코드에 따른 HTTP 상태 결정
     */
    private HttpStatus determineHttpStatus(String errorCode) {
        return switch (errorCode) {
            case "UNAUTHORIZED", "LOGIN_REQUIRED" -> HttpStatus.UNAUTHORIZED;
            case "FORBIDDEN", "ACCESS_DENIED" -> HttpStatus.FORBIDDEN;
            case "NOT_FOUND", "USER_NOT_FOUND", "NEWSLETTER_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "VALIDATION_FAILED", "INVALID_ARGUMENT", "BAD_REQUEST" -> HttpStatus.BAD_REQUEST;
            case "CONFLICT", "EMAIL_ALREADY_EXISTS" -> HttpStatus.CONFLICT;
            case "RATE_LIMIT_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "SERVICE_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
