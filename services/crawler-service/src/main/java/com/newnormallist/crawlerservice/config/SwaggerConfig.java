package com.newnormallist.crawlerservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addServersItem(new Server().url("http://localhost:8083").description("크롤러 서비스 직접"))
                .addServersItem(new Server().url("http://localhost:8000").description("게이트웨이"))
                .info(new Info()
                        .title("크롤러 Service API")
                        .version("v1.0.0")
                        .description("뉴스 크롤링 및 데이터 수집 서비스 API"));
    }
}
