package com.newnormallist.userservice.auth.repository;

import com.newnormallist.userservice.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    // 사용자 ID로 Refresh Token 조회
    Optional<RefreshToken> findByUserId(Long userId);
    // 토큰 값으로 Refresh Token 조회
    Optional<RefreshToken> findByTokenValue(String tokenValue);
    // 토큰 값으로 Refresh Token 삭제
    void deleteByTokenValue(String tokenValue);
    // 사용자ID로 Refresh Token 삭제
    void deleteByUserId(Long userId);
}
