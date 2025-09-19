package com.newnormallist.newsservice.recommendation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.newnormallist.newsservice.recommendation.service.DemoBaseProvider;
import com.newnormallist.newsservice.recommendation.service.impl.InMemoryDemoBaseProvider;

// 모듈 빈 등록. InMemoryDemoBaseProvider, 구현체들, 스케줄 설정 등
@Configuration
@EnableConfigurationProperties(RecommendationProperties.class)
public class RecommendationConfig {

    @Bean
    public DemoBaseProvider demoBaseProvider() {
        return new InMemoryDemoBaseProvider();
    }

    // WeightSelector, VectorBuilder, RecommendationService는 @Service 어노테이션으로 자동 등록됨
    // VectorBatchService도 @Service 어노테이션으로 자동 등록됨
}