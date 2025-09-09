package com.newsletterservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SubscriptionStatus {
    ACTIVE("활성"),
    PAUSED("일시정지"),
    UNSUBSCRIBED("구독취소");

    private final String description;

}
