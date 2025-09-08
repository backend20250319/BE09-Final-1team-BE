package com.newsletterservice.controller;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.common.exception.NewsletterException;
import com.newsletterservice.dto.*;
import com.newsletterservice.entity.SubscriptionStatus;
import com.newsletterservice.entity.UserNewsletterSubscription;
import com.newsletterservice.repository.NewsletterDeliveryRepository;
import com.newsletterservice.repository.UserNewsletterSubscriptionRepository;
import com.newsletterservice.service.EmailNewsletterRenderer;
import com.newsletterservice.service.KakaoMessageService;
import com.newsletterservice.service.NewsletterService;
import com.newsletterservice.util.JwtUtil;
import com.newsletterservice.client.dto.CategoryResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final UserNewsletterSubscriptionRepository subscriptionRepository;

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
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMySubscriptions(
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
            
            List<Map<String, Object>> result = new ArrayList<>();
            
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
                
                result.add(subscriptionCard);
            }
            
            log.info("활성 구독 목록 조회 완료: userId={}, count={}", userId, result.size());
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
            
            log.info("구독 상태 변경 시작: userId={}, category={}, isActive={}", userId, category, isActive);
            
            // 입력값 검증
            if (category == null || category.trim().isEmpty()) {
                log.warn("카테고리가 비어있습니다: category={}", category);
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_CATEGORY", "카테고리가 필요합니다."));
            }
            
            if (isActive == null) {
                log.warn("isActive 값이 null입니다: isActive={}", isActive);
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_ACTIVE_STATUS", "활성화 상태가 필요합니다."));
            }
            
            // 타임아웃 체크를 위한 CompletableFuture 사용
            CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // 기존 구독 정보 확인
                    Optional<UserNewsletterSubscription> existing = subscriptionRepository.findByUserIdAndCategory(userId, category);
                    
                    if (existing.isPresent()) {
                        // 기존 구독 정보 업데이트
                        int updatedRows = subscriptionRepository.updateSubscriptionStatus(userId, category, isActive);
                        log.info("구독 상태 업데이트 완료: userId={}, category={}, isActive={}, updatedRows={}", 
                                userId, category, isActive, updatedRows);
                    } else {
                        // 새로운 구독 정보 생성
                        UserNewsletterSubscription newSubscription = UserNewsletterSubscription.builder()
                            .userId(userId)
                            .category(category)
                            .isActive(isActive)
                            .subscribedAt(LocalDateTime.now())
                            .build();
                        subscriptionRepository.save(newSubscription);
                        log.info("새 구독 정보 생성 완료: userId={}, category={}, isActive={}", userId, category, isActive);
                    }
                    
                    // 업데이트된 구독자 수 조회 (fallback 처리)
                    Long updatedSubscriberCount = getSubscriberCountWithFallback(category);
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("message", isActive ? "구독이 활성화되었습니다." : "구독이 비활성화되었습니다.");
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
                
                // 기존 구독 정보가 없으면 생성
                if (!subscriptionRepository.existsByUserIdAndCategory(userId, category)) {
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
     * 액세스 토큰 추출 헬퍼 메서드
     */
    private String extractAccessToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }
}

