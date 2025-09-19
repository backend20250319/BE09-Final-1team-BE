package com.newnormallist.userservice.user.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.DeclareWarning;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
    ACTIVE("활성화"),
    INACTIVE("비활성화"),
    DELETED("삭제됨");

    private final String description;
}