package com.newnormallist.userservice.auth.scheduler;

import com.newnormallist.userservice.auth.token.PasswordResetTokenRepostitory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {
    private final PasswordResetTokenRepostitory passwordResetTokenRepostitory;
    // 매일 자정에 만료된 비밀번호 재설정 토큰 삭제
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void purgeExpiredPasswordResetTokens() {
        log.info("만료된 비밀번호 재설정 토큰 정리 작업을 시작합니다...");
        try {
            passwordResetTokenRepostitory.deleteByExpireDateBefore(LocalDateTime.now());
            log.info("만료된 비밀번호 재설정 토큰 정리 작업이 완료되었습니다.");
        } catch (Exception e) {
            log.error("만료된 비밀번호 재설정 토큰 정리 작업 중 오류가 발생했습니다.", e);
        }
    }
}
