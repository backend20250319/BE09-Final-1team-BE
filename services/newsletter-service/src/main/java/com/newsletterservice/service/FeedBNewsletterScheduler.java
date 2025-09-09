package com.newsletterservice.service;

import com.newsletterservice.client.UserServiceClient;
import com.newsletterservice.client.dto.UserResponse;
import com.newsletterservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 피드 B형 뉴스레터 자동 전송 스케줄러
 * 사용자들이 정기적으로 피드 B형 뉴스레터를 받을 수 있도록 합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedBNewsletterScheduler {
    
    private final UserServiceClient userServiceClient;
    private final NewsletterService newsletterService;
    
    /**
     * 매일 오전 9시에 피드 B형 트렌딩 뉴스레터 전송
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendDailyTrendingFeedBNewsletter() {
        log.info("일일 피드 B형 트렌딩 뉴스레터 전송 시작");
        
        try {
            // 구독자 목록 조회 (실제 구현에서는 구독자 테이블에서 조회)
            List<Long> subscribers = getActiveSubscribers();
            
            if (subscribers.isEmpty()) {
                log.info("전송할 구독자가 없습니다.");
                return;
            }
            
            // 비동기로 각 구독자에게 전송
            for (Long userId : subscribers) {
                CompletableFuture.runAsync(() -> {
                    try {
                        sendPersonalizedFeedBToUser(userId);
                    } catch (Exception e) {
                        log.error("사용자 {} 피드 B형 뉴스레터 전송 실패", userId, e);
                    }
                });
            }
            
            log.info("일일 피드 B형 트렌딩 뉴스레터 전송 완료: {}명", subscribers.size());
            
        } catch (Exception e) {
            log.error("일일 피드 B형 트렌딩 뉴스레터 전송 실패", e);
        }
    }
    
    /**
     * 매주 월요일 오전 10시에 피드 B형 카테고리별 뉴스레터 전송
     */
    @Scheduled(cron = "0 0 10 * * MON")
    public void sendWeeklyCategoryFeedBNewsletter() {
        log.info("주간 피드 B형 카테고리별 뉴스레터 전송 시작");
        
        try {
            // 주요 카테고리들
            String[] categories = {"정치", "경제", "사회", "IT/과학", "생활"};
            
            for (String category : categories) {
                try {
                    // 카테고리별 구독자 조회
                    List<Long> categorySubscribers = getCategorySubscribers(category);
                    
                    if (categorySubscribers.isEmpty()) {
                        log.info("카테고리 {} 구독자가 없습니다.", category);
                        continue;
                    }
                    
                    // 비동기로 각 구독자에게 전송
                    for (Long userId : categorySubscribers) {
                        CompletableFuture.runAsync(() -> {
                            try {
                                sendCategoryFeedBToUser(userId, category);
                            } catch (Exception e) {
                                log.error("사용자 {} 카테고리 {} 피드 B형 뉴스레터 전송 실패", userId, category, e);
                            }
                        });
                    }
                    
                    log.info("카테고리 {} 피드 B형 뉴스레터 전송 완료: {}명", category, categorySubscribers.size());
                    
                } catch (Exception e) {
                    log.error("카테고리 {} 피드 B형 뉴스레터 전송 실패", category, e);
                }
            }
            
        } catch (Exception e) {
            log.error("주간 피드 B형 카테고리별 뉴스레터 전송 실패", e);
        }
    }
    
    /**
     * 사용자에게 개인화된 피드 B형 뉴스레터 전송
     */
    private void sendPersonalizedFeedBToUser(Long userId) {
        try {
            log.info("사용자 {} 개인화 피드 B형 뉴스레터 전송 시작", userId);
            
            // 사용자 정보 조회
            ApiResponse<UserResponse> userResponse = userServiceClient.getUserById(userId);
            if (userResponse == null || userResponse.getData() == null) {
                log.warn("사용자 {} 정보를 찾을 수 없습니다.", userId);
                return;
            }
            
            // 사용자의 카카오 액세스 토큰 조회 (실제 구현에서는 토큰 저장소에서 조회)
            String accessToken = getUserKakaoToken(userId);
            if (accessToken == null) {
                log.warn("사용자 {} 카카오 액세스 토큰이 없습니다.", userId);
                return;
            }
            
            // 피드 B형 뉴스레터 전송
            newsletterService.sendPersonalizedFeedBNewsletter(userId, accessToken);
            
            log.info("사용자 {} 개인화 피드 B형 뉴스레터 전송 완료", userId);
            
        } catch (Exception e) {
            log.error("사용자 {} 개인화 피드 B형 뉴스레터 전송 실패", userId, e);
        }
    }
    
    /**
     * 사용자에게 카테고리별 피드 B형 뉴스레터 전송
     */
    private void sendCategoryFeedBToUser(Long userId, String category) {
        try {
            log.info("사용자 {} 카테고리 {} 피드 B형 뉴스레터 전송 시작", userId, category);
            
            // 사용자의 카카오 액세스 토큰 조회
            String accessToken = getUserKakaoToken(userId);
            if (accessToken == null) {
                log.warn("사용자 {} 카카오 액세스 토큰이 없습니다.", userId);
                return;
            }
            
            // 카테고리별 피드 B형 뉴스레터 전송
            newsletterService.sendCategoryFeedBNewsletter(category, accessToken);
            
            log.info("사용자 {} 카테고리 {} 피드 B형 뉴스레터 전송 완료", userId, category);
            
        } catch (Exception e) {
            log.error("사용자 {} 카테고리 {} 피드 B형 뉴스레터 전송 실패", userId, category, e);
        }
    }
    
    /**
     * 활성 구독자 목록 조회
     * 실제 구현에서는 구독자 테이블에서 조회
     */
    private List<Long> getActiveSubscribers() {
        // 실제 구현: 데이터베이스에서 활성 구독자 조회
        // UnifiedNewsletterScheduler로 대체 예정
        return List.of(1L, 2L, 3L, 4L, 5L);
    }
    
    /**
     * 카테고리별 구독자 목록 조회
     * 실제 구현에서는 카테고리별 구독 정보에서 조회
     */
    private List<Long> getCategorySubscribers(String category) {
        // 임시로 테스트용 사용자 ID 반환
        // 실제 구현에서는 카테고리별 구독자 조회
        return List.of(1L, 2L, 3L);
    }
    
    /**
     * 사용자의 카카오 액세스 토큰 조회
     * 실제 구현에서는 토큰 저장소에서 조회
     * @deprecated UnifiedNewsletterScheduler와 EnhancedKakaoIntegrationService로 대체됨
     */
    @Deprecated
    private String getUserKakaoToken(Long userId) {
        // 임시로 테스트용 토큰 반환
        // 실제 구현에서는 EnhancedKakaoIntegrationService를 통해 UserService의 토큰 저장소에서 조회
        return "test-access-token-" + userId;
    }
    
    /**
     * 수동으로 피드 B형 뉴스레터 전송 (관리자용)
     */
    public void sendManualFeedBNewsletter(String type, String param, List<Long> userIds) {
        log.info("수동 피드 B형 뉴스레터 전송 시작: type={}, param={}, userIds={}", type, param, userIds);
        
        try {
            for (Long userId : userIds) {
                CompletableFuture.runAsync(() -> {
                    try {
                        String accessToken = getUserKakaoToken(userId);
                        if (accessToken == null) {
                            log.warn("사용자 {} 카카오 액세스 토큰이 없습니다.", userId);
                            return;
                        }
                        
                        switch (type.toLowerCase()) {
                            case "personalized":
                                newsletterService.sendPersonalizedFeedBNewsletter(userId, accessToken);
                                break;
                            case "category":
                                newsletterService.sendCategoryFeedBNewsletter(param, accessToken);
                                break;
                            case "trending":
                                newsletterService.sendTrendingFeedBNewsletter(accessToken);
                                break;
                            default:
                                log.warn("지원하지 않는 타입: {}", type);
                        }
                        
                        log.info("사용자 {} 수동 피드 B형 뉴스레터 전송 완료", userId);
                        
                    } catch (Exception e) {
                        log.error("사용자 {} 수동 피드 B형 뉴스레터 전송 실패", userId, e);
                    }
                });
            }
            
            log.info("수동 피드 B형 뉴스레터 전송 완료: {}명", userIds.size());
            
        } catch (Exception e) {
            log.error("수동 피드 B형 뉴스레터 전송 실패", e);
        }
    }
}
