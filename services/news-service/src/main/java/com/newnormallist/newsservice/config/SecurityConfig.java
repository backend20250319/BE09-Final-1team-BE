package com.newnormallist.newsservice.config;

import com.newnormallist.newsservice.config.security.HeaderAuthenticationFilter;
import com.newnormallist.newsservice.config.security.RestAccessDeniedHandler;
import com.newnormallist.newsservice.config.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // 사용자 정의 인증 필터 (예: JWT 토큰 검증)
    private final HeaderAuthenticationFilter headerAuthenticationFilter;
    // 인증 실패 시 (예: 로그인 안 된 상태에서 인증 필요한 리소스 접근) 처리 핸들러
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    // 인가 실패 시 (예: 권한 없는 사용자가 접근) 처리 핸들러
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 기본 설정 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. 예외 처리 핸들러 설정
                .exceptionHandling(exception -> exception
                        // 인증 실패 시 (401 Unauthorized) 호출될 핸들러를 지정
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        // 인가 실패 시 (403 Forbidden - 권한 없음) 호출될 핸들러를 지정
                        .accessDeniedHandler(restAccessDeniedHandler)
                )

                // 3. 경로별 접근 권한 설정 (Default-Deny 정책: 명시적으로 허용하지 않으면 기본적으로 인증 필요)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(
                                "/actuator/**",
                                "/health",
                                "/error",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/news-api-docs/**",
                                "/news-swagger-ui.html"  // 새로운 Swagger UI 경로 추가
                        ).permitAll()
                        .requestMatchers(
                                "/api/news/summary",        // 본문(텍스트) 요약
                                "/api/news/summary/**",
                                "/api/news/*/summary"       // ID 요약: /api/news/{id}/summary
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/news/summary",
                                "/api/news/summary/**",
                                "/api/news/*/summary"
                        ).permitAll()
                        .requestMatchers("/api/summarizer/**").permitAll()

                        // 3-2. 특정 리소스 (컬렉션, 마이페이지 뉴스) 관련 경로는 반드시 인증이 필요
                        // "/api/collections/**": 모든 컬렉션 관련 API
                        // "/api/news/mypage/**": 마이페이지 뉴스 관련 API (예: 내가 스크랩한 뉴스)
                        .requestMatchers("/api/collections/**", "/api/news/mypage/**").authenticated()

                        // 3-3. 뉴스 목록 조회, 상세 조회 등 GET 요청은 누구나 가능하도록 허용합니다.
                        // "/api/news/**" 경로에 대한 GET 요청만 허용합니다. (예: /api/news/list, /api/news/123)
                        .requestMatchers(HttpMethod.GET, "/api/news/**").permitAll() // GET 요청은 모든 사용자에게 접근 허용
                        
                        // 3-4. 트렌딩 관련 API는 누구나 접근 가능하도록 허용합니다.
                        .requestMatchers(HttpMethod.GET, "/api/trending/**").permitAll() // 트렌딩 API는 모든 사용자에게 접근 허용
                        
                        // 3-5. 카테고리 관련 API는 누구나 접근 가능하도록 허용합니다.
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll() // 카테고리 API는 모든 사용자에게 접근 허용
                        
                        // 3-6. 검색 관련 API는 누구나 접근 가능하도록 허용합니다.
                        .requestMatchers(HttpMethod.GET, "/api/search/**").permitAll() // 검색 API는 모든 사용자에게 접근 허용
                        
                        .anyRequest().authenticated()
                )

                // 4. 커스텀 필터 추가
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();

    }
}
