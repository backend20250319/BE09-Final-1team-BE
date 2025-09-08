package com.newsletterservice.controller;

import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.service.EnhancedPersonalizationService;
import com.newsletterservice.service.EnhancedPersonalizationService.NewsletterPersonalizationProfile;
import com.newsletterservice.service.EnhancedPersonalizationService.PersonalizationSettingsRequest;
// import com.newsletterservice.util.BaseController; // BaseController 클래스 구현 필요
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 개인화 설정 관리 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/newsletter/personalization")
@RequiredArgsConstructor
public class PersonalizationController {

    private final EnhancedPersonalizationService personalizationService;

    /**
     * 개인화 프로필 조회
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<NewsletterPersonalizationProfile>> getPersonalizationProfile(
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = 1L; // extractUserIdFromToken(httpRequest); // BaseController 메서드 구현 필요
            log.info("개인화 프로필 조회 요청: userId={}", userId);
            
            NewsletterPersonalizationProfile profile = personalizationService.createPersonalizationProfile(userId);
            
            return ResponseEntity.ok(ApiResponse.success(profile, "개인화 프로필을 조회했습니다."));
            
        } catch (Exception e) {
            log.error("개인화 프로필 조회 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("PERSONALIZATION_PROFILE_ERROR", "개인화 프로필 조회에 실패했습니다."));
        }
    }

    /**
     * 개인화 설정 업데이트
     */
    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<Object>> updatePersonalizationSettings(
            @RequestBody PersonalizationSettingsRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = 1L; // extractUserIdFromToken(httpRequest); // BaseController 메서드 구현 필요
            log.info("개인화 설정 업데이트 요청: userId={}, settings={}", userId, request.getCategorySettings().size());
            
            // 개인화 설정 업데이트
            personalizationService.updatePersonalizationSettings(userId, request);
            
            // 업데이트된 프로필 조회
            NewsletterPersonalizationProfile updatedProfile = personalizationService.createPersonalizationProfile(userId);
            
            return ResponseEntity.ok(ApiResponse.success(updatedProfile, "개인화 설정이 업데이트되었습니다."));
            
        } catch (Exception e) {
            log.error("개인화 설정 업데이트 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("PERSONALIZATION_UPDATE_ERROR", "개인화 설정 업데이트에 실패했습니다."));
        }
    }

    /**
     * 개인화 점수 조회
     */
    @GetMapping("/score")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getPersonalizationScore(
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = 1L; // extractUserIdFromToken(httpRequest); // BaseController 메서드 구현 필요
            log.info("개인화 점수 조회 요청: userId={}", userId);
            
            NewsletterPersonalizationProfile profile = personalizationService.createPersonalizationProfile(userId);
            
            java.util.Map<String, Object> scoreInfo = java.util.Map.of(
                "userId", userId,
                "personalizationScore", profile.getPersonalizationScore(),
                "optimalSendTime", profile.getOptimalSendTime(),
                "preferredContentTypes", profile.getPreferredContentTypes(),
                "categoryKeywords", profile.getCategoryKeywords(),
                "createdAt", profile.getCreatedAt()
            );
            
            return ResponseEntity.ok(ApiResponse.success(scoreInfo, "개인화 점수를 조회했습니다."));
            
        } catch (Exception e) {
            log.error("개인화 점수 조회 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("PERSONALIZATION_SCORE_ERROR", "개인화 점수 조회에 실패했습니다."));
        }
    }

    /**
     * 최적 발송 시간 조회
     */
    @GetMapping("/optimal-send-time")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getOptimalSendTime(
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = 1L; // extractUserIdFromToken(httpRequest); // BaseController 메서드 구현 필요
            log.info("최적 발송 시간 조회 요청: userId={}", userId);
            
            NewsletterPersonalizationProfile profile = personalizationService.createPersonalizationProfile(userId);
            
            java.util.Map<String, Object> sendTimeInfo = java.util.Map.of(
                "userId", userId,
                "optimalSendTime", profile.getOptimalSendTime(),
                "personalizationScore", profile.getPersonalizationScore(),
                "recommendedFrequencies", java.util.Map.of(
                    "DAILY", "08:00",
                    "WEEKLY", "09:00",
                    "MONTHLY", "10:00"
                )
            );
            
            return ResponseEntity.ok(ApiResponse.success(sendTimeInfo, "최적 발송 시간을 조회했습니다."));
            
        } catch (Exception e) {
            log.error("최적 발송 시간 조회 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("OPTIMAL_SEND_TIME_ERROR", "최적 발송 시간 조회에 실패했습니다."));
        }
    }

    /**
     * 선호 콘텐츠 유형 조회
     */
    @GetMapping("/preferred-content")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getPreferredContentTypes(
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = 1L; // extractUserIdFromToken(httpRequest); // BaseController 메서드 구현 필요
            log.info("선호 콘텐츠 유형 조회 요청: userId={}", userId);
            
            NewsletterPersonalizationProfile profile = personalizationService.createPersonalizationProfile(userId);
            
            java.util.Map<String, Object> contentInfo = java.util.Map.of(
                "userId", userId,
                "preferredContentTypes", profile.getPreferredContentTypes(),
                "categoryKeywords", profile.getCategoryKeywords(),
                "personalizationScore", profile.getPersonalizationScore(),
                "subscriptionCount", profile.getSubscriptions().size()
            );
            
            return ResponseEntity.ok(ApiResponse.success(contentInfo, "선호 콘텐츠 유형을 조회했습니다."));
            
        } catch (Exception e) {
            log.error("선호 콘텐츠 유형 조회 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("PREFERRED_CONTENT_ERROR", "선호 콘텐츠 유형 조회에 실패했습니다."));
        }
    }

    /**
     * 개인화 설정 초기화
     */
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Object>> resetPersonalizationSettings(
            HttpServletRequest httpRequest) {
        
        try {
            Long userId = 1L; // extractUserIdFromToken(httpRequest); // BaseController 메서드 구현 필요
            log.info("개인화 설정 초기화 요청: userId={}", userId);
            
            // 기본 설정으로 초기화
            PersonalizationSettingsRequest resetRequest = new PersonalizationSettingsRequest();
            // 기본 설정 로직 구현
            
            personalizationService.updatePersonalizationSettings(userId, resetRequest);
            
            NewsletterPersonalizationProfile resetProfile = personalizationService.createPersonalizationProfile(userId);
            
            return ResponseEntity.ok(ApiResponse.success(resetProfile, "개인화 설정이 초기화되었습니다."));
            
        } catch (Exception e) {
            log.error("개인화 설정 초기화 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("PERSONALIZATION_RESET_ERROR", "개인화 설정 초기화에 실패했습니다."));
        }
    }
}
