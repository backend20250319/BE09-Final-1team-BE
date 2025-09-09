package com.newnormallist.gatewayservice.filter;

import com.newnormallist.gatewayservice.jwt.GatewayJwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

  private final GatewayJwtTokenProvider jwtTokenProvider;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();
    log.info("✅ [Gateway] Request Path: {}", path);

    // permitAll 경로는 토큰 검증 없이 통과
    if (isPermitAllPath(path)) {
      log.info("✅ [Gateway] PermitAll path, skipping token validation.");
      return chain.filter(exchange);
    }

    // Authorization 헤더에서 토큰 추출
    String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      log.error("❌ [Gateway] Authorization header is missing or does not start with Bearer.");
      return handleUnauthorized(exchange, "Authorization header is missing or invalid.");
    }

    String token = authHeader.substring(7);
    log.info("✅ [Gateway] Token found. Processing token...");

    try {
      // 토큰 유효성 검증
      if (!jwtTokenProvider.validateToken(token)) {
        // validateToken 내부에서 이미 로그를 찍고 false를 반환
        return handleUnauthorized(exchange, "Token validation failed.");
      }
      log.info("✅ [Gateway] Token validation successful. Extracting claims...");

      // 클레임 추출
      Long userId = jwtTokenProvider.getUserIdFromJWT(token);
      String role = jwtTokenProvider.getRoleFromJWT(token);

      // userId가 null인 경우 처리 (클레임 이름 불일치 등)
      if (userId == null) {
        log.error("❌ [Gateway] Could not extract userId from token. Check claim names ('USERID' vs 'userId').");
        return handleUnauthorized(exchange, "Invalid token claims.");
      }

      log.info("✅ [Gateway] Claims extracted. UserId: {}, Role: {}", userId, role);

      // 새로운 헤더를 추가하여 각 서비스로 전달
      ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
              .header("X-User-Id", String.valueOf(userId))
              .header("X-User-Role", role)
              .build();

      log.info("✅ [Gateway] X-User-Id and X-User-Role headers added. Forwarding request to downstream.");

      return chain.filter(exchange.mutate().request(mutatedRequest).build());

    } catch (Exception e) {
      log.error("❌ [Gateway] An unexpected error occurred during token processing.", e);
      return handleUnauthorized(exchange, "Error processing token.");
    }
  }

  /**
   * 인증이 필요하지 않은 경로인지 확인
   */
  private boolean isPermitAllPath(String path) {
    // /api/news/ 로 시작하지만, /mypage, /report, /scrap 을 포함하지 않는 경우에만 토큰 검증을 건너뜁니다.
    boolean isPublicNewsPath = path.startsWith("/api/news") &&
            !path.contains("/mypage") &&
            !path.contains("/report") &&
            !path.contains("/scrap") &&
            !path.contains("/collection") &&
            !path.contains("/feed");

    // /api/search/ 경로는 검색 기능으로 공개 접근 허용
    boolean isPublicSearchPath = path.startsWith("/api/search");

    // /api/trending/ 경로는 트렌딩 기능으로 공개 접근 허용
    boolean isPublicTrendingPath = path.startsWith("/api/trending");

    // /api/categories/ 경로는 카테고리 기능으로 공개 접근 허용
    boolean isPublicCategoriesPath = path.startsWith("/api/categories");

    // 뉴스레터 공개 경로들 - 인증 불필요
    boolean isPublicNewsletterPath = path.startsWith("/api/newsletter/stats/subscribers")
            || path.startsWith("/api/newsletter/trending-keywords")
            || path.startsWith("/api/newsletter/category/") && (path.contains("/trending-keywords") || path.contains("/headlines") || path.contains("/articles") || path.contains("/subscribers"))
            || path.startsWith("/api/newsletter/categories/subscribers")
            || path.startsWith("/api/newsletter/subscribe")
            || path.startsWith("/api/newsletter/confirm")
            || (path.startsWith("/api/newsletter/subscription/") && path.matches(".*/\\d+$")) // /api/newsletter/subscription/{id} 형태만 허용
            || path.startsWith("/api/newsletter/newsletters/unsubscribe");

    // 카카오 API 경로들 - 카카오 액세스 토큰을 사용하므로 JWT 토큰 검증 불필요
    boolean isKakaoApiPath = path.startsWith("/api/kakao/");

    return path.startsWith("/api/users/signup")
            || path.startsWith("/api/auth/")
            || path.startsWith("/api/users/categories")
            || isPublicNewsPath
            || isPublicSearchPath
            || isPublicTrendingPath
            || isPublicCategoriesPath
            || isPublicNewsletterPath
            || isKakaoApiPath
            || path.startsWith("/swagger-ui")
            || path.contains("api-docs");
  }

  /**
   * 401 Unauthorized 응답을 처리하는 헬퍼 메소드
   */
  private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    // 필요하다면 응답 본문에 에러 메시지를 추가할수 있음.
    return exchange.getResponse().setComplete();
  }

  @Override
  public int getOrder() {
    // 이 필터가 CookieToHeaderFilter(= -2) 다음에 실행되도록 설정
    return -1;
  }
}