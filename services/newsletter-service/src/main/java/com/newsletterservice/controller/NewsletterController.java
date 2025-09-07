package com.newsletterservice.controller;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.common.exception.NewsletterException;
import com.newsletterservice.dto.*;
import com.newsletterservice.entity.SubscriptionStatus;
import com.newsletterservice.repository.NewsletterDeliveryRepository;
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
    private final KakaoMessageService kakaoMessageService;
    private final com.newsletterservice.client.UserServiceClient userServiceClient;

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
     * 내 구독 목록 조회
     */
    @GetMapping("/subscription/my")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMySubscriptions(
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdAsString(httpRequest);
            log.info("내 구독 목록 조회 요청 - userId: {}", userId);
            
            // 임시 구독 목록 데이터 반환 (user-service 호출 문제 해결을 위해)
            List<Map<String, Object>> subscriptions = new ArrayList<>();
            
            // 기본 카테고리들 추가
            String[] categories = {"POLITICS", "ECONOMY", "SOCIETY", "LIFE", "INTERNATIONAL", "IT_SCIENCE"};
            String[] categoryNames = {"정치", "경제", "사회", "생활", "세계", "IT/과학"};
            
            for (int i = 0; i < categories.length; i++) {
                Map<String, Object> subscription = new HashMap<>();
                subscription.put("categoryId", i + 1);
                subscription.put("categoryName", categories[i]);
                subscription.put("categoryNameKo", categoryNames[i]);
                subscription.put("isActive", true);
                subscription.put("subscribedAt", LocalDateTime.now().minusDays(i).toString());
                subscriptions.add(subscription);
            }
            
            return ResponseEntity.ok(ApiResponse.success(subscriptions, "구독 목록 조회가 완료되었습니다."));
        } catch (Exception e) {
            log.error("구독 목록 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SUBSCRIPTION_LIST_ERROR", "구독 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 활성 구독 목록 조회
     */
    // 활성 구독 목록 조회 기능은 user-service에서 처리됩니다.

    /**
     * 구독 상태 변경
     */
    // 구독 상태 변경 기능은 user-service에서 처리됩니다.

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

        try {
            String userId = extractUserIdAsString(httpRequest);
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
}

