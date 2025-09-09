package com.newsletterservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 웹 푸시 메시지를 나타내는 모델 클래스
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushMessage {
    
    /**
     * 푸시 알림 제목
     */
    private String title;
    
    /**
     * 푸시 알림 본문
     */
    private String body;
    
    /**
     * 아이콘 URL
     */
    private String icon;
    
    /**
     * 배지 아이콘 URL
     */
    private String badge;
    
    /**
     * 클릭 시 이동할 URL
     */
    private String url;
    
    /**
     * 푸시 알림 태그 (중복 알림 방지용)
     */
    private String tag;
    
    /**
     * 푸시 알림 데이터 (추가 정보)
     */
    private String data;
    
    /**
     * 알림이 자동으로 사라지는 시간 (밀리초)
     */
    private Integer ttl;
    
    /**
     * 알림 우선순위 (high, normal, low)
     */
    private String priority;
    
    /**
     * 알림이 표시되는 시간 (밀리초)
     */
    private Long timestamp;
    
    
    /**
     * 제목과 본문만으로 푸시 메시지 생성
     */
    public static PushMessage of(String title, String body) {
        return PushMessage.builder()
                .title(title)
                .body(body)
                .timestamp(System.currentTimeMillis())
                .priority("normal")
                .ttl(86400000)
                .build();
    }
    
    /**
     * 뉴스레터용 푸시 메시지 생성
     */
    public static PushMessage forNewsletter(String title, String summary, String newsletterId) {
        return PushMessage.builder()
                .title(title)
                .body(summary)
                .icon("/images/newsletter-icon.png")
                .badge("/images/badge.png")
                .url("/newsletter/" + newsletterId)
                .tag("newsletter-" + newsletterId)
                .timestamp(System.currentTimeMillis())
                .priority("high")
                .ttl(86400000)
                .build();
    }
}
