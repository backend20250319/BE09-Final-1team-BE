package com.newsletterservice.controller;

import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.dto.FeedTemplate;
import com.newsletterservice.service.FeedBNewsletterScheduler;
import com.newsletterservice.service.FeedTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관리자용 뉴스레터 컨트롤러
 * 피드 B형 뉴스레터의 관리 및 수동 전송 기능을 제공합니다.
 */
@Tag(name = "Admin Newsletter", description = "관리자용 뉴스레터 API")
@RestController
@RequestMapping("/api/admin/newsletter")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminNewsletterController {
    
    private final FeedTemplateService feedTemplateService;
    private final FeedBNewsletterScheduler feedBNewsletterScheduler;
    
    /**
     * 피드 B형 뉴스레터 수동 전송
     */
    @Operation(
        summary = "피드 B형 뉴스레터 수동 전송",
        description = "관리자가 특정 사용자들에게 피드 B형 뉴스레터를 수동으로 전송합니다."
    )
    @PostMapping("/send/feed-b")
    public ResponseEntity<ApiResponse<Object>> sendManualFeedBNewsletter(
            @Parameter(description = "전송 타입 (personalized, category, trending)", required = true)
            @RequestParam String type,
            @Parameter(description = "파라미터 (userId 또는 category)", required = false)
            @RequestParam(required = false) String param,
            @Parameter(description = "전송할 사용자 ID 목록", required = true)
            @RequestBody List<Long> userIds) {
        
        try {
            log.info("관리자 피드 B형 뉴스레터 수동 전송 요청: type={}, param={}, userIds={}", type, param, userIds);
            
            // 입력 검증
            if (userIds == null || userIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("전송할 사용자 ID 목록이 필요합니다.", "MISSING_USER_IDS"));
            }
            
            if (type == null || type.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("전송 타입이 필요합니다.", "MISSING_TYPE"));
            }
            
            // 수동 전송 실행
            feedBNewsletterScheduler.sendManualFeedBNewsletter(type, param, userIds);
            
            return ResponseEntity.ok(ApiResponse.success(
                    String.format("피드 B형 뉴스레터 전송 요청 완료: %d명에게 %s 타입으로 전송", userIds.size(), type)
            ));
            
        } catch (Exception e) {
            log.error("관리자 피드 B형 뉴스레터 수동 전송 실패: type={}, param={}, userIds={}", type, param, userIds, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("뉴스레터 전송 실패: " + e.getMessage(), "SEND_ERROR"));
        }
    }
    
    /**
     * 피드 B형 뉴스레터 템플릿 미리보기 (관리자용)
     */
    @Operation(
        summary = "피드 B형 뉴스레터 템플릿 미리보기",
        description = "관리자가 피드 B형 뉴스레터 템플릿을 미리 확인할 수 있습니다."
    )
    @GetMapping("/preview/feed-b")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminFeedBPreview(
            @Parameter(description = "미리보기 타입 (personalized, category, trending)", required = false)
            @RequestParam(required = false) String type,
            @Parameter(description = "파라미터 (userId 또는 category)", required = false)
            @RequestParam(required = false) String param) {
        
        try {
            log.info("관리자 피드 B형 뉴스레터 미리보기 요청: type={}, param={}", type, param);
            
            // 기본값 설정
            if (type == null) type = "trending";
            if (param == null) param = "";
            
            FeedTemplate feedTemplate;
            
            // 타입에 따른 템플릿 생성
            switch (type.toLowerCase()) {
                case "personalized":
                    if (param.isEmpty()) {
                        return ResponseEntity.badRequest()
                                .body(ApiResponse.error("개인화 타입의 경우 userId가 필요합니다.", "MISSING_USER_ID"));
                    }
                    Long userId = Long.valueOf(param);
                    feedTemplate = feedTemplateService.createPersonalizedFeedTemplate(
                            userId, FeedTemplate.FeedType.FEED_B);
                    break;
                case "category":
                    if (param.isEmpty()) {
                        return ResponseEntity.badRequest()
                                .body(ApiResponse.error("카테고리 타입의 경우 category가 필요합니다.", "MISSING_CATEGORY"));
                    }
                    feedTemplate = feedTemplateService.createCategoryFeedTemplate(
                            param, FeedTemplate.FeedType.FEED_B);
                    break;
                case "trending":
                    feedTemplate = feedTemplateService.createTrendingFeedTemplate(
                            FeedTemplate.FeedType.FEED_B);
                    break;
                default:
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("지원하지 않는 타입입니다.", "INVALID_TYPE"));
            }
            
            // 미리보기 데이터 구성
            Map<String, Object> previewData = Map.of(
                    "success", true,
                    "type", type,
                    "param", param,
                    "feedType", feedTemplate.getFeedType().name(),
                    "template", feedTemplate,
                    "kakaoArgs", feedTemplate.toKakaoTemplateArgs(),
                    "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(ApiResponse.success(previewData));
            
        } catch (NumberFormatException e) {
            log.error("잘못된 userId 형식: {}", param, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("잘못된 userId 형식입니다.", "INVALID_USER_ID"));
        } catch (Exception e) {
            log.error("관리자 피드 B형 뉴스레터 미리보기 실패: type={}, param={}", type, param, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("미리보기 생성 실패: " + e.getMessage(), "PREVIEW_ERROR"));
        }
    }
    
    /**
     * 피드 B형 뉴스레터 전송 통계 조회
     */
    @Operation(
        summary = "피드 B형 뉴스레터 전송 통계 조회",
        description = "피드 B형 뉴스레터의 전송 통계를 조회합니다."
    )
    @GetMapping("/stats/feed-b")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFeedBStats() {
        try {
            log.info("피드 B형 뉴스레터 전송 통계 조회 요청");
            
            // 임시 통계 데이터 (실제 구현에서는 데이터베이스에서 조회)
            Map<String, Object> stats = Map.of(
                    "totalSent", 1250,
                    "successfulSent", 1180,
                    "failedSent", 70,
                    "successRate", 94.4,
                    "todaySent", 45,
                    "weeklySent", 315,
                    "monthlySent", 1250,
                    "lastUpdated", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(ApiResponse.success(stats));
            
        } catch (Exception e) {
            log.error("피드 B형 뉴스레터 전송 통계 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("통계 조회 실패: " + e.getMessage(), "STATS_ERROR"));
        }
    }
    
    /**
     * 피드 B형 뉴스레터 전송 테스트
     */
    @Operation(
        summary = "피드 B형 뉴스레터 전송 테스트",
        description = "관리자가 피드 B형 뉴스레터 전송을 테스트할 수 있습니다."
    )
    @PostMapping("/test/feed-b")
    public ResponseEntity<ApiResponse<Object>> testFeedBNewsletter(
            @Parameter(description = "테스트 타입 (personalized, category, trending)", required = true)
            @RequestParam String type,
            @Parameter(description = "테스트 파라미터", required = false)
            @RequestParam(required = false) String param,
            @Parameter(description = "테스트할 사용자 ID", required = true)
            @RequestParam Long testUserId) {
        
        try {
            log.info("피드 B형 뉴스레터 전송 테스트 요청: type={}, param={}, testUserId={}", type, param, testUserId);
            
            // 테스트용 사용자 ID 목록
            List<Long> testUserIds = List.of(testUserId);
            
            // 테스트 전송 실행
            feedBNewsletterScheduler.sendManualFeedBNewsletter(type, param, testUserIds);
            
            return ResponseEntity.ok(ApiResponse.success(
                    String.format("피드 B형 뉴스레터 테스트 전송 완료: 사용자 %d에게 %s 타입으로 전송", testUserId, type)
            ));
            
        } catch (Exception e) {
            log.error("피드 B형 뉴스레터 전송 테스트 실패: type={}, param={}, testUserId={}", type, param, testUserId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("테스트 전송 실패: " + e.getMessage(), "TEST_ERROR"));
        }
    }
}
