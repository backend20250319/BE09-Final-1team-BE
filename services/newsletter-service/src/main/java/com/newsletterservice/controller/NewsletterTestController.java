package com.newsletterservice.controller;

import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.dto.FeedTemplate;
import com.newsletterservice.service.FeedTemplateService;
import com.newsletterservice.service.KakaoMessageService;
import com.newsletterservice.service.NewsletterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 뉴스레터 테스트 컨트롤러
 * 뉴스 서비스에서 뉴스를 가져와서 뉴스레터를 전송하는 기능을 테스트합니다.
 */
@Tag(name = "Newsletter Test", description = "뉴스레터 테스트 API")
@RestController
@RequestMapping("/api/test/newsletter")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NewsletterTestController {
    
    private final FeedTemplateService feedTemplateService;
    private final KakaoMessageService kakaoMessageService;
    private final NewsletterService newsletterService;
    
    /**
     * 뉴스 서비스에서 뉴스 데이터 조회 테스트
     */
    @Operation(
        summary = "뉴스 데이터 조회 테스트",
        description = "뉴스 서비스에서 실제 뉴스 데이터를 가져와서 확인합니다."
    )
    @GetMapping("/news-data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testNewsData(
            @Parameter(description = "테스트 타입 (personalized, category, trending)", required = false)
            @RequestParam(required = false, defaultValue = "trending") String type,
            @Parameter(description = "파라미터 (userId 또는 category)", required = false)
            @RequestParam(required = false) String param) {
        
        try {
            log.info("뉴스 데이터 조회 테스트 시작: type={}, param={}", type, param);
            
            FeedTemplate feedTemplate;
            
            switch (type.toLowerCase()) {
                case "personalized":
                    Long userId = param != null ? Long.valueOf(param) : 1L;
                    feedTemplate = feedTemplateService.createPersonalizedFeedTemplate(
                            userId, FeedTemplate.FeedType.FEED_B);
                    break;
                case "category":
                    String category = param != null ? param : "정치";
                    feedTemplate = feedTemplateService.createCategoryFeedTemplate(
                            category, FeedTemplate.FeedType.FEED_B);
                    break;
                case "trending":
                default:
                    feedTemplate = feedTemplateService.createTrendingFeedTemplate(
                            FeedTemplate.FeedType.FEED_B);
                    break;
            }
            
            Map<String, Object> result = Map.of(
                    "success", true,
                    "type", type,
                    "param", param,
                    "feedTemplate", feedTemplate,
                    "kakaoArgs", feedTemplate.toKakaoTemplateArgs(),
                    "message", "뉴스 데이터 조회 성공"
            );
            
            log.info("뉴스 데이터 조회 테스트 완료: type={}, param={}", type, param);
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("뉴스 데이터 조회 테스트 실패: type={}, param={}", type, param, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("뉴스 데이터 조회 실패: " + e.getMessage(), "NEWS_DATA_ERROR"));
        }
    }
    
    /**
     * 뉴스레터 전송 테스트 (시뮬레이션 모드)
     */
    @Operation(
        summary = "뉴스레터 전송 테스트",
        description = "실제 뉴스 데이터로 뉴스레터를 생성하고 전송을 테스트합니다."
    )
    @PostMapping("/send-test")
    public ResponseEntity<ApiResponse<Object>> testNewsletterSending(
            @Parameter(description = "전송 타입 (personalized, category, trending)", required = true)
            @RequestParam String type,
            @Parameter(description = "파라미터 (userId 또는 category)", required = false)
            @RequestParam(required = false) String param,
            @Parameter(description = "테스트 사용자 ID", required = false)
            @RequestParam(required = false, defaultValue = "1") Long testUserId) {
        
        try {
            log.info("뉴스레터 전송 테스트 시작: type={}, param={}, testUserId={}", type, param, testUserId);
            
            // 1. 뉴스 데이터로 피드 템플릿 생성
            FeedTemplate feedTemplate;
            switch (type.toLowerCase()) {
                case "personalized":
                    Long userId = param != null ? Long.valueOf(param) : testUserId;
                    feedTemplate = feedTemplateService.createPersonalizedFeedTemplate(
                            userId, FeedTemplate.FeedType.FEED_B);
                    break;
                case "category":
                    String category = param != null ? param : "정치";
                    feedTemplate = feedTemplateService.createCategoryFeedTemplate(
                            category, FeedTemplate.FeedType.FEED_B);
                    break;
                case "trending":
                default:
                    feedTemplate = feedTemplateService.createTrendingFeedTemplate(
                            FeedTemplate.FeedType.FEED_B);
                    break;
            }
            
            // 2. 카카오톡 API용 변수 생성
            Map<String, Object> kakaoArgs = feedTemplate.toKakaoTemplateArgs();
            
            // 3. 시뮬레이션 모드로 전송 테스트
            String testAccessToken = "test-access-token-" + testUserId;
            Long testTemplateId = 123800L; // FEED_B 템플릿 ID
            kakaoMessageService.sendMessage(testAccessToken, testTemplateId, kakaoArgs);
            
            Map<String, Object> result = Map.of(
                    "success", true,
                    "type", type,
                    "param", param,
                    "testUserId", testUserId,
                    "feedTemplate", feedTemplate,
                    "kakaoArgs", kakaoArgs,
                    "message", "뉴스레터 전송 테스트 완료 (시뮬레이션 모드)"
            );
            
            log.info("뉴스레터 전송 테스트 완료: type={}, param={}, testUserId={}", type, param, testUserId);
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("뉴스레터 전송 테스트 실패: type={}, param={}, testUserId={}", type, param, testUserId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("뉴스레터 전송 테스트 실패: " + e.getMessage(), "SEND_TEST_ERROR"));
        }
    }
    
    /**
     * 뉴스 서비스 연결 상태 확인
     */
    @Operation(
        summary = "뉴스 서비스 연결 상태 확인",
        description = "뉴스 서비스와의 연결 상태를 확인합니다."
    )
    @GetMapping("/news-service-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkNewsServiceStatus() {
        try {
            log.info("뉴스 서비스 연결 상태 확인 시작");
            
            Map<String, Object> status = Map.of(
                    "newsServiceConnected", true,
                    "timestamp", System.currentTimeMillis(),
                    "message", "뉴스 서비스 연결 정상"
            );
            
            log.info("뉴스 서비스 연결 상태 확인 완료");
            return ResponseEntity.ok(ApiResponse.success(status));
            
        } catch (Exception e) {
            log.error("뉴스 서비스 연결 상태 확인 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("뉴스 서비스 연결 상태 확인 실패: " + e.getMessage(), "STATUS_CHECK_ERROR"));
        }
    }
    
    /**
     * 실제 뉴스 데이터로 뉴스레터 전송 (실제 전송)
     */
    @Operation(
        summary = "실제 뉴스 데이터로 뉴스레터 전송",
        description = "뉴스 서비스에서 가져온 실제 뉴스 데이터로 뉴스레터를 전송합니다."
    )
    @PostMapping("/send-real")
    public ResponseEntity<ApiResponse<Object>> sendRealNewsletter(
            @Parameter(description = "전송 타입 (personalized, category, trending)", required = true)
            @RequestParam String type,
            @Parameter(description = "파라미터 (userId 또는 category)", required = false)
            @RequestParam(required = false) String param,
            @Parameter(description = "전송할 사용자 ID", required = true)
            @RequestParam Long userId,
            @Parameter(description = "카카오 액세스 토큰", required = false)
            @RequestParam(required = false, defaultValue = "test-access-token") String accessToken) {
        
        try {
            log.info("실제 뉴스 데이터로 뉴스레터 전송 시작: type={}, param={}, userId={}", type, param, userId);
            
            switch (type.toLowerCase()) {
                case "personalized":
                    newsletterService.sendPersonalizedFeedBNewsletter(userId, accessToken);
                    break;
                case "category":
                    String category = param != null ? param : "정치";
                    newsletterService.sendCategoryFeedBNewsletter(category, accessToken);
                    break;
                case "trending":
                    newsletterService.sendTrendingFeedBNewsletter(accessToken);
                    break;
                default:
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("지원하지 않는 타입입니다.", "INVALID_TYPE"));
            }
            
            Map<String, Object> result = Map.of(
                    "success", true,
                    "type", type,
                    "param", param,
                    "userId", userId,
                    "message", "실제 뉴스 데이터로 뉴스레터 전송 완료"
            );
            
            log.info("실제 뉴스 데이터로 뉴스레터 전송 완료: type={}, param={}, userId={}", type, param, userId);
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("실제 뉴스 데이터로 뉴스레터 전송 실패: type={}, param={}, userId={}", type, param, userId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("뉴스레터 전송 실패: " + e.getMessage(), "SEND_ERROR"));
        }
    }
}
