package com.newsletterservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("admin").password("{noop}admin").roles("USER").build()
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 기본 설정
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. 경로별 접근 권한 설정
                .authorizeHttpRequests(authz -> authz
                        // 2-1. 공개 접근 허용 (모니터링, API 문서 등)
                        .requestMatchers(
                                "/actuator/**",
                                "/health",
                                "/error"
                        ).permitAll()

                        // 2-2. 뉴스레터 구독 관련 - 인증 불필요
                        .requestMatchers(HttpMethod.POST, "/api/newsletter/subscribe").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/subscriptions").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/confirm").permitAll()

                        // 2-3. 뉴스레터 콘텐츠 조회 - 인증 불필요
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/{newsletterId}/content").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/{newsletterId}/html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/{newsletterId}/preview").permitAll()

                        // 2-4. 구독 관리 - 인증 불필요 (개별 인증 로직으로 처리)
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/subscription/\\d+").permitAll() // 숫자 ID만 허용
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/subscription/my").permitAll() // 내 구독 조회
                        .requestMatchers(HttpMethod.DELETE, "/api/newsletter/subscription/{id}").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/newsletter/subscription/{subscriptionId}/status").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/newsletter/newsletters/unsubscribe").permitAll()

                        // 2-5. 공개 통계 및 트렌딩 정보 - 인증 불필요
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/trending-keywords").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/category/*/trending-keywords").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/category/*/headlines").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/category/*/articles").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/category/*/subscribers").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/categories/subscribers").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/stats/subscribers").permitAll()
                        
                        // 2-5-1. 차별화된 뉴스레터 서비스 - 인증 불필요 (하이브리드)
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/enhanced").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/hybrid").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/smart-recommendations").permitAll()

                        // 2-6. 레거시 경로 지원
                        .requestMatchers(HttpMethod.GET, "/newsletter/trending-keywords").permitAll()
                        .requestMatchers(HttpMethod.GET, "/newsletter/category/*/trending-keywords").permitAll()

                        // 2-7. 테스트 API - 인증 불필요
                        .requestMatchers("/api/newsletters/test-user-service/**").permitAll()
                        .requestMatchers("/api/newsletter/debug/**").permitAll()

                        // 2-8. 관리자 기능 - 인증 필요
                        .requestMatchers("/api/newsletter/delivery/**").authenticated()

                        // 2-9. 나머지 모든 요청은 인증 불필요 (개발용)
                        .anyRequest().permitAll()
                )

                // 3. HTTP Basic 인증 사용 (개발용)
                .httpBasic(basic -> {});

        return http.build();
    }
}