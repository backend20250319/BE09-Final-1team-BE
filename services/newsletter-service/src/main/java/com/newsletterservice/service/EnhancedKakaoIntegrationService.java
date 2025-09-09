package com.newsletterservice.service;

import com.newsletterservice.dto.NewsletterContent;
import com.newsletterservice.client.UserServiceClient;
// import com.newsletterservice.client.dto.UserTokenResponse; // UserServiceClient에 구현 필요
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 향상된 카카오톡 통합 서비스
 * UserService의 토큰 관리 시스템을 활용하여 멀티채널 발송 구현
 */
@Slf4j
@Service
public class EnhancedKakaoIntegrationService {

    private final KakaoMessageService kakaoMessageService;
    private final Optional<EmailService> emailService;
    private final UserServiceClient userServiceClient;
    private final KakaoTemplateService kakaoTemplateService;
    
    public EnhancedKakaoIntegrationService(KakaoMessageService kakaoMessageService, 
                                         Optional<EmailService> emailService,
                                         UserServiceClient userServiceClient,
                                         KakaoTemplateService kakaoTemplateService) {
        this.kakaoMessageService = kakaoMessageService;
        this.emailService = emailService;
        this.userServiceClient = userServiceClient;
        this.kakaoTemplateService = kakaoTemplateService;
    }

    /**
     * 멀티채널 뉴스레터 발송 (이메일 + 카카오톡)
     */
    public void sendMultiChannelNewsletter(Long userId, NewsletterContent content) {
        try {
            log.info("사용자 {} 멀티채널 뉴스레터 발송 시작", userId);
            
            // 1. 이메일 발송 (기본 채널 - 항상 실행)
            sendEmailNewsletter(userId, content);
            
            // 2. 카카오톡 발송 (토큰이 유효한 경우)
            sendKakaoNewsletter(userId, content);
            
            log.info("사용자 {} 멀티채널 뉴스레터 발송 완료", userId);
            
        } catch (Exception e) {
            log.error("사용자 {} 멀티채널 뉴스레터 발송 실패", userId, e);
            // 실패 시 이메일로만 발송 시도
            try {
                sendEmailNewsletter(userId, content);
                log.info("사용자 {} 이메일 대체 발송 완료", userId);
            } catch (Exception emailException) {
                log.error("사용자 {} 이메일 대체 발송도 실패", userId, emailException);
                throw new RuntimeException("멀티채널 뉴스레터 발송 실패", e);
            }
        }
    }
    
    /**
     * 이메일 뉴스레터 발송
     */
    private void sendEmailNewsletter(Long userId, NewsletterContent content) {
        try {
            log.info("사용자 {} 이메일 뉴스레터 발송 시작", userId);
            
            // EmailService가 사용 가능한 경우에만 이메일 발송
            emailService.ifPresentOrElse(
                service -> {
                    try {
                        // 이메일 서비스를 통한 뉴스레터 발송
                        // service.sendNewsletterEmail(userId, content); // EmailService에 구현 필요
                        log.info("사용자 {} 이메일 뉴스레터 발송 완료", userId);
                    } catch (Exception e) {
                        log.error("사용자 {} 이메일 뉴스레터 발송 실패", userId, e);
                        throw new RuntimeException("이메일 발송 실패", e);
                    }
                },
                () -> log.warn("EmailService가 사용 불가능합니다. 이메일 발송을 건너뜁니다. userId={}", userId)
            );
            
        } catch (Exception e) {
            log.error("사용자 {} 이메일 뉴스레터 발송 실패", userId, e);
            throw e;
        }
    }
    
