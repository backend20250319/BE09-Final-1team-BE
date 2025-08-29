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
import org.springframework.core.annotation.Order;
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

    @Bean
    @Order(1) // Swagger 관련 설정은 우선순위를 높게 설정합니다.
    public SecurityFilterChain swaggerChain(HttpSecurity http) throws Exception {
        http
                // Swagger 관련 URL에 대한 접근 허용
                .securityMatcher(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/user-api-docs/**",
                        "/user-swagger-ui.html"
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                        auth.anyRequest().permitAll()
                );
        return http.build();
    }

    @Bean
    @Order(2) // 사용자 서비스 관련 설정은 Swagger보다 낮은 우선순위로 설정합니다.
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 기본적인 stateless 설정 (CSRF, 폼 로그인 비활성화)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 2. 예외 처리 핸들러 설정
                .exceptionHandling(e ->
                        e.authenticationEntryPoint(restAuthenticationEntryPoint)
                                .accessDeniedHandler(restAccessDeniedHandler))
                // 3. 인가 규칙 설정
                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers(
//                                "/favicon.ico",
//                                "/api/auth/**", // 인증 관련 엔드포인트는 모두 허용
//                                "/api/users/signup" // 회원가입 엔드포인트는 모두 허용
//                        ).permitAll()
//                        .requestMatchers("/api/users/admin/**").hasRole("ADMIN")
//                        .anyRequest().authenticated() // 그 외의 요청은 인증 필요
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/api/auth/oauth2") // 카카오 인가 요청 URI 설정
                                .authorizationRequestRepository(cookieOAuth2AuthorizationRequestRepository)
                        )
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/api/auth/login/oauth2/code/*") // 카카오 리다이렉트 URI 패턴 설정
                        )
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )
                // 4. 게이트웨이가 추가한 헤더를 처리하는 커스텀 필터는 유지합니다.
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        // 5. .cors() 설정 및 CorsConfigurationSource Bean은 완전히 제거되었습니다.
        return http.build();
    }
}