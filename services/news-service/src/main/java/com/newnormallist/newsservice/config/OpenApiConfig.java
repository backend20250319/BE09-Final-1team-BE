package com.newnormallist.newsservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
// swagger 설정
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("News Service API")
                        .description("뉴스 서비스 API 문서")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("News Service Team")
                                .email("support@newnormallist.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Local Development Server"),
                        new Server().url("http://news-service:8082").description("Docker Development Server")
                ));
    }
}
