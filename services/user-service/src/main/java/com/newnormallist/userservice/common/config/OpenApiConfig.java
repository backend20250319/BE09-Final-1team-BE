package com.newnormallist.userservice.common.config; // common.config 등 적절한 패키지에 위치시키세요.

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Value("${app.gateway-url}")
    private String gatewayUrl;

    @Bean
    public OpenAPI openAPI() {
        // 1. API 문서의 기본 정보 설정
        Info info = new Info()
                .title("User Service API")
                .version("v1.0.0")
                .description("사용자 서비스의 API 명세서입니다.");

        // 2. JWT 인증 방식 정의 -  'bearerAuth'는 UserController에서 사용한 이름과 일치해야 함
        String jwtSchemeName = "bearerAuth";
        SecurityScheme securityScheme = new SecurityScheme()
                .name(jwtSchemeName) // 스키마 이름
                .type(SecurityScheme.Type.HTTP) // 인증 타입은 HTTP
                .scheme("bearer") // 스키마는 bearer
                .bearerFormat("JWT"); // 토큰 형식은 JWT

        // 3. 모든 API에 전역적으로 인증을 요구하는 설정을 추가
        // UserController의 @SecurityRequirement와 동일한 역할
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

        return new OpenAPI()
                .addServersItem(new Server().url(gatewayUrl))
                .info(info)
                // 4. 위에서 정의한 JWT 인증 스키마를 OpenAPI 문서의 Components에 추가
                .components(new Components().addSecuritySchemes(jwtSchemeName, securityScheme))
                // 5. 모든 API에 인증 요구사항을 적용
                .addSecurityItem(securityRequirement);
    }
}
