package com.newnormallist.newsservice.recommendation.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/*
    quotas(7,5,3), readHalfLifeDays, scrapHalfLifeDays,
    readThreshold(10), scrapThreshold(10),
    케이스별 기본 가중치 등을 YAML로 바인딩.
*/
@Data
@ConfigurationProperties(prefix = "reco")
public class RecommendationProperties {
    private List<Integer> quotas; // e.g. [7,5,3]
    private int readThreshold;
    private int scrapThreshold;
    private double readHalfLifeDays;
    private double scrapHalfLifeDays;

    private double case1Demo, case1Pref, case1Read, case1Scrap;
    private double case2Demo, case2Pref, case2Read, case2Scrap;
    private double case3Demo, case3Pref, case3Read, case3Scrap;
    private double case4Demo, case4Pref, case4Read, case4Scrap;
}
