package com.newsletterservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.crypto.SecretKey;
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

    // JWT 생성 시 서명할 키
    private SecretKey signingKey;

    /**
     * JWT 시크릿 키 초기화 (User Service와 동일한 방식)
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            // BASE64 디코딩된 시크릿 키 사용 (User Service와 동일한 방식)
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            signingKey = Keys.hmacShaKeyFor(keyBytes);
            log.debug("JWT 시크릿 키 초기화 완료");
        } catch (Exception e) {
            log.warn("BASE64 디코딩 실패, 일반 문자열로 시크릿 키 생성: {}", e.getMessage());
            // BASE64 디코딩 실패 시 기존 방식 사용
            byte[] keyBytes = secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            signingKey = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    /**
     * JWT 토큰에서 사용자 ID 추출 (User Service와 동일한 방식)
     */
    public String extractUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            // User Service에서는 userId 클레임을 사용하므로 동일하게 처리
            Long userId = claims.get("userId", Long.class);
            if (userId != null) {
                return userId.toString();
            }
            // userId 클레임이 없으면 subject 사용 (fallback)
            return claims.getSubject();
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
                .verifyWith(signingKey)
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
     * JWT 토큰에서 사용자 ID 추출 (안전한 방식)
     */
    public String extractUserIdSafely(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.debug("JWT 토큰이 null이거나 빈 문자열입니다.");
            return null;
        }

        try {
            // Bearer 접두사 제거
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 토큰이 여전히 비어있는지 확인
            if (token.trim().isEmpty()) {
                log.debug("Bearer 접두사 제거 후 토큰이 비어있습니다.");
                return null;
            }

            // JWT 토큰 형식 검증 (최소한의 형식 체크)
            if (!isValidJwtFormat(token)) {
                log.warn("유효하지 않은 JWT 토큰 형식: {}", token.length() > 20 ? token.substring(0, 20) + "..." : token);
                return null;
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

    /**
     * JWT 토큰 형식 검증 (기본적인 형식 체크)
     */
    private boolean isValidJwtFormat(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        // JWT는 최소 3개의 부분(header.payload.signature)으로 구성되어야 함
        String[] parts = token.split("\\.");
        return parts.length == 3;
    }
}
