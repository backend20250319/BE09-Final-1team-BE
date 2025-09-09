package com.newsletterservice.controller;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.common.exception.NewsletterException;
import com.newsletterservice.dto.*;
import com.newsletterservice.repository.NewsletterDeliveryRepository;
import com.newsletterservice.service.EmailNewsletterRenderer;
import com.newsletterservice.service.KakaoMessageService;
import com.newsletterservice.service.NewsletterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NewsletterController {

    // ========================================
    // Service Dependencies
    // ========================================
    private final NewsletterService newsletterService;
    private final EmailNewsletterRenderer emailRenderer;
    private final NewsletterDeliveryRepository deliveryRepository;
    private final KakaoMessageService kakaoMessageService;

    // ========================================
    // 1. 구독 관리 기능
    // ========================================

    /**
     * 뉴스레터 구독
     */
    @PostMapping("/subscribe")
    public ApiResponse<SubscriptionResponse> subscribe(
            @Valid @RequestBody SubscriptionRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            request.setUserId(Long.valueOf(userId));
            log.info("구독 요청: userId={}, email={}, frequency={}, categories={}", 
                    request.getUserId(), request.getEmail(), request.getFrequency(), request.getPreferredCategories());
            SubscriptionResponse subscription = newsletterService.subscribe(request, userId);
            return ApiResponse.success(subscription, "구독이 완료되었습니다.");
        } catch (Exception e) {
            log.error("구독 처리 중 오류 발생", e);
            return ApiResponse.error("SUBSCRIPTION_ERROR", "구독 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 구독 정보 조회
     */
<<<<<<< HEAD
    @GetMapping("/subscription/{id}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscription(
            @PathVariable Long id,
=======
    @GetMapping("/subscription/my")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMySubscriptions(
>>>>>>> develop
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("구독 정보 조회 요청 - userId: {}, subscriptionId: {}", userId, id);
            
            SubscriptionResponse subscription = newsletterService.getSubscription(id, Long.valueOf(userId));
            
            return ResponseEntity.ok(ApiResponse.success(subscription));
        } catch (NewsletterException e) {
            log.warn("구독 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("구독 정보 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SUBSCRIPTION_FETCH_ERROR", "구독 정보 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 내 구독 목록 조회
     */
    @GetMapping("/subscription/my")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getMySubscriptions(
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("내 구독 목록 조회 요청 - userId: {}", userId);
            
<<<<<<< HEAD
            List<SubscriptionResponse> subscriptions = newsletterService.getMySubscriptions(Long.valueOf(userId));
=======
            // 활성화된 구독 정보만 조회
            List<UserNewsletterSubscription> activeSubscriptions = subscriptionRepository.findActiveSubscriptionsByUserId(userId);
            
            // 카테고리 매핑
            Map<String, String> categoryNames = Map.of(
                "POLITICS", "정치",
                "ECONOMY", "경제", 
                "SOCIETY", "사회",
                "LIFE", "생활",
                "INTERNATIONAL", "세계",
                "IT_SCIENCE", "IT/과학",
                "VEHICLE", "자동차/교통",
                "TRAVEL_FOOD", "여행/음식",
                "ART", "예술"
            );
            
            List<Map<String, Object>> subscriptions = new ArrayList<>();
            List<String> preferredCategories = new ArrayList<>();
            
            // 활성화된 구독만 카드 형태로 반환
            for (UserNewsletterSubscription subscription : activeSubscriptions) {
                String categoryCode = subscription.getCategory();
                String categoryName = categoryNames.getOrDefault(categoryCode, categoryCode);
                
                Map<String, Object> subscriptionCard = new HashMap<>();
                subscriptionCard.put("id", subscription.getId());
                subscriptionCard.put("subscriptionId", subscription.getId());
                subscriptionCard.put("categoryId", categoryCode.hashCode());
                subscriptionCard.put("category", categoryName);
                subscriptionCard.put("categoryName", categoryCode);
                subscriptionCard.put("categoryNameKo", categoryName);
                subscriptionCard.put("isActive", subscription.getIsActive());
                subscriptionCard.put("subscribedAt", subscription.getSubscribedAt().toString());
                subscriptionCard.put("updatedAt", subscription.getUpdatedAt() != null ? subscription.getUpdatedAt().toString() : null);
                
                // 구독자 수 조회 (fallback 처리)
                Long subscriberCount = getSubscriberCountWithFallback(categoryCode);
                subscriptionCard.put("subscriberCount", subscriberCount);
                
                subscriptions.add(subscriptionCard);
                preferredCategories.add(categoryCode);
            }
            
            // 응답 데이터 구성
            Map<String, Object> result = new HashMap<>();
            result.put("count", subscriptions.size());
            result.put("subscriptions", subscriptions);
            result.put("preferredCategories", preferredCategories);
            result.put("userId", userId);
            result.put("timestamp", LocalDateTime.now().toString());
            
            log.info("활성 구독 목록 조회 완료: userId={}, count={}", userId, subscriptions.size());
            return ResponseEntity.ok(ApiResponse.success(result, "구독 목록 조회가 완료되었습니다."));
>>>>>>> develop
            
            return ResponseEntity.ok(ApiResponse.success(subscriptions));
        } catch (Exception e) {
            log.error("내 구독 목록 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SUBSCRIPTION_LIST_ERROR", "구독 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 구독 해지
     */
    @DeleteMapping("/subscription/{id}")
    public ResponseEntity<ApiResponse<String>> unsubscribe(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("구독 해지 요청 - userId: {}, subscriptionId: {}", userId, id);
            
            newsletterService.unsubscribe(id, Long.valueOf(userId));
            
            return ResponseEntity.ok(ApiResponse.success("구독이 해지되었습니다."));
        } catch (NewsletterException e) {
            log.warn("구독 해지 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("구독 해지 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("UNSUBSCRIBE_ERROR", "구독 해지 중 오류가 발생했습니다."));
        }
    }

    /**
     * 활성 구독 목록 조회
     */
    @GetMapping("/subscription/active")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getActiveSubscriptions(
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("활성 구독 목록 조회 요청 - userId: {}", userId);
            
            List<SubscriptionResponse> subscriptions = newsletterService.getActiveSubscriptions(Long.valueOf(userId));
            
            return ResponseEntity.ok(ApiResponse.success(subscriptions));
        } catch (Exception e) {
            log.error("활성 구독 목록 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ACTIVE_SUBSCRIPTION_ERROR", "활성 구독 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 구독 상태 변경
     */
    @PutMapping("/subscription/{subscriptionId}/status")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> changeSubscriptionStatus(
            @PathVariable Long subscriptionId,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            String newStatus = request.get("status");
            
<<<<<<< HEAD
            if (newStatus == null || newStatus.trim().isEmpty()) {
=======
            // isActive가 null인 경우 기본값으로 true 설정 (구독 요청의 경우)
            final Boolean finalIsActive = (isActive == null) ? true : isActive;
            if (isActive == null) {
                log.info("isActive 값이 null이므로 기본값 true로 설정");
            }
            
            log.info("구독 상태 변경 시작: userId={}, category={}, isActive={}", userId, category, finalIsActive);
            
            // 입력값 검증
            if (category == null || category.trim().isEmpty()) {
                log.warn("카테고리가 비어있습니다: category={}", category);
>>>>>>> develop
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_STATUS", "상태값이 필요합니다."));
            }
            
<<<<<<< HEAD
            log.info("구독 상태 변경 요청 - userId: {}, subscriptionId: {}, newStatus: {}", 
                    userId, subscriptionId, newStatus);
            
            SubscriptionResponse subscription = newsletterService.changeSubscriptionStatus(
                    subscriptionId, Long.valueOf(userId), newStatus);
=======
            // 타임아웃 체크를 위한 CompletableFuture 사용
            CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // 기존 구독 정보 확인 (다중 구독 지원)
                    List<UserNewsletterSubscription> existing = subscriptionRepository.findAllByUserIdAndCategory(userId, category);
                    
                    if (!existing.isEmpty()) {
                        // 기존 구독 정보 업데이트 (카테고리별 모든 구독)
                        int updatedRows = subscriptionRepository.updateSubscriptionStatus(userId, category, finalIsActive);
                        log.info("구독 상태 업데이트 완료: userId={}, category={}, isActive={}, updatedRows={}", 
                                userId, category, finalIsActive, updatedRows);
                    } else {
                        // 새로운 구독 정보 생성
                        UserNewsletterSubscription newSubscription = UserNewsletterSubscription.builder()
                            .userId(userId)
                            .category(category)
                            .isActive(finalIsActive)
                            .subscribedAt(LocalDateTime.now())
                            .build();
                        subscriptionRepository.save(newSubscription);
                        log.info("새 구독 정보 생성 완료: userId={}, category={}, isActive={}", userId, category, finalIsActive);
                    }
                    
                    // 업데이트된 구독자 수 조회 (fallback 처리)
                    Long updatedSubscriberCount = getSubscriberCountWithFallback(category);
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("message", finalIsActive ? "구독이 활성화되었습니다." : "구독이 비활성화되었습니다.");
                    result.put("category", category);
                    result.put("subscriberCount", updatedSubscriberCount);
                    result.put("isFallback", false);
                    
                    return result;
                    
                } catch (Exception e) {
                    log.error("구독 상태 변경 처리 중 오류: {}", e.getMessage());
                    throw new RuntimeException(e);
                }
            });
>>>>>>> develop
            
            return ResponseEntity.ok(ApiResponse.success(subscription, "구독 상태가 변경되었습니다."));
        } catch (NewsletterException e) {
            log.warn("구독 상태 변경 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
<<<<<<< HEAD
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
=======
                .body(ApiResponse.error("SUBSCRIPTION_STATS_ERROR", "구독 통계 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 테스트용 구독 데이터 초기화 (개발/테스트용)
     */
    @PostMapping("/subscription/init-test-data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initTestSubscriptionData(
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = super.extractUserIdFromToken(httpRequest);
            log.info("테스트 구독 데이터 초기화 요청 - userId: {}", userId);
            
            // 기본 카테고리들에 대한 구독 정보 생성 (2-3개만 활성화)
            String[] categories = {"POLITICS", "ECONOMY", "SOCIETY", "LIFE", "INTERNATIONAL", "IT_SCIENCE"};
            int createdCount = 0;
            
            for (int i = 0; i < Math.min(3, categories.length); i++) {
                String category = categories[i];
                
                // 다중 구독 허용하므로 항상 새 구독 생성
                UserNewsletterSubscription subscription = UserNewsletterSubscription.builder()
                    .userId(userId)
                    .category(category)
                    .isActive(true)
                    .subscribedAt(LocalDateTime.now())
                    .build();
                
                subscriptionRepository.save(subscription);
                createdCount++;
                log.info("테스트 구독 데이터 생성: userId={}, category={}", userId, category);
            }
            
            // 생성된 구독 정보 조회
            List<UserNewsletterSubscription> activeSubscriptions = subscriptionRepository.findActiveSubscriptionsByUserId(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "테스트 구독 데이터가 초기화되었습니다.");
            result.put("createdCount", createdCount);
            result.put("totalActiveSubscriptions", activeSubscriptions.size());
            result.put("subscriptions", activeSubscriptions.stream()
                .map(sub -> {
                    Map<String, Object> subInfo = new HashMap<>();
                    subInfo.put("category", sub.getCategory());
                    subInfo.put("isActive", sub.getIsActive());
                    subInfo.put("subscribedAt", sub.getSubscribedAt().toString());
                    return subInfo;
                })
                .toList());
            
            return ResponseEntity.ok(ApiResponse.success(result, "테스트 구독 데이터가 초기화되었습니다."));
            
>>>>>>> develop
        } catch (Exception e) {
            log.error("구독 상태 변경 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("STATUS_CHANGE_ERROR", "구독 상태 변경 중 오류가 발생했습니다."));
        }
    }

    /**
     * 카테고리별 헤드라인 조회 - 인증 불필요
     */
    @GetMapping("/category/{category}/headlines")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategoryHeadlines(
            @PathVariable String category,
            @RequestParam(defaultValue = "5") int limit) {
        
        try {
            log.info("카테고리별 헤드라인 조회 요청 - category: {}, limit: {}", category, limit);
            
            List<NewsletterContent.Article> headlines = newsletterService.getCategoryHeadlines(category, limit);
            List<Map<String, Object>> result = headlines.stream()
                    .map(article -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", article.getId());
                        map.put("title", article.getTitle() != null ? article.getTitle() : "");
                        map.put("summary", article.getSummary() != null ? article.getSummary() : "");
                        map.put("url", article.getUrl() != null ? article.getUrl() : "");
                        map.put("publishedAt", article.getPublishedAt() != null ? article.getPublishedAt() : "");
                        map.put("category", article.getCategory() != null ? article.getCategory() : "");
                        return map;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("카테고리별 헤드라인 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("CATEGORY_HEADLINES_ERROR", "카테고리별 헤드라인 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 카테고리별 기사 조회 (뉴스레터 카드용) - 인증 불필요
     */
    @GetMapping("/category/{category}/articles")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCategoryArticles(
            @PathVariable String category,
            @RequestParam(defaultValue = "5") int limit) {
        
        try {
            log.info("카테고리별 기사 조회 요청 - category: {}, limit: {}", category, limit);
            
            Map<String, Object> result = newsletterService.getCategoryArticlesWithTrendingKeywords(category, limit);
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("카테고리별 기사 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("CATEGORY_ARTICLES_ERROR", "카테고리별 기사 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 트렌드 키워드 조회 - 인증 불필요
     */
    @GetMapping("/trending-keywords")
    public ResponseEntity<ApiResponse<List<String>>> getTrendingKeywords(
            @RequestParam(defaultValue = "10") int limit) {

        try {
            log.info("트렌드 키워드 조회 요청 - limit: {}", limit);

            List<String> keywords = newsletterService.getTrendingKeywords(limit);

            return ResponseEntity.ok(ApiResponse.success(keywords));
        } catch (Exception e) {
            log.error("트렌드 키워드 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRENDING_KEYWORDS_ERROR", "트렌드 키워드 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 카테고리별 트렌드 키워드 조회 - 인증 불필요
     */
    @GetMapping("/category/{category}/trending-keywords")
    public ResponseEntity<ApiResponse<List<String>>> getCategoryTrendingKeywords(
            @PathVariable String category,
            @RequestParam(defaultValue = "8") int limit) {

        try {
            log.info("카테고리별 트렌드 키워드 조회 요청 - category: {}, limit: {}", category, limit);

            List<String> keywords = newsletterService.getTrendingKeywordsByCategory(category, limit);

            return ResponseEntity.ok(ApiResponse.success(keywords));
        } catch (Exception e) {
            log.error("카테고리별 트렌드 키워드 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("CATEGORY_KEYWORDS_ERROR", "카테고리별 트렌드 키워드 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 카테고리별 구독자 수 조회 - 인증 불필요
     */
    @GetMapping("/category/{category}/subscribers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCategorySubscriberCount(
            @PathVariable String category) {

        try {
            log.info("카테고리별 구독자 수 조회 요청 - category: {}", category);

            Map<String, Object> result = newsletterService.getCategorySubscriberStats(category);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("카테고리별 구독자 수 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("CATEGORY_SUBSCRIBERS_ERROR", "카테고리별 구독자 수 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 전체 카테고리별 구독자 수 조회 - 인증 불필요
     */
    @GetMapping("/categories/subscribers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllCategoriesSubscriberCount() {

        try {
            log.info("전체 카테고리별 구독자 수 조회 요청");

            Map<String, Object> result = newsletterService.getAllCategoriesSubscriberStats();

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("전체 카테고리별 구독자 수 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ALL_CATEGORIES_SUBSCRIBERS_ERROR", "전체 카테고리별 구독자 수 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 전체 구독자 통계 조회 - 인증 불필요
     */
    @GetMapping("/stats/subscribers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSubscriberStats(
            @RequestParam(required = false) String category) {

        try {
            log.info("구독자 통계 조회 요청: { category: {} }", category);

            Map<String, Object> result;
            if (category != null && !category.trim().isEmpty()) {
                // 특정 카테고리 구독자 통계
                result = newsletterService.getCategorySubscriberStats(category);
            } else {
                // 전체 카테고리 구독자 통계
                result = newsletterService.getAllCategoriesSubscriberStats();
            }

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("구독자 통계 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("SUBSCRIBER_STATS_ERROR", "구독자 통계 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 개인화된 뉴스레터 콘텐츠 조회 (JSON)
     */
    @GetMapping("/{newsletterId}/content")
    public ResponseEntity<ApiResponse<NewsletterContent>> getNewsletterContent(
            @PathVariable Long newsletterId,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("퍼스널라이즈드 뉴스레터 콘텐츠 조회 - userId: {}, newsletterId: {}", userId, newsletterId);
            
            NewsletterContent content = newsletterService.buildPersonalizedContent(Long.valueOf(userId), newsletterId);
            return ResponseEntity.ok(ApiResponse.success(content));
        } catch (NewsletterException e) {
            log.warn("뉴스레터 콘텐츠 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("뉴스레터 콘텐츠 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("CONTENT_FETCH_ERROR", "뉴스레터 콘텐츠 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 개인화된 뉴스레터 HTML 조회 (이메일용)
     */
    @GetMapping("/{newsletterId}/html")
    public ResponseEntity<String> getNewsletterHtml(
            @PathVariable Long newsletterId,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("퍼스널라이즈드 뉴스레터 HTML 조회 - userId: {}, newsletterId: {}", userId, newsletterId);
            
            NewsletterContent content = newsletterService.buildPersonalizedContent(Long.valueOf(userId), newsletterId);
            String htmlContent = emailRenderer.renderToHtml(content);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(htmlContent);
        } catch (NewsletterException e) {
            log.warn("뉴스레터 HTML 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body("<html><body><h1>오류</h1><p>" + e.getMessage() + "</p></body></html>");
        } catch (Exception e) {
            log.error("뉴스레터 HTML 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body("<html><body><h1>오류</h1><p>뉴스레터 HTML 조회 중 오류가 발생했습니다.</p></body></html>");
        }
    }

    /**
     * 개인화된 뉴스레터 미리보기 (HTML)
     */
    @GetMapping("/{newsletterId}/preview")
    public ResponseEntity<String> getNewsletterPreview(
            @PathVariable Long newsletterId,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("퍼스널라이즈드 뉴스레터 미리보기 - userId: {}, newsletterId: {}", userId, newsletterId);
            
            NewsletterContent content = newsletterService.buildPersonalizedContent(Long.valueOf(userId), newsletterId);
            String previewHtml = emailRenderer.renderToPreviewHtml(content);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(previewHtml);
        } catch (NewsletterException e) {
            log.warn("뉴스레터 미리보기 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body("<html><body><h1>오류</h1><p>" + e.getMessage() + "</p></body></html>");
        } catch (Exception e) {
            log.error("뉴스레터 미리보기 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body("<html><body><h1>오류</h1><p>뉴스레터 미리보기 중 오류가 발생했습니다.</p></body></html>");
        }
    }

    /**
     * 카테고리별 구독자 수 동기화 (관리자용)
     */
    @PostMapping("/admin/sync-category-subscribers")
    public ResponseEntity<ApiResponse<String>> syncCategorySubscriberCounts() {
        try {
            log.info("카테고리별 구독자 수 동기화 요청");
            
            newsletterService.syncCategorySubscriberCounts();
            
            return ResponseEntity.ok(ApiResponse.success("카테고리별 구독자 수 동기화가 완료되었습니다."));
        } catch (Exception e) {
            log.error("카테고리별 구독자 수 동기화 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("SYNC_ERROR", "카테고리별 구독자 수 동기화 중 오류가 발생했습니다."));
        }
    }

    /**
     * 구독 재활성화
     */
    @PutMapping("/subscription/{id}/reactivate")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> reactivateSubscription(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("구독 재활성화 요청 - userId: {}, subscriptionId: {}", userId, id);
            
            SubscriptionResponse subscription = newsletterService.reactivateSubscription(id, Long.valueOf(userId));
            
            return ResponseEntity.ok(ApiResponse.success(subscription, "구독이 재활성화되었습니다."));
        } catch (NewsletterException e) {
            log.warn("구독 재활성화 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("구독 재활성화 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("REACTIVATE_ERROR", "구독 재활성화 중 오류가 발생했습니다."));
        }
    }

    // ========================================
    // 2. 발송 관리 기능
    // ========================================

    /**
     * 뉴스레터 즉시 발송
     */
    @PostMapping("/delivery/send-now")
    public ResponseEntity<ApiResponse<DeliveryStats>> sendNewsletterNow(
            @Valid @RequestBody NewsletterDeliveryRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("뉴스레터 즉시 발송 요청 - userId: {}, newsletterId: {}, targetUserIds: {}, deliveryMethod: {}", 
                    userId, request.getNewsletterId(), request.getTargetUserIds(), request.getDeliveryMethod());
            
            DeliveryStats stats = newsletterService.sendNewsletterNow(request, Long.valueOf(userId));
            
            return ResponseEntity.ok(ApiResponse.success(stats, "뉴스레터 발송이 시작되었습니다."));
        } catch (NewsletterException e) {
            log.warn("뉴스레터 발송 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("뉴스레터 발송 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("DELIVERY_ERROR", "뉴스레터 발송 중 오류가 발생했습니다."));
        }
    }

    /**
     * 뉴스레터 예약 발송
     */
    @PostMapping("/delivery/schedule")
    public ResponseEntity<ApiResponse<DeliveryStats>> scheduleNewsletter(
            @Valid @RequestBody NewsletterDeliveryRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("뉴스레터 예약 발송 요청 - userId: {}, newsletterId: {}, targetUserIds: {}, deliveryMethod: {}", 
                    userId, request.getNewsletterId(), request.getTargetUserIds(), request.getDeliveryMethod());
            
            DeliveryStats stats = newsletterService.scheduleNewsletter(request, Long.valueOf(userId));
            
            return ResponseEntity.ok(ApiResponse.success(stats, "뉴스레터가 예약되었습니다."));
        } catch (NewsletterException e) {
            log.warn("뉴스레터 예약 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("뉴스레터 예약 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SCHEDULE_ERROR", "뉴스레터 예약 중 오류가 발생했습니다."));
        }
    }

    /**
     * 발송 취소
     */
    @PutMapping("/delivery/{deliveryId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelDelivery(
            @PathVariable Long deliveryId,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("발송 취소 요청 - userId: {}, deliveryId: {}", userId, deliveryId);
            
            newsletterService.cancelDelivery(deliveryId, Long.valueOf(userId));
            
            return ResponseEntity.ok(ApiResponse.success("발송이 취소되었습니다."));
        } catch (NewsletterException e) {
            log.warn("발송 취소 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("발송 취소 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("CANCEL_ERROR", "발송 취소 중 오류가 발생했습니다."));
        }
    }

    /**
     * 발송 재시도
     */
    @PutMapping("/delivery/{deliveryId}/retry")
    public ResponseEntity<ApiResponse<String>> retryDelivery(
        @PathVariable Long deliveryId,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("발송 재시도 요청 - userId: {}, deliveryId: {}", userId, deliveryId);
            
            newsletterService.retryDelivery(deliveryId, Long.valueOf(userId));
            
            return ResponseEntity.ok(ApiResponse.success("발송 재시도가 시작되었습니다."));
        } catch (NewsletterException e) {
            log.warn("발송 재시도 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("발송 재시도 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("RETRY_ERROR", "발송 재시도 중 오류가 발생했습니다."));
        }
    }

    /**
     * 공유 통계 기록
     */
    @PostMapping("/share")
    public ResponseEntity<ApiResponse<ShareStatsResponse>> recordShareStats(
            @RequestBody ShareStatsRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("공유 통계 기록 요청 - userId: {}, type: {}, newsId: {}, category: {}", 
                    userId, request.getType(), request.getNewsId(), request.getCategory());
            
            ShareStatsResponse response = newsletterService.recordShareStats(request, userId);
            
            return ResponseEntity.ok(ApiResponse.success(response, "공유 통계가 기록되었습니다."));
        } catch (NewsletterException e) {
            log.warn("공유 통계 기록 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("공유 통계 기록 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SHARE_STATS_ERROR", "공유 통계 기록 중 오류가 발생했습니다."));
        }
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    private String extractUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.info("Authorization 헤더에서 토큰 추출: {}", token);
            // TODO: 실제 JWT 토큰 파싱 로직 구
            // 현재는 임시로 토큰이 있으면 userId 1을 반환
            return "1";
        }
        // Authorization 헤더가 없으면 기본값 반환
        log.warn("Authorization 헤더가 없습니다. 기본 userId 사용");
        return "1";
    }

    /**
     * 인증 정보에서 사용자 ID 추출
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new NewsletterException("인증 정보가 없습니다.", "AUTHENTICATION_REQUIRED");
        }
        
        // 실제 구현에서는 JWT 토큰에서 userId를 추출해야 함
        // 여기서는 임시로 1L을 반환
        return 1L;
    }

    /**
     * 프론트엔드 카테고리명을 백엔드 카테고리명으로 변환
     */
    private String convertToBackendCategory(String frontendCategory) {
        if (frontendCategory == null || frontendCategory.trim().isEmpty()) {
            log.warn("프론트엔드 카테고리가 null이거나 비어있습니다: {}", frontendCategory);
            return "POLITICS"; // 기본값
        }
        
        String normalizedCategory = frontendCategory.trim();
        
        return switch (normalizedCategory) {
            case "정치" -> "POLITICS";
            case "경제" -> "ECONOMY";
            case "사회" -> "SOCIETY";
            case "생활" -> "LIFE";
            case "세계" -> "INTERNATIONAL";
            case "IT/과학" -> "IT_SCIENCE";
            case "자동차/교통" -> "VEHICLE";
            case "여행/음식" -> "TRAVEL_FOOD";
            case "예술" -> "ART";
            // 이미 영어인 경우 대소문자 정규화
            case "politics", "POLITICS" -> "POLITICS";
            case "economy", "ECONOMY" -> "ECONOMY";
            case "society", "SOCIETY" -> "SOCIETY";
            case "life", "LIFE" -> "LIFE";
            case "international", "INTERNATIONAL" -> "INTERNATIONAL";
            case "it_science", "IT_SCIENCE" -> "IT_SCIENCE";
            case "vehicle", "VEHICLE" -> "VEHICLE";
            case "travel_food", "TRAVEL_FOOD" -> "TRAVEL_FOOD";
            case "art", "ART" -> "ART";
            default -> {
                log.warn("알 수 없는 프론트엔드 카테고리: {}. 기본값 POLITICS 사용", normalizedCategory);
                yield "POLITICS";
            }
        };
    }

    /**
     * 카카오톡 뉴스레터 메시지 전송
     */
    @PostMapping("/{newsletterId}/send-kakao")
    public ResponseEntity<ApiResponse<String>> sendKakaoMessage(
            @PathVariable Long newsletterId,
            HttpServletRequest httpRequest) {

        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("카카오톡 뉴스레터 메시지 전송 요청: userId={}, newsletterId={}", userId, newsletterId);

            NewsletterContent content = newsletterService.buildPersonalizedContent(Long.valueOf(userId), newsletterId);
            kakaoMessageService.sendNewsletterMessage(content);

            return ResponseEntity.ok(ApiResponse.success("카카오톡 메시지가 전송되었습니다."));

        } catch (NewsletterException e) {
            log.warn("카카오톡 메시지 전송 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("카카오톡 메시지 전송 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("KAKAO_SEND_ERROR", "카카오톡 메시지 전송에 실패했습니다."));
        }
    }
<<<<<<< HEAD
=======

    // ========================================
    // ID 검증 및 오류 처리 헬퍼 메서드
    // ========================================



    /**
     * 간단한 개인화 뉴스레터 생성
     */
    @GetMapping("/generate/{userId}")
    public ResponseEntity<String> generatePersonalizedNewsletter(@PathVariable String userId) {
        try {
            log.info("개인화 뉴스레터 생성 요청: userId={}", userId);
            
            String htmlContent = newsletterService.generatePersonalizedNewsletter(userId);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(htmlContent);
            
        } catch (Exception e) {
            log.error("개인화 뉴스레터 생성 실패: userId={}", userId, e);
            return ResponseEntity.status(500)
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body("<html><body><h1>오류 발생</h1><p>뉴스레터 생성 중 오류가 발생했습니다.</p></body></html>");
        }
    }

    /**
     * 뉴스레터 생성 테스트 (디버깅용)
     */
    @GetMapping("/test-generation/{userId}")
    public ApiResponse<Map<String, Object>> testNewsletterGeneration(@PathVariable Long userId) {
        try {
            log.info("뉴스레터 생성 테스트 요청: userId={}", userId);
            
            Map<String, Object> result = newsletterService.testNewsletterGeneration(userId);
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("뉴스레터 생성 테스트 실패: userId={}", userId, e);
            return ApiResponse.error("뉴스레터 생성 테스트 중 오류가 발생했습니다.", "TEST_ERROR");
        }
    }

    // ========================================
    // 10. 이메일 뉴스레터 전송 기능
    // ========================================

    /**
     * 이메일 뉴스레터 전송
     */
    @PostMapping("/email/send")
    public ApiResponse<String> sendEmailNewsletter(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdAsString(httpRequest);
            log.info("이메일 뉴스레터 전송 요청: userId={}", userId);
            
            // 뉴스레터 콘텐츠 생성 (기본 뉴스레터)
            NewsletterContent content = newsletterService.buildPersonalizedContent(Long.valueOf(userId), 1L);
            
            // 이메일 전송
            newsletterService.sendEmailNewsletter(content);
            
            return ApiResponse.success("이메일 뉴스레터가 전송되었습니다.");
            
        } catch (Exception e) {
            log.error("이메일 뉴스레터 전송 실패", e);
            return ApiResponse.error("이메일 뉴스레터 전송 중 오류가 발생했습니다.", "EMAIL_SEND_ERROR");
        }
    }

    /**
     * 개인화된 이메일 뉴스레터 전송
     */
    @PostMapping("/email/send-personalized/{userId}")
    public ApiResponse<String> sendPersonalizedEmailNewsletter(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Long newsletterId,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("개인화된 이메일 뉴스레터 전송 요청: userId={}, newsletterId={}", userId, newsletterId);
            
            // 개인화된 이메일 전송
            newsletterService.sendPersonalizedEmailNewsletter(userId, newsletterId);
            
            return ApiResponse.success("개인화된 이메일 뉴스레터가 전송되었습니다.");
            
        } catch (Exception e) {
            log.error("개인화된 이메일 뉴스레터 전송 실패: userId={}", userId, e);
            return ApiResponse.error("개인화된 이메일 뉴스레터 전송 중 오류가 발생했습니다.", "PERSONALIZED_EMAIL_SEND_ERROR");
        }
    }

    /**
     * 테스트 이메일 전송
     */
    @PostMapping("/email/test")
    public ApiResponse<String> sendTestEmail(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        try {
            String to = request.get("to");
            String subject = request.getOrDefault("subject", "테스트 이메일");
            String content = request.getOrDefault("content", "이것은 테스트 이메일입니다.");
            
            if (to == null || to.trim().isEmpty()) {
                return ApiResponse.error("수신자 이메일 주소가 필요합니다.", "INVALID_EMAIL");
            }
            
            log.info("테스트 이메일 전송 요청: to={}, subject={}", to, subject);
            
            // 테스트 이메일 전송
            newsletterService.sendTestEmail(to, subject, content);
            
            return ApiResponse.success("테스트 이메일이 전송되었습니다.");
            
        } catch (Exception e) {
            log.error("테스트 이메일 전송 실패", e);
            return ApiResponse.error("테스트 이메일 전송 중 오류가 발생했습니다.", "TEST_EMAIL_SEND_ERROR");
        }
    }

    /**
     * 이메일 구독자 목록 조회 (관리자용)
     */
    @GetMapping("/email/subscribers")
    public ApiResponse<List<String>> getEmailSubscribers(HttpServletRequest httpRequest) {
        try {
            String userId = extractUserIdAsString(httpRequest);
            log.info("이메일 구독자 목록 조회 요청: userId={}", userId);
            
            // TODO: 관리자 권한 확인 로직 추가
            
            List<String> subscribers = newsletterService.getEmailNewsletterSubscribers();
            
            return ApiResponse.success(subscribers);
            
        } catch (Exception e) {
            log.error("이메일 구독자 목록 조회 실패", e);
            return ApiResponse.error("이메일 구독자 목록 조회 중 오류가 발생했습니다.", "SUBSCRIBER_LIST_ERROR");
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * 이메일 구독자 목록 조회 (NewsletterService에서 호출)
     */
    private List<String> getEmailNewsletterSubscribers() {
        try {
            return newsletterService.getEmailNewsletterSubscribers();
        } catch (Exception e) {
            log.error("이메일 구독자 목록 조회 실패", e);
            return new ArrayList<>();
        }
    }

    /**
     * 카테고리 설명 반환 헬퍼 메서드
     */
    private String getCategoryDescription(String categoryCode) {
        return switch (categoryCode) {
            case "POLITICS" -> "정치, 선거, 정책 관련 뉴스";
            case "ECONOMY" -> "경제, 금융, 증시 관련 뉴스";
            case "SOCIETY" -> "사회, 문화, 교육 관련 뉴스";
            case "LIFE" -> "생활, 건강, 라이프스타일 관련 뉴스";
            case "INTERNATIONAL" -> "해외, 국제정치, 글로벌 이슈";
            case "IT_SCIENCE" -> "IT, 과학, 기술 관련 뉴스";
            case "VEHICLE" -> "자동차, 교통, 모빌리티 관련 뉴스";
            case "TRAVEL_FOOD" -> "여행, 맛집, 레저 관련 뉴스";
            case "ART" -> "예술, 문화, 엔터테인먼트 관련 뉴스";
            default -> "기타 뉴스";
        };
    }
    
    // ========================================
    // 피드 B형 뉴스레터 관련 엔드포인트
    // ========================================
    
    /**
     * 피드 B형 개인화 뉴스레터 전송
     */
    @PostMapping("/send/feed-b/personalized/{userId}")
    public ResponseEntity<ApiResponse<Object>> sendPersonalizedFeedBNewsletter(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        try {
            log.info("피드 B형 개인화 뉴스레터 전송 요청: userId={}", userId);
            
            // 액세스 토큰 추출
            String accessToken = extractAccessToken(authorization);
            if (accessToken == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("액세스 토큰이 필요합니다.", "MISSING_ACCESS_TOKEN"));
            }
            
            // 피드 B형 뉴스레터 전송
            newsletterService.sendPersonalizedFeedBNewsletter(userId, accessToken);
            
            return ResponseEntity.ok(ApiResponse.success("피드 B형 개인화 뉴스레터 전송 완료"));
            
        } catch (Exception e) {
            log.error("피드 B형 개인화 뉴스레터 전송 실패: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("피드 B형 개인화 뉴스레터 전송 실패: " + e.getMessage(), "SEND_ERROR"));
        }
    }
    
    /**
     * 피드 B형 카테고리별 뉴스레터 전송
     */
    @PostMapping("/send/feed-b/category/{category}")
    public ResponseEntity<ApiResponse<Object>> sendCategoryFeedBNewsletter(
            @PathVariable String category,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        try {
            log.info("피드 B형 카테고리별 뉴스레터 전송 요청: category={}", category);
            
            // 액세스 토큰 추출
            String accessToken = extractAccessToken(authorization);
            if (accessToken == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("액세스 토큰이 필요합니다.", "MISSING_ACCESS_TOKEN"));
            }
            
            // 피드 B형 뉴스레터 전송
            newsletterService.sendCategoryFeedBNewsletter(category, accessToken);
            
            return ResponseEntity.ok(ApiResponse.success("피드 B형 카테고리별 뉴스레터 전송 완료"));
            
        } catch (Exception e) {
            log.error("피드 B형 카테고리별 뉴스레터 전송 실패: category={}", category, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("피드 B형 카테고리별 뉴스레터 전송 실패: " + e.getMessage(), "SEND_ERROR"));
        }
    }
    
    /**
     * 피드 B형 트렌딩 뉴스레터 전송
     */
    @PostMapping("/send/feed-b/trending")
    public ResponseEntity<ApiResponse<Object>> sendTrendingFeedBNewsletter(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        try {
            log.info("피드 B형 트렌딩 뉴스레터 전송 요청");
            
            // 액세스 토큰 추출
            String accessToken = extractAccessToken(authorization);
            if (accessToken == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("액세스 토큰이 필요합니다.", "MISSING_ACCESS_TOKEN"));
            }
            
            // 피드 B형 뉴스레터 전송
            newsletterService.sendTrendingFeedBNewsletter(accessToken);
            
            return ResponseEntity.ok(ApiResponse.success("피드 B형 트렌딩 뉴스레터 전송 완료"));
            
        } catch (Exception e) {
            log.error("피드 B형 트렌딩 뉴스레터 전송 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("피드 B형 트렌딩 뉴스레터 전송 실패: " + e.getMessage(), "SEND_ERROR"));
        }
    }
    
    
    /**
     * 디버깅용: 사용자 구독 상태 상세 조회
     */
    @GetMapping("/debug/subscriptions/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugUserSubscriptions(@PathVariable Long userId) {
        try {
            log.info("디버깅: 사용자 {} 구독 상태 상세 조회", userId);
            
            // 모든 구독 조회 (활성/비활성 포함)
            List<UserNewsletterSubscription> allSubscriptions = subscriptionRepository.findByUserId(userId);
            
            // 활성 구독만 조회
            List<UserNewsletterSubscription> activeSubscriptions = subscriptionRepository.findActiveSubscriptionsByUserId(userId);
            
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("userId", userId);
            debugInfo.put("totalSubscriptions", allSubscriptions.size());
            debugInfo.put("activeSubscriptions", activeSubscriptions.size());
            
            // 모든 구독 상세 정보
            List<Map<String, Object>> allSubsDetail = allSubscriptions.stream()
                .map(sub -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("id", sub.getId());
                    detail.put("category", sub.getCategory());
                    detail.put("isActive", sub.getIsActive());
                    detail.put("subscribedAt", sub.getSubscribedAt());
                    detail.put("updatedAt", sub.getUpdatedAt());
                    return detail;
                })
                .collect(Collectors.toList());
            
            debugInfo.put("allSubscriptions", allSubsDetail);
            
            // 활성 구독만 상세 정보
            List<Map<String, Object>> activeSubsDetail = activeSubscriptions.stream()
                .map(sub -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("id", sub.getId());
                    detail.put("category", sub.getCategory());
                    detail.put("isActive", sub.getIsActive());
                    detail.put("subscribedAt", sub.getSubscribedAt());
                    detail.put("updatedAt", sub.getUpdatedAt());
                    return detail;
                })
                .collect(Collectors.toList());
            
            debugInfo.put("activeSubscriptions", activeSubsDetail);
            
            return ResponseEntity.ok(ApiResponse.success(debugInfo, "구독 상태 디버깅 정보"));
            
        } catch (Exception e) {
            log.error("구독 상태 디버깅 중 오류 발생: userId={}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("DEBUG_ERROR", "구독 상태 디버깅 중 오류가 발생했습니다."));
        }
    }

    /**
     * 디버깅용: 경제 카테고리 구독 추가
     */
    @PostMapping("/debug/subscribe-economy/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugSubscribeEconomy(@PathVariable Long userId) {
        try {
            log.info("디버깅: 사용자 {} 경제 카테고리 구독 추가", userId);
            
            // 이미 경제 카테고리 구독이 있는지 확인
            Optional<UserNewsletterSubscription> existingEconomySub = subscriptionRepository
                .findByUserIdAndCategory(userId, "ECONOMY");
            
            if (existingEconomySub.isPresent()) {
                // 기존 구독이 있으면 활성화
                UserNewsletterSubscription existingSub = existingEconomySub.get();
                existingSub.setIsActive(true);
                existingSub.setUpdatedAt(LocalDateTime.now());
                subscriptionRepository.save(existingSub);
                
                log.info("기존 경제 구독 활성화: subscriptionId={}", existingSub.getId());
                
                Map<String, Object> result = new HashMap<>();
                result.put("action", "activated_existing");
                result.put("subscriptionId", existingSub.getId());
                result.put("category", "ECONOMY");
                result.put("isActive", true);
                
                return ResponseEntity.ok(ApiResponse.success(result, "기존 경제 구독이 활성화되었습니다."));
            } else {
                // 새로운 구독 생성
                UserNewsletterSubscription newSubscription = new UserNewsletterSubscription();
                newSubscription.setUserId(userId);
                newSubscription.setCategory("ECONOMY");
                newSubscription.setIsActive(true);
                newSubscription.setSubscribedAt(LocalDateTime.now());
                newSubscription.setUpdatedAt(LocalDateTime.now());
                
                UserNewsletterSubscription savedSubscription = subscriptionRepository.save(newSubscription);
                
                log.info("새로운 경제 구독 생성: subscriptionId={}", savedSubscription.getId());
                
                Map<String, Object> result = new HashMap<>();
                result.put("action", "created_new");
                result.put("subscriptionId", savedSubscription.getId());
                result.put("category", "ECONOMY");
                result.put("isActive", true);
                
                return ResponseEntity.ok(ApiResponse.success(result, "새로운 경제 구독이 생성되었습니다."));
            }
            
        } catch (Exception e) {
            log.error("경제 카테고리 구독 추가 중 오류 발생: userId={}", userId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SUBSCRIPTION_ERROR", "경제 카테고리 구독 추가 중 오류가 발생했습니다."));
        }
    }

    /**
     * 카테고리별 구독/해지 API
     */
    @PostMapping("/subscription/category/{category}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleCategorySubscription(
            @PathVariable String category,
            @RequestParam(defaultValue = "true") boolean subscribe,
            HttpServletRequest httpRequest) {
        
        try {
            // 임시로 테스트용 userId 사용 (실제 환경에서는 JWT에서 추출)
            Long userId = 1L;
            log.info("카테고리 구독 상태 변경 요청: userId={}, category={}, subscribe={}", userId, category, subscribe);
            
            // 카테고리 유효성 검사
            String englishCategory = convertCategoryToEnglish(category);
            if (englishCategory == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_CATEGORY", "유효하지 않은 카테고리입니다."));
            }
            
            // 기존 구독 확인
            Optional<UserNewsletterSubscription> existingSubscription = subscriptionRepository
                .findByUserIdAndCategory(userId, englishCategory);
            
            Map<String, Object> result = new HashMap<>();
            
            if (subscribe) {
                // 구독 요청
                if (existingSubscription.isPresent()) {
                    // 기존 구독이 있으면 활성화
                    UserNewsletterSubscription sub = existingSubscription.get();
                    if (sub.getIsActive()) {
                        result.put("action", "already_subscribed");
                        result.put("message", "이미 구독 중인 카테고리입니다.");
                    } else {
                        sub.setIsActive(true);
                        sub.setUpdatedAt(LocalDateTime.now());
                        subscriptionRepository.save(sub);
                        result.put("action", "reactivated");
                        result.put("message", "구독이 재활성화되었습니다.");
                    }
                } else {
                    // 새로운 구독 생성
                    UserNewsletterSubscription newSubscription = new UserNewsletterSubscription();
                    newSubscription.setUserId(userId);
                    newSubscription.setCategory(englishCategory);
                    newSubscription.setIsActive(true);
                    newSubscription.setSubscribedAt(LocalDateTime.now());
                    newSubscription.setUpdatedAt(LocalDateTime.now());
                    
                    UserNewsletterSubscription savedSubscription = subscriptionRepository.save(newSubscription);
                    result.put("action", "subscribed");
                    result.put("message", "구독이 완료되었습니다.");
                    result.put("subscriptionId", savedSubscription.getId());
                }
            } else {
                // 구독 해지 요청
                if (existingSubscription.isPresent()) {
                    UserNewsletterSubscription sub = existingSubscription.get();
                    if (!sub.getIsActive()) {
                        result.put("action", "already_unsubscribed");
                        result.put("message", "이미 구독 해지된 카테고리입니다.");
                    } else {
                        sub.setIsActive(false);
                        sub.setUpdatedAt(LocalDateTime.now());
                        subscriptionRepository.save(sub);
                        result.put("action", "unsubscribed");
                        result.put("message", "구독이 해지되었습니다.");
                    }
                } else {
                    result.put("action", "not_subscribed");
                    result.put("message", "구독하지 않은 카테고리입니다.");
                }
            }
            
            // 결과에 카테고리 정보 추가
            result.put("category", englishCategory);
            result.put("categoryKo", category);
            result.put("userId", userId);
            result.put("timestamp", LocalDateTime.now().toString());
            
            // 현재 활성 구독 수 조회
            List<UserNewsletterSubscription> activeSubscriptions = subscriptionRepository
                .findActiveSubscriptionsByUserId(userId);
            result.put("totalActiveSubscriptions", activeSubscriptions.size());
            
            log.info("카테고리 구독 상태 변경 완료: userId={}, category={}, action={}", 
                userId, englishCategory, result.get("action"));
            
            return ResponseEntity.ok(ApiResponse.success(result, "구독 상태가 변경되었습니다."));
            
        } catch (Exception e) {
            log.error("카테고리 구독 상태 변경 중 오류 발생: category={}, subscribe={}", category, subscribe, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SUBSCRIPTION_TOGGLE_ERROR", "구독 상태 변경 중 오류가 발생했습니다."));
        }
    }

    /**
     * 카테고리별 구독 상태 조회 API
     */
    @GetMapping("/subscription/category/{category}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCategorySubscriptionStatus(
            @PathVariable String category,
            HttpServletRequest httpRequest) {
        
        try {
            // 임시로 테스트용 userId 사용 (실제 환경에서는 JWT에서 추출)
            Long userId = 1L;
            log.info("카테고리 구독 상태 조회: userId={}, category={}", userId, category);
            
            String englishCategory = convertCategoryToEnglish(category);
            if (englishCategory == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_CATEGORY", "유효하지 않은 카테고리입니다."));
            }
            
            Optional<UserNewsletterSubscription> subscription = subscriptionRepository
                .findByUserIdAndCategory(userId, englishCategory);
            
            Map<String, Object> result = new HashMap<>();
            result.put("category", englishCategory);
            result.put("categoryKo", category);
            result.put("userId", userId);
            
            if (subscription.isPresent()) {
                UserNewsletterSubscription sub = subscription.get();
                result.put("isSubscribed", sub.getIsActive());
                result.put("subscriptionId", sub.getId());
                result.put("subscribedAt", sub.getSubscribedAt());
                result.put("updatedAt", sub.getUpdatedAt());
            } else {
                result.put("isSubscribed", false);
                result.put("subscriptionId", null);
                result.put("subscribedAt", null);
                result.put("updatedAt", null);
            }
            
            return ResponseEntity.ok(ApiResponse.success(result, "구독 상태 조회 완료"));
            
        } catch (Exception e) {
            log.error("카테고리 구독 상태 조회 중 오류 발생: category={}", category, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SUBSCRIPTION_STATUS_ERROR", "구독 상태 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 카테고리명을 영어로 변환하는 헬퍼 메서드
     */
    private String convertCategoryToEnglish(String categoryKo) {
        Map<String, String> categoryMap = Map.of(
            "정치", "POLITICS",
            "경제", "ECONOMY",
            "사회", "SOCIETY",
            "생활", "LIFE",
            "세계", "INTERNATIONAL",
            "IT/과학", "IT_SCIENCE",
            "자동차/교통", "VEHICLE",
            "여행/음식", "TRAVEL_FOOD",
            "예술", "ART"
        );
        return categoryMap.get(categoryKo);
    }

    /**
     * 액세스 토큰 추출 헬퍼 메서드
     */
    private String extractAccessToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }
>>>>>>> develop
}
