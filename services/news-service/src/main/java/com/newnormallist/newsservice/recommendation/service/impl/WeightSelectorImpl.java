package com.newnormallist.newsservice.recommendation.service.impl;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.newnormallist.newsservice.recommendation.service.WeightSelector;
import com.newnormallist.newsservice.recommendation.model.Weights;
import com.newnormallist.newsservice.recommendation.config.RecommendationProperties;

/*
    케이스 1~4 규칙과 신뢰도 보정(fRead, fScrap)을 반영해
    (wDemo,wPref,wRead,wScrap)을 반환.

    Case1(데이터 적음): (0.70, 0.30, 0, 0)
    Case2(조회 충분): (0.40, 0.20, 0.40×fRead, 0) → normalize
    Case3(스크랩 위주): (0.35, 0.25, 0, 0.40×fScrap) → normalize
    Case4(둘 다 충분): (0.20, 0.10, 0.35×fRead, 0.35×fScrap)(demo 하한 0.15) → normalize
*/
@Service
@RequiredArgsConstructor
public class WeightSelectorImpl implements WeightSelector {

    private final RecommendationProperties properties;
    private static final double NORMALIZATION_FACTOR = 30.0; // fRead, fScrap 계산용

    @Override
    public Weights choose(int readCount, int scrapCount) {
        
        // Case 1: 조회 < threshold & 스크랩 < threshold (데이터 적음)
        if (readCount < properties.getReadThreshold() && scrapCount < properties.getScrapThreshold()) {
            return new Weights(
                properties.getCase1Demo(), 
                properties.getCase1Pref(), 
                properties.getCase1Read(), 
                properties.getCase1Scrap()
            );
        }
        
        // Case 2: 조회 >= threshold & 스크랩 < threshold (조회 충분)
        if (readCount >= properties.getReadThreshold() && scrapCount < properties.getScrapThreshold()) {
            double fRead = Math.min(1.0, readCount / NORMALIZATION_FACTOR);
            Weights weights = new Weights(
                properties.getCase2Demo(),
                properties.getCase2Pref(),
                properties.getCase2Read() * fRead,
                properties.getCase2Scrap()
            );
            weights.normalize();
            return weights;
        }
        
        // Case 3: 스크랩 >= threshold & 조회 < threshold (스크랩 위주)
        if (scrapCount >= properties.getScrapThreshold() && readCount < properties.getReadThreshold()) {
            double fScrap = Math.min(1.0, scrapCount / NORMALIZATION_FACTOR);
            Weights weights = new Weights(
                properties.getCase3Demo(),
                properties.getCase3Pref(),
                properties.getCase3Read(),
                properties.getCase3Scrap() * fScrap
            );
            weights.normalize();
            return weights;
        }
        
        // Case 4: 조회 >= threshold & 스크랩 >= threshold (둘 다 충분)
        double fRead = Math.min(1.0, readCount / NORMALIZATION_FACTOR);
        double fScrap = Math.min(1.0, scrapCount / NORMALIZATION_FACTOR);
        
        Weights weights = new Weights(
            properties.getCase4Demo(),
            properties.getCase4Pref(),
            properties.getCase4Read() * fRead,
            properties.getCase4Scrap() * fScrap
        );
        weights.normalize();
        
        // demo 하한 0.15 보장
        if (weights.getWDemo() < 0.15) {
            double adjustment = 0.15 - weights.getWDemo();
            weights = new Weights(
                0.15,
                weights.getWPref() - adjustment * 0.5,
                weights.getWRead() - adjustment * 0.25,
                weights.getWScrap() - adjustment * 0.25
            );
            weights.normalize();
        }
        
        return weights;
    }
}
