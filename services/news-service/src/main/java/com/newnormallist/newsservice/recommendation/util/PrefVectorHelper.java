package com.newnormallist.newsservice.recommendation.util;

import com.newnormallist.newsservice.recommendation.entity.RecommendationCategory;
import com.newnormallist.newsservice.recommendation.repository.UserCategoryRepository;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.*;

@Component
@RequiredArgsConstructor
public class PrefVectorHelper {

    private final UserCategoryRepository userCategoryRepository;

    /**
     * 선호 카테고리 분포 P(c) 생성: 선택된 카테고리만 1/k, 나머지는 0
     */
    public Map<RecommendationCategory, Double> buildP(Long userId) {
        List<RecommendationCategory> cats = userCategoryRepository.findCategoriesByUserId(userId);
        Map<RecommendationCategory, Double> P = new EnumMap<>(RecommendationCategory.class);
        int k = cats.size();
        if (k == 0) return P; // 빈 맵(모두 0) 반환
        double w = 1.0 / k;
        for (RecommendationCategory c : cats) P.put(c, w);
        return P;
    }
}
