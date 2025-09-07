package com.newsletterservice.service.impl;

import com.newsletterservice.common.exception.NewsletterException;
import com.newsletterservice.dto.DeliveryStats;
import com.newsletterservice.dto.NewsletterContent;
import com.newsletterservice.dto.NewsletterDeliveryRequest;
import com.newsletterservice.entity.DeliveryStatus;
import com.newsletterservice.entity.NewsletterDelivery;
import com.newsletterservice.repository.NewsletterDeliveryRepository;
import com.newsletterservice.service.EmailNewsletterRenderer;
import com.newsletterservice.service.NewsletterContentService;
import com.newsletterservice.service.NewsletterDeliveryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

/**
 * 뉴스레터 발송 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NewsletterDeliveryServiceImpl implements NewsletterDeliveryService {

    private final NewsletterDeliveryRepository deliveryRepository;
    private final NewsletterContentService contentService;
    private final EmailNewsletterRenderer emailRenderer;

    @Override
    public DeliveryStats sendNewsletterNow(NewsletterDeliveryRequest request, Long senderId) {
        try {
            return processDeliveryRequest(request, false);
        } catch (Exception e) {
            log.error("뉴스레터 즉시 발송 실패", e);
            throw new NewsletterException("뉴스레터 발송 중 오류가 발생했습니다.", "DELIVERY_ERROR");
        }
    }

    @Override
    public DeliveryStats scheduleNewsletter(NewsletterDeliveryRequest request, Long userId) {
        log.info("뉴스레터 예약 발송: newsletterId={}, userId={}", request.getNewsletterId(), userId);
        
        try {
            return processDeliveryRequest(request, true);
        } catch (Exception e) {
            log.error("뉴스레터 예약 발송 실패: newsletterId={}, userId={}", request.getNewsletterId(), userId, e);
            throw new NewsletterException("뉴스레터 예약 발송 중 오류가 발생했습니다.", "SCHEDULE_ERROR");
        }
    }

    @Override
    public void cancelDelivery(Long deliveryId, Long userId) {
        try {
            NewsletterDelivery delivery = getDeliveryWithPermissionCheck(deliveryId, userId);
            
            if (delivery.getStatus() == DeliveryStatus.SENT || delivery.getStatus() == DeliveryStatus.FAILED) {
                throw new NewsletterException("이미 처리된 발송은 취소할 수 없습니다.", "INVALID_STATUS");
            }
            
            delivery.updateStatus(DeliveryStatus.CANCELLED);
            delivery.setUpdatedAt(LocalDateTime.now());
            deliveryRepository.save(delivery);
            
        } catch (Exception e) {
            log.error("발송 취소 실패: deliveryId={}", deliveryId, e);
            throw new NewsletterException("발송 취소 중 오류가 발생했습니다.", "CANCEL_ERROR");
        }
    }

    @Override
    public void retryDelivery(Long deliveryId, Long userId) {
        log.info("발송 재시도: deliveryId={}, userId={}", deliveryId, userId);
        
        try {
            NewsletterDelivery delivery = getDeliveryWithPermissionCheck(deliveryId, userId);
            
            if (delivery.getStatus() != DeliveryStatus.FAILED) {
                throw new NewsletterException("실패한 발송만 재시도할 수 있습니다.", "INVALID_STATUS");
            }
            
            delivery.updateStatus(DeliveryStatus.PROCESSING);
            delivery.setErrorMessage(null);
            delivery.setUpdatedAt(LocalDateTime.now());
            deliveryRepository.save(delivery);
            
            // 비동기로 발송 재시도
            performDeliveryAsync(delivery);
            
        } catch (Exception e) {
            log.error("발송 재시도 실패: deliveryId={}, userId={}", deliveryId, userId, e);
            throw new NewsletterException("발송 재시도 중 오류가 발생했습니다.", "RETRY_ERROR");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NewsletterDelivery> getDeliveriesByUser(Long userId, Pageable pageable) {
        return deliveryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    // Private Helper Methods
    private DeliveryStats processDeliveryRequest(NewsletterDeliveryRequest request, boolean isScheduled) {
        int totalTargets = request.getTargetUserIds().size();
        int successCount = 0;
        int failureCount = 0;
        
        for (Long targetUserId : request.getTargetUserIds()) {
            try {
                NewsletterDelivery delivery = createDeliveryRecord(request, targetUserId, isScheduled);
                deliveryRepository.save(delivery);
                
                if (!isScheduled) {
                    performDelivery(delivery);
                }
                
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("사용자 {} 발송 처리 실패: {}", targetUserId, e.getMessage());
            }
        }
        
        double successRate = totalTargets > 0 ? (double) successCount / totalTargets * 100 : 0.0;
        return DeliveryStats.builder()
                .deliveredCount(successCount)
                .totalRecipients(totalTargets)
                .deliveryTime(LocalDateTime.now())
                .status("COMPLETED")
                .build();
    }

    private NewsletterDelivery createDeliveryRecord(NewsletterDeliveryRequest request, Long userId, boolean isScheduled) {
        return NewsletterDelivery.builder()
                .userId(userId)
                .newsletterId(request.getNewsletterId())
                .deliveryMethod(request.getDeliveryMethod())
                .status(isScheduled ? DeliveryStatus.SCHEDULED : DeliveryStatus.PROCESSING)
                .scheduledAt(isScheduled ? request.getScheduledAt() : LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void performDelivery(NewsletterDelivery delivery) {
        try {
            switch (delivery.getDeliveryMethod()) {
                case EMAIL -> sendByEmail(delivery);
                case SMS -> throw new RuntimeException("SMS 발송 기능은 아직 구현되지 않았습니다.");
                case PUSH -> throw new RuntimeException("푸시 알림 발송 기능은 아직 구현되지 않았습니다.");
            }

            delivery.updateStatus(DeliveryStatus.SENT);
            delivery.setSentAt(LocalDateTime.now());

        } catch (Exception e) {
            delivery.updateStatus(DeliveryStatus.FAILED);
            delivery.setErrorMessage(e.getMessage());
        }

        delivery.setUpdatedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);
    }

    @Async
    private void performDeliveryAsync(NewsletterDelivery delivery) {
        performDelivery(delivery);
    }

    private void sendByEmail(NewsletterDelivery delivery) {
        try {
            NewsletterContent content = contentService.buildPersonalizedContent(delivery.getUserId(), delivery.getNewsletterId());
            String htmlContent = emailRenderer.renderToHtml(content);
            
            // TODO: 실제 이메일 발송 서비스 호출
            // emailService.sendHtmlEmail(delivery.getUserId(), content.getTitle(), htmlContent);
            
            log.info("이메일 발송 완료: userId={}, newsletterId={}", delivery.getUserId(), delivery.getNewsletterId());
            
        } catch (Exception e) {
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    private NewsletterDelivery getDeliveryWithPermissionCheck(Long deliveryId, Long userId) {
        NewsletterDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NewsletterException("발송 기록을 찾을 수 없습니다.", "DELIVERY_NOT_FOUND"));
        
        if (!delivery.getUserId().equals(userId)) {
            throw new NewsletterException("권한이 없습니다.", "UNAUTHORIZED");
        }
        
        return delivery;
    }
    
}
