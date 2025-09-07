package com.newsletterservice.service;

import com.newsletterservice.client.UserServiceClient;
import com.newsletterservice.client.dto.UserResponse;
import com.newsletterservice.model.PushSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 사용자 서비스 (User Service와의 통신)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserServiceClient userServiceClient;
    
    /**
     * 사용자 정보 조회
     * 
     * @param userId 사용자 ID
     * @return 사용자 정보
     */
    public UserResponse getUser(Long userId) {
        try {
            log.debug("사용자 정보 조회: userId={}", userId);
            return userServiceClient.getUserById(userId).getData();
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패: userId={}", userId, e);
            throw new RuntimeException("사용자 정보를 조회할 수 없습니다.", e);
        }
    }
    
    /**
     * 웹 푸시 구독자 목록 조회
     * 
     * @return 웹 푸시 구독자 목록
     */
    public List<PushSubscription> getWebPushSubscriptions() {
        try {
            log.debug("웹 푸시 구독자 목록 조회");
            
            // TODO: 실제 User Service에서 웹 푸시 구독자 목록을 조회하는 로직 구현
            // 현재는 빈 목록을 반환 (실제 구현 시 UserServiceClient를 통해 조회)
            
            return List.of();
            
        } catch (Exception e) {
            log.error("웹 푸시 구독자 목록 조회 실패", e);
            throw new RuntimeException("웹 푸시 구독자 목록을 조회할 수 없습니다.", e);
        }
    }
}