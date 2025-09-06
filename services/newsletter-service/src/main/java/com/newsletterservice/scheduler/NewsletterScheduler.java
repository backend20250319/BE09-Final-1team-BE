package com.newsletterservice.scheduler;

import com.newsletterservice.service.NewsletterDeliveryService;
import com.newsletterservice.service.PersonalizedRecommendationService;
import com.newsletterservice.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "newsletter.scheduling.enabled", havingValue = "true")
public class NewsletterScheduler {

    private final NewsletterDeliveryService deliveryService;
    private final PersonalizedRecommendationService recommendationService;
    private final UserServiceClient userServiceClient;

    /**
     * 예약된 뉴스레터 발송 처리 (매분 실행)
     */
    @Scheduled(fixedRate = 60000)
    public void processScheduledDeliveries() {
        log.debug("예약된 뉴스레터 발송 처리 시작");
        try {
            deliveryService.processScheduledDeliveries();
        } catch (Exception e) {
            log.error("예약된 뉴스레터 발송 처리 실패", e);
        }
    }

    /**
     * 개인화 뉴스레터 발송 (매일 오전 9시)
     * 사용자별 최적 빈도에 따라 발송
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendPersonalizedNewsletters() {
        log.info("개인화 뉴스레터 발송 시작");
        try {
            sendDailyPersonalizedNewsletters();
        } catch (Exception e) {
            log.error("개인화 뉴스레터 발송 실패", e);
        }
    }

    /**
     * 주간 개인화 뉴스레터 발송 (매주 월요일 오전 10시)
     */
    @Scheduled(cron = "0 0 10 * * MON", zone = "Asia/Seoul")
    public void sendWeeklyPersonalizedNewsletters() {
        log.info("주간 개인화 뉴스레터 발송 시작");
        try {
            sendWeeklyPersonalizedNewslettersInternal();
        } catch (Exception e) {
            log.error("주간 개인화 뉴스레터 발송 실패", e);
        }
    }

    /**
     * 월간 개인화 뉴스레터 발송 (매월 1일 오전 11시)
     */
    @Scheduled(cron = "0 0 11 1 * *", zone = "Asia/Seoul")
    public void sendMonthlyPersonalizedNewsletters() {
        log.info("월간 개인화 뉴스레터 발송 시작");
        try {
            sendMonthlyPersonalizedNewslettersInternal();
        } catch (Exception e) {
            log.error("월간 개인화 뉴스레터 발송 실패", e);
        }
    }

    /**
     * 사용자 행동 분석 기반 뉴스레터 빈도 최적화 (매일 오전 8시)
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
    public void optimizeNewsletterFrequency() {
        log.info("뉴스레터 빈도 최적화 시작");
        try {
            optimizeUserNewsletterFrequency();
        } catch (Exception e) {
            log.error("뉴스레터 빈도 최적화 실패", e);
        }
    }

    /**
     * 실패한 발송 재시도 (매 30분마다 실행)
     */
    @Scheduled(fixedRate = 1800000)
    public void retryFailedDeliveries() {
        log.debug("실패한 발송 재시도 처리 시작");
        try {
            deliveryService.retryFailedDeliveries();
        } catch (Exception e) {
            log.error("실패한 발송 재시도 처리 실패", e);
        }
    }

    /**
     * 일일 개인화 뉴스레터 발송
     */
    private void sendDailyPersonalizedNewsletters() {
        try {
            // 1. 활성 사용자 목록 조회
            List<Long> activeUsers = getActiveUsers();
            
            // 2. 각 사용자별 최적 빈도 확인 및 발송
            for (Long userId : activeUsers) {
                try {
                    String optimalFrequency = recommendationService.getOptimalNewsletterFrequency(userId);
                    
                    if ("DAILY".equals(optimalFrequency)) {
                        sendPersonalizedNewsletterToUser(userId, "DAILY");
                    }
                } catch (Exception e) {
                    log.warn("사용자 {} 일일 뉴스레터 발송 실패", userId, e);
                }
            }
            
            log.info("일일 개인화 뉴스레터 발송 완료: {}명", activeUsers.size());
            
        } catch (Exception e) {
            log.error("일일 개인화 뉴스레터 발송 처리 실패", e);
        }
    }

