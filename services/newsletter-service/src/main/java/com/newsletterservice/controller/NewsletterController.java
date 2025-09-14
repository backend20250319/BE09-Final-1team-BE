package com.newsletterservice.controller;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.common.exception.NewsletterException;
import com.newsletterservice.dto.*;
import com.newsletterservice.client.dto.*;
import com.newsletterservice.entity.NewsCategory;
import com.newsletterservice.entity.SubscriptionStatus;
import com.newsletterservice.entity.UserNewsletterSubscription;
import com.newsletterservice.repository.NewsletterDeliveryRepository;
import com.newsletterservice.repository.UserNewsletterSubscriptionRepository;
import com.newsletterservice.service.EmailNewsletterRenderer;
import com.newsletterservice.service.KakaoMessageService;
import com.newsletterservice.service.NewsletterService;
import com.newsletterservice.service.NewsletterServiceLevel;
import com.newsletterservice.client.NewsServiceClient;
import org.springframework.data.domain.Page;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NewsletterController extends BaseController {

    // ========================================
    // Service Dependencies
    // ========================================
    private final NewsletterService newsletterService;
    private final EmailNewsletterRenderer emailRenderer;
    private final NewsletterDeliveryRepository deliveryRepository;
    private final Optional<KakaoMessageService> kakaoMessageService;
    private final com.newsletterservice.client.UserServiceClient userServiceClient;
    private final com.newsletterservice.client.NewsServiceClient newsServiceClient;
    private final UserNewsletterSubscriptionRepository subscriptionRepository;
    private final NewsletterServiceLevel serviceLevel;

    // ========================================
    // 1. 구독 관리 기능
    // ========================================

    /**
     * 뉴스레터 구독
     */
    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<NewsletterSubscriptionResponse>> subscribeNewsletter(
            @Valid @RequestBody NewsletterSubscriptionRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("뉴스레터 구독 요청: email={}, frequency={}, categories={}, hasAuth={}", 
                    request.getEmail(), request.getFrequency(), request.getPreferredCategories(), request.getHasAuth());
            
            // 사용자 ID 추출 (인증된 경우)
            Long userId = null;
            if (request.getHasAuth() != null && request.getHasAuth()) {
                try {
                    userId = super.extractUserIdFromToken(httpRequest);
                    log.info("인증된 사용자 구독: userId={}", userId);
                } catch (Exception e) {
                    log.warn("토큰에서 사용자 ID 추출 실패, 비인증 구독으로 처리: {}", e.getMessage());
                }
            }
            
            // 구독 처리 (임시 구현 - 실제로는 user-service와 연동)
            NewsletterSubscriptionResponse response = NewsletterSubscriptionResponse.builder()
                    .subscriptionId(1L)
                    .userId(userId != null ? userId : 1L)
                    .email(request.getEmail())
                    .frequency(request.getFrequency())
                    .status(SubscriptionStatus.ACTIVE)
                    .preferredCategories(request.getPreferredCategories())
                    .keywords(request.getKeywords())
                    .sendTime(request.getSendTime())
                    .isPersonalized(request.getIsPersonalized())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            log.info("뉴스레터 구독 완료: subscriptionId={}, email={}", response.getSubscriptionId(), response.getEmail());
            
            return ResponseEntity.ok(ApiResponse.success(response, "구독이 완료되었습니다."));
            
        } catch (Exception e) {
            log.error("뉴스레터 구독 실패: email={}", request.getEmail(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SUBSCRIPTION_ERROR", "구독 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 내 구독 목록 조회 (활성화된 구독만 반환)
     */
    @GetMapping("/subscription/my")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMySubscriptions(
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = super.extractUserIdFromToken(httpRequest);
            log.info("내 구독 목록 조회 요청 - userId: {}", userId);
            
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
            
        } catch (Exception e) {
            log.error("구독 목록 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SUBSCRIPTION_LIST_ERROR", "구독 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 내 구독 목록 조회 (구독자 수 포함)
     */
    @GetMapping("/subscription/my-with-counts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMySubscriptionsWithCounts(
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = super.extractUserIdFromToken(httpRequest);
            log.info("내 구독 목록 조회 (구독자 수 포함) - userId: {}", userId);
            
            // 사용자 구독 정보 조회
            List<UserNewsletterSubscription> userSubscriptions = subscriptionRepository.findByUserId(userId);
            
            // 카테고리별 구독자 수 조회
            List<Object[]> subscriberCounts = subscriptionRepository.countActiveSubscribersByCategory();
            Map<String, Long> countMap = subscriberCounts.stream()
                .collect(Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                ));
            
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
            
            List<Map<String, Object>> result = new ArrayList<>();
            
            // 모든 카테고리에 대해 구독 상태와 구독자 수 포함
            for (Map.Entry<String, String> entry : categoryNames.entrySet()) {
                String categoryCode = entry.getKey();
                String categoryName = entry.getValue();
                
                // 해당 카테고리의 구독 정보 찾기
                Optional<UserNewsletterSubscription> subscription = userSubscriptions.stream()
                    .filter(s -> s.getCategory().equals(categoryCode))
                    .findFirst();
                
                // 해당 카테고리의 총 구독자 수
                Long subscriberCount = countMap.getOrDefault(categoryCode, 0L);
                
                Map<String, Object> categoryInfo = new HashMap<>();
                categoryInfo.put("categoryId", categoryCode.hashCode());
                categoryInfo.put("categoryName", categoryCode);
                categoryInfo.put("categoryNameKo", categoryName);
                categoryInfo.put("isActive", subscription.map(UserNewsletterSubscription::getIsActive).orElse(false));
                categoryInfo.put("subscriberCount", subscriberCount); // 구독자 수 추가
                categoryInfo.put("subscribedAt", 
                    subscription.map(s -> s.getSubscribedAt().toString())
                              .orElse(null));
                
                result.add(categoryInfo);
            }
            
            return ResponseEntity.ok(ApiResponse.success(result, "구독 목록과 구독자 수 조회가 완료되었습니다."));
            
        } catch (Exception e) {
            log.error("구독 목록 및 구독자 수 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SUBSCRIPTION_LIST_WITH_COUNT_ERROR", "구독 목록 및 구독자 수 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 구독 상태 변경 (구독자 수 실시간 업데이트) - Fallback 메커니즘 포함
     */
    @PostMapping("/subscription/toggle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleSubscription(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            Long userId = super.extractUserIdFromToken(httpRequest);
            String category = (String) request.get("category");
            Boolean isActive = (Boolean) request.get("isActive");
            
            // isActive가 null인 경우 기본값으로 true 설정 (구독 요청의 경우)
            final Boolean finalIsActive = (isActive == null) ? true : isActive;
            if (isActive == null) {
                log.info("isActive 값이 null이므로 기본값 true로 설정");
            }
            
            log.info("구독 상태 변경 시작: userId={}, category={}, isActive={}", userId, category, finalIsActive);
            
            // 입력값 검증
            if (category == null || category.trim().isEmpty()) {
                log.warn("카테고리가 비어있습니다: category={}", category);
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_CATEGORY", "카테고리가 필요합니다."));
            }
            
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
            
            // 8초 타임아웃으로 fallback 처리
            Map<String, Object> result = future.get(8, TimeUnit.SECONDS);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("구독 상태 변경 완료: userId={}, category={}, duration={}ms", userId, category, duration);
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (TimeoutException e) {
            log.warn("구독 상태 변경 타임아웃 발생 - fallback 모드로 동작: userId={}, category={}", 
                    request.get("userId"), request.get("category"));
            
            // Fallback 응답
            Map<String, Object> fallbackResult = new HashMap<>();
            fallbackResult.put("message", "서비스가 일시적으로 사용할 수 없습니다. 요청은 처리되었지만 구독자 수를 확인할 수 없습니다.");
            fallbackResult.put("category", request.get("category"));
            fallbackResult.put("subscriberCount", -1); // 알 수 없음을 나타냄
            fallbackResult.put("isFallback", true);
            fallbackResult.put("warning", "백엔드 서비스가 사용할 수 없음 - fallback 모드로 동작");
            
            return ResponseEntity.status(503)
                .body(ApiResponse.success(fallbackResult, "서비스가 일시적으로 사용할 수 없습니다."));
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("구독 상태 변경 중 오류 발생: userId={}, category={}, duration={}ms", 
                    request.get("userId"), request.get("category"), duration, e);
            
            // 에러 발생 시에도 fallback 응답 제공
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("message", "구독 상태 변경 중 오류가 발생했습니다.");
            errorResult.put("category", request.get("category"));
            errorResult.put("subscriberCount", -1);
            errorResult.put("isFallback", true);
            errorResult.put("error", e.getMessage());
            
            return ResponseEntity.status(500)
                .body(ApiResponse.error("SUBSCRIPTION_TOGGLE_ERROR", "구독 상태 변경 중 오류가 발생했습니다.", errorResult));
        }
    }
    
    /**
     * 구독자 수 조회 with Fallback
     */
    private Long getSubscriberCountWithFallback(String category) {
        try {
            // 3초 타임아웃으로 구독자 수 조회
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> 
                subscriptionRepository.countActiveSubscribersByCategory(category));
            
            return future.get(3, TimeUnit.SECONDS);
            
        } catch (TimeoutException e) {
            log.warn("구독자 수 조회 타임아웃 - fallback 값 반환: category={}", category);
            return -1L; // 알 수 없음을 나타냄
        } catch (Exception e) {
            log.warn("구독자 수 조회 실패 - fallback 값 반환: category={}, error={}", category, e.getMessage());
            return -1L;
        }
    }

    /**
     * 구독 통계 조회 (대시보드용)
     */
    @GetMapping("/subscription/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSubscriptionStats(
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = super.extractUserIdFromToken(httpRequest);
            log.info("구독 통계 조회 요청 - userId: {}", userId);
            
            // 사용자별 구독 통계
            List<UserNewsletterSubscription> allSubscriptions = subscriptionRepository.findByUserId(userId);
            List<UserNewsletterSubscription> activeSubscriptions = subscriptionRepository.findActiveSubscriptionsByUserId(userId);
            
            // 전체 구독자 수 통계
            Long totalSubscribers = subscriptionRepository.countTotalActiveSubscribers();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalSubscriptions", allSubscriptions.size());
            stats.put("activeSubscriptions", activeSubscriptions.size());
            stats.put("inactiveSubscriptions", allSubscriptions.size() - activeSubscriptions.size());
            stats.put("totalSubscribers", totalSubscribers);
            stats.put("averageReadingTime", "3.2분"); // 기본값
            stats.put("engagement", "0%"); // 기본값
            
            log.info("구독 통계 조회 완료: userId={}, active={}, total={}", userId, activeSubscriptions.size(), allSubscriptions.size());
            return ResponseEntity.ok(ApiResponse.success(stats, "구독 통계를 조회했습니다."));
            
        } catch (Exception e) {
            log.error("구독 통계 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
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
            
        } catch (Exception e) {
            log.error("테스트 구독 데이터 초기화 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("INIT_TEST_DATA_ERROR", "테스트 데이터 초기화 중 오류가 발생했습니다."));
        }
    }

    /**
     * 구독 정보 새로고침
     */
    @PostMapping("/subscription/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshSubscriptionData(
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = super.extractUserIdFromToken(httpRequest);
            log.info("구독 정보 새로고침 요청 - userId: {}", userId);
            
            // 활성화된 구독 정보 조회
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
            
            List<Map<String, Object>> subscriptionCards = new ArrayList<>();
            
            // 활성화된 구독만 카드 형태로 반환
            for (UserNewsletterSubscription subscription : activeSubscriptions) {
                String categoryCode = subscription.getCategory();
                String categoryName = categoryNames.getOrDefault(categoryCode, categoryCode);
                
                Map<String, Object> subscriptionCard = new HashMap<>();
                subscriptionCard.put("subscriptionId", subscription.getId());
                subscriptionCard.put("categoryId", categoryCode.hashCode());
                subscriptionCard.put("categoryName", categoryCode);
                subscriptionCard.put("categoryNameKo", categoryName);
                subscriptionCard.put("isActive", subscription.getIsActive());
                subscriptionCard.put("subscribedAt", subscription.getSubscribedAt().toString());
                subscriptionCard.put("updatedAt", subscription.getUpdatedAt() != null ? subscription.getUpdatedAt().toString() : null);
                
                // 구독자 수 조회 (fallback 처리)
                Long subscriberCount = getSubscriberCountWithFallback(categoryCode);
                subscriptionCard.put("subscriberCount", subscriberCount);
                
                subscriptionCards.add(subscriptionCard);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("subscriptions", subscriptionCards);
            result.put("totalCount", subscriptionCards.size());
            result.put("refreshedAt", LocalDateTime.now().toString());
            
            log.info("구독 정보 새로고침 완료: userId={}, count={}", userId, subscriptionCards.size());
            return ResponseEntity.ok(ApiResponse.success(result, "구독 정보가 새로고침되었습니다."));
            
        } catch (Exception e) {
            log.error("구독 정보 새로고침 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("REFRESH_ERROR", "구독 정보 새로고침 중 오류가 발생했습니다."));
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
     * 카테고리 목록과 구독자 수 조회 (구독자 수 포함)
     */
    @GetMapping("/categories/with-subscriber-count")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategoriesWithSubscriberCount() {
        try {
            log.info("카테고리별 구독자 수 포함 목록 조회");
            
            // 카테고리별 구독자 수 조회
            List<Object[]> subscriberCounts = subscriptionRepository.countActiveSubscribersByCategory();
            Map<String, Long> countMap = subscriberCounts.stream()
                .collect(Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                ));
            
            // 카테고리 정보와 구독자 수 결합
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
            
            List<Map<String, Object>> categories = new ArrayList<>();
            
            for (Map.Entry<String, String> entry : categoryNames.entrySet()) {
                String categoryCode = entry.getKey();
                String categoryName = entry.getValue();
                Long subscriberCount = countMap.getOrDefault(categoryCode, 0L);
                
                Map<String, Object> categoryInfo = new HashMap<>();
                categoryInfo.put("code", categoryCode);
                categoryInfo.put("name", categoryName);
                categoryInfo.put("subscriberCount", subscriberCount);
                categoryInfo.put("description", getCategoryDescription(categoryCode));
                
                categories.add(categoryInfo);
            }
            
            return ResponseEntity.ok(ApiResponse.success(categories, "카테고리 목록과 구독자 수를 조회했습니다."));
            
        } catch (Exception e) {
            log.error("카테고리별 구독자 수 조회 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("CATEGORIES_SUBSCRIBER_COUNT_ERROR", "카테고리별 구독자 수 조회 중 오류가 발생했습니다."));
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
     * 전체 통계 조회
     */
    @GetMapping("/stats/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverviewStats() {
        try {
            log.info("전체 통계 조회");
            
            // 전체 활성 구독자 수
            Long totalSubscribers = subscriptionRepository.countActiveSubscribers();
            
            // 카테고리별 구독자 수
            List<Object[]> categoryStats = subscriptionRepository.countActiveSubscribersByCategory();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalSubscribers", totalSubscribers);
            stats.put("categoryStats", categoryStats.stream()
                .collect(Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                )));
            
            return ResponseEntity.ok(ApiResponse.success(stats, "전체 통계를 조회했습니다."));
            
        } catch (Exception e) {
            log.error("전체 통계 조회 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("STATS_ERROR", "통계 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 뉴스레터 상세 조회 (ID 검증 강화)
     */
    @GetMapping("/{newsletterId}")
    public ResponseEntity<ApiResponse<Object>> getNewsletterDetail(
            @PathVariable String newsletterId) {
        
        log.info("뉴스레터 상세 조회 요청: newsletterId={}", newsletterId);
        
        // 1. ID 형식 검증
        Long id = super.validateAndParseId(newsletterId);
        if (id == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_ID_FORMAT", 
                    "뉴스레터 ID는 숫자여야 합니다. 입력값: " + newsletterId));
        }
        
        try {
            // 2. 뉴스레터 조회 로직
            Object newsletter = newsletterService.getNewsletterById(id);
            
            return ResponseEntity.ok(
                ApiResponse.success(newsletter, "뉴스레터 조회가 완료되었습니다."));
                
        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
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
            String userId = extractUserIdAsString(httpRequest);
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
            String userId = extractUserIdAsString(httpRequest);
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
     * 뉴스레터 미리보기 (ID 검증 강화)
     */
    @GetMapping("/{newsletterId}/preview")
    public ResponseEntity<?> getNewsletterPreview(
            @PathVariable String newsletterId) {
        
        log.info("뉴스레터 미리보기 요청: newsletterId={}", newsletterId);
        
        // 1. ID 형식 검증
        Long id = super.validateAndParseId(newsletterId);
        if (id == null) {
            String errorHtml = super.generateErrorHtml(
                "잘못된 ID 형식", 
                "뉴스레터 ID는 숫자여야 합니다. 입력값: " + newsletterId,
                "올바른 URL 형식: /newsletter/123/preview"
            );
            return ResponseEntity.badRequest()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(errorHtml);
        }
        
        try {
            // 2. 미리보기 HTML 생성
            String previewHtml = newsletterService.generatePreviewHtml(id);
            
            return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(previewHtml);
                
        } catch (RuntimeException e) {
            String errorHtml = super.generateErrorHtml(
                "뉴스레터를 찾을 수 없습니다",
                "ID " + id + "에 해당하는 뉴스레터가 존재하지 않습니다.",
                "뉴스레터 목록으로 돌아가서 올바른 ID를 확인해주세요."
            );
            return ResponseEntity.status(404)
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(errorHtml);
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

    // 구독 재활성화 기능은 user-service에서 처리됩니다.

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
            String userId = extractUserIdAsString(httpRequest);
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
     * 뉴스레터 발송 테스트 (개발용)
     */
    @PostMapping("/delivery/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testNewsletterDelivery(
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdAsString(httpRequest);
            log.info("뉴스레터 발송 테스트 요청 - userId: {}", userId);
            
            // 테스트용 뉴스레터 생성
            NewsletterContent testContent = createTestNewsletterContent();
            
            // 이메일 발송 테스트
            Map<String, Object> testResults = new HashMap<>();
            testResults.put("testContent", testContent);
            testResults.put("userId", userId);
            testResults.put("timestamp", LocalDateTime.now());
            
            // 실제 발송은 하지 않고 테스트 결과만 반환
            testResults.put("emailDeliveryTest", "테스트 뉴스레터 생성 완료");
            testResults.put("contentSections", testContent.getSections().size());
            testResults.put("totalArticles", testContent.getSections().stream()
                    .mapToInt(section -> section.getArticles().size())
                    .sum());
            
            return ResponseEntity.ok(ApiResponse.success(testResults, "뉴스레터 발송 테스트가 완료되었습니다."));
            
        } catch (Exception e) {
            log.error("뉴스레터 발송 테스트 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("TEST_ERROR", "뉴스레터 발송 테스트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 뉴스레터 실제 발송 테스트 (이메일)
     */
    @PostMapping("/delivery/test-email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testEmailNewsletterDelivery(
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdAsString(httpRequest);
            log.info("뉴스레터 이메일 발송 테스트 요청 - userId: {}", userId);
            
            // 테스트용 뉴스레터 생성
            NewsletterContent testContent = createTestNewsletterContent();
            
            // 실제 이메일 발송 테스트
            newsletterService.sendEmailNewsletter(testContent);
            
            Map<String, Object> testResults = new HashMap<>();
            testResults.put("deliveryMethod", "EMAIL");
            testResults.put("status", "SENT");
            testResults.put("sentAt", LocalDateTime.now());
            testResults.put("contentTitle", testContent.getTitle());
            testResults.put("contentSections", testContent.getSections().size());
            testResults.put("totalArticles", testContent.getSections().stream()
                    .mapToInt(section -> section.getArticles().size())
                    .sum());
            
            return ResponseEntity.ok(ApiResponse.success(testResults, "뉴스레터 이메일 발송 테스트가 완료되었습니다."));
            
        } catch (Exception e) {
            log.error("뉴스레터 이메일 발송 테스트 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("EMAIL_TEST_ERROR", "뉴스레터 이메일 발송 테스트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 뉴스레터 실제 발송 테스트 (카카오톡)
     */
    @PostMapping("/delivery/test-kakao")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testKakaoNewsletterDelivery(
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdAsString(httpRequest);
            log.info("뉴스레터 카카오톡 발송 테스트 요청 - userId: {}", userId);
            
            // 테스트용 뉴스레터 생성
            NewsletterContent testContent = createTestNewsletterContent();
            
            // 카카오톡 발송 테스트 (시뮬레이션 모드)
            kakaoMessageService.get().sendNewsletterMessage(testContent);
            
            Map<String, Object> testResults = new HashMap<>();
            testResults.put("deliveryMethod", "KAKAO");
            testResults.put("status", "SENT");
            testResults.put("sentAt", LocalDateTime.now());
            testResults.put("contentTitle", testContent.getTitle());
            testResults.put("contentSections", testContent.getSections().size());
            testResults.put("totalArticles", testContent.getSections().stream()
                    .mapToInt(section -> section.getArticles().size())
                    .sum());
            testResults.put("note", "카카오톡 발송은 시뮬레이션 모드로 실행되었습니다.");
            
            return ResponseEntity.ok(ApiResponse.success(testResults, "뉴스레터 카카오톡 발송 테스트가 완료되었습니다."));
            
        } catch (Exception e) {
            log.error("뉴스레터 카카오톡 발송 테스트 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("KAKAO_TEST_ERROR", "뉴스레터 카카오톡 발송 테스트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 뉴스레터 발송 상태 확인
     */
    @GetMapping("/delivery/status/{deliveryId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDeliveryStatus(
            @PathVariable Long deliveryId,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdAsString(httpRequest);
            log.info("뉴스레터 발송 상태 확인 - deliveryId: {}, userId: {}", deliveryId, userId);
            
            // 발송 상태 조회 로직 (실제 구현 필요)
            Map<String, Object> status = new HashMap<>();
            status.put("deliveryId", deliveryId);
            status.put("status", "SENT"); // 임시 상태
            status.put("sentAt", LocalDateTime.now());
            status.put("recipientCount", 10);
            status.put("successCount", 8);
            status.put("failureCount", 2);
            
            return ResponseEntity.ok(ApiResponse.success(status, "발송 상태를 조회했습니다."));
            
        } catch (Exception e) {
            log.error("발송 상태 확인 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("STATUS_ERROR", "발송 상태 확인 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 뉴스레터 발송 통계 조회
     */
    @GetMapping("/delivery/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDeliveryStats(
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdAsString(httpRequest);
            log.info("뉴스레터 발송 통계 조회 - userId: {}", userId);
            
            // 발송 통계 조회 로직 (실제 구현 필요)
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalDeliveries", 25);
            stats.put("successfulDeliveries", 23);
            stats.put("failedDeliveries", 2);
            stats.put("successRate", 92.0);
            stats.put("lastDeliveryAt", LocalDateTime.now().minusHours(2));
            stats.put("averageDeliveryTime", "2.5초");
            
            return ResponseEntity.ok(ApiResponse.success(stats, "발송 통계를 조회했습니다."));
            
        } catch (Exception e) {
            log.error("발송 통계 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("STATS_ERROR", "발송 통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 테스트용 뉴스레터 콘텐츠 생성
     */
    private NewsletterContent createTestNewsletterContent() {
        List<NewsletterContent.Article> articles = new ArrayList<>();
        
        // 테스트 뉴스 기사 생성
        NewsletterContent.Article article1 = NewsletterContent.Article.builder()
                .title("테스트 뉴스 1: 최신 기술 동향")
                .summary("인공지능과 머신러닝 기술의 최신 동향을 살펴봅니다.")
                .url("https://example.com/news/1")
                .category("IT/과학")
                .publishedAt(LocalDateTime.now().minusHours(1))
                .isPersonalized(false)
                .build();
        
        NewsletterContent.Article article2 = NewsletterContent.Article.builder()
                .title("테스트 뉴스 2: 경제 전망")
                .summary("올해 경제 전망과 주요 이슈들을 분석합니다.")
                .url("https://example.com/news/2")
                .category("경제")
                .publishedAt(LocalDateTime.now().minusHours(2))
                .isPersonalized(false)
                .build();
        
        NewsletterContent.Article article3 = NewsletterContent.Article.builder()
                .title("테스트 뉴스 3: 사회 이슈")
                .summary("최근 사회적 관심사와 정책 변화를 다룹니다.")
                .url("https://example.com/news/3")
                .category("사회")
                .publishedAt(LocalDateTime.now().minusHours(3))
                .isPersonalized(true)
                .build();
        
        articles.add(article1);
        articles.add(article2);
        articles.add(article3);
        
        NewsletterContent.Section section = NewsletterContent.Section.builder()
                .heading("오늘의 주요 뉴스")
                .description("최신 뉴스와 트렌딩 정보를 확인하세요")
                .sectionType("MAIN")
                .articles(articles)
                .build();
        
        return NewsletterContent.builder()
                .newsletterId(999L)
                .userId(1L)
                .title("테스트 뉴스레터")
                .subtitle("개발용 테스트 뉴스레터입니다")
                .sections(List.of(section))
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 뉴스레터 예약 발송
     */
    @PostMapping("/delivery/schedule")
    public ResponseEntity<ApiResponse<DeliveryStats>> scheduleNewsletter(
            @Valid @RequestBody NewsletterDeliveryRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdAsString(httpRequest);
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
            String userId = extractUserIdAsString(httpRequest);
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
            String userId = extractUserIdAsString(httpRequest);
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
            String userId = extractUserIdAsString(httpRequest);
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

    /**
     * 뉴스 읽기 기록 추가
     */
    @PostMapping("/history/{newsId}")
    public ResponseEntity<ApiResponse<String>> addReadHistory(
            @PathVariable Long newsId,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdAsString(httpRequest);
            log.info("뉴스 읽기 기록 추가 요청 - userId: {}, newsId: {}", userId, newsId);
            
            newsletterService.addReadHistory(Long.valueOf(userId), newsId);
            
            return ResponseEntity.ok(ApiResponse.success("뉴스 읽기 기록이 추가되었습니다."));
        } catch (NewsletterException e) {
            log.warn("뉴스 읽기 기록 추가 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("뉴스 읽기 기록 추가 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("READ_HISTORY_ERROR", "뉴스 읽기 기록 추가 중 오류가 발생했습니다."));
        }
    }

    /**
     * 뉴스레터에서 기사 클릭 추적
     */
    @PostMapping("/track-click")
    public ResponseEntity<ApiResponse<String>> trackNewsClick(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdAsString(httpRequest);
            Long newsId = Long.valueOf(request.get("newsId").toString());
            
            log.info("뉴스레터 기사 클릭 추적: userId={}, newsId={}", userId, newsId);
            
            // 읽기 기록 추가
            newsletterService.addReadHistory(Long.valueOf(userId), newsId);
            
            return ResponseEntity.ok(ApiResponse.success("읽기 기록이 저장되었습니다."));
        } catch (Exception e) {
            log.error("읽기 기록 저장 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TRACK_ERROR", "읽기 기록 저장에 실패했습니다."));
        }
    }

    /**
     * 사용자 개인화 정보 조회
     */
    @GetMapping("/personalization-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPersonalizationInfo(
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdAsString(httpRequest);
            log.info("개인화 정보 조회 요청: userId={}", userId);
            
            Map<String, Object> personalizationInfo = newsletterService.getPersonalizationInfo(Long.valueOf(userId));
            
            return ResponseEntity.ok(ApiResponse.success(personalizationInfo, "개인화 정보 조회가 완료되었습니다."));
        } catch (Exception e) {
            log.error("개인화 정보 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PERSONALIZATION_INFO_ERROR", "개인화 정보 조회에 실패했습니다."));
        }
    }

    /**
     * 실제 뉴스 데이터로 뉴스레터 테스트 생성
     */
    @GetMapping("/test-with-real-data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testNewsletterWithRealData(
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdAsString(httpRequest);
            log.info("실제 뉴스 데이터로 뉴스레터 테스트: userId={}", userId);
            
            // 개인화된 뉴스레터 콘텐츠 생성
            NewsletterContent content = newsletterService.buildPersonalizedContent(Long.valueOf(userId), 1L);
            
            // HTML 렌더링
            String htmlContent = emailRenderer.renderToHtml(content);
            
            Map<String, Object> result = new HashMap<>();
            result.put("content", content);
            result.put("htmlContent", htmlContent);
            result.put("articleCount", content.getSections().stream()
                    .mapToInt(section -> section.getArticles().size())
                    .sum());
            
            return ResponseEntity.ok(ApiResponse.success(result, "실제 뉴스 데이터로 뉴스레터가 생성되었습니다."));
        } catch (Exception e) {
            log.error("실제 뉴스 데이터 뉴스레터 테스트 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("TEST_NEWSLETTER_ERROR", "뉴스레터 테스트에 실패했습니다."));
        }
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    /**
     * JWT 토큰에서 사용자 ID 추출 (BaseController 메서드 사용)
     */
    private String extractUserIdAsString(HttpServletRequest request) {
        Long userId = super.extractUserIdFromToken(request);
        return userId != null ? userId.toString() : "1";
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
     * 개발/테스트용 샘플 뉴스레터 생성
     */
    @PostMapping("/sample")
    public ResponseEntity<ApiResponse<Object>> createSampleNewsletter() {
        log.info("샘플 뉴스레터 생성 요청");
        
        try {
            Object sampleNewsletter = newsletterService.createSampleNewsletter();
            
            return ResponseEntity.ok(
                ApiResponse.success(sampleNewsletter, "샘플 뉴스레터가 생성되었습니다."));
                
        } catch (Exception e) {
            log.error("샘플 뉴스레터 생성 실패", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("SAMPLE_CREATION_FAILED", "샘플 생성에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 뉴스레터 목록 조회 (페이징)
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Object>> getNewsletterList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("뉴스레터 목록 조회: page={}, size={}", page, size);
        
        try {
            Object newsletterList = newsletterService.getNewsletterList(page, size);
            
            return ResponseEntity.ok(
                ApiResponse.success(newsletterList, "뉴스레터 목록 조회가 완료되었습니다."));
                
        } catch (Exception e) {
            log.error("뉴스레터 목록 조회 실패", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("LIST_QUERY_FAILED", "목록 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 카카오톡 뉴스레터 메시지 전송
     */
    @PostMapping("/{newsletterId}/send-kakao")
    public ResponseEntity<ApiResponse<String>> sendKakaoMessage(
            @PathVariable Long newsletterId,
            HttpServletRequest httpRequest) {

        if (kakaoMessageService.isEmpty()) {
            log.warn("KakaoMessageService가 사용할 수 없습니다. 카카오톡 메시지 전송을 건너뜁니다.");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("KAKAO_SERVICE_UNAVAILABLE", "카카오톡 메시지 서비스가 사용할 수 없습니다."));
        }

        try {
            String userId = extractUserIdAsString(httpRequest);
            log.info("카카오톡 뉴스레터 메시지 전송 요청: userId={}, newsletterId={}", userId, newsletterId);

            NewsletterContent content = newsletterService.buildPersonalizedContent(Long.valueOf(userId), newsletterId);
            kakaoMessageService.get().sendNewsletterMessage(content);

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

    // ========================================
    // Enhanced API - 실시간 뉴스 필터링
    // ========================================

    /**
     * 🔄 Enhanced 뉴스레터 API - 로그인 상태별 차별화된 서비스
     * 비로그인: 기본 뉴스 + 로그인 유도
     * 로그인: 확장된 뉴스 + 구독 관리
     * 구독자: 완전 개인화 + AI 추천
     */
    /**
     * Enhanced 뉴스레터 API - 캐스팅 오류 수정
     */
    /**
     * 🔧 단계별 테스트 - 1단계: 기본 응답
     */
    @GetMapping("/test-step1")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testStep1() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("step", 1);
            result.put("status", "OK");
            result.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(ApiResponse.success(result, "1단계 성공"));
        } catch (Exception e) {
            log.error("1단계 테스트 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("STEP1_ERROR", e.getMessage()));
        }
    }

    /**
     * 🔧 단계별 테스트 - 2단계: JWT 토큰 처리
     */
    @GetMapping("/test-step2")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testStep2(HttpServletRequest httpRequest) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("step", 2);
            
            // JWT 토큰 처리 테스트
            Long userId = null;
            boolean hasToken = false;
            String errorMsg = null;
            
            try {
                userId = super.extractUserIdFromToken(httpRequest);
                hasToken = true;
            } catch (Exception e) {
                errorMsg = e.getMessage();
            }
            
            result.put("hasToken", hasToken);
            result.put("userId", userId);
            result.put("tokenError", errorMsg);
            result.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(ApiResponse.success(result, "2단계 성공"));
        } catch (Exception e) {
            log.error("2단계 테스트 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("STEP2_ERROR", e.getMessage()));
        }
    }

    /**
     * 🔧 단계별 테스트 - 3단계: DB 조회
     */
    @GetMapping("/test-step3")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testStep3(HttpServletRequest httpRequest) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("step", 3);
            
            // JWT 처리
            Long userId = null;
            try {
                userId = super.extractUserIdFromToken(httpRequest);
                } catch (Exception e) {
                // 무시
            }
            
            // DB 조회 테스트
            Long totalSubscriptions = null;
            Long userSubscriptions = null;
            String dbError = null;
            
            try {
                totalSubscriptions = subscriptionRepository.count();
                if (userId != null) {
                    List<UserNewsletterSubscription> userSubs = subscriptionRepository.findActiveSubscriptionsByUserId(userId);
                    userSubscriptions = (long) userSubs.size();
                }
            } catch (Exception e) {
                dbError = e.getMessage();
            }
            
            result.put("userId", userId);
            result.put("totalSubscriptions", totalSubscriptions);
            result.put("userSubscriptions", userSubscriptions);
            result.put("dbError", dbError);
            result.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(ApiResponse.success(result, "3단계 성공"));
        } catch (Exception e) {
            log.error("3단계 테스트 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("STEP3_ERROR", e.getMessage()));
        }
    }

    /**
     * 🔧 Repository 테스트
     */
    @GetMapping("/debug/repository")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testRepository(HttpServletRequest httpRequest) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // JWT 처리
            Long userId = null;
            try {
                userId = super.extractUserIdFromToken(httpRequest);
            } catch (Exception e) {
                // 무시
            }
            
            // Repository 메서드들 테스트
            Map<String, Object> repoTests = new HashMap<>();
            
            try {
                // 전체 구독 수
                Long totalCount = subscriptionRepository.count();
                repoTests.put("totalCount", totalCount);
                
                // 활성 구독 수
                Long activeCount = subscriptionRepository.countActiveSubscribers();
                repoTests.put("activeCount", activeCount);
                
                // 사용자별 구독 조회
                if (userId != null) {
                    List<UserNewsletterSubscription> userSubs = subscriptionRepository.findActiveSubscriptionsByUserId(userId);
                    repoTests.put("userSubscriptions", userSubs.size());
                    repoTests.put("userSubscriptionDetails", userSubs.stream()
                        .map(sub -> Map.of(
                            "id", sub.getId(),
                            "category", sub.getCategory(),
                            "isActive", sub.getIsActive()
                        ))
                        .toList());
                }
                
                repoTests.put("status", "SUCCESS");
                
            } catch (Exception e) {
                repoTests.put("status", "ERROR");
                repoTests.put("error", e.getMessage());
            }
            
            result.put("userId", userId);
            result.put("repositoryTests", repoTests);
            result.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(ApiResponse.success(result, "Repository 테스트 완료"));
        } catch (Exception e) {
            log.error("Repository 테스트 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("REPOSITORY_TEST_ERROR", e.getMessage()));
        }
    }

    /**
     * 🔧 NewsServiceClient 테스트
     */
    @GetMapping("/debug/news-client")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testNewsClient() {
        try {
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> clientTests = new HashMap<>();
            
            // 1. 기본 클라이언트 정보
            clientTests.put("clientStatus", "AVAILABLE");
            clientTests.put("clientClass", newsServiceClient.getClass().getSimpleName());
            
            // 2. 실제 API 호출 테스트
            try {
                Page<NewsResponse> response = newsServiceClient.getNewsByCategory("SOCIETY", 0, 1);
                clientTests.put("apiCallTest", Map.of(
                    "success", response != null,
                    "hasData", response != null && !response.getContent().isEmpty(),
                    "articleCount", response != null ? response.getContent().size() : 0
                ));
                
                if (response != null && !response.getContent().isEmpty()) {
                    NewsResponse firstNews = response.getContent().get(0);
                    clientTests.put("sampleNews", Map.of(
                        "title", firstNews.getTitle(),
                        "category", firstNews.getCategoryName(),
                        "publishedAt", firstNews.getPublishedAt()
                    ));
                }
            
        } catch (Exception e) {
                clientTests.put("apiCallTest", Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "errorClass", e.getClass().getSimpleName()
                ));
            }
            
            result.put("newsClientTests", clientTests);
            result.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(ApiResponse.success(result, "NewsServiceClient 테스트 완료"));
        } catch (Exception e) {
            log.error("NewsServiceClient 테스트 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("NEWS_CLIENT_TEST_ERROR", e.getMessage()));
        }
    }

    /**
     * 🔧 간소화된 Enhanced 버전
     */
    @GetMapping("/debug/enhanced-simple")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEnhancedSimple(HttpServletRequest httpRequest) {
        try {
        Map<String, Object> result = new HashMap<>();
            
            // 기본 정보
            result.put("phase", "simple_initialization");
            result.put("timestamp", LocalDateTime.now().toString());
            
            // 인증 확인
            Long userId = null;
            boolean isAuthenticated = false;
            
            try {
                userId = super.extractUserIdFromToken(httpRequest);
                isAuthenticated = true;
            } catch (Exception e) {
                // 비인증 사용자
            }
            
            result.put("userAuthenticated", isAuthenticated);
            result.put("userId", userId);
            
            // 서비스 레벨 결정
            String serviceLevel = "PUBLIC";
            if (isAuthenticated && userId != null) {
                try {
            List<UserNewsletterSubscription> subscriptions = 
                subscriptionRepository.findActiveSubscriptionsByUserId(userId);
            
            if (!subscriptions.isEmpty()) {
                        serviceLevel = "PERSONALIZED_PREMIUM";
                    } else {
                        serviceLevel = "AUTHENTICATED_BASIC";
                    }
                } catch (Exception e) {
                    serviceLevel = "AUTHENTICATED_BASIC";
                }
            }
            
            result.put("serviceLevel", serviceLevel);
            result.put("categories", Map.of(
                "정치", Map.of("status", "ready", "count", 0),
                "경제", Map.of("status", "ready", "count", 0),
                "사회", Map.of("status", "ready", "count", 0)
            ));
            
            result.put("trendingKeywords", Arrays.asList("테스트", "키워드", "간소화"));
            result.put("phase", "complete");
            
            return ResponseEntity.ok(ApiResponse.success(result, "간소화된 Enhanced 뉴스레터"));
            
        } catch (Exception e) {
            log.error("간소화된 Enhanced 뉴스레터 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("ENHANCED_SIMPLE_ERROR", e.getMessage()));
        }
    }

    /**
     * 🔧 완전 수정된 Enhanced 버전
     */
    @GetMapping("/debug/enhanced-fixed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEnhancedFixed(HttpServletRequest httpRequest) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            log.info("📰 Enhanced Fixed 뉴스레터 요청 시작");
            
            // 1. 기본 정보
            result.put("phase", "initialization");
            result.put("timestamp", LocalDateTime.now().toString());
            log.info("✅ Phase 1: 기본 초기화 완료");
            
            // 2. 인증 확인
            Long userId = null;
            boolean isAuthenticated = false;
            
            try {
                userId = super.extractUserIdFromToken(httpRequest);
                isAuthenticated = true;
                log.info("✅ Phase 2: 사용자 인증 성공 - userId: {}", userId);
                    } catch (Exception e) {
                log.info("ℹ️ Phase 2: 비인증 사용자 - {}", e.getMessage());
            }
            
            result.put("userAuthenticated", isAuthenticated);
            result.put("userId", userId);
            result.put("phase", "authentication_complete");
            
            // 3. 서비스 레벨 결정
            String serviceLevel = "PUBLIC";
            int newsLimit = 5;
            
            if (isAuthenticated && userId != null) {
                try {
                    List<UserNewsletterSubscription> subscriptions = 
                        subscriptionRepository.findActiveSubscriptionsByUserId(userId);
                    
                    if (!subscriptions.isEmpty()) {
                        serviceLevel = "PERSONALIZED_PREMIUM";
                        newsLimit = 10;
                    } else {
                        serviceLevel = "AUTHENTICATED_BASIC";
                        newsLimit = 7;
                    }
                    
                result.put("subscriptionCount", subscriptions.size());
                    log.info("✅ Phase 3: 서비스 레벨 결정 완료 - {}", serviceLevel);
                    
                } catch (Exception e) {
                    log.warn("⚠️ Phase 3: DB 조회 실패 - {}", e.getMessage());
                    serviceLevel = "AUTHENTICATED_BASIC";
                    newsLimit = 7;
                }
            }
            
            result.put("serviceLevel", serviceLevel);
            result.put("newsLimit", newsLimit);
            result.put("phase", "service_level_complete");
            
            // 4. 카테고리별 뉴스 수집 (안전한 방법)
            Map<String, Object> categoryData = new HashMap<>();
            String[] categories = {"정치", "경제", "사회"};
            
            for (String category : categories) {
                try {
                    // 실제 뉴스 조회는 일단 스킵하고 기본 정보만 제공
                    Map<String, Object> categoryInfo = new HashMap<>();
                    categoryInfo.put("articles", Collections.emptyList());
                    categoryInfo.put("count", 0);
                    categoryInfo.put("category", category);
                    categoryInfo.put("limit", newsLimit);
                    categoryInfo.put("status", "ready");
                    
                    // 구독 여부 확인
                    if (isAuthenticated && userId != null) {
                        try {
                            List<UserNewsletterSubscription> userSubs = 
                                subscriptionRepository.findActiveSubscriptionsByUserId(userId);
                            boolean isSubscribed = userSubs.stream()
                                .anyMatch(sub -> convertEnglishToKorean(sub.getCategory()).equals(category));
                            categoryInfo.put("isSubscribed", isSubscribed);
                            categoryInfo.put("priority", isSubscribed ? "HIGH" : "NORMAL");
                } catch (Exception e) {
                            categoryInfo.put("isSubscribed", false);
                            categoryInfo.put("priority", "NORMAL");
                        }
                    } else {
                        categoryInfo.put("isSubscribed", false);
                        categoryInfo.put("priority", "NORMAL");
                    }
                    
                    categoryData.put(category, categoryInfo);
                    log.debug("카테고리 {} 처리 완료", category);
                    
                } catch (Exception e) {
                    log.warn("카테고리 {} 처리 실패: {}", category, e.getMessage());
                    categoryData.put(category, Map.of(
                        "articles", Collections.emptyList(),
                        "count", 0,
                        "category", category,
                        "status", "error",
                        "error", e.getMessage()
                    ));
                }
            }
            
            // 5. 트렌딩 키워드 (기본값)
            List<String> trendingKeywords = Arrays.asList("정치", "경제", "사회", "테스트", "키워드");
            
            // 6. 응답 구성
            result.put("categories", categoryData);
            result.put("trendingKeywords", trendingKeywords);
            result.put("totalCategories", categories.length);
            result.put("newsPerCategory", newsLimit);
            result.put("phase", "complete");
            
            // 7. 사용자별 추가 정보
            if (isAuthenticated && userId != null) {
                try {
                    List<UserNewsletterSubscription> subscriptions = 
                        subscriptionRepository.findActiveSubscriptionsByUserId(userId);
                    
                    if (subscriptions.isEmpty()) {
                        result.put("subscriptionPrompt", "🎯 관심 카테고리를 구독하면 개인화된 뉴스를 받아보실 수 있어요!");
            } else {
                        result.put("userStats", Map.of(
                            "totalSubscriptions", subscriptions.size(),
                            "subscribedCategories", subscriptions.stream()
                                .map(sub -> convertEnglishToKorean(sub.getCategory()))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList())
                        ));
                    }
                } catch (Exception e) {
                    log.warn("구독 정보 조회 실패: {}", e.getMessage());
                    result.put("subscriptionPrompt", "🎯 관심 카테고리를 구독하면 개인화된 뉴스를 받아보실 수 있어요!");
                }
            } else {
                result.put("upgradeMessage", "🔐 로그인하시면 더 많은 뉴스와 맞춤 추천을 받아보실 수 있어요!");
            }
            
            log.info("🎉 Enhanced Fixed 뉴스레터 응답 완료 - serviceLevel: {}", serviceLevel);
            
            return ResponseEntity.ok(ApiResponse.success(result, 
                String.format("%s Enhanced Fixed 뉴스레터", serviceLevel)));
            
        } catch (Exception e) {
            log.error("💥 Enhanced Fixed 뉴스레터 처리 중 심각한 오류", e);
            
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("errorClass", e.getClass().getSimpleName());
            errorDetails.put("errorMessage", e.getMessage());
            errorDetails.put("timestamp", LocalDateTime.now().toString());
            
            if (e.getCause() != null) {
                errorDetails.put("rootCause", e.getCause().getMessage());
            }
            
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("ENHANCED_FIXED_ERROR", 
                    "Enhanced Fixed API 오류: " + e.getMessage(), errorDetails));
        }
    }

    @GetMapping("/enhanced")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEnhancedNewsletter(
            @RequestParam(defaultValue = "5") int headlinesPerCategory,
            @RequestParam(defaultValue = "8") int trendingKeywordsLimit,
            HttpServletRequest httpRequest) {
        
        log.info("📰 Enhanced 뉴스레터 요청 시작");
        
        try {
        Map<String, Object> result = new HashMap<>();
            
            // 단계 1: 기본 정보
            result.put("phase", "initialization");
            result.put("timestamp", LocalDateTime.now().toString());
            log.info("✅ Phase 1: 기본 초기화 완료");
            
            // 단계 2: 인증 확인
            Long userId = null;
            boolean isAuthenticated = false;
            
            try {
                userId = super.extractUserIdFromToken(httpRequest);
                isAuthenticated = true;
                log.info("✅ Phase 2: 사용자 인증 성공 - userId: {}", userId);
            } catch (Exception e) {
                log.info("ℹ️ Phase 2: 비인증 사용자 - {}", e.getMessage());
            }
            
            result.put("userAuthenticated", isAuthenticated);
            result.put("userId", userId);
            result.put("phase", "authentication_complete");
            
            // 단계 3: 서비스 레벨 결정
            String serviceLevel = "PUBLIC";
            int newsLimit = 5;
            
            if (isAuthenticated && userId != null) {
                try {
                    List<UserNewsletterSubscription> subscriptions = 
                        subscriptionRepository.findActiveSubscriptionsByUserId(userId);
                    
                    if (!subscriptions.isEmpty()) {
                        serviceLevel = "PERSONALIZED_PREMIUM";
                        newsLimit = 10;
                    } else {
                        serviceLevel = "AUTHENTICATED_BASIC";
                        newsLimit = 7;
                    }
                    
                    result.put("subscriptionCount", subscriptions.size());
                    log.info("✅ Phase 3: 서비스 레벨 결정 완료 - {}", serviceLevel);
                    
                } catch (Exception e) {
                    log.warn("⚠️ Phase 3: DB 조회 실패 - {}", e.getMessage());
                    serviceLevel = "AUTHENTICATED_BASIC";
                    newsLimit = 7;
                }
            }
            
            result.put("serviceLevel", serviceLevel);
            result.put("newsLimit", newsLimit);
            result.put("phase", "service_level_complete");
            
            // 단계 4: 실제 뉴스 데이터 수집
            Map<String, Object> categoryData = new HashMap<>();
            String[] categories = {"정치", "경제", "사회"};
            
            for (String category : categories) {
                try {
                    // 실제 뉴스 조회
                    List<Map<String, Object>> headlines = getCategoryHeadlinesSafely(category, newsLimit);
                    
                    Map<String, Object> categoryInfo = new HashMap<>();
                    categoryInfo.put("articles", headlines);
                    categoryInfo.put("count", headlines.size());
                    categoryInfo.put("category", category);
                    categoryInfo.put("limit", newsLimit);
                    categoryInfo.put("status", "success");
                    
                    // 구독 여부 확인 (인증된 사용자만)
                    if (isAuthenticated && userId != null) {
                        try {
                            List<UserNewsletterSubscription> userSubs = 
                                subscriptionRepository.findActiveSubscriptionsByUserId(userId);
                            boolean isSubscribed = userSubs.stream()
                                .anyMatch(sub -> convertEnglishToKorean(sub.getCategory()).equals(category));
                            categoryInfo.put("isSubscribed", isSubscribed);
                            categoryInfo.put("priority", isSubscribed ? "HIGH" : "NORMAL");
                        } catch (Exception e) {
                            categoryInfo.put("isSubscribed", false);
                            categoryInfo.put("priority", "NORMAL");
                        }
                    } else {
                        categoryInfo.put("isSubscribed", false);
                        categoryInfo.put("priority", "NORMAL");
                    }
                    
                    categoryData.put(category, categoryInfo);
                    log.debug("카테고리 {} 뉴스 {}개 수집 완료", category, headlines.size());
                    
                } catch (Exception e) {
                    log.warn("카테고리 {} 뉴스 수집 실패: {}", category, e.getMessage());
                    // 실패한 카테고리는 빈 데이터로 처리
                    categoryData.put(category, Map.of(
                        "articles", Collections.emptyList(),
                        "count", 0,
                        "category", category,
                        "limit", newsLimit,
                        "status", "error",
                        "isSubscribed", false,
                        "priority", "NORMAL",
                        "error", "뉴스를 가져올 수 없습니다"
                    ));
                }
            }
            
            // 단계 5: 트렌딩 키워드 수집
            List<String> trendingKeywords = getTrendingKeywordsSafely(trendingKeywordsLimit);
            
            // 단계 6: 응답 구성
            result.put("categories", categoryData);
            result.put("trendingKeywords", trendingKeywords);
            result.put("totalCategories", categories.length);
            result.put("newsPerCategory", newsLimit);
            result.put("phase", "complete");
            
            // 단계 7: 사용자별 추가 정보
            if (isAuthenticated && userId != null) {
                try {
                    List<UserNewsletterSubscription> subscriptions = 
                        subscriptionRepository.findActiveSubscriptionsByUserId(userId);
                    
                    if (subscriptions.isEmpty()) {
                        result.put("subscriptionPrompt", "🎯 관심 카테고리를 구독하면 개인화된 뉴스를 받아보실 수 있어요!");
                    } else {
                        result.put("userStats", Map.of(
                            "totalSubscriptions", subscriptions.size(),
                            "subscribedCategories", subscriptions.stream()
                                .map(sub -> convertEnglishToKorean(sub.getCategory()))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList())
                        ));
                    }
                } catch (Exception e) {
                    log.warn("구독 정보 조회 실패: {}", e.getMessage());
                    result.put("subscriptionPrompt", "🎯 관심 카테고리를 구독하면 개인화된 뉴스를 받아보실 수 있어요!");
                }
            } else {
                result.put("upgradeMessage", "🔐 로그인하시면 더 많은 뉴스와 맞춤 추천을 받아보실 수 있어요!");
            }
            
            log.info("🎉 Enhanced 뉴스레터 응답 완료 - serviceLevel: {}", serviceLevel);
            
            return ResponseEntity.ok(ApiResponse.success(result, 
                String.format("%s Enhanced 뉴스레터", serviceLevel)));
            
        } catch (Exception e) {
            log.error("💥 Enhanced 뉴스레터 처리 중 심각한 오류", e);
            
            // 상세한 오류 정보 포함
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("errorClass", e.getClass().getSimpleName());
            errorDetails.put("errorMessage", e.getMessage());
            errorDetails.put("timestamp", LocalDateTime.now().toString());
            
            if (e.getCause() != null) {
                errorDetails.put("rootCause", e.getCause().getMessage());
            }
            
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("ENHANCED_NEWSLETTER_ERROR", 
                    "Enhanced API 오류: " + e.getMessage(), errorDetails));
        }
    }

    /**
     * 🎯 개인화된 카테고리 데이터 생성 (구독자용)
     */
    private Map<String, Object> getPersonalizedCategoryData(Long userId, int newsLimit, String[] allCategories) {
        Map<String, Object> categoryData = new HashMap<>();
        
        // 1. 구독 카테고리 우선 처리 (더 많은 뉴스)
        List<UserNewsletterSubscription> subscriptions = 
            subscriptionRepository.findActiveSubscriptionsByUserId(userId);
        
        List<String> subscribedCategories = subscriptions.stream()
            .map(sub -> convertEnglishToKorean(sub.getCategory()))
            .filter(Objects::nonNull)
            .toList();
        
        // 구독 카테고리는 더 많은 뉴스 제공 (newsLimit)
        for (String category : subscribedCategories) {
            try {
                List<NewsletterContent.Article> headlines = 
                    newsletterService.getCategoryHeadlines(category, newsLimit);
                
                Map<String, Object> categoryInfo = new HashMap<>();
                categoryInfo.put("articles", headlines);
                categoryInfo.put("isSubscribed", true);
                categoryInfo.put("articleCount", headlines.size());
                categoryInfo.put("priority", "HIGH");
                
                categoryData.put(category, categoryInfo);
                log.debug("구독 카테고리 {} 뉴스 {}개 수집 완료", category, headlines.size());
                
            } catch (Exception e) {
                log.warn("구독 카테고리 {} 뉴스 수집 실패", category, e);
            }
        }
        
        // 2. 나머지 카테고리는 기본 수량 제공
        int basicNewsLimit = Math.max(3, newsLimit / 2); // 구독 카테고리의 절반
        
        for (String category : allCategories) {
            if (!subscribedCategories.contains(category)) {
                try {
                    List<NewsletterContent.Article> headlines = 
                        newsletterService.getCategoryHeadlines(category, basicNewsLimit);
                    
                    Map<String, Object> categoryInfo = new HashMap<>();
                    categoryInfo.put("articles", headlines);
                    categoryInfo.put("isSubscribed", false);
                    categoryInfo.put("articleCount", headlines.size());
                    categoryInfo.put("priority", "NORMAL");
                    
                    categoryData.put(category, categoryInfo);
                    
                } catch (Exception e) {
                    log.warn("일반 카테고리 {} 뉴스 수집 실패", category, e);
                }
            }
        }
        
        return categoryData;
    }
    
    /**
     * 📰 표준 카테고리 데이터 생성 (공개/로그인 사용자용)
     */
    private Map<String, Object> getStandardCategoryData(String[] categories, int newsLimit) {
        Map<String, Object> categoryData = new HashMap<>();
        
        for (String category : categories) {
            try {
                List<NewsletterContent.Article> headlines = 
                    newsletterService.getCategoryHeadlines(category, newsLimit);
                
                Map<String, Object> categoryInfo = new HashMap<>();
                categoryInfo.put("articles", headlines);
                categoryInfo.put("isSubscribed", false);
                categoryInfo.put("articleCount", headlines.size());
                categoryInfo.put("priority", "NORMAL");
                
                categoryData.put(category, categoryInfo);
                log.debug("표준 카테고리 {} 뉴스 {}개 수집 완료", category, headlines.size());
            
        } catch (Exception e) {
                log.warn("표준 카테고리 {} 뉴스 수집 실패", category, e);
                categoryData.put(category, Map.of(
                    "articles", List.of(),
                    "isSubscribed", false,
                    "articleCount", 0,
                    "priority", "NORMAL",
                    "error", "뉴스를 가져올 수 없습니다"
                ));
            }
        }
        
        return categoryData;
    }
    
    /**
     * 개인화된 트렌딩 키워드 생성
     */
    private List<String> getPersonalizedTrendingKeywords(Long userId, int limit) {
        try {
            // 구독 카테고리 기반 키워드 가중치 적용
            List<UserNewsletterSubscription> subscriptions = 
                subscriptionRepository.findActiveSubscriptionsByUserId(userId);
            
            if (!subscriptions.isEmpty()) {
                // 구독 카테고리별 키워드 수집 후 통합
                List<String> personalizedKeywords = new ArrayList<>();
                
                for (UserNewsletterSubscription subscription : subscriptions) {
                    try {
                        String categoryKo = convertEnglishToKorean(subscription.getCategory());
                        if (categoryKo != null) {
                            List<String> categoryKeywords = 
                                newsletterService.getTrendingKeywordsByCategory(categoryKo, 3);
                                personalizedKeywords.addAll(categoryKeywords);
                        }
                    } catch (Exception e) {
                        log.warn("카테고리별 키워드 조회 실패: {}", subscription.getCategory(), e);
                    }
                }
                
                // 중복 제거 및 제한
                    return personalizedKeywords.stream()
                        .distinct()
                        .limit(limit)
                        .collect(Collectors.toList());
                }
        } catch (Exception e) {
            log.warn("개인화 키워드 생성 실패, 일반 키워드 사용: userId={}", userId, e);
            }
            
            // 폴백: 일반 트렌딩 키워드
        try {
            ResponseEntity<ApiResponse<List<String>>> response = getTrendingKeywords(limit);
            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
        } catch (Exception e) {
            log.warn("일반 트렌딩 키워드 조회 실패", e);
        }
        return new ArrayList<>();
    }
    
    /**
     * 안전한 카테고리 헤드라인 조회 - 캐스팅 오류 방지
     */
    private List<Map<String, Object>> getCategoryHeadlinesSafely(String category, int limit) {
        try {
            // 한국어 카테고리를 영어로 변환
            String englishCategory = convertKoreanToEnglish(category);
            if (englishCategory == null) {
                log.warn("카테고리 변환 실패: {}", category);
                return createFallbackNews(category, limit);
            }
            
            // NewsServiceClient 직접 호출
            Page<NewsResponse> response = newsServiceClient.getNewsByCategory(englishCategory, 0, limit);
            
            if (response != null && !response.getContent().isEmpty()) {
                return response.getContent().stream()
                    .map(this::convertNewsResponseToMap)
                    .collect(Collectors.toList());
            } else {
                log.warn("뉴스 서비스 응답 실패: category={} (english: {})", category, englishCategory);
                return createFallbackNews(category, limit);
            }
            
        } catch (Exception e) {
            log.warn("뉴스 서비스 호출 실패: category={}, error={}", category, e.getMessage());
            return createFallbackNews(category, limit);
        }
    }

    /**
     * NewsResponse를 Map으로 변환
     */
    private Map<String, Object> convertNewsResponseToMap(NewsResponse news) {
        Map<String, Object> articleMap = new HashMap<>();
        articleMap.put("id", news.getNewsId());
        articleMap.put("title", news.getTitle());
        articleMap.put("summary", news.getSummary() != null ? news.getSummary() : "");
        articleMap.put("url", news.getLink() != null ? news.getLink() : "#");
        articleMap.put("publishedAt", news.getPublishedAt() != null ? news.getPublishedAt() : LocalDateTime.now().toString());
        articleMap.put("category", news.getCategoryName());
        articleMap.put("viewCount", news.getViewCount() != null ? news.getViewCount() : 0);
        articleMap.put("shareCount", news.getShareCount() != null ? news.getShareCount() : 0);
        return articleMap;
    }

    /**
     * 안전한 트렌딩 키워드 조회
     */
    private List<String> getTrendingKeywordsSafely(int limit) {
        try {
            // NewsServiceClient를 통한 트렌딩 키워드 조회
            ApiResponse<List<TrendingKeywordDto>> response = newsServiceClient.getTrendingKeywordsByCategory("GENERAL", limit, "24h", 24);
                
            if (response != null && response.isSuccess() && response.getData() != null && !response.getData().isEmpty()) {
                return response.getData().stream()
                    .map(TrendingKeywordDto::getKeyword)
                    .filter(Objects::nonNull)
                    .limit(limit)
                    .collect(Collectors.toList());
            } else {
                log.warn("트렌딩 뉴스 서비스 응답 실패");
                return createFallbackKeywords(limit);
            }
            
        } catch (Exception e) {
            log.warn("트렌딩 뉴스 조회 실패: {}", e.getMessage());
            return createFallbackKeywords(limit);
        }
    }

    /**
     * 서비스 레벨별 뉴스 수량 결정
     */
    private int determineNewsLimit(Long userId) {
        if (userId == null) {
            return 5; // 📰 비로그인: 5개
        }
        
        try {
            List<UserNewsletterSubscription> subscriptions = 
                subscriptionRepository.findActiveSubscriptionsByUserId(userId);
            
            if (subscriptions.isEmpty()) {
                return 7; // 🔐 로그인 (구독 없음): 7개
            } else {
                return 10; // 🎯 구독자: 10개
            }
        } catch (Exception e) {
            log.warn("구독 정보 조회 실패, 기본값 사용: userId={}", userId, e);
            // 데이터베이스 연결 실패 시 기본값 반환
            return userId == null ? 5 : 7;
        }
    }

    /**
     * 서비스 레벨 결정
     */
    private String determineServiceLevel(Long userId) {
        if (userId == null) {
            return "PUBLIC";
        }
        
        try {
            List<UserNewsletterSubscription> subscriptions = 
                subscriptionRepository.findActiveSubscriptionsByUserId(userId);
            
            if (subscriptions.isEmpty()) {
                return "AUTHENTICATED_BASIC";
            } else {
                return "PERSONALIZED_PREMIUM";
            }
        } catch (Exception e) {
            log.warn("서비스 레벨 결정 실패: userId={}", userId, e);
            // 데이터베이스 연결 실패 시 기본값 반환
            return userId == null ? "PUBLIC" : "AUTHENTICATED_BASIC";
        }
    }

    /**
     * 서비스 레벨별 정보 제공
     */
    private Map<String, Object> getServiceLevelInfo(String serviceLevel, Long userId) {
        Map<String, Object> info = new HashMap<>();
        
        switch (serviceLevel) {
            case "PUBLIC" -> {
                info.put("displayName", "📰 공개 서비스");
                info.put("description", "기본 뉴스 제공");
                info.put("newsLimit", 5);
                info.put("features", List.of("기본 뉴스", "트렌딩 키워드"));
                info.put("limitations", List.of("제한된 뉴스 수", "개인화 없음"));
            }
            case "AUTHENTICATED_BASIC" -> {
                info.put("displayName", "🔐 로그인 서비스");
                info.put("description", "확장된 뉴스 제공");
                info.put("newsLimit", 7);
                info.put("features", List.of("더 많은 뉴스", "구독 관리", "읽기 기록"));
                info.put("limitations", List.of("개인화 제한적"));
            }
            case "PERSONALIZED_PREMIUM" -> {
                info.put("displayName", "🎯 개인화 서비스");
                info.put("description", "완전 맞춤 서비스");
                info.put("newsLimit", 10);
                info.put("features", List.of("개인화 뉴스", "AI 추천", "맞춤 키워드", "구독 관리"));
                info.put("limitations", Collections.emptyList());
            }
        }
        
        return info;
    }

    /**
     * 카테고리 구독 여부 확인
     */
    private boolean checkCategorySubscription(Long userId, String categoryKo) {
        try {
            String categoryEn = convertKoreanToEnglish(categoryKo);
            if (categoryEn != null) {
                Optional<UserNewsletterSubscription> subscription = 
                    subscriptionRepository.findByUserIdAndCategory(userId, categoryEn);
                return subscription.isPresent() && subscription.get().getIsActive();
            }
        } catch (Exception e) {
            log.warn("구독 여부 확인 실패: userId={}, category={}", userId, categoryKo, e);
        }
        return false;
    }


    /**
     * 폴백 뉴스 생성
     */
    private List<Map<String, Object>> createFallbackNews(String category, int limit) {
        List<Map<String, Object>> fallbackNews = new ArrayList<>();
        
        for (int i = 1; i <= limit; i++) {
            Map<String, Object> article = new HashMap<>();
            article.put("id", "fallback_" + category + "_" + i);
            article.put("title", String.format("[%s] 샘플 뉴스 %d", category, i));
            article.put("summary", "현재 뉴스를 가져올 수 없습니다. 잠시 후 다시 시도해주세요.");
            article.put("url", "");
            article.put("publishedAt", LocalDateTime.now().minusHours(i).toString());
            article.put("category", category);
            article.put("viewCount", 0);
            article.put("shareCount", 0);
            article.put("isFallback", true);
            
            fallbackNews.add(article);
        }
        
        return fallbackNews;
    }

    /**
     * 폴백 키워드 생성
     */
    private List<String> createFallbackKeywords(int limit) {
        List<String> fallbackKeywords = Arrays.asList(
            "경제", "정치", "사회", "기술", "문화", "스포츠", "국제", "환경", "교육", "건강"
        );
        
        return fallbackKeywords.stream()
            .limit(limit)
                            .collect(Collectors.toList());
    }

    /**
     * 서비스 레벨별 성공 메시지
     */
    private String getSuccessMessage(NewsletterServiceLevel.ServiceCapability capability) {
        return switch (capability) {
            case PUBLIC -> "📰 일반 뉴스레터를 제공합니다. 로그인하시면 더 많은 혜택을 받으실 수 있어요!";
            case AUTHENTICATED_BASIC -> "🔐 로그인 사용자 전용 확장 뉴스레터입니다. 구독하시면 개인화 서비스를 이용하실 수 있어요!";
            case PERSONALIZED_PREMIUM -> "🎯 구독 기반 개인화된 뉴스레터를 제공합니다!";
        };
    }

    /**
     * 🔄 하이브리드 뉴스레터 API - 로그인 상태에 따라 자동 전환
     * 로그인: 개인화 / 비로그인: 공개
     */
    @GetMapping("/hybrid")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHybridNewsletter(
            @RequestParam(defaultValue = "5") int limit,
            HttpServletRequest request) {
        
        try {
            log.info("하이브리드 뉴스레터 요청: limit={}", limit);
            
            // JWT 토큰 확인 (선택적)
            Long userId = null;
            boolean isAuthenticated = false;
            
            try {
                userId = super.extractUserIdFromToken(request);
                isAuthenticated = true;
                log.info("인증된 사용자 하이브리드 뉴스레터 요청: userId={}", userId);
            } catch (Exception e) {
                log.info("비인증 사용자 하이브리드 뉴스레터 요청");
            }
            
            Map<String, Object> result = new HashMap<>();
            
            if (isAuthenticated && userId != null) {
                // 🎯 개인화된 서비스 제공
                Integer subscriptionCount = 0;
                try {
                    List<UserNewsletterSubscription> subscriptions = 
                        subscriptionRepository.findActiveSubscriptionsByUserId(userId);
                    subscriptionCount = subscriptions.size();
                } catch (Exception e) {
                    log.warn("구독 정보 조회 실패", e);
                }
                
                com.newsletterservice.enums.ServiceLevel serviceLevel = 
                    com.newsletterservice.enums.ServiceLevel.determineLevel(isAuthenticated, subscriptionCount);
                
                if (serviceLevel == com.newsletterservice.enums.ServiceLevel.PERSONALIZED_PREMIUM) {
                    // 개인화된 콘텐츠 생성
                    result = new HashMap<>();
                    String[] categories = {"정치", "경제", "사회", "생활", "IT/과학", "세계"};
                    Map<String, Object> categoryData = getPersonalizedCategoryData(userId, limit, categories);
                    List<String> trendingKeywords = getPersonalizedTrendingKeywords(userId, 8);
                    
            result.put("categories", categoryData);
            result.put("trendingKeywords", trendingKeywords);
                    result.put("type", "HYBRID_PERSONALIZED");
                    result.put("message", "🎯 개인화된 뉴스를 제공합니다");
                } else {
                    // 인증 기본 콘텐츠 생성
                    result = new HashMap<>();
                    String[] categories = {"정치", "경제", "사회", "생활", "IT/과학", "세계"};
                    Map<String, Object> categoryData = getStandardCategoryData(categories, limit);
                    List<String> trendingKeywords = getPersonalizedTrendingKeywords(userId, 8);
                    
                    result.put("categories", categoryData);
                    result.put("trendingKeywords", trendingKeywords);
                    result.put("type", "HYBRID_AUTHENTICATED");
                    result.put("message", "🔐 로그인하셨습니다. 카테고리를 구독하면 맞춤 뉴스를 받아보실 수 있어요!");
                }
                
            } else {
                // 📰 공개 서비스 제공
                result = new HashMap<>();
                String[] categories = {"정치", "경제", "사회", "IT/과학", "세계"};
                Map<String, Object> categoryData = getStandardCategoryData(categories, limit);
                List<String> trendingKeywords = getPersonalizedTrendingKeywords(null, 8);
                
                result.put("categories", categoryData);
                result.put("trendingKeywords", trendingKeywords);
                result.put("type", "HYBRID_PUBLIC");
                result.put("message", "📰 일반 뉴스를 제공합니다");
                result.put("upgradeMessage", "🔐 로그인하시면 관심사 기반 맞춤 뉴스를 받아보실 수 있어요!");
            }
            
            // 공통 메타데이터
            result.put("userAuthenticated", isAuthenticated);
            result.put("userId", userId);
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("serviceType", "HYBRID");
            
            return ResponseEntity.ok(ApiResponse.success(result, 
                isAuthenticated ? "개인화된 뉴스를 제공합니다" : "일반 뉴스를 제공합니다. 로그인하면 맞춤 뉴스를 받아보실 수 있어요!"));
                
        } catch (Exception e) {
            log.error("하이브리드 뉴스레터 조회 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("HYBRID_NEWSLETTER_ERROR", "뉴스레터 조회 중 오류가 발생했습니다"));
        }
    }

    /**
     * 📊 스마트 추천 API - 로그인 상태별 다른 로직
     */
    @GetMapping("/smart-recommendations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSmartRecommendations(
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {
        
        try {
            log.info("스마트 추천 요청: limit={}", limit);
            
            Long userId = null;
            boolean isAuthenticated = false;
            
            try {
                userId = super.extractUserIdFromToken(request);
                isAuthenticated = true;
            } catch (Exception e) {
                // 인증 실패는 정상적인 시나리오
            }
            
            Map<String, Object> result = new HashMap<>();
            
            if (isAuthenticated && userId != null) {
                // 🎯 개인화 추천
                try {
                    // TODO: PersonalizedRecommendationService와 연동
                    result.put("recommendationType", "PERSONALIZED");
                    result.put("message", "행동 패턴과 관심사를 분석한 맞춤 뉴스입니다");
                    result.put("userId", userId);
                    result.put("isPersonalized", true);
                    result.put("preview", "개인화 추천 기능이 준비되어 있습니다");
                    
                } catch (Exception e) {
                    log.warn("개인화 추천 생성 실패", e);
                    result.put("recommendationType", "FALLBACK");
                    result.put("message", "개인화 추천을 생성할 수 없어 일반 추천을 제공합니다");
                }
                
            } else {
                // 📈 트렌딩 추천
                try {
                    com.newsletterservice.common.ApiResponse<org.springframework.data.domain.Page<com.newsletterservice.client.dto.NewsResponse>> trendingResponse = 
                        newsServiceClient.getTrendingNews(24, limit);
                    
                    if (trendingResponse.isSuccess() && trendingResponse.getData() != null) {
                        List<Map<String, Object>> trendingNews = trendingResponse.getData().getContent().stream()
                            .map(news -> {
                                Map<String, Object> newsItem = new HashMap<>();
                                newsItem.put("id", news.getNewsId());
                                newsItem.put("title", news.getTitle());
                                newsItem.put("summary", news.getSummary());
                                newsItem.put("url", news.getLink());
                                newsItem.put("publishedAt", news.getPublishedAt());
                                newsItem.put("category", news.getCategoryName());
                                newsItem.put("imageUrl", news.getImageUrl());
                                return newsItem;
                            })
                            .collect(Collectors.toList());
                        
                        result.put("recommendationType", "TRENDING");
                        result.put("message", "지금 많은 사람들이 읽고 있는 인기 뉴스입니다");
                        result.put("recommendations", trendingNews);
                        result.put("isPersonalized", false);
                        
                    } else {
                        result.put("recommendationType", "EMPTY");
                        result.put("message", "현재 추천 뉴스를 가져올 수 없습니다");
                        result.put("recommendations", Collections.emptyList());
                    }
                    
                } catch (Exception e) {
                    log.warn("트렌딩 뉴스 조회 실패, 폴백 사용", e);
                    result.put("recommendationType", "EMPTY");
                    result.put("message", "현재 추천 뉴스를 가져올 수 없습니다");
                    result.put("recommendations", Collections.emptyList());
                }
            }
            
            result.put("userAuthenticated", isAuthenticated);
            result.put("userId", userId);
            result.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("스마트 추천 조회 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("SMART_RECOMMENDATION_ERROR", "추천 뉴스 조회 중 오류가 발생했습니다"));
        }
    }

    /**
     * 카테고리별 상세 정보 조회 - 확장된 뉴스 정보
     */
    @GetMapping("/enhanced/category/{category}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEnhancedCategoryDetails(
            @PathVariable String category,
            @RequestParam(defaultValue = "10") int headlinesLimit,
            @RequestParam(defaultValue = "8") int keywordsLimit,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("카테고리 상세 정보 조회: category={}, headlinesLimit={}, keywordsLimit={}", 
                    category, headlinesLimit, keywordsLimit);
            
            // 사용자 ID 추출 (선택적)
            Long userId = null;
            try {
                userId = super.extractUserIdFromToken(httpRequest);
            } catch (Exception e) {
                log.info("비인증 사용자 접근");
            }
            
            Map<String, Object> result = new HashMap<>();
            
            // 1. 카테고리별 헤드라인 (더 많은 수)
            List<NewsletterContent.Article> headlines = newsletterService.getCategoryHeadlines(category, headlinesLimit);
            List<Map<String, Object>> headlineList = headlines.stream()
                    .map(article -> {
                        Map<String, Object> headline = new HashMap<>();
                        headline.put("id", article.getId());
                        headline.put("title", article.getTitle());
                        headline.put("summary", article.getSummary());
                        headline.put("url", article.getUrl());
                        headline.put("publishedAt", article.getPublishedAt());
                        headline.put("category", article.getCategory());
                        return headline;
                    })
                    .collect(Collectors.toList());
            
            // 2. 카테고리별 트렌딩 키워드
            List<String> categoryKeywords = new ArrayList<>();
            try {
                // NewsServiceClient를 통해 실제 카테고리별 키워드 조회
                String englishCategory = convertCategoryToEnglish(category);
                if (englishCategory != null) {
                    com.newsletterservice.common.ApiResponse<List<com.newsletterservice.client.dto.TrendingKeywordDto>> response = 
                            newsServiceClient.getTrendingKeywordsByCategory(englishCategory, keywordsLimit, "24h", 24);
                    
                    if (response.isSuccess() && response.getData() != null) {
                        categoryKeywords = response.getData().stream()
                                .map(com.newsletterservice.client.dto.TrendingKeywordDto::getKeyword)
                                .collect(Collectors.toList());
                        log.info("실제 카테고리별 키워드 수집 완료: category={}, keywords={}", category, categoryKeywords.size());
                    } else {
                        // 폴백: 기본 키워드 사용
                        categoryKeywords = getDefaultCategoryKeywords(category);
                        log.warn("카테고리별 키워드 API 응답 실패, 기본 키워드 사용: category={}", category);
                    }
                }
            } catch (Exception e) {
                log.warn("카테고리별 키워드 조회 실패, 기본 키워드 사용: category={}, error={}", category, e.getMessage());
                categoryKeywords = getDefaultCategoryKeywords(category);
            }
            
            // 3. 사용자 구독 상태 (인증된 경우)
            Map<String, Object> subscriptionStatus = new HashMap<>();
            if (userId != null) {
                try {
                    String englishCategory = convertCategoryToEnglish(category);
                    if (englishCategory != null) {
                        Optional<UserNewsletterSubscription> subscription = subscriptionRepository
                                .findByUserIdAndCategory(userId, englishCategory);
                        
                        subscriptionStatus.put("isSubscribed", subscription.isPresent() && subscription.get().getIsActive());
                        subscriptionStatus.put("subscriptionId", subscription.map(UserNewsletterSubscription::getId).orElse(null));
                        subscriptionStatus.put("subscribedAt", subscription.map(UserNewsletterSubscription::getSubscribedAt).orElse(null));
                    }
                } catch (Exception e) {
                    log.warn("구독 상태 조회 실패: {}", e.getMessage());
                }
            }
            
            // 4. 결과 조립
            result.put("category", category);
            result.put("categoryEn", convertCategoryToEnglish(category));
            result.put("headlines", headlineList);
            result.put("trendingKeywords", categoryKeywords);
            result.put("subscriptionStatus", subscriptionStatus);
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("totalHeadlines", headlineList.size());
            result.put("totalKeywords", categoryKeywords.size());
            
            log.info("카테고리 상세 정보 조회 완료: category={}, headlines={}, keywords={}", 
                    category, headlineList.size(), categoryKeywords.size());
            
            return ResponseEntity.ok(ApiResponse.success(result, "카테고리 상세 정보가 조회되었습니다."));
            
        } catch (Exception e) {
            log.error("카테고리 상세 정보 조회 실패: category={}", category, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("CATEGORY_DETAILS_ERROR", "카테고리 상세 정보 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 영어 카테고리를 한국어로 변환하는 헬퍼 메서드 (NewsCategory enum 사용)
     */
    private String convertEnglishToKorean(String englishCategory) {
        try {
            NewsCategory category = NewsCategory.valueOf(englishCategory);
            return category.getCategoryName();
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 카테고리: {}", englishCategory);
            return null;
        }
    }

    /**
     * 한국어 카테고리를 영어로 변환하는 헬퍼 메서드 (NewsCategory enum 사용)
     */
    private String convertKoreanToEnglish(String koreanCategory) {
        for (NewsCategory category : NewsCategory.values()) {
            if (category.getCategoryName().equals(koreanCategory)) {
                return category.name();
            }
        }
        log.warn("알 수 없는 한국어 카테고리: {}", koreanCategory);
        return null;
    }

    /**
     * 카테고리별 기본 키워드를 반환하는 헬퍼 메서드
     */
    /**
     * 디버깅용: 뉴스 서비스 연결 테스트
     */
    @GetMapping("/debug/news-service-test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testNewsServiceConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("뉴스 서비스 연결 테스트 시작");
            
            // 1. 카테고리 조회 테스트
            try {
                ApiResponse<List<CategoryDto>> categoriesResponse = newsServiceClient.getCategories();
                result.put("categoriesTest", Map.of(
                    "success", categoriesResponse != null,
                    "hasData", categoriesResponse != null && categoriesResponse.getData() != null,
                    "count", categoriesResponse != null && categoriesResponse.getData() != null ? 
                        categoriesResponse.getData().size() : 0
                ));
            } catch (Exception e) {
                result.put("categoriesTest", Map.of(
                    "success", false,
                    "error", e.getMessage()
                ));
            }
            
            // 2. 뉴스 조회 테스트 (직접 영어 카테고리 사용)
            try {
                Page<NewsResponse> newsResponse = newsServiceClient.getNewsByCategory("POLITICS", 0, 5);
                result.put("newsTest", Map.of(
                    "success", newsResponse != null,
                    "hasData", newsResponse != null && !newsResponse.getContent().isEmpty(),
                    "count", newsResponse != null ? newsResponse.getContent().size() : 0
                ));
            } catch (Exception e) {
                result.put("newsTest", Map.of(
                    "success", false,
                    "error", e.getMessage()
                ));
            }
            
            // 3. 트렌딩 키워드 테스트
            try {
                ApiResponse<List<TrendingKeywordDto>> keywordsResponse = newsServiceClient.getTrendingKeywordsByCategory("GENERAL", 5, "24h", 24);
                result.put("keywordsTest", Map.of(
                    "success", keywordsResponse != null && keywordsResponse.isSuccess(),
                    "hasData", keywordsResponse != null && keywordsResponse.isSuccess() && keywordsResponse.getData() != null && !keywordsResponse.getData().isEmpty(),
                    "count", keywordsResponse != null && keywordsResponse.isSuccess() && keywordsResponse.getData() != null ? keywordsResponse.getData().size() : 0
                ));
            } catch (Exception e) {
                result.put("keywordsTest", Map.of(
                    "success", false,
                    "error", e.getMessage()
                ));
            }
            
            result.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(ApiResponse.success(result, "뉴스 서비스 연결 테스트 완료"));
            
        } catch (Exception e) {
            log.error("뉴스 서비스 연결 테스트 실패", e);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("NEWS_SERVICE_TEST_ERROR", "뉴스 서비스 연결 테스트 실패", result));
        }
    }

    private List<String> getDefaultCategoryKeywords(String category) {
        Map<String, List<String>> categoryKeywordMap = Map.of(
            "정치", List.of("대통령", "국회", "선거", "정당", "정책", "외교", "국방", "안보"),
            "경제", List.of("주식", "부동산", "금리", "인플레이션", "GDP", "고용", "기업", "투자"),
            "사회", List.of("교육", "의료", "복지", "범죄", "교통", "환경", "노동", "문화"),
            "생활", List.of("건강", "요리", "육아", "여행", "쇼핑", "패션", "뷰티", "취미"),
            "세계", List.of("미국", "중국", "일본", "유럽", "러시아", "북한", "국제", "글로벌"),
            "IT/과학", List.of("AI", "반도체", "스마트폰", "게임", "소프트웨어", "하드웨어", "연구", "기술"),
            "자동차/교통", List.of("전기차", "자율주행", "교통", "대중교통", "도로", "주차", "운전", "모터쇼"),
            "여행/음식", List.of("해외여행", "국내여행", "맛집", "레스토랑", "카페", "호텔", "항공", "관광"),
            "예술", List.of("영화", "음악", "미술", "연극", "뮤지컬", "전시", "공연", "문화")
        );
        return categoryKeywordMap.getOrDefault(category, new ArrayList<>());
    }
}

