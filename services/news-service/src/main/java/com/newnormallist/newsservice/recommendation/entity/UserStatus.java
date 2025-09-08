package com.newnormallist.newsservice.recommendation.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
    // ACTIVE = 활성, INACTIVE = 비활성, DELETED = 탈퇴
    ACTIVE,
    INACTIVE,
    DELETED;
}
