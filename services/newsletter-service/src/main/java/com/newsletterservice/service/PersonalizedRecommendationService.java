package com.newsletterservice.service;

import com.newsletterservice.client.UserServiceClient;
import com.newsletterservice.client.NewsServiceClient;
import com.newsletterservice.client.dto.NewsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalizedRecommendationService {

    private final UserServiceClient userServiceClient;
    private final NewsServiceClient newsServiceClient;

    /**
     * 사용자별 개인화된 뉴스 추천
     */
    public List<NewsResponse> getPersonalizedNews(Long userId, int limit) {
        log.info("사용자 {}의 개인화 뉴스 추천 시작", userId);

        try {
            // 1. 사용자 관심사 조회
            com.newsletterservice.common.ApiResponse<com.newsletterservice.client.dto.UserInterestResponse> interestResponse = 
                userServiceClient.getUserInterests(userId);
            com.newsletterservice.client.dto.UserInterestResponse userInterests = 
                interestResponse != null ? interestResponse.getData() : null;
            
            // 2. 사용자 행동 분석 조회
            com.newsletterservice.common.ApiResponse<com.newsletterservice.client.dto.UserBehaviorAnalysis> behaviorResponse = 
                userServiceClient.getUserBehaviorAnalysis(userId);
            com.newsletterservice.client.dto.UserBehaviorAnalysis userBehavior = 
                behaviorResponse != null ? behaviorResponse.getData() : null;
            
            // 3. 이미 읽은 뉴스 ID 조회 (중복 방지)
            com.newsletterservice.common.ApiResponse<List<Long>> readNewsResponse = userServiceClient.getReadNewsIds(userId, 0, 100);
            Set<Long> readNewsIds = new HashSet<>(readNewsResponse != null && readNewsResponse.getData() != null ? readNewsResponse.getData() : Collections.emptyList());
            
            // 4. 관심사가 있는 경우
            if (userInterests != null && !userInterests.getTopInterests().isEmpty()) {
                return getNewsBasedOnInterests(userInterests, userBehavior, readNewsIds, limit);
            }
            
            // 5. 관심사가 없는 경우 - 행동 기반 추천
            else if (userBehavior != null && userBehavior.getTotalReadCount() > 0) {
                return getNewsBasedOnBehavior(userBehavior, readNewsIds, limit);
            }
            
            // 6. 신규 사용자 - 트렌딩 뉴스
            else {
                return getTrendingNewsForNewUser(readNewsIds, limit);
            }
            
        } catch (Exception e) {
            log.error("개인화 뉴스 추천 실패: userId={}", userId, e);
            return getTrendingNewsForNewUser(new HashSet<>(), limit); // 폴백
        }
    }

    /**
     * 관심사 기반 뉴스 추천
     */
    private List<NewsResponse> getNewsBasedOnInterests(
            com.newsletterservice.client.dto.UserInterestResponse userInterests, 
            com.newsletterservice.client.dto.UserBehaviorAnalysis userBehavior, 
            Set<Long> readNewsIds,
            int limit) {
        
        List<NewsResponse> recommendedNews = new ArrayList<>();
        
        // 카테고리별 선호도 점수 가져오기
        Map<String, Double> categoryPreferences = userBehavior != null ? 
            userBehavior.getCategoryPreferences() : new HashMap<>();
        
        // 카테고리별 뉴스 수 배분 계산
        Map<String, Integer> categoryLimits = calculateCategoryLimits(
            userInterests.getTopInterests(), categoryPreferences, limit);
        
        for (Map.Entry<String, Integer> entry : categoryLimits.entrySet()) {
            String category = entry.getKey();
            int categoryLimit = entry.getValue();
            
            if (categoryLimit > 0) {
                try {
                    // 카테고리별 뉴스 조회
                    Page<NewsResponse> categoryNewsPage = newsServiceClient.getNewsByCategory(
                        category, 0, categoryLimit * 2); // 여분 확보
                    
                    List<NewsResponse> categoryNews = categoryNewsPage.getContent().stream()
                        .filter(news -> !readNewsIds.contains(news.getNewsId())) // 중복 제거
                        .map(news -> {
                            double score = calculatePersonalizationScore(news, userInterests, userBehavior);
                            // NewsResponse에 personalizationScore 필드가 있다고 가정
                            // news.setPersonalizationScore(score);
                            return news;
                        })
                        .sorted((n1, n2) -> {
                            double score1 = calculatePersonalizationScore(n1, userInterests, userBehavior);
                            double score2 = calculatePersonalizationScore(n2, userInterests, userBehavior);
                            return Double.compare(score2, score1);
                        })
                        .limit(categoryLimit)
                        .collect(Collectors.toList());
                    
                    recommendedNews.addAll(categoryNews);
                    
                } catch (Exception e) {
                    log.warn("카테고리 {} 뉴스 조회 실패", category, e);
                }
            }
        }
        
        // 다양성 확보를 위한 셔플
        Collections.shuffle(recommendedNews);
        
        return recommendedNews.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * 행동 기반 뉴스 추천 (관심사 미설정 사용자)
     */
    private List<NewsResponse> getNewsBasedOnBehavior(
            com.newsletterservice.client.dto.UserBehaviorAnalysis userBehavior, 
            Set<Long> readNewsIds,
            int limit) {
        
        List<NewsResponse> behaviorBasedNews = new ArrayList<>();
        Map<String, Long> categoryReadCounts = userBehavior.getCategoryReadCounts();
        
        // 읽기 횟수가 많은 카테고리 순으로 정렬
        List<Map.Entry<String, Long>> sortedCategories = categoryReadCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .collect(Collectors.toList());
        
        long totalReads = userBehavior.getTotalReadCount();
        
        for (Map.Entry<String, Long> entry : sortedCategories) {
            String category = entry.getKey();
            double ratio = (double) entry.getValue() / totalReads;
            int categoryLimit = Math.max(1, (int) Math.ceil(limit * ratio));
            
            try {
                Page<NewsResponse> categoryNewsPage = newsServiceClient.getNewsByCategory(
                    category, 0, categoryLimit * 2);
                
                List<NewsResponse> categoryNews = categoryNewsPage.getContent().stream()
                    .filter(news -> !readNewsIds.contains(news.getNewsId()))
                    .limit(categoryLimit)
                    .collect(Collectors.toList());
                
                behaviorBasedNews.addAll(categoryNews);
                
            } catch (Exception e) {
                log.warn("행동 기반 카테고리 {} 뉴스 조회 실패", category, e);
            }
        }
        
        // 인기도 + 최신성 기준 정렬
        return behaviorBasedNews.stream()
            .sorted((n1, n2) -> {
                double score1 = calculatePopularityScore(n1) + calculateRecencyScore(n1);
                double score2 = calculatePopularityScore(n2) + calculateRecencyScore(n2);
                return Double.compare(score2, score1);
            })
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 신규 사용자용 트렌딩 뉴스
     */
    private List<NewsResponse> getTrendingNewsForNewUser(Set<Long> readNewsIds, int limit) {
        try {
            // 트렌딩 뉴스 조회
            Page<NewsResponse> trendingPage = newsServiceClient.getTrendingNews(24, limit * 2);
            
            return trendingPage.getContent().stream()
                .filter(news -> !readNewsIds.contains(news.getNewsId()))
                .limit(limit)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("트렌딩 뉴스 조회 실패", e);
            
            // 폴백: 인기 뉴스
            try {
                Page<NewsResponse> popularPage = newsServiceClient.getPopularNews(limit);
                return popularPage.getContent().stream()
                    .filter(news -> !readNewsIds.contains(news.getNewsId()))
                    .limit(limit)
                    .collect(Collectors.toList());
            } catch (Exception fallbackException) {
                log.error("인기 뉴스 조회도 실패", fallbackException);
                return Collections.emptyList();
            }
        }
    }

    /**
     * 카테고리별 뉴스 수 배분 계산
     */
    private Map<String, Integer> calculateCategoryLimits(
            List<String> topInterests, 
            Map<String, Double> categoryPreferences, 
            int totalLimit) {
        
        Map<String, Integer> categoryLimits = new HashMap<>();
        
        if (topInterests.isEmpty()) {
            return categoryLimits;
        }
        
        // 상위 3개 관심사에 대해 비율 계산
        double totalScore = 0.0;
        Map<String, Double> scores = new HashMap<>();
        
        for (int i = 0; i < Math.min(3, topInterests.size()); i++) {
            String category = topInterests.get(i);
            double score = categoryPreferences.getOrDefault(category, 0.5); // 기본값 0.5
            
            // 순위에 따른 가중치 적용 (1위: 1.0, 2위: 0.7, 3위: 0.5)
            double rankWeight = 1.0 - (i * 0.3);
            score *= rankWeight;
            
            scores.put(category, score);
            totalScore += score;
        }
        
        // 각 카테고리별 뉴스 수 계산
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            String category = entry.getKey();
            double score = entry.getValue();
            int limit = totalScore > 0 ? 
                (int) Math.ceil((score / totalScore) * totalLimit) : 
                totalLimit / scores.size();
            
            categoryLimits.put(category, Math.max(1, limit)); // 최소 1개
        }
        
        return categoryLimits;
    }

    /**
     * 개인화 점수 계산
     */
    private double calculatePersonalizationScore(
            NewsResponse news, 
            com.newsletterservice.client.dto.UserInterestResponse userInterests, 
            com.newsletterservice.client.dto.UserBehaviorAnalysis userBehavior) {
        
        double score = 0.0;
        
        // 1. 카테고리 선호도 점수 (40%)
        if (userInterests != null && userInterests.getTopInterests().contains(news.getCategoryName())) {
            int rank = userInterests.getTopInterests().indexOf(news.getCategoryName());
            score += (0.4 * (1.0 - rank * 0.2)); // 순위에 따른 차등 점수
        }
        
        // 2. 인기도 점수 (30%)
        score += calculatePopularityScore(news) * 0.3;
        
        // 3. 최신성 점수 (20%)
        score += calculateRecencyScore(news) * 0.2;
        
        // 4. 행동 유사성 점수 (10%)
        if (userBehavior != null && userBehavior.getCategoryPreferences() != null) {
            Double categoryPreference = userBehavior.getCategoryPreferences()
                .get(news.getCategoryName());
            if (categoryPreference != null) {
                score += categoryPreference * 0.1;
            }
        }
        
        return score;
    }

    /**
     * 인기도 점수 계산 (0~1)
     */
    private double calculatePopularityScore(NewsResponse news) {
        // 뉴스 조회수 기반 (실제로는 news-service에서 제공되어야 함)
        // 현재는 간단한 더미 로직
        return 0.5; // TODO: 실제 인기도 지표 연동
    }

    /**
     * 최신성 점수 계산 (0~1)
     */
    private double calculateRecencyScore(NewsResponse news) {
        if (news.getPublishedAt() == null) {
            return 0.5;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime publishedAt = news.getPublishedAt();
        
        long hoursOld = ChronoUnit.HOURS.between(publishedAt, now);
        
        // 24시간 이내: 1.0, 그 이후 지수적 감소
        if (hoursOld <= 24) {
            return 1.0 - (hoursOld / 24.0) * 0.3; // 24시간 후 0.7
        } else {
            return Math.max(0.1, 0.7 * Math.exp(-(hoursOld - 24) / 72.0)); // 3일 반감기
        }
    }

    /**
     * 사용자별 최적 뉴스레터 빈도 결정
     */
    public String getOptimalNewsletterFrequency(Long userId) {
        try {
            ResponseEntity<String> response = userServiceClient.getOptimalNewsletterFrequency(userId);
            return response.getBody() != null ? response.getBody() : "WEEKLY";
            
        } catch (Exception e) {
            log.error("최적 뉴스레터 빈도 계산 실패: userId={}", userId, e);
            return "WEEKLY"; // 폴백
        }
    }

    /**
     * 개인화 뉴스레터 콘텐츠 생성
     */
    public PersonalizedNewsletterContent generatePersonalizedContent(Long userId) {
        try {
            // 1. 개인화된 뉴스 조회
            List<NewsResponse> personalizedNews = getPersonalizedNews(userId, 10);
            
            // 2. 사용자 관심사 조회
            com.newsletterservice.common.ApiResponse<com.newsletterservice.client.dto.UserInterestResponse> interestResponse = 
                userServiceClient.getUserInterests(userId);
            com.newsletterservice.client.dto.UserInterestResponse interests = 
                interestResponse != null ? interestResponse.getData() : null;
            
            // 3. 최적 빈도 계산
            String optimalFrequency = getOptimalNewsletterFrequency(userId);
            
            return PersonalizedNewsletterContent.builder()
                .userId(userId)
                .personalizedNews(personalizedNews)
                .userInterests(interests != null ? interests.getTopInterests() : Collections.emptyList())
                .recommendedFrequency(optimalFrequency)
                .generatedAt(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("개인화 뉴스레터 콘텐츠 생성 실패: userId={}", userId, e);
            throw new RuntimeException("개인화 콘텐츠 생성 실패", e);
        }
    }

    // 개인화 뉴스레터 콘텐츠 DTO
    public static class PersonalizedNewsletterContent {
        private Long userId;
        private List<NewsResponse> personalizedNews;
        private List<String> userInterests;
        private String recommendedFrequency;
        private LocalDateTime generatedAt;

        public static PersonalizedNewsletterContentBuilder builder() {
            return new PersonalizedNewsletterContentBuilder();
        }

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public List<NewsResponse> getPersonalizedNews() { return personalizedNews; }
        public void setPersonalizedNews(List<NewsResponse> personalizedNews) { this.personalizedNews = personalizedNews; }
        public List<String> getUserInterests() { return userInterests; }
        public void setUserInterests(List<String> userInterests) { this.userInterests = userInterests; }
        public String getRecommendedFrequency() { return recommendedFrequency; }
        public void setRecommendedFrequency(String recommendedFrequency) { this.recommendedFrequency = recommendedFrequency; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

        public static class PersonalizedNewsletterContentBuilder {
            private Long userId;
            private List<NewsResponse> personalizedNews;
            private List<String> userInterests;
            private String recommendedFrequency;
            private LocalDateTime generatedAt;

            public PersonalizedNewsletterContentBuilder userId(Long userId) {
                this.userId = userId;
                return this;
            }

            public PersonalizedNewsletterContentBuilder personalizedNews(List<NewsResponse> personalizedNews) {
                this.personalizedNews = personalizedNews;
                return this;
            }

            public PersonalizedNewsletterContentBuilder userInterests(List<String> userInterests) {
                this.userInterests = userInterests;
                return this;
            }

            public PersonalizedNewsletterContentBuilder recommendedFrequency(String recommendedFrequency) {
                this.recommendedFrequency = recommendedFrequency;
                return this;
            }

            public PersonalizedNewsletterContentBuilder generatedAt(LocalDateTime generatedAt) {
                this.generatedAt = generatedAt;
                return this;
            }

            public PersonalizedNewsletterContent build() {
                PersonalizedNewsletterContent content = new PersonalizedNewsletterContent();
                content.setUserId(this.userId);
                content.setPersonalizedNews(this.personalizedNews);
                content.setUserInterests(this.userInterests);
                content.setRecommendedFrequency(this.recommendedFrequency);
                content.setGeneratedAt(this.generatedAt);
                return content;
            }
        }
    }
}
