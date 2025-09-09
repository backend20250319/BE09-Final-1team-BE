package com.newsletterservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long userId;
    private String email;
    private String nickname;
    private boolean letterOk;
    private List<String> hobbies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 웹 푸시 권한이 있는지 확인
     * 
     * @return 웹 푸시 권한 여부
     */
    public boolean hasWebPushPermission() {
        // TODO: 실제 웹 푸시 권한 확인 로직 구현
        // 현재는 기본값으로 true 반환
        return true;
    }
}