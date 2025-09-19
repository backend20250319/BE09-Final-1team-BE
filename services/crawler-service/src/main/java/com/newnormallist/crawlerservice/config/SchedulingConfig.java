package com.newnormallist.crawlerservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 설정 클래스
 * 
 * 역할:
 * - 크롤링 스케줄러 활성화/비활성화 제어
 * - 개발 환경에서는 수동 실행만 허용
 * - 운영 환경에서만 자동 스케줄링 활성화
 * 
 * 설정:
 * - crawler.scheduling.enabled=true: 스케줄링 활성화
 * - crawler.scheduling.enabled=false: 스케줄링 비활성화 (기본값)
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "crawler.scheduling.enabled", havingValue = "true", matchIfMissing = false)
public class SchedulingConfig {
    // 스케줄링이 필요할 때만 활성화됨
    // matchIfMissing = false: 설정이 없으면 비활성화
}
