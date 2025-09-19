package com.newsletterservice.config;

import feign.FeignException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;

@Configuration
@Slf4j
@Validated
public class FeignTimeoutConfig {
    
    /**
     * Feign 재시도 설정 - 더 강력한 재시도 메커니즘
     */
    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(1000, 3000, 3); // (period, maxPeriod, maxAttempts) - 3회 재시도, 최대 3초 간격
    }
    
    /**
     * Feign 타임아웃 설정 - 적절한 타임아웃으로 안정성 확보
     */
    @Bean
    public feign.Request.Options feignOptions() {
        return new feign.Request.Options(
            (int) java.time.Duration.ofSeconds(5).toMillis(),  // connectTimeout
            (int) java.time.Duration.ofSeconds(10).toMillis()  // readTimeout
        );
    }
    
    /**
     * Feign 에러 디코더
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            log.error("Feign 호출 실패: methodKey={}, status={}, reason={}", 
                    methodKey, response.status(), response.reason());
            
            try {
                if (response.status() == 401) {
                    log.error("인증 실패: 401 Unauthorized");
                    return new FeignException.Unauthorized("인증이 필요합니다", response.request(), response.body().asInputStream().readAllBytes(), null);
                } else if (response.status() == 404) {
                    log.error("리소스를 찾을 수 없음: 404 Not Found");
                    return new FeignException.NotFound("리소스를 찾을 수 없습니다", response.request(), response.body().asInputStream().readAllBytes(), null);
                } else if (response.status() >= 500) {
                    log.error("서버 오류: {} {}", response.status(), response.reason());
                    return new FeignException.InternalServerError("서버 오류가 발생했습니다", response.request(), response.body().asInputStream().readAllBytes(), null);
                }
                
                return new FeignException.FeignServerException(response.status(), response.reason(), response.request(), response.body().asInputStream().readAllBytes(), null);
            } catch (IOException e) {
                log.error("응답 본문 읽기 실패: {}", e.getMessage());
                return new FeignException.FeignServerException(response.status(), response.reason(), response.request(), new byte[0], null);
            }
        };
    }
}
