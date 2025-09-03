package com.newnormallist.userservice.auth.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepostitory extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    // 특정 시간 이전에 만료된 모든 토큰 삭제
    @Transactional
    @Modifying
    void deleteByExpireDateBefore(LocalDateTime now);
}
