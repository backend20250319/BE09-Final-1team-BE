package com.newnormallist.newsservice.recommendation.service.impl;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.newnormallist.newsservice.recommendation.service.VectorBuilder;
import com.newnormallist.newsservice.recommendation.service.DemoBaseProvider;
import com.newnormallist.newsservice.recommendation.service.WeightSelector;
import com.newnormallist.newsservice.recommendation.entity.*;
import com.newnormallist.newsservice.recommendation.repository.*;
import com.newnormallist.newsservice.recommendation.util.PrefVectorHelper;
import com.newnormallist.newsservice.recommendation.util.MathUtils;
import com.newnormallist.newsservice.recommendation.config.RecommendationProperties;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/* 사용자 벡터 계산기 구현체.

    한 유저에 대해:
    D(c): DemoBaseProvider에서 (연령/성별별) 베이스 분포 수집
    P(c): UserCategoryRepository → 선택된 카테고리 균등분포(1/k)
    R(c): UserReadHistoryRepository → 최근 7일 감쇠 가중합으로 비율화
    S(c): NewsScrapRepository → 최근 30일 감쇠 가중합으로 비율화
    WeightSelector.choose(readCount, scrapCount)로 케이스별 가중치 선택
    최종식 Score(c)=Norm(wD·D + wP·P + wR·R + wS·S)로 9개 값 계산
    UserPrefVector 9행으로 만들어 반환 
*/

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorBuilderImpl implements VectorBuilder {

    private final DemoBaseProvider demoBaseProvider;
    private final WeightSelector weightSelector;
    private final RecommendationProperties properties;
    
    private final UserReadHistoryRepository userReadHistoryRepository;
    private final NewsScraperRepository newsScrapRepository;
    private final PrefVectorHelper prefVectorHelper;

    @Override
    public List<UserPrefVector> recomputeForUser(UserEntity userEntity) {
        
        // 1. 사용자 연령/성별 정보 추출
        AgeBucket ageBucket = calculateAgeBucket(userEntity.getBirthYear());
        
        // 2. D(c) - 인구통계학적 기본 분포
        Map<RecommendationCategory, Double> D = demoBaseProvider.getBase(ageBucket, userEntity.getGender());
        
        // 3. P(c) - 사용자 선호 카테고리 분포
        Map<RecommendationCategory, Double> P = prefVectorHelper.buildP(userEntity.getId());
        
        // 4. R(c) - 최근 7일 조회 기록
        Map<RecommendationCategory, Double> R = buildReadVector(userEntity.getId());
        
        // 5. S(c) - 최근 30일 스크랩 기록
        Map<RecommendationCategory, Double> S = buildScrapVector(userEntity.getId());
        
        // 6. 가중치 선택
        int readCount = (int) R.values().stream().mapToDouble(Double::doubleValue).sum();
        int scrapCount = (int) S.values().stream().mapToDouble(Double::doubleValue).sum();
        var weights = weightSelector.choose(readCount, scrapCount);
        
        // 7. 최종 점수 계산 및 UserPrefVector 생성
        List<UserPrefVector> vectors = new ArrayList<>();
        for (RecommendationCategory category : RecommendationCategory.values()) {
            double score = weights.getWDemo() * D.getOrDefault(category, 0.0) +
                         weights.getWPref() * P.getOrDefault(category, 0.0) +
                         weights.getWRead() * R.getOrDefault(category, 0.0) +
                         weights.getWScrap() * S.getOrDefault(category, 0.0);
            
            vectors.add(UserPrefVector.builder()
                .userId(userEntity.getId())
                .category(category)
                .score(score)
                .wDemo(weights.getWDemo())
                .wPref(weights.getWPref())
                .wRead(weights.getWRead())
                .wScrap(weights.getWScrap())
                .build());
        }
        
        // 8. 정규화 (합이 1이 되도록)
        double totalScore = vectors.stream().mapToDouble(UserPrefVector::getScore).sum();
        if (totalScore > 0) {
            vectors.forEach(vector -> vector.setScore(vector.getScore() / totalScore));
        }
        
        return vectors;
    }
    
    private AgeBucket calculateAgeBucket(Integer birthYear) {
        int currentYear = LocalDateTime.now().getYear();
        int age = currentYear - birthYear;
        
        if (age < 30) return AgeBucket.AGE_20s;
        if (age < 40) return AgeBucket.AGE_30s;
        if (age < 50) return AgeBucket.AGE_40s;
        if (age < 60) return AgeBucket.AGE_50s;
        return AgeBucket.AGE_60s_PLUS;
    }
    
    private Map<RecommendationCategory, Double> buildReadVector(Long userId) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        
        List<UserReadHistory> readHistories = userReadHistoryRepository.findByUserId(userId)
            .stream()
            .filter(history -> history.getCreatedAt().isAfter(sevenDaysAgo))
            .collect(Collectors.toList());
        
        Map<RecommendationCategory, Double> categoryWeights = new HashMap<>();
        double totalWeight = 0.0;
        
        for (UserReadHistory history : readHistories) {
            long daysDiff = java.time.Duration.between(history.getCreatedAt(), LocalDateTime.now()).toDays();
            double weight = MathUtils.dayWeight(daysDiff, properties.getReadHalfLifeDays());
            
            categoryWeights.merge(history.getCategoryName(), weight, Double::sum);
            totalWeight += weight;
        }
        
        // 정규화
        final double finalTotalWeight = totalWeight;
        if (finalTotalWeight > 0) {
            categoryWeights.replaceAll((k, v) -> v / finalTotalWeight);
        }
        
        return categoryWeights;
    }
    
    private Map<RecommendationCategory, Double> buildScrapVector(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        try {
            // 먼저 JPA 방식 시도
            List<NewsScraper> scraps = newsScrapRepository.findRecentScrapsByUserId(userId, thirtyDaysAgo);
            return calculateScrapWeights(scraps);
        } catch (Exception e) {
            try {
                // 날짜 파싱 오류 시 Native Query 사용
                String sinceStr = thirtyDaysAgo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                List<Object[]> scrapData = newsScrapRepository.findRecentScrapsByUserIdNative(userId, sinceStr);
                return calculateScrapWeightsFromNative(scrapData);
            } catch (Exception e2) {
                // 스크랩 저장소가 없거나 모든 방법이 실패한 경우 빈 맵 반환
                log.debug("사용자 {}의 스크랩 데이터가 없습니다. 빈 스크랩 벡터를 사용합니다.", userId);
                return new HashMap<>();
            }
        }
    }
    
    private Map<RecommendationCategory, Double> calculateScrapWeights(List<NewsScraper> scraps) {
        Map<RecommendationCategory, Double> categoryWeights = new HashMap<>();
        double totalWeight = 0.0;
        
        for (NewsScraper scrap : scraps) {
            long daysDiff = java.time.Duration.between(scrap.getCreatedAt(), LocalDateTime.now()).toDays();
            double weight = MathUtils.dayWeight(daysDiff, properties.getScrapHalfLifeDays());
            
            categoryWeights.merge(scrap.getNewsEntity().getCategoryName(), weight, Double::sum);
            totalWeight += weight;
        }
        
        // 정규화
        final double finalTotalWeight = totalWeight;
        if (finalTotalWeight > 0) {
            categoryWeights.replaceAll((k, v) -> v / finalTotalWeight);
        }
        
        return categoryWeights;
    }
    
    private Map<RecommendationCategory, Double> calculateScrapWeightsFromNative(List<Object[]> scrapData) {
        Map<RecommendationCategory, Double> categoryWeights = new HashMap<>();
        double totalWeight = 0.0;
        
        for (Object[] row : scrapData) {
            try {
                // Object[]에서 필요한 데이터 추출 (SELECT ns.created_at, n.category 순서)
                String createdAtStr = row[0].toString(); // created_at 컬럼
                String categoryStr = row[1].toString();  // category 컬럼
                
                // 여러 날짜 형식 시도
                LocalDateTime createdAt;
                try {
                    // 마이크로초 형식 시도
                    createdAt = LocalDateTime.parse(createdAtStr, 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
                } catch (Exception e1) {
                    try {
                        // 밀리초 형식 시도
                        createdAt = LocalDateTime.parse(createdAtStr, 
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                    } catch (Exception e2) {
                        // 기본 형식 시도
                        createdAt = LocalDateTime.parse(createdAtStr, 
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    }
                }
                RecommendationCategory category = RecommendationCategory.valueOf(categoryStr);
                
                long daysDiff = java.time.Duration.between(createdAt, LocalDateTime.now()).toDays();
                double weight = MathUtils.dayWeight(daysDiff, properties.getScrapHalfLifeDays());
                
                categoryWeights.merge(category, weight, Double::sum);
                totalWeight += weight;
            } catch (Exception e) {
                // 개별 레코드 파싱 실패 시 건너뛰기
                continue;
            }
        }
        
        // 정규화
        final double finalTotalWeight = totalWeight;
        if (finalTotalWeight > 0) {
            categoryWeights.replaceAll((k, v) -> v / finalTotalWeight);
        }
        
        return categoryWeights;
    }
}
