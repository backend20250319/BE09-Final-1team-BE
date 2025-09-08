package com.newsletterservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaRepositories
@EnableFeignClients(basePackages = "com.newsletterservice.client")
@ComponentScan(basePackages = "com.newsletterservice")
@EnableScheduling
public class NewsletterServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewsletterServiceApplication.class, args);
    }
}
