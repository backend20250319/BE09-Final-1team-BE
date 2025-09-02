package com.newnormallist.newsservice.summarizer.exception;

import java.time.Instant;

public record ApiError(
        int status,          // HTTP status code
        String code,         // 앱 레벨 코드 (e.g., NOT_FOUND, SUMMARIZER_ERROR)
        String message,      // 사용자/개발자용 메시지
        String path,         // 요청 경로
        String traceId,      // 요청 추적 ID
        Instant timestamp    // 발생 시각
) {
    public static ApiError of(int status, String code, String message, String path, String traceId) {
        return new ApiError(status, code, message, path, traceId, Instant.now());
    }
}