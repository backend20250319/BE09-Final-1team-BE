package com.newnormallist.newsservice.recommendation.service;

import java.util.Map;

import com.newnormallist.newsservice.recommendation.entity.RecommendationCategory;
import com.newnormallist.newsservice.recommendation.entity.AgeBucket;


// CI/CD를 위한 주석 추가 3
// 연령/성별에 따른 기본 분포 D(c) 제공 인터페이스.
public interface DemoBaseProvider {
    Map<RecommendationCategory, Double> getBase(AgeBucket age, String gender); // sum=1
}