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
            "/api/trending/**",  // 트렌딩 API는 인증 필터 건너뛰기
            "/api/categories/**", // 카테고리 API는 인증 필터 건너뛰기
            "/api/search/**",    // 검색 API는 인증 필터 건너뛰기
            "/swagger-ui/**", "/v3/api-docs/**",
            "/actuator/**", "/health", "/error"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 건너뛸 경로인지 확인
        String requestPath = request.getRequestURI();
        for (String skipPath : SKIP) {
            if (PATH.match(skipPath, requestPath)) {
                log.debug("인증 필터 건너뛰기: {}", requestPath);
                filterChain.doFilter(request, response);
                return;
            }
        }

        // API Gateway가 전달한 헤더 읽기
        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");
        
        // 서비스간 통신 헤더 확인
        String serviceName = request.getHeader("X-Service-Name");
        String serviceCall = request.getHeader("X-Service-Call");

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
        } else if (serviceName != null && "true".equals(serviceCall)) {
            log.info("✅ [News-Service] Service-to-service call from: {}", serviceName);
            
            // 서비스간 통신을 위한 인증 객체 생성
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "service-" + serviceName,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("SERVICE"))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            log.warn("❌ [News-Service] User ID or Role header not found. Skipping authentication.");
        }

        filterChain.doFilter(request, response);
    }
}
