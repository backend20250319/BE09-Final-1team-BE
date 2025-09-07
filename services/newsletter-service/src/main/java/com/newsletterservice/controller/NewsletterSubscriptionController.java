package com.newsletterservice.controller;

import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.common.exception.NewsletterException;
import com.newsletterservice.entity.UserNewsletterSubscription;
import com.newsletterservice.repository.UserNewsletterSubscriptionRepository;
import com.newsletterservice.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/newsletter/subscription-management")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NewsletterSubscriptionController extends BaseController {

    private final UserNewsletterSubscriptionRepository subscriptionRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 뉴스레터 구독
     */
    @PostMapping("/subscribe")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> subscribe(
            @Valid @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = super.extractUserIdFromToken(httpRequest);
            String category = (String) request.get("category");
            
            log.info("뉴스레터 구독 요청: userId={}, category={}", userId, category);
            
            // 기존 구독 확인
            Optional<UserNewsletterSubscription> existing = subscriptionRepository.findByUserIdAndCategory(userId, category);
            
            if (existing.isPresent()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ALREADY_SUBSCRIBED", "이미 구독 중인 카테고리입니다."));
            }
            
            // 새 구독 생성
            UserNewsletterSubscription subscription = UserNewsletterSubscription.builder()
                    .userId(userId)
                    .category(category)
                    .isActive(true)
                    .build();
            
            subscriptionRepository.save(subscription);
            entityManager.flush();
            
            log.info("뉴스레터 구독 생성 완료: subscriptionId={}, userId={}, category={}, isActive={}", 
                    subscription.getId(), userId, category, subscription.getIsActive());
            
            Map<String, Object> response = new HashMap<>();
            response.put("subscriptionId", subscription.getId());
            response.put("category", category);
            response.put("isActive", true);
            response.put("subscribedAt", subscription.getSubscribedAt());
            response.put("updatedAt", subscription.getUpdatedAt());
            
            return ResponseEntity.ok(ApiResponse.success(response, "구독이 완료되었습니다."));
            
        } catch (Exception e) {
            log.error("구독 처리 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SUBSCRIPTION_ERROR", "구독 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 뉴스레터 구독 취소
     */
    @PostMapping("/unsubscribe")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> unsubscribe(
            @Valid @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = super.extractUserIdFromToken(httpRequest);
            String category = (String) request.get("category");
            
            log.info("뉴스레터 구독 취소 요청: userId={}, category={}", userId, category);
            
            Optional<UserNewsletterSubscription> subscription = subscriptionRepository.findByUserIdAndCategory(userId, category);
            
            if (subscription.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("SUBSCRIPTION_NOT_FOUND", "구독 정보를 찾을 수 없습니다."));
            }
            
            // Repository의 효율적인 업데이트 메서드 사용
            int updatedRows = subscriptionRepository.updateSubscriptionStatus(userId, category, false);
            
            if (updatedRows == 0) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("SUBSCRIPTION_NOT_FOUND", "구독 정보를 찾을 수 없습니다."));
            }
            
            log.info("뉴스레터 구독 취소 완료: userId={}, category={}, updatedRows={}", userId, category, updatedRows);
            
            Map<String, Object> response = new HashMap<>();
            response.put("category", category);
            response.put("isActive", false);
            
            return ResponseEntity.ok(ApiResponse.success(response, "구독이 취소되었습니다."));
            
        } catch (Exception e) {
            log.error("구독 취소 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("UNSUBSCRIBE_ERROR", "구독 취소 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 뉴스레터 구독 취소 (ID로)
     */
    @DeleteMapping("/{subscriptionId}")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> unsubscribeById(
            @PathVariable Long subscriptionId,
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = super.extractUserIdFromToken(httpRequest);
            
            log.info("뉴스레터 구독 취소 요청: userId={}, subscriptionId={}", userId, subscriptionId);
            
            Optional<UserNewsletterSubscription> subscription = subscriptionRepository.findById(subscriptionId);
            
            if (subscription.isEmpty() || !subscription.get().getUserId().equals(userId)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("SUBSCRIPTION_NOT_FOUND", "구독 정보를 찾을 수 없습니다."));
            }
            
            // Repository의 효율적인 업데이트 메서드 사용
            int updatedRows = subscriptionRepository.updateSubscriptionStatusById(subscriptionId, userId, false);
            
            if (updatedRows == 0) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("SUBSCRIPTION_NOT_FOUND", "구독 정보를 찾을 수 없습니다."));
            }
            
            log.info("뉴스레터 구독 취소 완료: userId={}, subscriptionId={}, updatedRows={}", userId, subscriptionId, updatedRows);
            
            Map<String, Object> response = new HashMap<>();
            response.put("subscriptionId", subscriptionId);
            response.put("isActive", false);
            
            return ResponseEntity.ok(ApiResponse.success(response, "구독이 취소되었습니다."));
            
        } catch (Exception e) {
            log.error("구독 취소 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("UNSUBSCRIBE_ERROR", "구독 취소 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 구독 상태 변경
     */
    @PutMapping("/{subscriptionId}/status")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSubscriptionStatus(
            @PathVariable Long subscriptionId,
            @Valid @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = super.extractUserIdFromToken(httpRequest);
            Boolean isActive = (Boolean) request.get("isActive");
            
            log.info("구독 상태 변경 요청: userId={}, subscriptionId={}, isActive={}", userId, subscriptionId, isActive);
            
            // 업데이트 전 구독 정보 로그
            Optional<UserNewsletterSubscription> beforeUpdate = subscriptionRepository.findById(subscriptionId);
            if (beforeUpdate.isPresent()) {
                log.info("상태 변경 전 구독 정보: subscriptionId={}, isActive={}, updatedAt={}", 
                        subscriptionId, beforeUpdate.get().getIsActive(), beforeUpdate.get().getUpdatedAt());
            }
            
            Optional<UserNewsletterSubscription> subscription = subscriptionRepository.findById(subscriptionId);
            
            if (subscription.isEmpty() || !subscription.get().getUserId().equals(userId)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("SUBSCRIPTION_NOT_FOUND", "구독 정보를 찾을 수 없습니다."));
            }
            
            // Repository의 효율적인 업데이트 메서드 사용
            int updatedRows = subscriptionRepository.updateSubscriptionStatusById(subscriptionId, userId, isActive);
            
            if (updatedRows == 0) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("SUBSCRIPTION_NOT_FOUND", "구독 정보를 찾을 수 없습니다."));
            }
            
            // 업데이트된 구독 정보 다시 조회
            Optional<UserNewsletterSubscription> updatedSubscription = subscriptionRepository.findById(subscriptionId);
            
            if (updatedSubscription.isPresent()) {
                log.info("상태 변경 후 구독 정보: subscriptionId={}, isActive={}, updatedAt={}", 
                        subscriptionId, updatedSubscription.get().getIsActive(), updatedSubscription.get().getUpdatedAt());
            }
            
            log.info("구독 상태 변경 완료: userId={}, subscriptionId={}, isActive={}, updatedRows={}", 
                    userId, subscriptionId, isActive, updatedRows);
            
            Map<String, Object> response = new HashMap<>();
            response.put("subscriptionId", subscriptionId);
            response.put("isActive", isActive);
            if (updatedSubscription.isPresent()) {
                response.put("updatedAt", updatedSubscription.get().getUpdatedAt());
            }
            
            return ResponseEntity.ok(ApiResponse.success(response, "구독 상태가 변경되었습니다."));
            
        } catch (Exception e) {
            log.error("구독 상태 변경 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("STATUS_UPDATE_ERROR", "구독 상태 변경 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 내 구독 목록 조회
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMySubscriptions(
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = super.extractUserIdFromToken(httpRequest);
            
            log.info("내 구독 목록 조회 요청: userId={}", userId);
            
            List<UserNewsletterSubscription> subscriptions = subscriptionRepository.findByUserId(userId);
            
            List<Map<String, Object>> result = subscriptions.stream()
                    .map(sub -> {
                        Map<String, Object> subMap = new HashMap<>();
                        subMap.put("subscriptionId", sub.getId());
                        subMap.put("category", sub.getCategory());
                        subMap.put("isActive", sub.getIsActive());
                        subMap.put("subscribedAt", sub.getSubscribedAt());
                        subMap.put("updatedAt", sub.getUpdatedAt());
                        return subMap;
                    })
                    .toList();
            
            return ResponseEntity.ok(ApiResponse.success(result, "구독 목록을 조회했습니다."));
            
        } catch (Exception e) {
            log.error("구독 목록 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SUBSCRIPTION_LIST_ERROR", "구독 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 활성 구독 목록 조회
     */
    @GetMapping("/list/active")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyActiveSubscriptions(
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = super.extractUserIdFromToken(httpRequest);
            
            log.info("활성 구독 목록 조회 요청: userId={}", userId);
            
            List<UserNewsletterSubscription> subscriptions = subscriptionRepository.findActiveSubscriptionsByUserId(userId);
            
            List<Map<String, Object>> result = subscriptions.stream()
                    .map(sub -> {
                        Map<String, Object> subMap = new HashMap<>();
                        subMap.put("subscriptionId", sub.getId());
                        subMap.put("category", sub.getCategory());
                        subMap.put("isActive", sub.getIsActive());
                        subMap.put("subscribedAt", sub.getSubscribedAt());
                        subMap.put("updatedAt", sub.getUpdatedAt());
                        return subMap;
                    })
                    .toList();
            
            return ResponseEntity.ok(ApiResponse.success(result, "활성 구독 목록을 조회했습니다."));
            
        } catch (Exception e) {
            log.error("활성 구독 목록 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ACTIVE_SUBSCRIPTION_LIST_ERROR", "구독 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 구독 정보 업데이트
     */
    @PutMapping("/{subscriptionId}")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSubscription(
            @PathVariable Long subscriptionId,
            @Valid @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = super.extractUserIdFromToken(httpRequest);
            
            log.info("구독 정보 업데이트 요청: userId={}, subscriptionId={}, request={}", userId, subscriptionId, request);
            
            // 업데이트 전 구독 정보 로그
            Optional<UserNewsletterSubscription> beforeUpdate = subscriptionRepository.findById(subscriptionId);
            if (beforeUpdate.isPresent()) {
                log.info("업데이트 전 구독 정보: subscriptionId={}, isActive={}, updatedAt={}", 
                        subscriptionId, beforeUpdate.get().getIsActive(), beforeUpdate.get().getUpdatedAt());
            }
            
            Optional<UserNewsletterSubscription> subscription = subscriptionRepository.findById(subscriptionId);
            
            if (subscription.isEmpty() || !subscription.get().getUserId().equals(userId)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("SUBSCRIPTION_NOT_FOUND", "구독 정보를 찾을 수 없습니다."));
            }
            
            // 업데이트 가능한 필드들
            if (request.containsKey("isActive")) {
                Boolean isActive = (Boolean) request.get("isActive");
                
                // Repository의 효율적인 업데이트 메서드 사용
                int updatedRows = subscriptionRepository.updateSubscriptionStatusById(subscriptionId, userId, isActive);
                
                if (updatedRows == 0) {
                    return ResponseEntity.badRequest()
                        .body(ApiResponse.error("SUBSCRIPTION_NOT_FOUND", "구독 정보를 찾을 수 없습니다."));
                }
                
                log.info("구독 정보 업데이트 완료: userId={}, subscriptionId={}, isActive={}, updatedRows={}", 
                        userId, subscriptionId, isActive, updatedRows);
                
                // 업데이트된 구독 정보 다시 조회
                subscription = subscriptionRepository.findById(subscriptionId);
                
                if (subscription.isPresent()) {
                    log.info("업데이트 후 구독 정보: subscriptionId={}, isActive={}, updatedAt={}", 
                            subscriptionId, subscription.get().getIsActive(), subscription.get().getUpdatedAt());
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("subscriptionId", subscriptionId);
            response.put("category", subscription.get().getCategory());
            response.put("isActive", subscription.get().getIsActive());
            response.put("updatedAt", subscription.get().getUpdatedAt());
            
            return ResponseEntity.ok(ApiResponse.success(response, "구독 정보가 업데이트되었습니다."));
            
        } catch (Exception e) {
            log.error("구독 정보 업데이트 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SUBSCRIPTION_UPDATE_ERROR", "구독 정보 업데이트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 구독 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSubscriptionStats(
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = super.extractUserIdFromToken(httpRequest);
            
            log.info("구독 통계 조회 요청: userId={}", userId);
            
            List<UserNewsletterSubscription> allSubscriptions = subscriptionRepository.findByUserId(userId);
            List<UserNewsletterSubscription> activeSubscriptions = subscriptionRepository.findActiveSubscriptionsByUserId(userId);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalSubscriptions", allSubscriptions.size());
            stats.put("activeSubscriptions", activeSubscriptions.size());
            stats.put("inactiveSubscriptions", allSubscriptions.size() - activeSubscriptions.size());
            
            return ResponseEntity.ok(ApiResponse.success(stats, "구독 통계를 조회했습니다."));
            
        } catch (Exception e) {
            log.error("구독 통계 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SUBSCRIPTION_STATS_ERROR", "통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 카테고리별 구독 통계 조회
     */
    @GetMapping("/stats/by-category")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSubscriptionStatsByCategory(
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = super.extractUserIdFromToken(httpRequest);
            
            log.info("카테고리별 구독 통계 조회 요청: userId={}", userId);
            
            List<UserNewsletterSubscription> subscriptions = subscriptionRepository.findByUserId(userId);
            
            Map<String, Long> categoryStats = new HashMap<>();
            for (UserNewsletterSubscription sub : subscriptions) {
                String category = sub.getCategory();
                categoryStats.put(category, categoryStats.getOrDefault(category, 0L) + 1);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("categoryStats", categoryStats);
            result.put("totalCategories", categoryStats.size());
            
            return ResponseEntity.ok(ApiResponse.success(result, "카테고리별 구독 통계를 조회했습니다."));
            
        } catch (Exception e) {
            log.error("카테고리별 구독 통계 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("CATEGORY_STATS_ERROR", "통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 구독 여부 확인
     */
    @GetMapping("/check/{category}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkSubscription(
            @PathVariable String category,
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = super.extractUserIdFromToken(httpRequest);
            
            log.info("구독 여부 확인 요청: userId={}, category={}", userId, category);
            
            Optional<UserNewsletterSubscription> subscription = subscriptionRepository.findByUserIdAndCategory(userId, category);
            
            Map<String, Object> result = new HashMap<>();
            result.put("category", category);
            result.put("isSubscribed", subscription.isPresent());
            
            if (subscription.isPresent()) {
                result.put("subscriptionId", subscription.get().getId());
                result.put("isActive", subscription.get().getIsActive());
                result.put("subscribedAt", subscription.get().getSubscribedAt());
            }
            
            return ResponseEntity.ok(ApiResponse.success(result, "구독 여부를 확인했습니다."));
            
        } catch (Exception e) {
            log.error("구독 여부 확인 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SUBSCRIPTION_CHECK_ERROR", "구독 여부 확인 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
