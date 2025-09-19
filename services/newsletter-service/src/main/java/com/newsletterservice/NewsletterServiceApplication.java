package com.newsletterservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaRepositories
@EnableFeignClients(basePackages = "com.newsletterservice.client")
@EnableScheduling
@EnableConfigurationProperties
public class NewsletterServiceApplication {

    public static void main(String[] args) {
        // 연결 재시도를 위한 시스템 프로퍼티 설정
        System.setProperty("spring.datasource.hikari.connection-timeout", "10000");
        System.setProperty("spring.datasource.hikari.maximum-pool-size", "2");
        System.setProperty("spring.datasource.hikari.minimum-idle", "1");
        System.setProperty("spring.jpa.hibernate.ddl-auto", "none");
        
        SpringApplication.run(NewsletterServiceApplication.class, args);
    }
}
