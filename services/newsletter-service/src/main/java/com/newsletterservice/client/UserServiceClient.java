// ========================================
// 1. 개선된 UserServiceClient
// ========================================
package com.newsletterservice.client;

import com.newsletterservice.client.dto.*;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.config.FeignTimeoutConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "user-service",
        url = "${user.base-url:http://localhost:8081}",
        contextId = "newsletterUserServiceClient",
        path = "/api/users",
        configuration = FeignTimeoutConfig.class
)
public interface UserServiceClient {
    
    // ========================================
    // 사용자 기본 정보
    // ========================================
    
    @GetMapping("/{userId}")      
    ApiResponse<UserResponse> getUserById(@PathVariable("userId") Long userId);
    
    @GetMapping("/email/{email}")
    ApiResponse<UserResponse> getUserByEmail(@PathVariable("email") String email);
    
    @PostMapping("/batch")        
    ApiResponse<List<UserResponse>> getUsersByIds(@RequestBody List<Long> userIds);
    
    @GetMapping("/active")
    ApiResponse<List<UserResponse>> getActiveUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    );

    @GetMapping("/in-app-notification-enabled")
    ApiResponse<List<Long>> getInAppNotificationEnabledUsers();

    @GetMapping("/{userId}/exists")
    ApiResponse<Boolean> userExists(@PathVariable("userId") Long userId);

    // ========================================
    // 사용자 선호도 및 관심사
    // ========================================
    
    @GetMapping("/{userId}/categories")
    ApiResponse<List<CategoryResponse>> getUserPreferences(@PathVariable("userId") Long userId);
    
    @GetMapping("/{userId}/interests")
    ApiResponse<UserInterestResponse> getUserInterests(@PathVariable("userId") Long userId);
    
    @GetMapping("/{userId}/behavior-analysis")
    ApiResponse<UserBehaviorAnalysis> getUserBehaviorAnalysis(@PathVariable("userId") Long userId);

    @GetMapping("/{userId}/optimal-newsletter-frequency")
    ApiResponse<String> getOptimalNewsletterFrequency(@PathVariable("userId") Long userId);

    // ========================================
    // 읽기 기록 관리
    // ========================================

    @GetMapping("/{userId}/read-news-ids")
    ApiResponse<List<Long>> getReadNewsIds(
            @PathVariable("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    );

    @GetMapping("/mypage/history/index")
    ApiResponse<Page<ReadHistoryResponse>> getReadHistory(
            @RequestParam("userId") Long userId,
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(value = "sort", defaultValue = "updatedAt,desc") String sort
    );

    @GetMapping("/{userId}/read-news/{newsId}/exists")
    ApiResponse<Boolean> hasReadNews(
            @PathVariable("userId") Long userId,
            @PathVariable("newsId") Long newsId
    );

    @PostMapping("/mypage/history/{newsId}")
    ApiResponse<String> addReadHistory(
            @RequestParam("userId") Long userId,
            @PathVariable("newsId") Long newsId
    );

    // ========================================
    // 웹 푸시 구독 관리
    // ========================================
    
    @GetMapping("/webpush/subscriptions")
    ApiResponse<List<PushSubscriptionResponse>> getWebPushSubscriptions();
    
    @GetMapping("/{userId}/webpush/subscription")
    ApiResponse<PushSubscriptionResponse> getUserWebPushSubscription(@PathVariable("userId") Long userId);
    
    @PostMapping("/{userId}/webpush/subscription")
    ApiResponse<String> registerWebPushSubscription(
            @PathVariable("userId") Long userId,
            @RequestBody PushSubscriptionRequest request
    );
    
    @DeleteMapping("/{userId}/webpush/subscription")
    ApiResponse<String> unregisterWebPushSubscription(@PathVariable("userId") Long userId);

    // ========================================
    // 개인화 정보 조회
    // ========================================
    
    @GetMapping("/{userId}/personalization-info")
    ApiResponse<Map<String, Object>> getPersonalizationInfo(@PathVariable("userId") Long userId);

    // ========================================
    // 이메일 뉴스레터 구독 관리
    // ========================================
    
    @GetMapping("/email-newsletter/subscribers")
    ApiResponse<List<String>> getEmailNewsletterSubscribers();
    
    @GetMapping("/{userId}/email")
    ApiResponse<String> getUserEmail(@PathVariable("userId") Long userId);
    
    @GetMapping("/email-newsletter/subscribers/count")
    ApiResponse<Long> getEmailNewsletterSubscriberCount();

    // ========================================
    // 카카오 연동 관리
    // ========================================
    
    @GetMapping("/{userId}/kakao/token")
    ApiResponse<String> getUserKakaoToken(@PathVariable("userId") Long userId);
    
    @GetMapping("/kakao/connected-users")
    ApiResponse<List<Long>> getKakaoConnectedUsers();
}
