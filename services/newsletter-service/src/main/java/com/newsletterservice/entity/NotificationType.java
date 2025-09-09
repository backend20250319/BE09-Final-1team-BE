package com.newsletterservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public enum NotificationType {
    NEWSLETTER("뉴스레터"),
    NEWS_UPDATE("뉴스 업데이트"),
    SYSTEM("시스템 알림"),
    PROMOTION("프로모션"),
    REMINDER("리마인더"),
    WELCOME("환영 메시지"),
    CUSTOM("사용자 정의"),
    PERMISSION_REQUEST("권한 요청");

    private final String description;
    
    NotificationType(String description) {
        this.description = description;
    }
}
