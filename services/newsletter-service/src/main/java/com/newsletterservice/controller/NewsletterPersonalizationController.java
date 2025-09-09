package com.newsletterservice.controller;

import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.service.PersonalizedRecommendationService;
import com.newsletterservice.service.PersonalizedRecommendationService.PersonalizedNewsletterContent;
import com.newsletterservice.client.dto.NewsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/v1/newsletter/personalization")
@RequiredArgsConstructor
@Tag(name = "Newsletter Personalization", description = "뉴스레터 개인화 API")
public class NewsletterPersonalizationController {

    private final PersonalizedRecommendationService personalizedRecommendationService;

    /**
     * 사용자별 개인화된 뉴스 추천
     */
    @GetMapping("/users/{userId}/recommended-news")
    @Operation(summary = "개인화된 뉴스 추천", description = "사용자의 관심사와 행동 패턴을 분석하여 맞춤 뉴스를 추천합니다.")
    public ResponseEntity<ApiResponse<List<NewsResponse>>> getPersonalizedNews(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            @Parameter(description = "추천 뉴스 개수") @RequestParam(defaultValue = "10") int limit) {
        
        log.info("개인화된 뉴스 추천 요청: userId={}, limit={}", userId, limit);
        
        try {
            List<NewsResponse> personalizedNews = 
                personalizedRecommendationService.getPersonalizedNews(userId, limit);
            
            return ResponseEntity.ok(
                ApiResponse.success(personalizedNews, "개인화된 뉴스 추천이 완료되었습니다."));
                
        } catch (Exception e) {
            log.error("개인화된 뉴스 추천 실패: userId={}", userId, e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("RECOMMENDATION_FAILED", "개인화된 뉴스 추천에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 사용자별 최적 뉴스레터 빈도 조회
     */
    @GetMapping("/users/{userId}/optimal-frequency")
    @Operation(summary = "최적 뉴스레터 빈도", description = "사용자의 참여도를 분석하여 최적의 뉴스레터 발송 빈도를 제안합니다.")
    public ResponseEntity<ApiResponse<String>> getOptimalFrequency(
            @Parameter(description = "사용자 ID") @PathVariable Long userId) {
        
        log.info("최적 뉴스레터 빈도 조회: userId={}", userId);
        
        try {
            String optimalFrequency = 
                personalizedRecommendationService.getOptimalNewsletterFrequency(userId);
            
            return ResponseEntity.ok(
                ApiResponse.success(optimalFrequency, "최적 뉴스레터 빈도 조회가 완료되었습니다."));
                
        } catch (Exception e) {
            log.error("최적 뉴스레터 빈도 조회 실패: userId={}", userId, e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("FREQUENCY_ANALYSIS_FAILED", "최적 빈도 분석에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 완전한 개인화 뉴스레터 콘텐츠 생성
     */
    @GetMapping("/users/{userId}/newsletter-content")
    @Operation(summary = "개인화 뉴스레터 콘텐츠", description = "사용자 맞춤 뉴스레터 전체 콘텐츠를 생성합니다.")
    public ResponseEntity<ApiResponse<PersonalizedNewsletterContent>> generatePersonalizedContent(
            @Parameter(description = "사용자 ID") @PathVariable Long userId) {
        
        log.info("개인화 뉴스레터 콘텐츠 생성 요청: userId={}", userId);
        
        try {
            PersonalizedNewsletterContent content = 
                personalizedRecommendationService.generatePersonalizedContent(userId);
            
            return ResponseEntity.ok(
                ApiResponse.success(content, "개인화 뉴스레터 콘텐츠 생성이 완료되었습니다."));
                
        } catch (Exception e) {
            log.error("개인화 뉴스레터 콘텐츠 생성 실패: userId={}", userId, e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("CONTENT_GENERATION_FAILED", "개인화 콘텐츠 생성에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 여러 사용자 일괄 개인화 뉴스 조회
     */
    @PostMapping("/users/batch/recommended-news")
    @Operation(summary = "일괄 개인화 뉴스 추천", description = "여러 사용자의 개인화된 뉴스를 일괄 조회합니다.")
    public ResponseEntity<ApiResponse<BatchPersonalizedNewsResponse>> getBatchPersonalizedNews(
            @RequestBody BatchPersonalizedNewsRequest request) {
        
        log.info("일괄 개인화 뉴스 추천 요청: 사용자 수={}", request.getUserIds().size());
        
        try {
            BatchPersonalizedNewsResponse response = new BatchPersonalizedNewsResponse();
            
            for (Long userId : request.getUserIds()) {
                try {
                    List<NewsResponse> userNews = 
                        personalizedRecommendationService.getPersonalizedNews(userId, request.getLimit());
                    response.addUserNews(userId, userNews);
                    
                } catch (Exception e) {
                    log.warn("사용자 {}의 개인화 뉴스 조회 실패", userId, e);
                    response.addFailedUser(userId, e.getMessage());
                }
            }
            
            return ResponseEntity.ok(
                ApiResponse.success(response, "일괄 개인화 뉴스 추천이 완료되었습니다."));
                
        } catch (Exception e) {
            log.error("일괄 개인화 뉴스 추천 실패", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("BATCH_RECOMMENDATION_FAILED", "일괄 추천에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 개인화 뉴스레터 미리보기
     */
    @GetMapping("/users/{userId}/preview")
    @Operation(summary = "개인화 뉴스레터 미리보기", description = "사용자별 개인화된 뉴스레터 HTML을 미리보기로 제공합니다.")
    public ResponseEntity<String> previewPersonalizedNewsletter(
            @Parameter(description = "사용자 ID") @PathVariable Long userId) {
        
        log.info("개인화 뉴스레터 미리보기 요청: userId={}", userId);
        
        try {
            PersonalizedNewsletterContent content = 
                personalizedRecommendationService.generatePersonalizedContent(userId);
            
            // HTML 미리보기 생성 (간단한 버전)
            String htmlPreview = generatePreviewHtml(content);
            
            return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(htmlPreview);
                
        } catch (Exception e) {
            log.error("개인화 뉴스레터 미리보기 실패: userId={}", userId, e);
            String errorHtml = "<html><body><h1>오류</h1><p>미리보기 생성에 실패했습니다.</p></body></html>";
            return ResponseEntity.status(500)
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(errorHtml);
        }
    }

    /**
     * 미리보기 HTML 생성
     */
    private String generatePreviewHtml(PersonalizedNewsletterContent content) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>")
            .append("<html lang='ko'>")
            .append("<head>")
            .append("<meta charset='UTF-8'>")
            .append("<title>개인화 뉴스레터 미리보기</title>")
            .append("<style>")
            .append("body { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; }")
            .append(".header { background: #2196f3; color: white; padding: 20px; text-align: center; }")
            .append(".content { background: white; padding: 20px; }")
            .append(".news-item { border: 1px solid #ddd; margin: 10px 0; padding: 15px; }")
            .append(".interests { background: #e3f2fd; padding: 10px; margin: 10px 0; }")
            .append("</style>")
            .append("</head>")
            .append("<body>");
            
        // 헤더
        html.append("<div class='header'>")
            .append("<h1>🎯 맞춤 뉴스레터</h1>")
            .append("<p>생성일: ").append(content.getGeneratedAt()).append("</p>")
            .append("</div>");
            
        // 관심사 표시
        if (!content.getUserInterests().isEmpty()) {
            html.append("<div class='interests'>")
                .append("<strong>관심사:</strong> ")
                .append(String.join(", ", content.getUserInterests()))
                .append("</div>");
        }
        
        // 뉴스 목록
        html.append("<div class='content'>");
        for (int i = 0; i < content.getPersonalizedNews().size(); i++) {
            NewsResponse news = content.getPersonalizedNews().get(i);
            html.append("<div class='news-item'>")
                .append("<h3>").append(i + 1).append(". ").append(news.getTitle()).append("</h3>")
                .append("<p><strong>카테고리:</strong> ").append(news.getCategoryName()).append("</p>");
                
            // NewsResponse에 summary 필드가 있는지 확인 후 사용
            try {
                if (news.getSummary() != null) {
                    html.append("<p>").append(news.getSummary()).append("</p>");
                }
            } catch (Exception e) {
                // summary 필드가 없는 경우 무시
                log.debug("NewsResponse에 summary 필드가 없습니다.");
            }
            
            html.append("</div>");
        }
        html.append("</div>");
        
        // 추천 빈도
        html.append("<div style='text-align: center; margin-top: 20px; color: #666;'>")
            .append("<p>추천 발송 빈도: ").append(content.getRecommendedFrequency()).append("</p>")
            .append("</div>");
            
        html.append("</body></html>");
        
        return html.toString();
    }

    // Request/Response DTO 클래스들
    public static class BatchPersonalizedNewsRequest {
        private List<Long> userIds;
        private int limit = 10;

        public List<Long> getUserIds() { return userIds; }
        public void setUserIds(List<Long> userIds) { this.userIds = userIds; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
    }

    public static class BatchPersonalizedNewsResponse {
        private Map<Long, List<NewsResponse>> userNewsMap = new HashMap<>();
        private Map<Long, String> failedUsers = new HashMap<>();

        public void addUserNews(Long userId, List<NewsResponse> news) {
            userNewsMap.put(userId, news);
        }

        public void addFailedUser(Long userId, String errorMessage) {
            failedUsers.put(userId, errorMessage);
        }

        public Map<Long, List<NewsResponse>> getUserNewsMap() { return userNewsMap; }
        public void setUserNewsMap(Map<Long, List<NewsResponse>> userNewsMap) { this.userNewsMap = userNewsMap; }
        public Map<Long, String> getFailedUsers() { return failedUsers; }
        public void setFailedUsers(Map<Long, String> failedUsers) { this.failedUsers = failedUsers; }
    }
}
