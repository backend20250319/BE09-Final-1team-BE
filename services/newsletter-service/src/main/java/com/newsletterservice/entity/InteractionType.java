package com.newsletterservice.entity;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public enum InteractionType {
    VIEW("조회"),
    CLICK("클릭"),
    SHARE("공유"),
    LIKE("좋아요"),
    BOOKMARK("북마크");

    private final String description;
    
    InteractionType(String description) {
        this.description = description;
    }
}
