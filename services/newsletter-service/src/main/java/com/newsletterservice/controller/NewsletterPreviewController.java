package com.newsletterservice.controller;

import com.newsletterservice.dto.FeedTemplate;
import com.newsletterservice.service.FeedTemplateService;
import com.newsletterservice.service.NewsletterAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 뉴스레터 미리보기 컨트롤러
 * 프론트엔드 미리보기 페이지용 API를 제공합니다.
 */
@Tag(name = "Newsletter Preview", description = "뉴스레터 미리보기 API")
@RestController
@RequestMapping("/api/newsletter/preview")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NewsletterPreviewController {
    
    private final FeedTemplateService feedTemplateService;
    private final NewsletterAnalyticsService analyticsService;
    
    /**
     * 피드 B형 개인화 뉴스레터 미리보기
     */
    @Operation(
        summary = "피드 B형 개인화 뉴스레터 미리보기",
        description = "사용자의 관심사를 반영한 피드 B형 뉴스레터 미리보기를 제공합니다."
    )
    @GetMapping("/feed-b/personalized/{userId}")
    public ResponseEntity<Map<String, Object>> getPersonalizedFeedBPreview(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId) {
        
        log.info("피드 B형 개인화 뉴스레터 미리보기 요청: userId={}", userId);
        
        try {
            // 피드 B형 템플릿 생성
            FeedTemplate feedTemplate = feedTemplateService.createPersonalizedFeedTemplate(
                    userId, FeedTemplate.FeedType.FEED_B);
            
            // 미리보기용 응답 데이터 구성
            Map<String, Object> previewData = createPreviewResponse(feedTemplate, "personalized", userId.toString());
            
            // 뉴스레터 발송 통계 기록
            analyticsService.recordNewsletterDelivery(userId, "FEED_B_PERSONALIZED", true);
            
            log.info("피드 B형 개인화 뉴스레터 미리보기 완료: userId={}", userId);
            return ResponseEntity.ok(previewData);
            
        } catch (Exception e) {
            log.error("피드 B형 개인화 뉴스레터 미리보기 실패: userId={}", userId, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("미리보기 생성 실패"));
        }
    }
    
    /**
     * 피드 B형 트렌딩 뉴스레터 미리보기
     */
    @Operation(
        summary = "피드 B형 트렌딩 뉴스레터 미리보기",
        description = "트렌딩 뉴스로 구성된 피드 B형 뉴스레터 미리보기를 제공합니다."
    )
    @GetMapping("/feed-b/trending")
    public ResponseEntity<Map<String, Object>> getTrendingFeedBPreview() {
        
        log.info("피드 B형 트렌딩 뉴스레터 미리보기 요청");
        
        try {
            // 피드 B형 템플릿 생성
            FeedTemplate feedTemplate = feedTemplateService.createTrendingFeedTemplate(
                    FeedTemplate.FeedType.FEED_B);
            
            // 미리보기용 응답 데이터 구성
            Map<String, Object> previewData = createPreviewResponse(feedTemplate, "trending", null);
            
            log.info("피드 B형 트렌딩 뉴스레터 미리보기 완료");
            return ResponseEntity.ok(previewData);
            
        } catch (Exception e) {
            log.error("피드 B형 트렌딩 뉴스레터 미리보기 실패", e);
            return ResponseEntity.internalServerError().body(createErrorResponse("미리보기 생성 실패"));
        }
    }
    
    /**
     * 피드 B형 카테고리별 뉴스레터 미리보기
     */
    @Operation(
        summary = "피드 B형 카테고리별 뉴스레터 미리보기",
        description = "특정 카테고리의 뉴스로 구성된 피드 B형 뉴스레터 미리보기를 제공합니다."
    )
    @GetMapping("/feed-b/category/{category}")
    public ResponseEntity<Map<String, Object>> getCategoryFeedBPreview(
            @Parameter(description = "카테고리명", required = true)
            @PathVariable String category) {
        
        log.info("피드 B형 카테고리별 뉴스레터 미리보기 요청: category={}", category);
        
        try {
            // 피드 B형 템플릿 생성
            FeedTemplate feedTemplate = feedTemplateService.createCategoryFeedTemplate(
                    category, FeedTemplate.FeedType.FEED_B);
            
            // 미리보기용 응답 데이터 구성
            Map<String, Object> previewData = createPreviewResponse(feedTemplate, "category", category);
            
            log.info("피드 B형 카테고리별 뉴스레터 미리보기 완료: category={}", category);
            return ResponseEntity.ok(previewData);
            
        } catch (Exception e) {
            log.error("피드 B형 카테고리별 뉴스레터 미리보기 실패: category={}", category, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("미리보기 생성 실패"));
        }
    }
    
    /**
     * 피드 B형 뉴스레터 미리보기 (쿼리 파라미터 방식)
     */
    @Operation(
        summary = "피드 B형 뉴스레터 미리보기",
        description = "type과 param 쿼리 파라미터를 사용하여 피드 B형 뉴스레터 미리보기를 제공합니다."
    )
    @GetMapping("/feed-b")
    public ResponseEntity<Map<String, Object>> getFeedBPreview(
            @Parameter(description = "미리보기 타입 (trending, personalized, category)")
            @RequestParam(required = false, defaultValue = "trending") String type,
            @Parameter(description = "타입별 파라미터 (personalized: userId, category: 카테고리명)")
            @RequestParam(required = false) String param) {
        
        log.info("피드 B형 뉴스레터 미리보기 요청: type={}, param={}", type, param);
        
        try {
            FeedTemplate feedTemplate;
            
            switch (type.toLowerCase()) {
                case "personalized":
                    if (param == null || param.trim().isEmpty()) {
                        log.warn("개인화 미리보기 요청에서 userId 파라미터가 누락됨");
                        return ResponseEntity.badRequest().body(createErrorResponse("userId 파라미터가 필요합니다"));
                    }
                    Long userId = Long.valueOf(param);
                    feedTemplate = feedTemplateService.createPersonalizedFeedTemplate(
                            userId, FeedTemplate.FeedType.FEED_B);
                    break;
                case "category":
                    if (param == null || param.trim().isEmpty()) {
                        log.warn("카테고리별 미리보기 요청에서 category 파라미터가 누락됨");
                        return ResponseEntity.badRequest().body(createErrorResponse("category 파라미터가 필요합니다"));
                    }
                    feedTemplate = feedTemplateService.createCategoryFeedTemplate(
                            param, FeedTemplate.FeedType.FEED_B);
                    break;
                case "trending":
                default:
                    feedTemplate = feedTemplateService.createTrendingFeedTemplate(
                            FeedTemplate.FeedType.FEED_B);
                    break;
            }
            
            // 미리보기용 응답 데이터 구성
            Map<String, Object> previewData = createPreviewResponse(feedTemplate, type, param);
            
            log.info("피드 B형 뉴스레터 미리보기 완료: type={}, param={}", type, param);
            return ResponseEntity.ok(previewData);
            
        } catch (NumberFormatException e) {
            log.error("잘못된 userId 형식: param={}", param, e);
            return ResponseEntity.badRequest().body(createErrorResponse("잘못된 userId 형식입니다"));
        } catch (Exception e) {
            log.error("피드 B형 뉴스레터 미리보기 실패: type={}, param={}", type, param, e);
            return ResponseEntity.internalServerError().body(createErrorResponse("미리보기 생성 실패"));
        }
    }
    
    /**
     * 미리보기용 응답 데이터 생성
     */
    private Map<String, Object> createPreviewResponse(FeedTemplate feedTemplate, String type, String param) {
        Map<String, Object> response = new HashMap<>();
        
        // 기본 정보
        response.put("success", true);
        response.put("type", type);
        response.put("param", param);
        response.put("timestamp", System.currentTimeMillis());
        
        // 피드 템플릿 정보
        response.put("feedType", feedTemplate.getFeedType().name());
        response.put("template", feedTemplate);
        
        // 카카오톡 API용 변수
        response.put("kakaoArgs", feedTemplate.toKakaoTemplateArgs());
        
        // 미리보기용 추가 정보
        Map<String, Object> previewInfo = new HashMap<>();
        previewInfo.put("title", feedTemplate.getContent() != null ? feedTemplate.getContent().getTitle() : "뉴스레터");
        previewInfo.put("description", feedTemplate.getContent() != null ? feedTemplate.getContent().getDescription() : "뉴스 설명");
        previewInfo.put("imageUrl", feedTemplate.getContent() != null ? feedTemplate.getContent().getImageUrl() : null);
        previewInfo.put("buttonCount", feedTemplate.getButtons() != null ? feedTemplate.getButtons().size() : 0);
        previewInfo.put("itemCount", feedTemplate.getItemContents() != null ? feedTemplate.getItemContents().size() : 0);
        
        response.put("previewInfo", previewInfo);
        
        return response;
    }
    
    /**
     * 에러 응답 생성
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        return errorResponse;
    }
}
