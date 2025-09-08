package com.newsletterservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient; 

/**
 * WebClient 설정 클래스
 * 
 * 실무에서 선호하는 WebClient를 사용하여 HTTP 클라이언트를 설정합니다.
 * - 비동기/반응형 프로그래밍 지원
 * - 높은 처리량과 성능
 * - 현대적인 Spring 생태계와의 호환성
 */
@Configuration
public class WebClientConfig {
    
    /**
     * 기본 WebClient 빈 설정
     * - 메모리 크기 제한: 2MB (카카오톡 API 응답용)
     * - 타임아웃 설정: 기본값 사용
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();
    }
    
    /**
     * 카카오톡 API 전용 WebClient
     * - 카카오톡 API 특성에 맞는 설정
     */
    @Bean("kakaoWebClient")
    public WebClient kakaoWebClient() {
        return WebClient.builder()
            .baseUrl("https://kapi.kakao.com")
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();
    }
}
