package com.newsletterservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase 설정 클래스
 * Firebase Admin SDK를 초기화하고 FirebaseMessaging 빈을 생성합니다.
 */
@Slf4j
@Configuration
@Validated
@ConditionalOnProperty(name = "webpush.enabled", havingValue = "true", matchIfMissing = false)
public class FirebaseConfig {
    
    @Value("${firebase.config.path:firebase-service-account.json}")
    private String firebaseConfigPath;
    
    @Value("${firebase.project.id:}")
    private String projectId;
    
    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                log.info("Firebase 초기화 시작...");
                
                // Firebase 설정 파일 존재 여부 확인
                ClassPathResource resource = new ClassPathResource(firebaseConfigPath);
                if (!resource.exists()) {
                    log.warn("Firebase 설정 파일을 찾을 수 없습니다: {}. Firebase 기능이 비활성화됩니다.", firebaseConfigPath);
                    log.warn("Firebase 기능을 사용하려면 다음을 수행하세요:");
                    log.warn("1. Firebase Console에서 서비스 계정 키를 다운로드");
                    log.warn("2. 파일을 src/main/resources/firebase-service-account.json에 배치");
                    log.warn("3. application.yml에서 firebase.project.id 설정");
                    return;
                }
                
                // 프로젝트 ID 확인
                if (projectId == null || projectId.trim().isEmpty()) {
                    log.warn("Firebase 프로젝트 ID가 설정되지 않았습니다. Firebase 기능이 비활성화됩니다.");
                    log.warn("application.yml에서 firebase.project.id를 설정하세요.");
                    return;
                }
                
                InputStream serviceAccount = resource.getInputStream();
                
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setProjectId(projectId)
                        .build();
                
                FirebaseApp.initializeApp(options);
                log.info("Firebase 초기화 완료: projectId={}", projectId);
                
            } else {
                log.info("Firebase가 이미 초기화되어 있습니다.");
            }
            
        } catch (IOException e) {
            log.error("Firebase 초기화 실패: {}", e.getMessage(), e);
            log.warn("Firebase 기능이 비활성화됩니다. 웹 푸시 알림을 사용하려면 Firebase 설정을 확인하세요.");
            // RuntimeException을 던지지 않고 경고만 출력하여 애플리케이션이 계속 실행되도록 함
        }
    }
    
    @Bean
    public FirebaseMessaging firebaseMessaging() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return FirebaseMessaging.getInstance();
            } else {
                log.warn("Firebase가 초기화되지 않았습니다. FirebaseMessaging 빈을 생성할 수 없습니다.");
                return null;
            }
        } catch (Exception e) {
            log.error("FirebaseMessaging 빈 생성 실패: {}", e.getMessage(), e);
            return null;
        }
    }
}
