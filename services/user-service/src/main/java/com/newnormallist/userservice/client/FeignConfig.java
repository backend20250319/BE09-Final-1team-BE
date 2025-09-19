package com.newnormallist.userservice.client;

import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign Client 설정
 * 다른 서비스에서 User Service를 호출할 때 사용하는 설정
 */
@Configuration
public class FeignConfig {

    /**
     * Feign 로깅 레벨 설정
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * 요청 인터셉터 설정 (필요시 헤더 추가 등)
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 필요시 공통 헤더 추가
                // template.header("X-Service-Name", "user-service");
            }
        };
    }
}
