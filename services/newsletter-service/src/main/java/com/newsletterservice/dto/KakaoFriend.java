package com.newsletterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 카카오 친구 목록 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KakaoFriend {
    
    private List<Friend> elements;
    private Integer totalCount;
    private String beforeUrl;
    private String afterUrl;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Friend {
        private Long id;
        private String uuid;
        private Boolean favorite;
        private String profileNickname;
        private String profileThumbnailImage;
        private Boolean allowedMsg;
    }
    
    /**
     * 즐겨찾기 친구 수를 반환합니다.
     * @return 즐겨찾기 친구 수
     */
    public Integer getFavoriteCount() {
        if (elements == null) {
            return 0;
        }
        return (int) elements.stream()
                .filter(friend -> friend.getFavorite() != null && friend.getFavorite())
                .count();
    }
}
