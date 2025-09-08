package com.newnormallist.userservice.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 웹 관련 설정 클래스
 */
@Configuration
public class WebConfig {

    /**
     * RestTemplate 빈 설정
     * 외부 API 호출을 위해 사용
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
