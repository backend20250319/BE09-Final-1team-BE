package com.newnormallist.tooltipservice.config;

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
                .addServersItem(new Server().url("http://localhost:8086").description("툴팁 서비스 직접"))
                .addServersItem(new Server().url("http://localhost:8000").description("게이트웨이"))
                .info(new Info()
                        .title("툴팁 서비스 API")
                        .version("v1.0.0")
                        .description("뉴스 본문 분석 및 어려운 단어 툴팁 서비스 API"));
    }
}

