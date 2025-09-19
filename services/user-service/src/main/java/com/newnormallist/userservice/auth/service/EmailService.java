package com.newnormallist.userservice.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    // 이메일 전송 메소드
    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("비밀번호 재설정 요청");

        String resetUrl = "http://localhost:3000/reset-password?token=" + token;
        message.setText("비밀번호 재설정을 원하시면 아래 링크를 클릭하세요:\n" + resetUrl);

        mailSender.send(message);
    }
}
