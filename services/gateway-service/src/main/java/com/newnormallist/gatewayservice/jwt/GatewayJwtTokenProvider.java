package com.newnormallist.gatewayservice.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Slf4j
@Component
public class GatewayJwtTokenProvider {

  @Value("${jwt.secret}")
  private String jwtSecret;

  private SecretKey secretKey;

  @PostConstruct // 빈 생성 후 초기화 메서드 호출
  // 초기화 메서드에서 SecretKey 생성
  public void init() {
    byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
    secretKey = Keys.hmacShaKeyFor(keyBytes);
  }
    // 토큰 유효성 검사 메서드
  public boolean validateToken(String token) {
    try {
      Jwts.parser()
              .verifyWith(secretKey)
              .build()
              .parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      log.error("Invalid JWT Token: {}", e.getMessage());
      return false;
    }
  }
    // JWT 토큰에서 클레임을 추출하는 공통 메서드
  private Claims getClaims(String token) {
    return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
  }

  public Long getUserIdFromJWT(String token) {
    // 클레임을 가져오는 로직을 공통 메소드로 분리하여 중복 제거
    return getClaims(token).get("userId", Long.class);
  }

  public String getRoleFromJWT(String token) {
    return getClaims(token).get("role", String.class);
  }
}
