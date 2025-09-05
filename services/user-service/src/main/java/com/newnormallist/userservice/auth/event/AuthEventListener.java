package com.newnormallist.userservice.auth.event;

import com.newnormallist.userservice.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventListener {
    private final EmailService emailService;
    // 비밀번호 재설정 이메일 이벤트 처리
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordResetRequest(OnPasswordResetRequestEvent event) {
        log.info("비밀번호 재설정 트랜잭션 커밋 완료. 이메일 발송 시작, 수신자 : {}", event.getEmail());
        try {
            // 이메일 발송 로직 실행
            emailService.sendPasswordResetEmail(event.getEmail(), event.getToken());
            log.info("비밀번호 재설정 이메일 발송 성공, 수신자 : {}", event.getEmail());
        } catch (Exception e) {
            // 트랜잭션 커밋엔 성공했지만 이메일 발송에 실패한 경우
            log.error("비밀번호 재설정 이메일 발송 실패, 수신자 : {}, 오류 : {}", event.getEmail(), e.getMessage());
        }
    }
}
