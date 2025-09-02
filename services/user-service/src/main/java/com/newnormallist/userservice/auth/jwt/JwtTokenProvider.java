package com.newnormallist.userservice.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Value("${jwt.expiration}")
  private long accessTokenExpiration;

  @Value("${jwt.refresh-expiration}")
  private long refreshTokenExpiration;
  // JWT 생성 시 서명할 키
  private SecretKey secretKey;

  @PostConstruct
  public void init() {
    // 빈 초기화 후, BASE64인코딩 문자열을 디코딩하여 SecretKey로 변환
    byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
    secretKey = Keys.hmacShaKeyFor(keyBytes); // JWT 서명에 사용
  }

  // access token 생성 메소드 (claim에 userId 추가)
  public String createAccessToken(String emailId, String role, Long userId) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + accessTokenExpiration);
    return Jwts.builder()
        .subject(emailId)                 // sub: jwt에서 사용자 식별자 역할, 인증 주체
        .claim("role", role)               // 사용자 권한 정보
        .claim("userId", userId)           // 사용자 ID
        .issuedAt(now)                     // iat: 발급 시간
        .expiration(expiryDate)            // exp: 만료 시간
        .signWith(secretKey)               // 서명 키
        .compact();                        // 최종적으로 JWT 문자열로 생성
  }

  // refresh token 생성 메소드 (claim에 userId 추가)
  public String createRefreshToken(String emailId, String role, Long userId, String deviceId) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);
    return Jwts.builder()
        .subject(emailId)
        .claim("role", role)
        .claim("userId", userId)
        .claim("deviceId", deviceId)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(secretKey)
        .compact();
  }


    public boolean validateToken(String token) {
        try {
            // 서명 검증 + 만료 기간 포함해서 파싱 (문제가 없으면 true 반환)
            Jwts.parser()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
            return true;

        } catch (SecurityException | MalformedJwtException e) {
            // JWT 서명이 올바르지 않거나, 형식 자체가 잘못된 경우
            // 예: 토큰 구조가 3부분으로 되어 있지 않음, 서명 위조
            throw new BadCredentialsException("Invalid JWT Token", e);

        } catch (ExpiredJwtException e) {
            // 토큰이 유효기간을 초과한 경우
            throw new BadCredentialsException("Expired JWT Token", e);

        } catch (UnsupportedJwtException e) {
            // 지원되지 않는 형식의 JWT인 경우
            // 예: 압축 알고리즘이 이상하거나, 일반적으로 파싱이 불가능한 포맷
            throw new BadCredentialsException("Unsupported JWT Token", e);

        } catch (IllegalArgumentException e) {
            // 토큰이 null이거나 비어 있는 경우
            throw new BadCredentialsException("JWT Token claims empty", e);
        }
    }

    // 토큰에서 emailId(subject) 추출
  public String getEmailIdFromJWT(String token) {
    Claims claims = Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getBody();
    return claims.getSubject();
  }

  public Long getUserIdFromJWT(String token) {
    Claims claims = Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getBody();
    return claims.get("userId", Long.class);
  }

    public String getRoleFromJWT(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
        return claims.get("role", String.class);
    }


  public long getRefreshExpiration() {
    return refreshTokenExpiration;
  }
    /**
     * OAuth2 신규 유저를 위한 임시 토큰 생성
     * - 유효시간을 짧게 설정 (예: 10분)
     * - 권한(role) 정보를 포함하지 않아 다른 API 접근을 막음
     */
    public String createTempToken(String email, Long userId) {
        Date now = new Date();
        long tempTokenValidityInMilliseconds = 10 * 60 * 1000L; // 10분
        return Jwts.builder().subject(email)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + tempTokenValidityInMilliseconds))
                .signWith(secretKey)
                .compact();
    }
    /**
     * 각 토큰의 유효 시간을 가져오는 public getter 메소드
     * */
    public long getAccessTokenValidityInMilliseconds() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenValidityInMilliseconds() {
        return refreshTokenExpiration;
    }
}
