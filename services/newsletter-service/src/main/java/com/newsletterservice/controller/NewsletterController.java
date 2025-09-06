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
    @GetMapping("/subscription/{id}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscription(
            @PathVariable Long id,
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
            
            List<SubscriptionResponse> subscriptions = newsletterService.getMySubscriptions(Long.valueOf(userId));
            
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
            
            if (newStatus == null || newStatus.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_STATUS", "상태값이 필요합니다."));
            }
            
            log.info("구독 상태 변경 요청 - userId: {}, subscriptionId: {}, newStatus: {}", 
                    userId, subscriptionId, newStatus);
            
            SubscriptionResponse subscription = newsletterService.changeSubscriptionStatus(
                    subscriptionId, Long.valueOf(userId), newStatus);
            
            return ResponseEntity.ok(ApiResponse.success(subscription, "구독 상태가 변경되었습니다."));
        } catch (NewsletterException e) {
            log.warn("구독 상태 변경 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
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
     * 뉴스레터 상세 조회 (ID 검증 강화)
     */
    @GetMapping("/{newsletterId}")
    public ResponseEntity<ApiResponse<Object>> getNewsletterDetail(
            @PathVariable String newsletterId) {
        
        log.info("뉴스레터 상세 조회 요청: newsletterId={}", newsletterId);
        
        // 1. ID 형식 검증
        Long id = validateAndParseId(newsletterId);
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
     * 뉴스레터 미리보기 (ID 검증 강화)
     */
    @GetMapping("/{newsletterId}/preview")
    public ResponseEntity<?> getNewsletterPreview(
            @PathVariable String newsletterId) {
        
        log.info("뉴스레터 미리보기 요청: newsletterId={}", newsletterId);
        
        // 1. ID 형식 검증
        Long id = validateAndParseId(newsletterId);
        if (id == null) {
            String errorHtml = generateErrorHtml(
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
            String errorHtml = generateErrorHtml(
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

    // ========================================
    // ID 검증 및 오류 처리 헬퍼 메서드
    // ========================================

    /**
     * ID 형식 검증 및 파싱
     */
    private Long validateAndParseId(String idString) {
        if (idString == null || idString.trim().isEmpty()) {
            return null;
        }
        
        // 템플릿 문자열 체크
        if (idString.contains("{") || idString.contains("}")) {
            log.warn("템플릿 문자열이 ID로 전달됨: {}", idString);
            return null;
        }
        
        try {
            return Long.parseLong(idString.trim());
        } catch (NumberFormatException e) {
            log.warn("잘못된 ID 형식: {}", idString);
            return null;
        }
    }

    /**
     * 오류 HTML 생성
     */
    private String generateErrorHtml(String title, String message, String suggestion) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        max-width: 600px;
                        margin: 50px auto;
                        padding: 20px;
                        text-align: center;
                        background-color: #f5f5f5;
                    }
                    .error-container {
                        background: white;
                        padding: 40px;
                        border-radius: 10px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    }
                    .error-icon {
                        font-size: 48px;
                        color: #e74c3c;
                        margin-bottom: 20px;
                    }
                    .error-title {
                        color: #e74c3c;
                        font-size: 24px;
                        margin-bottom: 10px;
                    }
                    .error-message {
                        color: #666;
                        margin-bottom: 20px;
                        line-height: 1.6;
                    }
                    .suggestion {
                        background: #e3f2fd;
                        padding: 15px;
                        border-radius: 5px;
                        color: #1976d2;
                        margin-bottom: 20px;
                    }
                    .back-button {
                        display: inline-block;
                        background: #2196f3;
                        color: white;
                        padding: 10px 20px;
                        text-decoration: none;
                        border-radius: 5px;
                        margin-top: 10px;
                    }
                    .back-button:hover {
                        background: #1976d2;
                    }
                </style>
            </head>
            <body>
                <div class="error-container">
                    <div class="error-icon">⚠️</div>
                    <h1 class="error-title">%s</h1>
                    <p class="error-message">%s</p>
                    <div class="suggestion">
                        💡 %s
                    </div>
                    <a href="javascript:history.back()" class="back-button">뒤로 가기</a>
                </div>
            </body>
            </html>
            """, title, title, message, suggestion);
    }
}

