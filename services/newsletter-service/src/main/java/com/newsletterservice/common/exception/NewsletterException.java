package com.newsletterservice.common.exception;

import lombok.Getter;

@Getter
public class NewsletterException extends RuntimeException {
    
    private final String errorCode;
    
    public NewsletterException(String message) {
        super(message);
        this.errorCode = "NEWSLETTER_ERROR";
    }
    
    public NewsletterException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public NewsletterException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "NEWSLETTER_ERROR";
    }
}
