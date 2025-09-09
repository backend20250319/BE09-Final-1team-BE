package com.newsletterservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DeliveryMethod {
    EMAIL("이메일", 1),
    SMS("SMS", 2),
    PUSH("푸시 알림", 3);

    private final String description;
    private final int level;
}