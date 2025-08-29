package com.newnormallist.newsservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * News Service 전체 Swagger/OpenAPI 설정
 * 
 * 포함 도메인:
 * - 뉴스 관리 API (NewsController, LegacyNewsController 등)
 * - 개인화 추천 API (FeedController, RecommendationController)
 * - 사용자 관련 API (MyPageController, ScrapController 등)
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("뉴스 서비스 API")
                        .version("v1.0.0")
                        .description("뉴스 플랫폼의 모든 API 엔드포인트 명세서\n\n" +
                                "## 주요 기능\n" +
                                "- **뉴스 관리**: 뉴스 조회, 검색, 카테고리별 분류\n" +
                                "- **개인화 추천**: 사용자 맞춤형 뉴스 피드 제공\n" +
                                "- **사용자 기능**: 마이페이지, 스크랩, 신고 등\n" +
                                "- **컬렉션**: 뉴스 모음집 관리"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", 
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT 토큰을 입력하세요 (Bearer 제외)")));
    }
}
