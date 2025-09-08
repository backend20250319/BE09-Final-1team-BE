package com.newnormallist.userservice.auth.token;

/**
 * OAuth 토큰 제공자 타입을 나타내는 enum
 */
public enum TokenProvider {
    KAKAO("카카오"),
    GOOGLE("구글"),
    NAVER("네이버");
    
    private final String description;
    
    TokenProvider(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
