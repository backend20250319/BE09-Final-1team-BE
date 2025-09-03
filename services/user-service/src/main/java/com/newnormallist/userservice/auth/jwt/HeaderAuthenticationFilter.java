package com.newnormallist.userservice.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j; // Slf4j import
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // OPTIONS 메서드에 대해서는 필터를 건너뛰도록 처리
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        log.info("✅ [User-Service] HeaderAuthenticationFilter is running for path: {}", request.getRequestURI());
        // API Gateway가 전달한 헤더 읽기
        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");

        log.info("✅ [User-Service] Received Header X-User-Id: {}", userId);
        log.info("✅ [User-Service] Received Header X-User-Role: {}", role);

        if (userId != null && !userId.equals("0") && role != null) {
            log.info("✅ [User-Service] Headers found. Creating Authentication object...");

            PreAuthenticatedAuthenticationToken authentication =
                    new PreAuthenticatedAuthenticationToken(userId, null,
                            Collections.singletonList(new SimpleGrantedAuthority(role)));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("✅ [User-Service] Authentication object created and set in SecurityContext for userId: {}", userId);
        } else {
            log.warn("❌ [User-Service] Headers not found or user is GUEST. Skipping authentication setup.");
        }

        filterChain.doFilter(request, response);
    }
}