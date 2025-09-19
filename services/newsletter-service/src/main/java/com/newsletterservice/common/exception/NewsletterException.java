package com.newsletterservice.common.exception;

import lombok.Getter;

@Getter
public class NewsletterException extends RuntimeException {
    private final String errorCode;
    private final Object data;

    public NewsletterException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.data = null;
    }

    public NewsletterException(String message, String errorCode, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }

    public NewsletterException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.data = null;
    }
}
