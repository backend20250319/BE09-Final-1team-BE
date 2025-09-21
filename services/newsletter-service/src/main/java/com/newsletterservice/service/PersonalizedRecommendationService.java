package com.newsletterservice.service;

import com.newsletterservice.client.UserServiceClient;
import com.newsletterservice.client.NewsServiceClient;
import com.newsletterservice.client.dto.NewsResponse;
import com.newsletterservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
                    Page<NewsResponse> categoryNewsResponse = newsServiceClient.getNewsByCategory(
                        category, 0, categoryLimit * 2); // 여분 확보
                    Page<NewsResponse> categoryNewsPage = categoryNewsResponse;
                    
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
                Page<NewsResponse> categoryNewsResponse = newsServiceClient.getNewsByCategory(
                    category, 0, categoryLimit * 2);
                Page<NewsResponse> categoryNewsPage = categoryNewsResponse;
                
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
            ApiResponse<Page<NewsResponse>> trendingResponse = newsServiceClient.getTrendingNews(24, limit * 2);
            Page<NewsResponse> trendingPage = trendingResponse.getData();
            
            return trendingPage.getContent().stream()
                .filter(news -> !readNewsIds.contains(news.getNewsId()))
                .limit(limit)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("트렌딩 뉴스 조회 실패", e);
            
            // 폴백: 인기 뉴스
            try {
                ApiResponse<Page<NewsResponse>> popularResponse = newsServiceClient.getPopularNews(limit);
                Page<NewsResponse> popularPage = popularResponse.getData();
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
        
        // 2. 인기도 점수 (30%) - 조회수와 공유수를 기반으로 계산
        double popularityScore = calculatePopularityScore(news);
        score += popularityScore * 0.3;
        
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
     * 사용자별 최적 뉴스레터 빈도 결정
     */
    public String getOptimalNewsletterFrequency(Long userId) {
        try {
            ApiResponse<String> response = userServiceClient.getOptimalNewsletterFrequency(userId);
            return response.getData() != null ? response.getData() : "WEEKLY";
            
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

    /**
     * 뉴스의 인기도 점수 계산
     * 
     * @param news 뉴스 정보
     * @return 인기도 점수 (0.0 ~ 1.0)
     */
    private double calculatePopularityScore(NewsResponse news) {
        if (news == null) {
            return 0.0;
        }

        // 조회수와 공유수를 기반으로 인기도 점수 계산
        long viewCount = news.getViewCount() != null ? news.getViewCount().longValue() : 0;
        long shareCount = news.getShareCount() != null ? news.getShareCount() : 0;
        
        // 로그 스케일링을 사용하여 점수 정규화
        double viewScore = Math.log10(Math.max(viewCount, 1)) / 6.0; // 최대 1M 조회수 기준
        double shareScore = Math.log10(Math.max(shareCount, 1)) / 4.0; // 최대 10K 공유수 기준
        
        // 가중 평균 (조회수 70%, 공유수 30%)
        double popularityScore = (viewScore * 0.7) + (shareScore * 0.3);
        
        return Math.min(popularityScore, 1.0); // 최대 1.0으로 제한
    }

    /**
     * 뉴스의 최신성 점수 계산
     * 
     * @param news 뉴스 정보
     * @return 최신성 점수 (0.0 ~ 1.0)
     */
    private double calculateRecencyScore(NewsResponse news) {
        if (news == null || news.getPublishedAt() == null) {
            return 0.0;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime publishedAt = parsePublishedAt(news.getPublishedAt());
        
        // 발행 시간으로부터 경과된 시간 (시간 단위)
        long hoursElapsed = ChronoUnit.HOURS.between(publishedAt, now);
        
        // 시간이 지날수록 점수 감소 (지수적 감소)
        // 1시간: 1.0, 6시간: 0.5, 24시간: 0.1, 72시간: 0.01
        double recencyScore = Math.exp(-hoursElapsed / 8.0); // 반감기 8시간
        
        return Math.min(recencyScore, 1.0); // 최대 1.0으로 제한
    }
    
    /**
     * String을 LocalDateTime으로 변환하는 유틸리티 메서드
     */
    private LocalDateTime parsePublishedAt(String publishedAtStr) {
        if (publishedAtStr == null || publishedAtStr.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        
        try {
            // ISO 8601 형식 시도
            return LocalDateTime.parse(publishedAtStr);
        } catch (DateTimeParseException e1) {
            try {
                // 다른 일반적인 형식들 시도
                DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                };
                
                for (DateTimeFormatter formatter : formatters) {
                    try {
                        return LocalDateTime.parse(publishedAtStr, formatter);
                    } catch (DateTimeParseException ignored) {
                        // 다음 포맷 시도
                    }
                }
            } catch (Exception e2) {
                log.warn("날짜 파싱 실패: {}, 현재 시간 사용", publishedAtStr);
            }
        }
        
        return LocalDateTime.now();
    }

}
