package com.newnormallist.newsservice.news.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class NewsExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NewsNotFoundException.class)
    public ResponseEntity<String> handleNewsNotFoundException(NewsNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류가 발생했습니다: " + e.getMessage());
    }

    @ExceptionHandler(value = { NewsHiddenException.class })
    protected ResponseEntity<Object> handleNewsHidden(RuntimeException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(value = { UnauthenticatedUserException.class })
    protected ResponseEntity<Object> handleUnauthenticatedUser(RuntimeException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value = { IllegalArgumentException.class })
    protected ResponseEntity<Object> handleIllegalArgument(RuntimeException ex, WebRequest request) {
        String bodyOfResponse = "잘못된 요청입니다: " + ex.getMessage();
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = { NullPointerException.class })
    protected ResponseEntity<Object> handleNullPointer(RuntimeException ex, WebRequest request) {
        String bodyOfResponse = "서버 내부 오류가 발생했습니다: " + ex.getMessage();
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
