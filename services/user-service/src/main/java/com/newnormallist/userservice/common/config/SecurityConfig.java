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