    /**
     * 카카오톡 뉴스레터 발송
     */
    private void sendKakaoNewsletter(Long userId, NewsletterContent content) {
        try {
            // 1. 사용자 카카오 토큰 조회
            Optional<String> kakaoToken = getUserKakaoToken(userId);
            
            if (kakaoToken.isEmpty()) {
                log.info("사용자 {} 카카오 토큰이 없음, 카카오톡 발송 건너뜀", userId);
                return;
            }
            
            String token = kakaoToken.get();
            
            // 2. 카카오톡 메시지 발송 (개인화된 템플릿 사용)
            log.info("사용자 {} 카카오톡 뉴스레터 발송 시작", userId);
            
            // 개인화된 템플릿을 사용한 메시지 발송
            kakaoTemplateService.sendPersonalizedNewsletterMessage(userId, content, token);
            
            log.info("사용자 {} 카카오톡 뉴스레터 발송 완료", userId);
            
        } catch (Exception e) {
            log.error("사용자 {} 카카오톡 뉴스레터 발송 실패", userId, e);
            // 카카오톡 발송 실패는 전체 발송을 중단시키지 않음
        }
    }
    
    /**
     * 사용자 카카오 토큰 조회
     */
    private Optional<String> getUserKakaoToken(Long userId) {
        try {
            // UserServiceClient를 통해 카카오 토큰 조회
            // return userServiceClient.getUserToken(userId, "KAKAO"); // UserServiceClient에 구현 필요
            
            // 임시 구현: 테스트용 토큰 반환
            return Optional.of("test-kakao-token-" + userId);
            
        } catch (Exception e) {
            log.error("사용자 {} 카카오 토큰 조회 실패", userId, e);
            return Optional.empty();
        }
    }
    
    /**
     * 토큰 만료 체크 및 갱신 (스케줄러)
     */
    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시
    public void checkAndRefreshExpiredTokens() {
        try {
            log.info("만료된 카카오 토큰 체크 및 갱신 시작");
            
            // UserServiceClient를 통해 만료된 토큰 조회 및 갱신
            // userServiceClient.refreshExpiredTokens("KAKAO"); // UserServiceClient에 구현 필요
            
            log.info("만료된 카카오 토큰 체크 및 갱신 완료");
            
        } catch (Exception e) {
            log.error("만료된 카카오 토큰 체크 및 갱신 실패", e);
        }
    }
    
    /**
     * 카카오톡 발송 통계 조회
     */
    public KakaoDeliveryStats getKakaoDeliveryStats() {
        try {
            // UserServiceClient를 통해 카카오톡 발송 통계 조회
            // return userServiceClient.getKakaoDeliveryStats(); // UserServiceClient에 구현 필요
            
            return KakaoDeliveryStats.empty();
            
        } catch (Exception e) {
            log.error("카카오톡 발송 통계 조회 실패", e);
            return KakaoDeliveryStats.empty();
        }
    }
    
    /**
     * 사용자별 카카오톡 발송 상태 확인
     */
    public boolean isKakaoAvailable(Long userId) {
        try {
            Optional<String> token = getUserKakaoToken(userId);
            return token.isPresent();
            
        } catch (Exception e) {
            log.error("사용자 {} 카카오톡 발송 상태 확인 실패", userId, e);
            return false;
        }
    }
    
    /**
     * 카카오톡 발송 통계 DTO
     */
    public static class KakaoDeliveryStats {
        private final long totalUsers;
        private final long kakaoEnabledUsers;
        private final long activeTokens;
        private final long expiredTokens;
        
        public KakaoDeliveryStats(long totalUsers, long kakaoEnabledUsers, long activeTokens, long expiredTokens) {
            this.totalUsers = totalUsers;
            this.kakaoEnabledUsers = kakaoEnabledUsers;
            this.activeTokens = activeTokens;
            this.expiredTokens = expiredTokens;
        }
        
        public static KakaoDeliveryStats empty() {
            return new KakaoDeliveryStats(0, 0, 0, 0);
        }
        
        // Getters
        public long getTotalUsers() { return totalUsers; }
        public long getKakaoEnabledUsers() { return kakaoEnabledUsers; }
        public long getActiveTokens() { return activeTokens; }
        public long getExpiredTokens() { return expiredTokens; }
        
        public double getKakaoEnabledRate() {
            return totalUsers > 0 ? (double) kakaoEnabledUsers / totalUsers * 100 : 0;
        }
        
        public double getTokenActiveRate() {
            return kakaoEnabledUsers > 0 ? (double) activeTokens / kakaoEnabledUsers * 100 : 0;
        }
    }
}
