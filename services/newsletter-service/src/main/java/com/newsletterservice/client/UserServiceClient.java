package com.newsletterservice.client;

import com.newsletterservice.client.dto.CategoryResponse;
import com.newsletterservice.client.dto.UserResponse;
import com.newsletterservice.client.dto.ReadHistoryResponse;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.config.FeignTimeoutConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "user-service",
        url = "${user.base-url:http://localhost:8081}",
        contextId = "newsletterUserServiceClient",
        path = "/api/users",
        configuration = FeignTimeoutConfig.class
)
public interface UserServiceClient {
    
    /**
     * 사용자 정보 조회
     */
    @GetMapping("/api/users/{userId}")
    ApiResponse<UserResponse> getUserById(@PathVariable("userId") Long userId);
    
    /**
     * 사용자 이메일로 정보 조회
     */
    @GetMapping("/api/users/email/{email}")
    ApiResponse<UserResponse> getUserByEmail(@PathVariable("email") String email);
    
    /**
     * 여러 사용자 정보 일괄 조회
     */
    @PostMapping("/api/users/batch")
    ApiResponse<List<UserResponse>> getUsersByIds(@RequestBody List<Long> userIds);
    
    /**
     * 사용자 선호 카테고리 조회
     */
    @GetMapping("/api/users/{userId}/categories")
    ApiResponse<List<CategoryResponse>> getUserPreferences(@PathVariable("userId") Long userId);
    
    /**
     * 활성 사용자 목록 조회 (뉴스레터 발송용)
     */
    @GetMapping("/api/users/active")
    ApiResponse<List<UserResponse>> getActiveUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    );

    // ========================================
    // UserReadHistory 관련 메서드들 (새로 추가)
    // ========================================

    /**
     * 사용자가 읽은 뉴스 기록 조회 (페이징)
     * 실제 엔드포인트: /api/users/mypage/history/index
     */
    @GetMapping("/mypage/history/index")
    ApiResponse<Page<ReadHistoryResponse>> getReadHistory(
            @RequestParam("userId") Long userId,
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(value = "sort", defaultValue = "updatedAt,desc") String sort
    );

    /**
     * 사용자가 읽은 뉴스 ID 목록 조회
     * 실제 엔드포인트: /api/users/{userId}/read-news-ids
     */
    @GetMapping("/{userId}/read-news-ids")
    ApiResponse<List<Long>> getReadNewsIds(
            @PathVariable("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    );

    /**
     * 특정 뉴스를 읽었는지 확인
     * 실제 엔드포인트: /api/users/{userId}/read-news/{newsId}/exists
     */
    @GetMapping("/{userId}/read-news/{newsId}/exists")
    ApiResponse<Boolean> hasReadNews(
            @PathVariable("userId") Long userId,
            @PathVariable("newsId") Long newsId
    );

    /**
     * 뉴스 읽음 기록 추가
     * 실제 엔드포인트: /api/users/mypage/history/{newsId}
     */
    @PostMapping("/mypage/history/{newsId}")
    ApiResponse<String> addReadHistory(
            @RequestParam("userId") Long userId,
            @PathVariable("newsId") Long newsId
    );
}
