package com.newsletterservice.util;

import com.newsletterservice.common.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.function.Supplier;

public final class ApiResponseUtils {

    private ApiResponseUtils() {
        // 유틸리티 클래스
    }

    /**
     * 안전한 API 응답 처리
     */
    public static <T> T handleApiResponse(ApiResponse<T> response, Supplier<T> fallback) {
        if (response != null && response.isSuccess() && response.getData() != null) {
            return response.getData();
        }
        return fallback.get();
    }

    /**
     * 안전한 페이지 응답 처리
     */
    public static <T> Page<T> handlePageResponse(ApiResponse<Page<T>> response, Pageable pageable) {
        if (response != null && response.isSuccess() && response.getData() != null) {
            return response.getData();
        }
        return Page.empty(pageable);
    }

    /**
     * 안전한 리스트 응답 처리
     */
    public static <T> List<T> handleListResponse(ApiResponse<List<T>> response) {
        return handleApiResponse(response, List::of);
    }

    /**
     * 안전한 값 응답 처리
     */
    public static <T> T handleValueResponse(ApiResponse<T> response, T defaultValue) {
        return handleApiResponse(response, () -> defaultValue);
    }
}
