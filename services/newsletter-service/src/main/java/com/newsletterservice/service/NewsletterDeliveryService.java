package com.newsletterservice.service;

import com.newsletterservice.entity.NewsletterDelivery;
import com.newsletterservice.entity.DeliveryStatus;
import com.newsletterservice.entity.DeliveryMethod;
import com.newsletterservice.repository.NewsletterDeliveryRepository;
import com.newsletterservice.client.UserServiceClient;
import com.newsletterservice.client.dto.UserResponse;
import com.newsletterservice.service.PersonalizedRecommendationService.PersonalizedNewsletterContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NewsletterDeliveryService {

    private final NewsletterDeliveryRepository deliveryRepository;
    private final PersonalizedRecommendationService personalizedRecommendationService;
    private final UserServiceClient userServiceClient;

    /**
     * 개인화된 뉴스레터 발송 예약
     */
    public NewsletterDelivery schedulePersonalizedDelivery(Long userId, LocalDateTime scheduledAt) {
        log.info("개인화된 뉴스레터 발송 예약: userId={}, scheduledAt={}", userId, scheduledAt);
        
        try {
            // 1. 사용자 정보 조회
            com.newsletterservice.common.ApiResponse<UserResponse> userResponse = userServiceClient.getUserById(userId);
            UserResponse user = userResponse != null ? userResponse.getData() : null;
            
            if (user == null) {
                throw new RuntimeException("사용자를 찾을 수 없습니다: " + userId);
            }
            
            // 2. 개인화된 콘텐츠 생성
            PersonalizedNewsletterContent personalizedContent = 
                personalizedRecommendationService.generatePersonalizedContent(userId);
            
            // 3. HTML 콘텐츠 생성
            String htmlContent = generatePersonalizedHtmlContent(personalizedContent);
            
            // 4. 뉴스레터 배송 엔티티 생성
            NewsletterDelivery delivery = NewsletterDelivery.builder()
                    .userId(userId)
                    .recipientEmail(user.getEmail())
                    .title("맞춤 뉴스레터 - " + LocalDateTime.now().toLocalDate())
                    .content(htmlContent)
                    .personalizedContent(personalizedContent.toString()) // JSON 형태로 저장
                    .deliveryMethod(DeliveryMethod.EMAIL)
                    .scheduledAt(scheduledAt)
                    .status(DeliveryStatus.SCHEDULED)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            return deliveryRepository.save(delivery);
            
        } catch (Exception e) {
            log.error("개인화된 뉴스레터 발송 예약 실패: userId={}", userId, e);
            throw new RuntimeException("뉴스레터 예약 실패", e);
        }
    }

    /**
     * 개인화된 뉴스레터 즉시 발송
     */
    public NewsletterDelivery sendPersonalizedNewsletterImmediately(Long userId) {
        log.info("개인화된 뉴스레터 즉시 발송: userId={}", userId);
        
        try {
            // 1. 예약 생성
            NewsletterDelivery delivery = schedulePersonalizedDelivery(userId, LocalDateTime.now());
            
            // 2. 즉시 발송 처리
            return processDelivery(delivery);
            
        } catch (Exception e) {
            log.error("개인화된 뉴스레터 즉시 발송 실패: userId={}", userId, e);
            throw new RuntimeException("뉴스레터 발송 실패", e);
        }
    }

    /**
     * 여러 사용자에게 개인화된 뉴스레터 일괄 발송
     */
    @Transactional
    public List<NewsletterDelivery> sendBatchPersonalizedNewsletters(List<Long> userIds, LocalDateTime scheduledAt) {
        log.info("일괄 개인화 뉴스레터 발송: 사용자 수={}, scheduledAt={}", userIds.size(), scheduledAt);
        
        List<NewsletterDelivery> deliveries = new ArrayList<>();
        List<CompletableFuture<NewsletterDelivery>> futures = new ArrayList<>();
        
        // 비동기로 각 사용자별 뉴스레터 생성
        for (Long userId : userIds) {
            CompletableFuture<NewsletterDelivery> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return schedulePersonalizedDelivery(userId, scheduledAt);
                } catch (Exception e) {
                    log.error("사용자 {}의 뉴스레터 생성 실패", userId, e);
                    return createFailedDelivery(userId, e.getMessage());
                }
            });
            futures.add(future);
        }
        
        // 모든 작업 완료 대기
        for (CompletableFuture<NewsletterDelivery> future : futures) {
            try {
                deliveries.add(future.get());
            } catch (Exception e) {
                log.error("비동기 뉴스레터 생성 중 오류", e);
            }
        }
        
        log.info("일괄 개인화 뉴스레터 발송 완료: 성공={}", deliveries.size());
        return deliveries;
    }

    /**
     * 스마트 뉴스레터 발송 (사용자별 최적 빈도 적용)
     */
    @Transactional
    public void sendSmartNewsletters() {
        log.info("스마트 뉴스레터 발송 시작");
        
        try {
            // 1. 활성 사용자 목록 조회
            com.newsletterservice.common.ApiResponse<List<UserResponse>> activeUsersResponse = userServiceClient.getActiveUsers(0, 1000);
            List<UserResponse> activeUsers = activeUsersResponse != null ? activeUsersResponse.getData() : null;
            
            if (activeUsers == null || activeUsers.isEmpty()) {
                log.info("발송할 활성 사용자가 없습니다.");
                return;
            }
            
            // 2. 각 사용자별 최적 빈도 확인 및 발송
            for (UserResponse user : activeUsers) {
                try {
                    String optimalFrequency = personalizedRecommendationService
                        .getOptimalNewsletterFrequency(user.getId());
                    
                    if (shouldSendToday(user.getId(), optimalFrequency)) {
                        schedulePersonalizedDelivery(user.getId(), LocalDateTime.now().plusMinutes(5));
                        log.debug("사용자 {}에게 뉴스레터 발송 예약 (빈도: {})", user.getId(), optimalFrequency);
                    }
                    
                } catch (Exception e) {
                    log.warn("사용자 {}의 스마트 뉴스레터 처리 실패", user.getId(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("스마트 뉴스레터 발송 실패", e);
        }
        
        log.info("스마트 뉴스레터 발송 완료");
    }

    /**
     * 예약된 뉴스레터 자동 발송 처리
     */
    @Transactional
    public void processScheduledDeliveries() {
        log.debug("예약된 뉴스레터 발송 처리 시작");
        
        LocalDateTime now = LocalDateTime.now();
        List<NewsletterDelivery> scheduledDeliveries = 
            deliveryRepository.findByStatusAndScheduledAtBefore(DeliveryStatus.SCHEDULED, now);
        
        if (scheduledDeliveries.isEmpty()) {
            return;
        }
        
        log.info("처리할 예약된 뉴스레터 수: {}", scheduledDeliveries.size());
        
        for (NewsletterDelivery delivery : scheduledDeliveries) {
            try {
                processDelivery(delivery);
            } catch (Exception e) {
                log.error("예약된 뉴스레터 처리 실패: ID={}", delivery.getId(), e);
                markDeliveryFailed(delivery, e.getMessage());
            }
        }
    }

    /**
     * 실패한 발송 재시도
     */
    @Transactional
    public void retryFailedDeliveries() {
        log.debug("실패한 발송 재시도 처리 시작");
        
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<NewsletterDelivery> failedDeliveries = 
            deliveryRepository.findByStatusAndUpdatedAtAfterAndRetryCountLessThan(
                DeliveryStatus.FAILED, cutoff, 3);
        
        if (failedDeliveries.isEmpty()) {
            return;
        }
        
        log.info("재시도할 실패한 발송 수: {}", failedDeliveries.size());
        
        for (NewsletterDelivery delivery : failedDeliveries) {
            try {
                retryDelivery(delivery);
            } catch (Exception e) {
                log.error("실패한 발송 재시도 중 오류: ID={}", delivery.getId(), e);
            }
        }
    }

    /**
     * 개인화 뉴스레터 발송 (기존 메소드 유지)
     */
    public void sendPersonalizedNewsletter(Long userId, PersonalizedNewsletterContent content, String frequency) {
        try {
            log.info("개인화 뉴스레터 발송 시작: userId={}, frequency={}", userId, frequency);
            
            // 1. 뉴스레터 발송 기록 생성
            NewsletterDelivery delivery = NewsletterDelivery.builder()
                    .userId(userId)
                    .deliveryMethod(DeliveryMethod.EMAIL)
                    .status(DeliveryStatus.PENDING)
                    .scheduledAt(LocalDateTime.now())
                    .build();
            
            deliveryRepository.save(delivery);
            
            // 2. 실제 뉴스레터 발송 처리
            processDelivery(delivery);
            
            log.info("개인화 뉴스레터 발송 완료: userId={}, deliveryId={}", userId, delivery.getId());
            
        } catch (Exception e) {
            log.error("개인화 뉴스레터 발송 실패: userId={}, frequency={}", userId, frequency, e);
            throw new RuntimeException("개인화 뉴스레터 발송 실패", e);
        }
    }

    /**
     * 개별 배송 처리
     */
    private NewsletterDelivery processDelivery(NewsletterDelivery delivery) {
        log.info("뉴스레터 발송 처리: ID={}, 수신자={}", delivery.getId(), delivery.getRecipientEmail());
        
        delivery.updateStatus(DeliveryStatus.IN_PROGRESS);
        deliveryRepository.save(delivery);
        
        try {
            // 실제 발송 로직 수행
            performActualDelivery(delivery);
            
            delivery.updateStatus(DeliveryStatus.SENT);
            delivery.setSentAt(LocalDateTime.now());
            
            log.info("뉴스레터 발송 완료: ID={}", delivery.getId());
            
        } catch (Exception e) {
            log.error("뉴스레터 발송 실패: ID={}", delivery.getId(), e);
            markDeliveryFailed(delivery, e.getMessage());
        }
        
        return deliveryRepository.save(delivery);
    }

    /**
     * 실제 발송 수행
     */
    private void performActualDelivery(NewsletterDelivery delivery) {
        switch (delivery.getDeliveryMethod()) {
            case EMAIL -> sendEmail(delivery);
            case SMS -> sendSms(delivery);
            case PUSH_NOTIFICATION -> sendPushNotification(delivery);
            default -> throw new RuntimeException("지원하지 않는 발송 방법: " + delivery.getDeliveryMethod());
        }
    }

    /**
     * 이메일 발송 (실제 구현 필요)
     */
    private void sendEmail(NewsletterDelivery delivery) {
        // TODO: 실제 이메일 발송 로직 구현
        log.info("이메일 발송: {}", delivery.getRecipientEmail());
        
        // 시뮬레이션을 위한 간단한 지연
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * SMS 발송 (실제 구현 필요)
     */
    private void sendSms(NewsletterDelivery delivery) {
        // TODO: 실제 SMS 발송 로직 구현
        log.info("SMS 발송: {}", delivery.getRecipientEmail());
    }

    /**
     * 푸시 알림 발송 (실제 구현 필요)
     */
    private void sendPushNotification(NewsletterDelivery delivery) {
        // TODO: 실제 푸시 알림 발송 로직 구현
        log.info("푸시 알림 발송: {}", delivery.getRecipientEmail());
    }

    /**
     * 개인화된 HTML 콘텐츠 생성
     */
    private String generatePersonalizedHtmlContent(PersonalizedNewsletterContent content) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>")
            .append("<html><head><meta charset='UTF-8'>")
            .append("<title>맞춤 뉴스레터</title>")
            .append("<style>")
            .append("body { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; }")
            .append(".header { background: #2196f3; color: white; padding: 20px; text-align: center; }")
            .append(".content { padding: 20px; }")
            .append(".news-item { border: 1px solid #ddd; margin: 10px 0; padding: 15px; }")
            .append(".interests { background: #e3f2fd; padding: 10px; margin: 10px 0; }")
            .append("</style>")
            .append("</head><body>");
            
        // 헤더
        html.append("<div class='header'>")
            .append("<h1>🎯 당신을 위한 맞춤 뉴스</h1>")
            .append("</div>");
            
        // 관심사 표시
        if (!content.getUserInterests().isEmpty()) {
            html.append("<div class='interests'>")
                .append("<strong>관심 분야:</strong> ")
                .append(String.join(", ", content.getUserInterests()))
                .append("</div>");
        }
        
        // 뉴스 목록
        html.append("<div class='content'>");
        for (int i = 0; i < content.getPersonalizedNews().size(); i++) {
            var news = content.getPersonalizedNews().get(i);
            html.append("<div class='news-item'>")
                .append("<h3>").append(i + 1).append(". ").append(news.getTitle()).append("</h3>")
                .append("<p><strong>카테고리:</strong> ").append(news.getCategoryName()).append("</p>");
                
            // NewsResponse 필드 안전하게 접근
            try {
                if (news.getSummary() != null) {
                    html.append("<p>").append(news.getSummary()).append("</p>");
                }
            } catch (Exception e) {
                log.debug("NewsResponse에 summary 필드가 없습니다.");
            }
            
            try {
                if (news.getLink() != null) {
                    html.append("<p><a href='").append(news.getLink()).append("'>전체 기사 읽기</a></p>");
                }
            } catch (Exception e) {
                log.debug("NewsResponse에 link 필드가 없습니다.");
            }
            
            html.append("</div>");
        }
        html.append("</div>");
        
        html.append("</body></html>");
        
        return html.toString();
    }

    /**
     * 배송 실패 처리
     */
    private void markDeliveryFailed(NewsletterDelivery delivery, String errorMessage) {
        delivery.updateStatus(DeliveryStatus.FAILED);
        delivery.setErrorMessage(errorMessage);
        deliveryRepository.save(delivery);
    }

    /**
     * 실패한 배송 재시도
     */
    private void retryDelivery(NewsletterDelivery delivery) {
        log.info("실패한 발송 재시도: ID={}", delivery.getId());
        
        delivery.updateStatus(DeliveryStatus.IN_PROGRESS);
        delivery.incrementRetryCount();
        deliveryRepository.save(delivery);
        
        processDelivery(delivery);
    }

    /**
     * 실패한 배송 엔티티 생성
     */
    private NewsletterDelivery createFailedDelivery(Long userId, String errorMessage) {
        try {
            com.newsletterservice.common.ApiResponse<UserResponse> userResponse = userServiceClient.getUserById(userId);
            UserResponse user = userResponse != null ? userResponse.getData() : null;
            
            NewsletterDelivery delivery = NewsletterDelivery.builder()
                    .userId(userId)
                    .recipientEmail(user != null ? user.getEmail() : "unknown@example.com")
                    .title("뉴스레터 생성 실패")
                    .content("개인화 콘텐츠 생성 실패")
                    .deliveryMethod(DeliveryMethod.EMAIL)
                    .status(DeliveryStatus.FAILED)
                    .errorMessage(errorMessage)
                    .createdAt(LocalDateTime.now())
                    .build();
                    
            return deliveryRepository.save(delivery);
            
        } catch (Exception e) {
            log.error("실패한 배송 엔티티 생성 중 오류", e);
            throw new RuntimeException("실패 처리 중 오류", e);
        }
    }

    /**
     * 오늘 발송해야 하는지 확인
     */
    private boolean shouldSendToday(Long userId, String frequency) {
        // TODO: 실제 비즈니스 로직 구현
        // 마지막 발송일과 빈도를 고려하여 오늘 발송할지 결정
        
        switch (frequency) {
            case "DAILY" -> {
                return true; // 매일 발송
            }
            case "WEEKLY" -> {
                // 주간: 월요일에만 발송
                return LocalDateTime.now().getDayOfWeek().getValue() == 1;
            }
            case "MONTHLY" -> {
                // 월간: 매월 1일에만 발송
                return LocalDateTime.now().getDayOfMonth() == 1;
            }
            default -> {
                return false;
            }
        }
    }

    // 기존 메서드들도 유지
    public Page<NewsletterDelivery> getDeliveriesByStatus(DeliveryStatus status, Pageable pageable) {
        return deliveryRepository.findByStatus(status, pageable);
    }

    public List<NewsletterDelivery> getScheduledDeliveries() {
        return deliveryRepository.findByStatus(DeliveryStatus.SCHEDULED, org.springframework.data.domain.Pageable.unpaged()).getContent();
    }

    public NewsletterDelivery cancelDelivery(Long deliveryId) {
        NewsletterDelivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(() -> new RuntimeException("배송 정보를 찾을 수 없습니다."));
            
        if (delivery.getStatus() != DeliveryStatus.SCHEDULED) {
            throw new IllegalStateException("예약된 상태가 아닌 배송은 취소할 수 없습니다.");
        }
        
        delivery.updateStatus(DeliveryStatus.CANCELLED);
        return deliveryRepository.save(delivery);
    }

    public NewsletterDelivery retryDelivery(Long deliveryId) {
        NewsletterDelivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(() -> new RuntimeException("배송 정보를 찾을 수 없습니다."));
            
        if (delivery.getStatus() != DeliveryStatus.FAILED) {
            throw new IllegalStateException("실패한 상태가 아닌 배송은 재시도할 수 없습니다.");
        }
        
        return processDelivery(delivery);
    }
}