package com.newnormallist.newsservice.news.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class NewsForbiddenException extends RuntimeException {
    public NewsForbiddenException(String message) {
        super(message);
    }
}
