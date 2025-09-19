package com.newnormallist.newsservice.recommendation.util;

import java.util.Map;

import com.newnormallist.newsservice.recommendation.entity.RecommendationCategory;

// 감쇠 가중치 : exp(- ln2/halfLifeDays × deltaDays)
// 정규화 : 9개 값 합 = 1 보장

public class MathUtils {
    public static double dayWeight(long deltaDays, double halfLifeDays) {
        double lambda = Math.log(2.0) / halfLifeDays;
        return Math.exp(-lambda * deltaDays);
    }
    public static void normalize(Map<RecommendationCategory, Double> m) {
        double sum = m.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum <= 0) return;
        for (RecommendationCategory c: m.keySet()) m.put(c, m.get(c)/sum);
    }
}
