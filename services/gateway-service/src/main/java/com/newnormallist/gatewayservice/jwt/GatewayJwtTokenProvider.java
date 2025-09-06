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

  @Value("${jwt.secret}") // application-secret.yml에서 비밀 키를 주입
  private String jwtSecret;

  private SecretKey secretKey;

  @PostConstruct
  public void init() {
    byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
    secretKey = Keys.hmacShaKeyFor(keyBytes);
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser()
              .verifyWith(secretKey) // 최신 스타일로 통일
              .build()
              .parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      log.error("Invalid JWT Token: {}", e.getMessage());
      return false;
    }
  }

  private Claims getClaims(String token) {
    return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
  }

  public Long getUserIdFromJWT(String token) {
    // 클레임을 가져오는 로직을 별도 메소드로 분리하여 중복 제거
    return getClaims(token).get("userId", Long.class);
  }

  public String getRoleFromJWT(String token) {
    // 동일한 최신 스타일의 메소드를 사용하도록 수정
    return getClaims(token).get("role", String.class);
  }

}
