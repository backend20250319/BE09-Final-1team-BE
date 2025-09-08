package com.newsletterservice.controller;

import com.newsletterservice.dto.FeedTemplate;
import com.newsletterservice.service.FeedTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 피드 템플릿 컨트롤러
 * 카카오톡 피드 템플릿 관련 API를 제공합니다.
 */
@Tag(name = "Feed Template", description = "피드 템플릿 API")
@RestController
@RequestMapping("/api/feed-template")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class FeedTemplateController {
    
    private final FeedTemplateService feedTemplateService;
    
    /**
     * 개인화된 피드 템플릿 생성 (피드 A형)
     */
    @Operation(
        summary = "개인화된 피드 A형 템플릿 생성",
        description = "사용자의 관심사를 반영한 개인화된 피드 A형 템플릿을 생성합니다."
    )
    @GetMapping("/personalized/feed-a/{userId}")
    public ResponseEntity<FeedTemplate> createPersonalizedFeedA(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId) {
        
        log.info("개인화된 피드 A형 템플릿 생성 요청: userId={}", userId);
        
        try {
            FeedTemplate feedTemplate = feedTemplateService.createPersonalizedFeedTemplate(
                    userId, FeedTemplate.FeedType.FEED_A);
            
            log.info("개인화된 피드 A형 템플릿 생성 완료: userId={}", userId);
            return ResponseEntity.ok(feedTemplate);
            
        } catch (Exception e) {
            log.error("개인화된 피드 A형 템플릿 생성 실패: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 개인화된 피드 템플릿 생성 (피드 B형)
     */
    @Operation(
        summary = "개인화된 피드 B형 템플릿 생성",
        description = "사용자의 관심사를 반영한 개인화된 피드 B형 템플릿을 생성합니다."
    )
    @GetMapping("/personalized/feed-b/{userId}")
    public ResponseEntity<FeedTemplate> createPersonalizedFeedB(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId) {
        
        log.info("개인화된 피드 B형 템플릿 생성 요청: userId={}", userId);
        
        try {
            FeedTemplate feedTemplate = feedTemplateService.createPersonalizedFeedTemplate(
                    userId, FeedTemplate.FeedType.FEED_B);
            
            log.info("개인화된 피드 B형 템플릿 생성 완료: userId={}", userId);
            return ResponseEntity.ok(feedTemplate);
            
        } catch (Exception e) {
            log.error("개인화된 피드 B형 템플릿 생성 실패: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 카테고리별 피드 템플릿 생성 (피드 A형)
     */
    @Operation(
        summary = "카테고리별 피드 A형 템플릿 생성",
        description = "특정 카테고리의 뉴스로 구성된 피드 A형 템플릿을 생성합니다."
    )
    @GetMapping("/category/feed-a/{category}")
    public ResponseEntity<FeedTemplate> createCategoryFeedA(
            @Parameter(description = "카테고리명", required = true)
            @PathVariable String category) {
        
        log.info("카테고리별 피드 A형 템플릿 생성 요청: category={}", category);
        
        try {
            FeedTemplate feedTemplate = feedTemplateService.createCategoryFeedTemplate(
                    category, FeedTemplate.FeedType.FEED_A);
            
            log.info("카테고리별 피드 A형 템플릿 생성 완료: category={}", category);
            return ResponseEntity.ok(feedTemplate);
            
        } catch (Exception e) {
            log.error("카테고리별 피드 A형 템플릿 생성 실패: category={}", category, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 카테고리별 피드 템플릿 생성 (피드 B형)
     */
    @Operation(
        summary = "카테고리별 피드 B형 템플릿 생성",
        description = "특정 카테고리의 뉴스로 구성된 피드 B형 템플릿을 생성합니다."
    )
    @GetMapping("/category/feed-b/{category}")
    public ResponseEntity<FeedTemplate> createCategoryFeedB(
            @Parameter(description = "카테고리명", required = true)
            @PathVariable String category) {
        
        log.info("카테고리별 피드 B형 템플릿 생성 요청: category={}", category);
        
        try {
            FeedTemplate feedTemplate = feedTemplateService.createCategoryFeedTemplate(
                    category, FeedTemplate.FeedType.FEED_B);
            
            log.info("카테고리별 피드 B형 템플릿 생성 완료: category={}", category);
            return ResponseEntity.ok(feedTemplate);
            
        } catch (Exception e) {
            log.error("카테고리별 피드 B형 템플릿 생성 실패: category={}", category, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 트렌딩 뉴스 피드 템플릿 생성 (피드 A형)
     */
    @Operation(
        summary = "트렌딩 뉴스 피드 A형 템플릿 생성",
        description = "트렌딩 뉴스로 구성된 피드 A형 템플릿을 생성합니다."
    )
    @GetMapping("/trending/feed-a")
    public ResponseEntity<FeedTemplate> createTrendingFeedA() {
        
        log.info("트렌딩 뉴스 피드 A형 템플릿 생성 요청");
        
        try {
            FeedTemplate feedTemplate = feedTemplateService.createTrendingFeedTemplate(
                    FeedTemplate.FeedType.FEED_A);
            
            log.info("트렌딩 뉴스 피드 A형 템플릿 생성 완료");
            return ResponseEntity.ok(feedTemplate);
            
        } catch (Exception e) {
            log.error("트렌딩 뉴스 피드 A형 템플릿 생성 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 트렌딩 뉴스 피드 템플릿 생성 (피드 B형)
     */
    @Operation(
        summary = "트렌딩 뉴스 피드 B형 템플릿 생성",
        description = "트렌딩 뉴스로 구성된 피드 B형 템플릿을 생성합니다."
    )
    @GetMapping("/trending/feed-b")
    public ResponseEntity<FeedTemplate> createTrendingFeedB() {
        
        log.info("트렌딩 뉴스 피드 B형 템플릿 생성 요청");
        
        try {
            FeedTemplate feedTemplate = feedTemplateService.createTrendingFeedTemplate(
                    FeedTemplate.FeedType.FEED_B);
            
            log.info("트렌딩 뉴스 피드 B형 템플릿 생성 완료");
            return ResponseEntity.ok(feedTemplate);
            
        } catch (Exception e) {
            log.error("트렌딩 뉴스 피드 B형 템플릿 생성 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 최신 뉴스 피드 템플릿 생성 (피드 A형)
     */
    @Operation(
        summary = "최신 뉴스 피드 A형 템플릿 생성",
        description = "최신 뉴스로 구성된 피드 A형 템플릿을 생성합니다."
    )
    @GetMapping("/latest/feed-a")
    public ResponseEntity<FeedTemplate> createLatestFeedA() {
        
        log.info("최신 뉴스 피드 A형 템플릿 생성 요청");
        
        try {
            FeedTemplate feedTemplate = feedTemplateService.createLatestFeedTemplate(
                    FeedTemplate.FeedType.FEED_A);
            
            log.info("최신 뉴스 피드 A형 템플릿 생성 완료");
            return ResponseEntity.ok(feedTemplate);
            
        } catch (Exception e) {
            log.error("최신 뉴스 피드 A형 템플릿 생성 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 최신 뉴스 피드 템플릿 생성 (피드 B형)
     */
    @Operation(
        summary = "최신 뉴스 피드 B형 템플릿 생성",
        description = "최신 뉴스로 구성된 피드 B형 템플릿을 생성합니다."
    )
    @GetMapping("/latest/feed-b")
    public ResponseEntity<FeedTemplate> createLatestFeedB() {
        
        log.info("최신 뉴스 피드 B형 템플릿 생성 요청");
        
        try {
            FeedTemplate feedTemplate = feedTemplateService.createLatestFeedTemplate(
                    FeedTemplate.FeedType.FEED_B);
            
            log.info("최신 뉴스 피드 B형 템플릿 생성 완료");
            return ResponseEntity.ok(feedTemplate);
            
        } catch (Exception e) {
            log.error("최신 뉴스 피드 B형 템플릿 생성 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 피드 템플릿을 카카오톡 API용 템플릿 변수로 변환
     */
    @Operation(
        summary = "피드 템플릿을 카카오톡 API용 변수로 변환",
        description = "피드 템플릿을 카카오톡 API에서 사용할 수 있는 템플릿 변수 형태로 변환합니다."
    )
    @PostMapping("/convert-to-kakao-args")
    public ResponseEntity<Map<String, Object>> convertToKakaoArgs(
            @Parameter(description = "피드 템플릿", required = true)
            @RequestBody FeedTemplate feedTemplate) {
        
        log.info("피드 템플릿을 카카오톡 API용 변수로 변환 요청: feedType={}", feedTemplate.getFeedType());
        
        try {
            Map<String, Object> kakaoArgs = feedTemplate.toKakaoTemplateArgs();
            
            log.info("피드 템플릿을 카카오톡 API용 변수로 변환 완료");
            return ResponseEntity.ok(kakaoArgs);
            
        } catch (Exception e) {
            log.error("피드 템플릿을 카카오톡 API용 변수로 변환 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 피드 템플릿 미리보기 (개발/테스트용)
     */
    @Operation(
        summary = "피드 템플릿 미리보기",
        description = "피드 템플릿의 구조와 내용을 확인할 수 있는 미리보기를 제공합니다."
    )
    @GetMapping("/preview/{feedType}")
    public ResponseEntity<Map<String, Object>> previewFeedTemplate(
            @Parameter(description = "피드 타입 (FEED_A 또는 FEED_B)", required = true)
            @PathVariable String feedType) {
        
        log.info("피드 템플릿 미리보기 요청: feedType={}", feedType);
        
        try {
            FeedTemplate.FeedType type = FeedTemplate.FeedType.valueOf(feedType.toUpperCase());
            FeedTemplate feedTemplate = feedTemplateService.createTrendingFeedTemplate(type);
            
            Map<String, Object> preview = Map.of(
                    "feedType", feedTemplate.getFeedType(),
                    "content", feedTemplate.getContent(),
                    "buttons", feedTemplate.getButtons(),
                    "itemContents", feedTemplate.getItemContents(),
                    "kakaoArgs", feedTemplate.toKakaoTemplateArgs()
            );
            
            log.info("피드 템플릿 미리보기 완료: feedType={}", feedType);
            return ResponseEntity.ok(preview);
            
        } catch (Exception e) {
            log.error("피드 템플릿 미리보기 실패: feedType={}", feedType, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
