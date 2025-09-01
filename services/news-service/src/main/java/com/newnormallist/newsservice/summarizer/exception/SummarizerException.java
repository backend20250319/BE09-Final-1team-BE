package com.newnormallist.newsservice.summarizer.exception;

public class SummarizerException extends RuntimeException {
    public SummarizerException(String message) { super(message); }
    public SummarizerException(String message, Throwable cause) { super(message, cause); }
}
