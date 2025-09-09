package com.newsletterservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public enum DeliveryStatus {
    PENDING("발송 대기"),
    PROCESSING("발송 중"),
    SENT("발송 완료"),
    OPENED("열람 완료"),
    FAILED("발송 실패"),
    BOUNCED("반송"),
    SCHEDULED("예약됨"),
    CANCELLED("취소됨");

    private final String description;
    
    DeliveryStatus(String description) {
        this.description = description;
    }
}