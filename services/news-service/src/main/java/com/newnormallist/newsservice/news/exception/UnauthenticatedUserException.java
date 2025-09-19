package com.newnormallist.newsservice.news.exception;

public class UnauthenticatedUserException extends RuntimeException {
    public UnauthenticatedUserException(String message) {
        super(message);
    }
}