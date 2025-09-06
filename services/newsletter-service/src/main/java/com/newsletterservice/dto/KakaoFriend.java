package com.newsletterservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class KakaoFriend {
    @JsonProperty("elements")
    private List<Friend> elements;
    
    @JsonProperty("total_count")
    private Integer totalCount;
    
    @JsonProperty("before_url")
    private String beforeUrl;
    
    @JsonProperty("after_url")
    private String afterUrl;
    
    @JsonProperty("favorite_count")
    private Integer favoriteCount;

    @Data
    public static class Friend {
        private Long id;
        private String uuid;
        private Boolean favorite;
        
        @JsonProperty("profile_nickname")
        private String profileNickname;
        
        @JsonProperty("profile_thumbnail_image")
        private String profileThumbnailImage;
        
        // Deprecated: allowed_msg는 더 이상 사용되지 않음
        @JsonProperty("allowed_msg")
        @Deprecated
        private Boolean allowedMsg;
        
        @JsonProperty("msg_blocked")
        private String msgBlocked;
    }
}
