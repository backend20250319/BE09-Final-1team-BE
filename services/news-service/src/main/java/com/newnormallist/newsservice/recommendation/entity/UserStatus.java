package com.newnormallist.newsservice.recommendation.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    DELETED;
}
