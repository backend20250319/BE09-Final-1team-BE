package com.newnormallist.userservice.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newnormallist.userservice.auth.token.TokenProvider;
import com.newnormallist.userservice.auth.token.UserToken;
import com.newnormallist.userservice.auth.token.UserTokenRepository;
import com.newnormallist.userservice.user.entity.User;
import com.newnormallist.userservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * 카카오 토큰 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoTokenService {

    private final UserTokenRepository userTokenRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Value("${kakao.client-secret}")
    private String kakaoClientSecret;

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";

    /**
     * 카카오 리프레시 토큰으로 액세스 토큰 갱신
     */
    public String refreshKakaoToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            log.warn("리프레시 토큰이 없습니다.");
            return null;
        }

        try {
            log.debug("카카오 토큰 갱신 시도: refreshToken={}", refreshToken.substring(0, Math.min(10, refreshToken.length())) + "...");

            // 카카오 토큰 갱신 요청 파라미터 구성
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("client_id", kakaoClientId);
            params.add("client_secret", kakaoClientSecret);
            params.add("refresh_token", refreshToken);

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            // 카카오 API 호출
            ResponseEntity<String> response = restTemplate.postForEntity(KAKAO_TOKEN_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tokenResponse = objectMapper.readValue(response.getBody(), Map.class);
                
                String newAccessToken = (String) tokenResponse.get("access_token");
                String newRefreshToken = (String) tokenResponse.get("refresh_token");
                Integer expiresIn = (Integer) tokenResponse.get("expires_in");

                if (newAccessToken != null) {
                    log.info("카카오 토큰 갱신 성공: expiresIn={}초", expiresIn);
                    
                    // 새로운 리프레시 토큰이 있으면 반환값에 포함
                    if (newRefreshToken != null) {
                        log.debug("새로운 리프레시 토큰도 발급됨");
                    }
                    
                    return newAccessToken;
                } else {
                    log.error("카카오 토큰 갱신 응답에 액세스 토큰이 없습니다: {}", response.getBody());
                    return null;
                }
            } else {
                log.error("카카오 토큰 갱신 실패: statusCode={}, body={}", 
                         response.getStatusCode(), response.getBody());
                return null;
            }

        } catch (Exception e) {
            log.error("카카오 토큰 갱신 중 오류 발생", e);
            return null;
        }
    }

    /**
     * 사용자의 카카오 토큰 조회 및 자동 갱신
     */
    public String getUserKakaoToken(String userId) {
        try {
            log.debug("사용자 카카오 토큰 조회: userId={}", userId);
            
            Optional<UserToken> userToken = userTokenRepository.findByUserIdAndProvider(
                Long.valueOf(userId), TokenProvider.KAKAO);
            
            if (userToken.isPresent()) {
                UserToken token = userToken.get();
                
                // 토큰 만료 확인
                if (token.isExpired()) {
                    log.warn("카카오 토큰이 만료됨: userId={}", userId);
                    
                    // 리프레시 토큰으로 갱신 시도
                    String refreshedToken = refreshKakaoToken(token.getRefreshToken());
                    if (refreshedToken != null) {
                        // 토큰 갱신 성공 시 DB 업데이트
                        token.updateAccessToken(refreshedToken);
                        userTokenRepository.save(token);
                        log.info("카카오 토큰 갱신 및 저장 완료: userId={}", userId);
                        return refreshedToken;
                    }
                    
                    log.error("카카오 토큰 갱신 실패: userId={}", userId);
                    return null; // 갱신 실패
                }
                
                log.debug("유효한 카카오 토큰 반환: userId={}", userId);
                return token.getAccessToken();
            }
            
            log.warn("카카오 토큰이 없음: userId={}", userId);
            return null; // 토큰 없음
            
        } catch (Exception e) {
            log.error("카카오 토큰 조회 실패: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 사용자 카카오 토큰 저장
     */
    public void saveUserKakaoToken(Long userId, String accessToken, String refreshToken, Integer expiresIn) {
        try {
            // 기존 토큰 조회
            Optional<UserToken> existingToken = userTokenRepository.findByUserIdAndProvider(userId, TokenProvider.KAKAO);
            
            LocalDateTime expiresAt = null;
            if (expiresIn != null) {
                expiresAt = LocalDateTime.now().plusSeconds(expiresIn);
            }
            
            if (existingToken.isPresent()) {
                // 기존 토큰 업데이트
                UserToken token = existingToken.get();
                token.updateTokens(accessToken, refreshToken, expiresAt);
                userTokenRepository.save(token);
                log.info("카카오 토큰 업데이트 완료: userId={}", userId);
            } else {
                // 새 토큰 생성 - User 엔티티 조회
                Optional<User> userOptional = userRepository.findById(userId);
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    UserToken newToken = UserToken.builder()
                            .user(user)
                            .provider(TokenProvider.KAKAO)
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .expiresAt(expiresAt)
                            .build();
                    
                    userTokenRepository.save(newToken);
                    log.info("새 카카오 토큰 저장 완료: userId={}", userId);
                } else {
                    log.error("사용자를 찾을 수 없습니다: userId={}", userId);
                }
            }
            
        } catch (Exception e) {
            log.error("카카오 토큰 저장 실패: userId={}", userId, e);
        }
    }
}
