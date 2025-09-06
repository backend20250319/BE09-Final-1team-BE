package com.newnormallist.newsservice.recommendation.service;

import java.util.Map;

import com.newnormallist.newsservice.recommendation.entity.RecommendationCategory;
import com.newnormallist.newsservice.recommendation.entity.AgeBucket;


// 자체 기준
// 연령/성별에 따른 기본 분포 D(c) 제공 인터페이스.
public interface DemoBaseProvider {
    Map<RecommendationCategory, Double> getBase(AgeBucket age, String gender); // sum=1
}