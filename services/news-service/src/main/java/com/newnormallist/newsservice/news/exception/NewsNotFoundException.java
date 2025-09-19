package com.newnormallist.newsservice.news.exception;

public class NewsNotFoundException extends RuntimeException {

    public NewsNotFoundException(String message) {
        super(message);
    }

    public NewsNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
