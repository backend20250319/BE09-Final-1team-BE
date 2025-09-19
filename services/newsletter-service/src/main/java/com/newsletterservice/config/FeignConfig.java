package com.newsletterservice.config;

import com.newsletterservice.common.exception.NewsletterException;
import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableFeignClients(basePackages = "com.newsletterservice.client")
@Slf4j
@Validated
public class FeignConfig {
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    /**
     * Feign 요청에 JWT 토큰 자동 추가 (서비스간 통신 지원) - 보안 강화
     * 단, 트렌딩 키워드 API는 인증이 필요하지 않으므로 제외
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // 트렌딩 키워드 API는 인증이 필요하지 않으므로 토큰 추가하지 않음
            if (isTrendingKeywordsApi(requestTemplate.url())) {
                log.debug("트렌딩 키워드 API 호출 - 인증 헤더 추가하지 않음: {}", requestTemplate.url());
                addServiceHeaders(requestTemplate);
                return;
            }
            
            String token = getCurrentJwtToken();
            if (token != null && !token.isEmpty()) {
                requestTemplate.header(AUTHORIZATION_HEADER, token);
                log.debug("JWT 토큰이 Feign 요청에 추가되었습니다");
            } else {
                log.debug("JWT 토큰이 없습니다. 서비스간 통신으로 처리됩니다.");
                addServiceHeaders(requestTemplate);
            }
        };
    }
    
    /**
     * 향상된 Feign 에러 디코더
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new EnhancedErrorDecoder();
    }
    
    /**
     * Feign 로그 레벨 설정 (운영환경에서는 BASIC으로 변경 권장)
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.HEADERS; // 토큰 정보는 로그에 포함되지 않음
    }

    /**
     * Feign 재시도 설정 - 더 보수적인 설정
     */
    @Bean
    public feign.Retryer feignRetryer() {
        return new feign.Retryer.Default(1000, 3000, 3); // 초기 1초, 최대 3초, 최대 3회
    }
    
    /**
     * 현재 요청에서 JWT 토큰 추출 (보안 강화)
     */
    private String getCurrentJwtToken() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader(AUTHORIZATION_HEADER);
                
                if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                    return authHeader; // "Bearer " 포함하여 반환
                }
            }
        } catch (Exception e) {
            log.warn("JWT 토큰 추출 중 오류 발생 (토큰 정보는 로그에 포함하지 않음)");
        }
        return null;
    }
    
    /**
     * 트렌딩 키워드 API인지 확인
     */
    private boolean isTrendingKeywordsApi(String url) {
        return url != null && (
            url.contains("/api/trending/trending-keywords") ||
            url.contains("/api/trending/latest") ||
            url.contains("/api/trending/popular") ||
            url.contains("/api/categories") ||
            url.contains("/api/search") ||
            url.contains("/api/news/by-category")
        );
    }
    
    /**
     * 서비스간 통신을 위한 안전한 헤더 추가
     */
    private void addServiceHeaders(feign.RequestTemplate requestTemplate) {
        requestTemplate.header("X-Service-Name", "newsletter-service");
        requestTemplate.header("X-Service-Call", "true");
        requestTemplate.header("X-Request-Source", "internal");
        log.debug("서비스간 통신 헤더 추가: {}", requestTemplate.url());
    }
    
    /**
     * 향상된 커스텀 에러 디코더
     */
    public static class EnhancedErrorDecoder implements ErrorDecoder {
        
        private final ErrorDecoder defaultErrorDecoder = new Default();
        
        @Override
        public Exception decode(String methodKey, feign.Response response) {
            String errorMessage = String.format("서비스 호출 실패: %s", methodKey);
            
            switch (response.status()) {
                case 400:
                    return new NewsletterException("잘못된 요청: " + methodKey, "BAD_REQUEST");
                case 401:
                    return new NewsletterException("인증 실패: " + methodKey, "UNAUTHORIZED");
                case 403:
                    return new NewsletterException("접근 권한 없음: " + methodKey, "FORBIDDEN");
                case 404:
                    return new NewsletterException("리소스를 찾을 수 없음: " + methodKey, "NOT_FOUND");
                case 429:
                    return new NewsletterException("요청 한도 초과: " + methodKey, "RATE_LIMIT_EXCEEDED");
                case 500:
                    return new NewsletterException("내부 서버 오류: " + methodKey, "INTERNAL_SERVER_ERROR");
                case 503:
                    return new NewsletterException("서비스 이용 불가: " + methodKey, "SERVICE_UNAVAILABLE");
                default:
                    log.error("예상하지 못한 HTTP 상태 코드: {} for {}", response.status(), methodKey);
                    return defaultErrorDecoder.decode(methodKey, response);
            }
        }
    }
}
