package com.newnormallist.newsservice.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 공통 응답 래퍼 (success/data/message)
@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final String message;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message);
    }
}