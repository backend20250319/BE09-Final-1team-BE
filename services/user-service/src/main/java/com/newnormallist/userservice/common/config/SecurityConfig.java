package com.newnormallist.userservice.common.config;

import com.newnormallist.userservice.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.newnormallist.userservice.auth.jwt.HeaderAuthenticationFilter;
import com.newnormallist.userservice.auth.jwt.RestAccessDeniedHandler;
import com.newnormallist.userservice.auth.jwt.RestAuthenticationEntryPoint;
import com.newnormallist.userservice.auth.repository.CookieOAuth2AuthorizationRequestRepository;
import com.newnormallist.userservice.auth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    private final HeaderAuthenticationFilter headerAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final CookieOAuth2AuthorizationRequestRepository cookieOAuth2AuthorizationRequestRepository;

    // 공통 경로 상수로 관리
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",
            "/api/users/signup",
            "/api/users/categories"
    };
   // 서비스간 통신용 경로 (인증 면제) - 간소화됨
   private static final String[] INTERNAL_SERVICE_ENDPOINTS = {
        "/api/users/*",                    // 기본 사용자 정보 조회
        "/api/users/*/read-news-ids",      // 읽은 뉴스 ID 목록
        "/api/users/*/read-news/**",       // 읽기 기록 관련
        "/api/users/*/interests",          // 관심사 분석
        "/api/users/*/behavior-analysis",  // 행동 분석
        "/api/users/*/categories",         // 카테고리 선호도
        "/api/users/*/optimal-newsletter-frequency", // 최적 뉴스레터 빈도
        "/api/users/mypage/history/**",    // 마이페이지 히스토리
        "/api/users/active",               // 활성 사용자 목록
        "/api/users/batch",                // 배치 사용자 조회
        "/api/users/email/*",              // 이메일로 사용자 조회
        "/api/users/*/exists"              // 사용자 존재 확인
    };

    private static final String[] SWAGGER_ENDPOINTS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/user-api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // 기본 보안 설정
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 예외 처리 핸들러
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))

                // 인가 규칙 - 하나의 체인에서 모든 경로 관리
                .authorizeHttpRequests(auth -> auth
                        // Swagger 관련 경로 - 완전 공개
                        .requestMatchers(SWAGGER_ENDPOINTS).permitAll()
                        // 공개 API 엔드포인트
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // 서비스간 통신용 경로 - 인증 면제
                        .requestMatchers(INTERNAL_SERVICE_ENDPOINTS).permitAll()
                        // 관리자 전용 엔드포인트
                        .requestMatchers("/api/users/admin/**").hasRole("ADMIN")
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/api/auth/oauth2")
                                .authorizationRequestRepository(cookieOAuth2AuthorizationRequestRepository))
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/api/auth/login/oauth2/code/*"))
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler))

                // 커스텀 인증 필터
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}