package com.newsletterservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 처리 유틸리티
 */
@Component
@Slf4j
@Validated
public class JwtUtil {

    @Value("${jwt.secret:defaultSecretKeyForNewsletterService}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}") // 24시간
    private long expiration;

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    public String extractUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getSubject(); // JWT의 subject는 보통 userId
        } catch (Exception e) {
            log.error("JWT 토큰에서 사용자 ID 추출 실패", e);
            return null;
        }
    }

    /**
     * JWT 토큰에서 모든 클레임 추출
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * JWT 토큰 유효성 검증
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !isTokenExpired(claims);
        } catch (Exception e) {
            log.error("JWT 토큰 유효성 검증 실패", e);
            return false;
        }
    }

    /**
     * 토큰 만료 여부 확인
     */
    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    /**
     * 서명 키 생성
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * JWT 토큰에서 사용자 ID 추출 (안전한 방식)
     */
    public String extractUserIdSafely(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        try {
            // Bearer 접두사 제거
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 토큰 유효성 검증
            if (!isTokenValid(token)) {
                log.warn("유효하지 않은 JWT 토큰");
                return null;
            }

            return extractUserId(token);
        } catch (Exception e) {
            log.error("JWT 토큰 처리 중 오류 발생", e);
            return null;
        }
    }
}
