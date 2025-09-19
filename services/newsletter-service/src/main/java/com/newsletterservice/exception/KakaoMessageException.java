package com.newsletterservice.exception;

/**
 * 카카오 메시지 전송 관련 예외 클래스
 */
public class KakaoMessageException extends RuntimeException {
    
    private String errorCode;
    
    public KakaoMessageException(String message) {
        super(message);
    }
    
    public KakaoMessageException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public KakaoMessageException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