    /**
     * 주간 개인화 뉴스레터 발송
     */
    private void sendWeeklyPersonalizedNewslettersInternal() {
        try {
            List<Long> activeUsers = getActiveUsers();
            
            for (Long userId : activeUsers) {
                try {
                    String optimalFrequency = recommendationService.getOptimalNewsletterFrequency(userId);
                    
                    if ("WEEKLY".equals(optimalFrequency) || "DAILY".equals(optimalFrequency)) {
                        sendPersonalizedNewsletterToUser(userId, "WEEKLY");
                    }
                } catch (Exception e) {
                    log.warn("사용자 {} 주간 뉴스레터 발송 실패", userId, e);
                }
            }
            
            log.info("주간 개인화 뉴스레터 발송 완료: {}명", activeUsers.size());
            
        } catch (Exception e) {
            log.error("주간 개인화 뉴스레터 발송 처리 실패", e);
        }
    }

    /**
     * 월간 개인화 뉴스레터 발송
     */
    private void sendMonthlyPersonalizedNewslettersInternal() {
        try {
            List<Long> activeUsers = getActiveUsers();
            
            for (Long userId : activeUsers) {
                try {
                    // 모든 사용자에게 월간 뉴스레터 발송 (참여도 낮은 사용자 포함)
                    sendPersonalizedNewsletterToUser(userId, "MONTHLY");
                } catch (Exception e) {
                    log.warn("사용자 {} 월간 뉴스레터 발송 실패", userId, e);
                }
            }
            
            log.info("월간 개인화 뉴스레터 발송 완료: {}명", activeUsers.size());
            
        } catch (Exception e) {
            log.error("월간 개인화 뉴스레터 발송 처리 실패", e);
        }
    }

    /**
     * 개별 사용자에게 개인화 뉴스레터 발송
     */
    private void sendPersonalizedNewsletterToUser(Long userId, String frequency) {
        try {
            // 1. 개인화된 콘텐츠 생성
            PersonalizedRecommendationService.PersonalizedNewsletterContent content = 
                recommendationService.generatePersonalizedContent(userId);
            
            // 2. 뉴스레터 발송
            deliveryService.sendPersonalizedNewsletter(userId, content, frequency);
            
            log.debug("사용자 {} {} 뉴스레터 발송 완료", userId, frequency);
            
        } catch (Exception e) {
            log.error("사용자 {} {} 뉴스레터 발송 실패", userId, frequency, e);
            throw e;
        }
    }

    /**
     * 사용자별 뉴스레터 빈도 최적화
     */
    private void optimizeUserNewsletterFrequency() {
        try {
            List<Long> activeUsers = getActiveUsers();
            Map<String, Long> frequencyStats = activeUsers.stream()
                .collect(Collectors.groupingBy(
                    userId -> {
                        try {
                            return recommendationService.getOptimalNewsletterFrequency(userId);
                        } catch (Exception e) {
                            log.warn("사용자 {} 빈도 조회 실패", userId, e);
                            return "WEEKLY"; // 기본값
                        }
                    },
                    Collectors.counting()
                ));
            
            log.info("뉴스레터 빈도 최적화 완료: {}", frequencyStats);
            
        } catch (Exception e) {
            log.error("뉴스레터 빈도 최적화 실패", e);
        }
    }

    /**
     * 활성 사용자 목록 조회
     */
    private List<Long> getActiveUsers() {
        try {
            
            List<Long> allActiveUsers = new ArrayList<>();
            int page = 0;
            int size = 100; // 배치 크기
            boolean hasMore = true;
            
            while (hasMore) {
                try {
                    // UserServiceClient를 통해 활성 사용자 조회
                    var response = userServiceClient.getActiveUsers(page, size);
                    
                    if (response != null && response.getData() != null) {
                        List<Long> pageUsers = response.getData().stream()
                            .map(user -> user.getId()) // UserResponse에서 ID 추출
                            .collect(Collectors.toList());
                        
                        allActiveUsers.addAll(pageUsers);
                        
                        // 더 이상 데이터가 없으면 종료
                        if (pageUsers.size() < size) {
                            hasMore = false;
                        } else {
                            page++;
                        }
                    } else {
                        hasMore = false;
                    }
                    
                } catch (Exception e) {
                    log.warn("활성 사용자 조회 중 오류 발생 (page={}): {}", page, e.getMessage());
                    hasMore = false;
                }
            }
            
            log.info("활성 사용자 조회 완료: 총 {}명", allActiveUsers.size());
            return allActiveUsers;
            
        } catch (Exception e) {
            log.error("활성 사용자 조회 실패", e);
            return List.of();
        }
    }
}