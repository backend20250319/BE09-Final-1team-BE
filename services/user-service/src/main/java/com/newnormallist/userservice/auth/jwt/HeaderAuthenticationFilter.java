package com.newnormallist.userservice.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // OPTIONS 메서드에 대해서는 필터를 건너뛰도록 처리 (CORS Preflight)
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("✅ [User-Service] HeaderAuthenticationFilter is running for path: {}", request.getRequestURI());

        // API Gateway가 전달한 헤더 읽기
        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");

        if (userId != null && userRole != null) {
            log.info("✅ [User-Service] Headers found. UserId: {}, Role: {}", userId, userRole);

            try {

                // ✅ [수정 2] Spring Security의 hasRole()과 연동을 위해 "ROLE_" 접두사 추가
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + userRole);

                // 인증 토큰 생성
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, // Principal: 사용자 ID
                        null,      // Credentials: 비밀번호는 필요 없음
                        Collections.singletonList(authority) // Authorities: 권한
                );

                // SecurityContext에 인증 정보를 등록
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("✅ [User-Service] Authentication object created and set in SecurityContext.");

            } catch (NumberFormatException e) {
                log.error("❌ [User-Service] Invalid X-User-Id format: {}", userId, e);
                // ID 형식이 잘못된 경우, 인증을 설정하지 않고 다음 필터로 넘어감
            }
        }

        filterChain.doFilter(request, response);
    }
}