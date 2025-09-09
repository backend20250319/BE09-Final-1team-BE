package com.newsletterservice.service;

import com.newsletterservice.client.UserServiceClient;
import com.newsletterservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Optional;

/**
 * 다중 채널 알림 통합 서비스
 * 이메일, 카카오톡 등 여러 채널을 통한 알림 전송을 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final Optional<KakaoMessageService> kakaoMessageService;
    private final UserServiceClient userServiceClient;
    // private final EmailService emailService; // 이메일 서비스 (향후 구현)
    
    /**
     * 다중 채널 알림 전송
     * 
     * @param userId 사용자 ID
     * @param title 알림 제목
     * @param content 알림 내용
     * @param url 관련 URL
     */
    public void sendMultiChannelNotification(String userId, String title, String content, String url) {
        log.info("다중 채널 알림 전송 시작: userId={}, title={}", userId, title);
        
        // 1. 이메일 전송 (향후 구현)
        try {
            // emailService.sendNotification(userId, title, content, url);
            log.info("이메일 전송 기능은 향후 구현 예정입니다.");
        } catch (Exception e) {
            log.warn("이메일 전송 실패: userId={}", userId, e);
        }
        
        // 2. 카카오톡 전송 (선택적)
        if (kakaoMessageService.isPresent()) {
            try {
                String accessToken = getUserKakaoToken(userId);
                if (accessToken != null) {
                    kakaoMessageService.get().sendNewsletterMessage(accessToken, title, content, url);
                    log.info("카카오톡 알림 전송 성공: userId={}", userId);
                } else {
                    log.info("사용자의 카카오 토큰이 없습니다: userId={}", userId);
                }
            } catch (Exception e) {
                log.warn("카카오톡 전송 실패: userId={}", userId, e);
            }
        } else {
            log.info("KakaoMessageService가 사용할 수 없습니다. 카카오톡 전송을 건너뜁니다.");
        }
        
        log.info("다중 채널 알림 전송 완료: userId={}", userId);
    }
    
    /**
     * 뉴스레터 알림 전송
     * 
     * @param userId 사용자 ID
     * @param newsletterTitle 뉴스레터 제목
     * @param newsletterSummary 뉴스레터 요약
     * @param newsletterUrl 뉴스레터 URL
     */
    public void sendNewsletterNotification(String userId, String newsletterTitle, 
                                         String newsletterSummary, String newsletterUrl) {
        String title = "새로운 뉴스레터가 도착했습니다!";
        String content = String.format("제목: %s\n\n요약: %s", newsletterTitle, newsletterSummary);
        
        sendMultiChannelNotification(userId, title, content, newsletterUrl);
    }
    
    /**
     * 환영 메시지 전송
     * 
     * @param userId 사용자 ID
     * @param userName 사용자 이름
     */
    public void sendWelcomeNotification(String userId, String userName) {
        String title = "뉴스레터 서비스에 오신 것을 환영합니다!";
        String content = String.format("%s님, 뉴스레터 서비스에 가입해주셔서 감사합니다.", userName);
        String url = "http://be09-final-1team-fe-env.eba-92qhhhzz.ap-northeast-2.elasticbeanstalk.com";
        
        sendMultiChannelNotification(userId, title, content, url);
    }
    
    /**
     * 사용자의 카카오 토큰 조회
     * 
     * @param userId 사용자 ID
     * @return 카카오 액세스 토큰 (없으면 null)
     */
    private String getUserKakaoToken(String userId) {
        try {
            log.debug("사용자 카카오 토큰 조회: userId={}", userId);
            
            // String userId를 Long으로 변환
            Long userIdLong = Long.parseLong(userId);
            
            // user-service에서 카카오 토큰 조회
            ApiResponse<String> response = userServiceClient.getUserKakaoToken(userIdLong);
            
            if (response != null && response.getData() != null && !response.getData().trim().isEmpty()) {
                log.debug("카카오 토큰 조회 성공: userId={}", userId);
                return response.getData();
            }
            
            log.debug("사용자의 카카오 토큰이 없습니다: userId={}", userId);
            return null;
            
        } catch (NumberFormatException e) {
            log.error("잘못된 사용자 ID 형식: userId={}", userId, e);
            return null;
        } catch (Exception e) {
            log.error("카카오 토큰 조회 실패: userId={}", userId, e);
            return null;
        }
    }
}
