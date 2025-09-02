package com.newnormallist.newsservice.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {


    private static final AntPathMatcher PATH = new AntPathMatcher();
    private static final String[] SKIP = {
            "/api/news/summary",
            "/api/news/summary/**",
            "/api/news/*/summary",
            "/swagger-ui/**", "/v3/api-docs/**",
            "/actuator/**", "/health", "/error"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // API Gateway가 전달한 헤더 읽기
        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");

        if (userId != null && userRole != null) {
            log.info("✅ [News-Service] Authenticating user ID: {}, Role: {}", userId, userRole);

            // Spring Security가 이해할 수 있는 인증 객체(Authentication)를 생성
            // Principal(주체)은 userId, Authorities(권한)는 userRole을 사용
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(userRole))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            log.warn("❌ [News-Service] User ID or Role header not found. Skipping authentication.");
        }

        filterChain.doFilter(request, response);
    }
}
