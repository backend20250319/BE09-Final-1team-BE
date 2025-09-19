package com.newnormallist.gatewayservice.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CookieToHeaderFilter implements GlobalFilter, Ordered {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "access-token";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();

        // 1. Authorization 헤더가 이미 있는지 확인
        if (headers.containsKey(HttpHeaders.AUTHORIZATION)) {
            // 헤더가 있으면 이 필터는 아무 작업도 하지 않음
            return chain.filter(exchange);
        }

        // 2. 헤더가 없으면 'access-token' 쿠키를 찾기
        HttpCookie accessTokenCookie = request.getCookies().getFirst(ACCESS_TOKEN_COOKIE_NAME);

        if (accessTokenCookie != null) {
            String token = accessTokenCookie.getValue();
            log.info("✅ [Gateway] Found access-token in cookie. Converting to Authorization header.");

            // 3. 쿠키 값을 사용하여 Authorization 헤더 생성
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }
        // 헤더도 없고 쿠키도 없으면 그냥 다음 필터로 보냄. (JwtAuthenticationFilter가 처리할 것임)
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 기존 JwtAuthenticationFilter(-1)보다 먼저 실행되어야 합니다.
        return -2;
    }
}