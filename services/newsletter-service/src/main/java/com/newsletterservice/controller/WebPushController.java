package com.newsletterservice.controller;

import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.model.PushMessage;
import com.newsletterservice.model.PushSubscription;
import com.newsletterservice.service.WebPushService;
import com.newsletterservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 웹 푸시 알림 관리 컨트롤러
 */
@Tag(name = "Web Push", description = "웹 푸시 알림 관리 API")
@RestController
@RequestMapping("/api/webpush")
@RequiredArgsConstructor
@Slf4j
@Validated
public class WebPushController {
    
    private final WebPushService webPushService;
    private final UserService userService;
    
    /**
     * 테스트 푸시 알림 전송
     */
    @Operation(summary = "테스트 푸시 알림 전송", description = "모든 구독자에게 테스트 푸시 알림을 전송합니다.")
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<String>> sendTestNotification(
            @RequestParam String title,
            @RequestParam String body) {
        
        try {
            log.info("테스트 푸시 알림 전송 요청: title={}", title);
            
            // 웹 푸시 구독자 조회
            List<PushSubscription> subscriptions = userService.getWebPushSubscriptions();
            
            if (subscriptions.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("구독자가 없습니다."));
            }
            
            // 테스트 메시지 생성
            PushMessage testMessage = PushMessage.builder()
                    .title(title)
                    .body(body)
                    .icon("/images/test-icon.png")
                    .badge("/images/badge.png")
                    .url("/test")
                    .tag("test-notification")
                    .priority("normal")
                    .build();
            
            // 푸시 알림 전송
            int successCount = webPushService.sendBulkNotification(subscriptions, testMessage);
            
            String message = String.format("테스트 푸시 알림 전송 완료: %d/%d 성공", 
                    successCount, subscriptions.size());
            
            log.info(message);
            return ResponseEntity.ok(ApiResponse.success(message));
            
        } catch (Exception e) {
            log.error("테스트 푸시 알림 전송 실패", e);
            return ResponseEntity.ok(ApiResponse.error("PUSH_SEND_ERROR", "테스트 푸시 알림 전송에 실패했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 뉴스레터 푸시 알림 전송
     */
    @Operation(summary = "뉴스레터 푸시 알림 전송", description = "모든 구독자에게 뉴스레터 푸시 알림을 전송합니다.")
    @PostMapping("/newsletter")
    public ResponseEntity<ApiResponse<String>> sendNewsletterNotification(
            @RequestParam String title,
            @RequestParam String summary,
            @RequestParam String newsletterId) {
        
        try {
            log.info("뉴스레터 푸시 알림 전송 요청: title={}, newsletterId={}", title, newsletterId);
            
            // 웹 푸시 구독자 조회
            List<PushSubscription> subscriptions = userService.getWebPushSubscriptions();
            
            if (subscriptions.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("구독자가 없습니다."));
            }
            
            // 뉴스레터 메시지 생성
            PushMessage newsletterMessage = PushMessage.forNewsletter(title, summary, newsletterId);
            
            // 푸시 알림 전송
            int successCount = webPushService.sendBulkNotification(subscriptions, newsletterMessage);
            
            String message = String.format("뉴스레터 푸시 알림 전송 완료: %d/%d 성공", 
                    successCount, subscriptions.size());
            
            log.info(message);
            return ResponseEntity.ok(ApiResponse.success(message));
            
        } catch (Exception e) {
            log.error("뉴스레터 푸시 알림 전송 실패", e);
            return ResponseEntity.ok(ApiResponse.error("PUSH_SEND_ERROR", "뉴스레터 푸시 알림 전송에 실패했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 푸시 알림 통계 조회
     */
    @Operation(summary = "푸시 알림 통계 조회", description = "푸시 알림 전송 통계를 조회합니다.")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<WebPushService.PushStats>> getPushStats() {
        try {
            log.info("푸시 알림 통계 조회 요청");
            
            WebPushService.PushStats stats = webPushService.getPushStats();
            
            return ResponseEntity.ok(ApiResponse.success(stats));
            
        } catch (Exception e) {
            log.error("푸시 알림 통계 조회 실패", e);
            return ResponseEntity.ok(ApiResponse.error("STATS_ERROR", "푸시 알림 통계 조회에 실패했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 구독자 수 조회
     */
    @Operation(summary = "구독자 수 조회", description = "웹 푸시 구독자 수를 조회합니다.")
    @GetMapping("/subscribers/count")
    public ResponseEntity<ApiResponse<Integer>> getSubscriberCount() {
        try {
            log.info("구독자 수 조회 요청");
            
            List<PushSubscription> subscriptions = userService.getWebPushSubscriptions();
            int count = subscriptions.size();
            
            return ResponseEntity.ok(ApiResponse.success(count));
            
        } catch (Exception e) {
            log.error("구독자 수 조회 실패", e);
            return ResponseEntity.ok(ApiResponse.error("SUBSCRIBER_COUNT_ERROR", "구독자 수 조회에 실패했습니다: " + e.getMessage()));
        }
    }
}
