package com.newsletterservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

/**
 * 카카오 API 관련 설정 클래스
 */
@Configuration
@EnableRetry
public class KakaoConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // 타임아웃 설정
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("Connection", "close");
            return execution.execute(request, body);
        });
        
        return restTemplate;
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
