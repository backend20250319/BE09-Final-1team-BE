package com.newsletterservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 설정
 * newsletter.scheduling.enabled=true일 때만 활성화
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = "newsletter.scheduling.enabled", 
    havingValue = "true", 
    matchIfMissing = false
)
public class SchedulingConfig {
    
    // 스케줄링 관련 추가 설정이 필요한 경우 여기에 추가
    // 예: ThreadPoolTaskScheduler 설정, 스케줄러 모니터링 등
}