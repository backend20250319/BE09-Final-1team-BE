package com.newsletterservice.service;

import com.newsletterservice.client.NewsServiceClient;
import com.newsletterservice.client.UserServiceClient;
import com.newsletterservice.client.dto.NewsResponse;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.dto.FeedTemplate;
import com.newsletterservice.dto.NewsletterContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 피드 템플릿 서비스
 * 뉴스 데이터를 카카오톡 피드 템플릿 형태로 변환하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedTemplateService {
    
    private final NewsServiceClient newsServiceClient;
    private final UserServiceClient userServiceClient;
    
    /**
     * 사용자별 개인화된 피드 템플릿 생성
     */
    public FeedTemplate createPersonalizedFeedTemplate(Long userId, FeedTemplate.FeedType feedType) {
        log.info("개인화된 피드 템플릿 생성 시작: userId={}, feedType={}", userId, feedType);
        
        try {
            // 1. 사용자 관심사 조회
            ApiResponse<com.newsletterservice.client.dto.UserInterestResponse> interestResponse = null;
            com.newsletterservice.client.dto.UserInterestResponse userInterests = null;
            
            try {
                interestResponse = userServiceClient.getUserInterests(userId);
                userInterests = interestResponse != null ? interestResponse.getData() : null;
                log.info("사용자 관심사 조회 성공: userId={}", userId);
            } catch (Exception e) {
                log.warn("사용자 관심사 조회 실패: userId={}, 기본 템플릿 사용", userId, e);
            }
            
            // 2. 개인화된 뉴스 조회
            List<NewsResponse> personalizedNews = getPersonalizedNews(userId, userInterests);
            
            // 3. 뉴스 데이터를 뉴스레터 아티클로 변환
            List<NewsletterContent.Article> articles = convertToArticles(personalizedNews);
            
            // 4. 피드 템플릿 생성
            FeedTemplate feedTemplate = FeedTemplate.createFromNews(articles, feedType);
            
            log.info("개인화된 피드 템플릿 생성 완료: userId={}, articleCount={}", userId, articles.size());
            return feedTemplate;
            
        } catch (Exception e) {
            log.error("개인화된 피드 템플릿 생성 실패: userId={}", userId, e);
            return createDefaultFeedTemplate(feedType);
        }
    }
    
    /**
     * 카테고리별 피드 템플릿 생성
     */
    public FeedTemplate createCategoryFeedTemplate(String category, FeedTemplate.FeedType feedType) {
        log.info("카테고리별 피드 템플릿 생성 시작: category={}, feedType={}", category, feedType);
        
        try {
            // 1. 카테고리별 뉴스 조회
            String englishCategory = convertCategoryToEnglish(category);
            List<NewsResponse> categoryNews = new ArrayList<>();
            
            try {
                Page<NewsResponse> newsResponse = 
                        newsServiceClient.getNewsByCategory(englishCategory, 0, 10);
                categoryNews = newsResponse != null ? newsResponse.getContent() : new ArrayList<>();
                log.info("카테고리별 뉴스 조회 성공: category={}, count={}", category, categoryNews.size());
            } catch (Exception e) {
                log.warn("카테고리별 뉴스 조회 실패: category={}, 기본 템플릿 사용", category, e);
            }
            
            // 2. 뉴스 데이터를 뉴스레터 아티클로 변환
            List<NewsletterContent.Article> articles = convertToArticles(categoryNews);
            
            // 3. 피드 템플릿 생성
            FeedTemplate feedTemplate = FeedTemplate.createFromNews(articles, feedType);
            
            log.info("카테고리별 피드 템플릿 생성 완료: category={}, articleCount={}", category, articles.size());
            return feedTemplate;
            
        } catch (Exception e) {
            log.error("카테고리별 피드 템플릿 생성 실패: category={}", category, e);
            return createDefaultFeedTemplate(feedType);
        }
    }
    
    /**
     * 트렌딩 뉴스 피드 템플릿 생성
     */
    public FeedTemplate createTrendingFeedTemplate(FeedTemplate.FeedType feedType) {
        log.info("트렌딩 뉴스 피드 템플릿 생성 시작: feedType={}", feedType);
        
        try {
            // 1. 트렌딩 뉴스 조회
            List<NewsResponse> trendingNewsList = new ArrayList<>();
            
            try {
                ApiResponse<Page<NewsResponse>> trendingResponse = 
                        newsServiceClient.getTrendingNews(24, 10);
                Page<NewsResponse> trendingNews = trendingResponse != null ? trendingResponse.getData() : null;
                trendingNewsList = trendingNews != null ? trendingNews.getContent() : new ArrayList<>();
                log.info("트렌딩 뉴스 조회 성공: count={}", trendingNewsList.size());
            } catch (Exception e) {
                log.warn("트렌딩 뉴스 조회 실패, 기본 템플릿 사용", e);
            }
            
            // 2. 뉴스 데이터를 뉴스레터 아티클로 변환
            List<NewsletterContent.Article> articles = convertToArticles(trendingNewsList);
            
            // 3. 피드 템플릿 생성
            FeedTemplate feedTemplate = FeedTemplate.createFromNews(articles, feedType);
            
            log.info("트렌딩 뉴스 피드 템플릿 생성 완료: articleCount={}", articles.size());
            return feedTemplate;
            
        } catch (Exception e) {
            log.error("트렌딩 뉴스 피드 템플릿 생성 실패", e);
            return createDefaultFeedTemplate(feedType);
        }
    }
    
    /**
     * 최신 뉴스 피드 템플릿 생성
     */
    public FeedTemplate createLatestFeedTemplate(FeedTemplate.FeedType feedType) {
        log.info("최신 뉴스 피드 템플릿 생성 시작: feedType={}", feedType);
        
        try {
            // 1. 최신 뉴스 조회
            ApiResponse<Page<NewsResponse>> latestResponse = 
                    newsServiceClient.getLatestNews(null, 10);
            List<NewsResponse> latestNewsList = latestResponse != null && latestResponse.isSuccess() && latestResponse.getData() != null ? 
                    latestResponse.getData().getContent() : new ArrayList<>();
            
            // 2. 뉴스 데이터를 뉴스레터 아티클로 변환
            List<NewsletterContent.Article> articles = convertToArticles(latestNewsList);
            
            // 3. 피드 템플릿 생성
            FeedTemplate feedTemplate = FeedTemplate.createFromNews(articles, feedType);
            
            log.info("최신 뉴스 피드 템플릿 생성 완료: articleCount={}", articles.size());
            return feedTemplate;
            
        } catch (Exception e) {
            log.error("최신 뉴스 피드 템플릿 생성 실패", e);
            return createDefaultFeedTemplate(feedType);
        }
    }
    
    /**
     * 개인화된 뉴스 조회
     */
    private List<NewsResponse> getPersonalizedNews(Long userId, 
            com.newsletterservice.client.dto.UserInterestResponse userInterests) {
        List<NewsResponse> personalizedNews = new ArrayList<>();
        
        if (userInterests != null && userInterests.getTopCategories() != null && 
            !userInterests.getTopCategories().isEmpty()) {
            
            // 관심사가 있는 경우 - 여러 관심 카테고리에서 뉴스 수집
            List<String> topCategories = userInterests.getTopCategories();
            log.info("사용자 관심사 기반 뉴스 수집: userId={}, categories={}", userId, topCategories);
            
            // 각 관심 카테고리에서 뉴스 수집 (최대 3개 카테고리)
            for (int i = 0; i < Math.min(topCategories.size(), 3); i++) {
                String category = topCategories.get(i);
                String englishCategory = convertCategoryToEnglish(category);
                
                try {
                    // 카테고리별로 3-4개씩 뉴스 수집
                    int newsPerCategory = i == 0 ? 4 : 3; // 첫 번째 관심사는 더 많이
                    Page<NewsResponse> newsResponse = 
                            newsServiceClient.getNewsByCategory(englishCategory, 0, newsPerCategory);
                    List<NewsResponse> categoryNews = newsResponse != null ? newsResponse.getContent() : new ArrayList<>();
                    
                    personalizedNews.addAll(categoryNews);
                    log.info("관심사 기반 뉴스 조회 성공: category={}, count={}", category, categoryNews.size());
                } catch (Exception e) {
                    log.warn("관심사 기반 뉴스 조회 실패: category={}, error={}", category, e.getMessage());
                }
            }
        }
        
        // 관심사가 없거나 조회 실패한 경우 - 다양한 소스에서 뉴스 수집
        if (personalizedNews.isEmpty()) {
            log.info("관심사가 없거나 조회 실패, 다양한 소스에서 뉴스 수집: userId={}", userId);
            personalizedNews = fetchDiverseNews();
        }
        
        // 최대 10개로 제한하고 중복 제거
        personalizedNews = personalizedNews.stream()
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
        
        log.info("개인화 뉴스 수집 완료: userId={}, totalCount={}", userId, personalizedNews.size());
        return personalizedNews;
    }
    
    /**
     * 다양한 소스에서 뉴스 수집 (관심사가 없을 때 사용)
     */
    private List<NewsResponse> fetchDiverseNews() {
        List<NewsResponse> diverseNews = new ArrayList<>();
        
        try {
            // 1. 트렌딩 뉴스 (4개)
            ApiResponse<Page<NewsResponse>> trendingResponse = 
                    newsServiceClient.getTrendingNews(24, 4);
            Page<NewsResponse> trendingNews = trendingResponse != null ? trendingResponse.getData() : null;
            if (trendingNews != null) {
                diverseNews.addAll(trendingNews.getContent());
            }
            
            // 2. 인기 뉴스 (3개)
            ApiResponse<Page<NewsResponse>> popularResponse = 
                    newsServiceClient.getPopularNews(3);
            Page<NewsResponse> popularNews = popularResponse != null ? popularResponse.getData() : null;
            if (popularNews != null) {
                diverseNews.addAll(popularNews.getContent());
            }
            
            // 3. 최신 뉴스 (3개)
            ApiResponse<Page<NewsResponse>> latestResponse = 
                    newsServiceClient.getLatestNews(null, 3);
            if (latestResponse != null && latestResponse.isSuccess() && latestResponse.getData() != null) {
                diverseNews.addAll(latestResponse.getData().getContent());
            }
            
            log.info("다양한 소스에서 뉴스 수집 완료: count={}", diverseNews.size());
            
        } catch (Exception e) {
            log.warn("다양한 소스 뉴스 수집 실패", e);
        }
        
        return diverseNews;
    }
    
    /**
     * NewsResponse를 NewsletterContent.Article로 변환
     */
    private List<NewsletterContent.Article> convertToArticles(List<NewsResponse> newsList) {
        return newsList.stream()
                .map(this::convertToArticle)
                .collect(Collectors.toList());
    }
    
    /**
     * NewsResponse를 NewsletterContent.Article로 변환
     */
    private NewsletterContent.Article convertToArticle(NewsResponse news) {
        // 요약이 없으면 본문에서 추출
        String summary = news.getSummary();
        if (summary == null || summary.trim().isEmpty()) {
            summary = truncateText(news.getContent(), 200);
        }
        
        // 이미지 URL이 없으면 기본 이미지 사용
        String imageUrl = news.getImageUrl();
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            imageUrl = "https://via.placeholder.com/800x400?text=News+Image";
        }
        
        // 카테고리명을 한국어로 변환
        String categoryName = convertCategoryToKorean(news.getCategoryName());
        
        return NewsletterContent.Article.builder()
                .id(news.getNewsId())
                .title(news.getTitle())
                .summary(summary)
                .category(categoryName)
                .url(news.getLink())
                .publishedAt(parsePublishedAt(news.getPublishedAt()))
                .imageUrl(imageUrl)
                .viewCount(news.getViewCount() != null ? news.getViewCount().longValue() : 0L)
                .shareCount(news.getShareCount() != null ? news.getShareCount() : 0L)
                .personalizedScore(calculatePersonalizedScore(news))
                .trendScore(calculateTrendScore(news))
                .isPersonalized(true) // 실제 뉴스 데이터이므로 개인화됨
                .build();
    }
    
    /**
     * 개인화 점수 계산
     */
    private Double calculatePersonalizedScore(NewsResponse news) {
        double score = 0.7; // 기본 개인화 점수
        
        // 조회수가 높을수록 개인화 점수 증가
        if (news.getViewCount() != null && news.getViewCount() > 100) {
            score += 0.1;
        }
        
        // 공유수가 높을수록 개인화 점수 증가
        if (news.getShareCount() != null && news.getShareCount() > 10) {
            score += 0.1;
        }
        
        // 최근 뉴스일수록 개인화 점수 증가
        LocalDateTime publishedAt = parsePublishedAt(news.getPublishedAt());
        if (publishedAt != null) {
            long hoursSincePublished = java.time.Duration.between(
                    publishedAt, 
                    java.time.LocalDateTime.now()
            ).toHours();
            
            if (hoursSincePublished < 24) {
                score += 0.1;
            }
        }
        
        return Math.min(1.0, score);
    }
    
    /**
     * 트렌드 점수 계산
     */
    private Double calculateTrendScore(NewsResponse news) {
        double score = 0.5; // 기본 점수
        
        // 조회수 기반 점수
        if (news.getViewCount() != null && news.getViewCount() > 0) {
            score += Math.min(0.3, news.getViewCount() / 1000.0);
        }
        
        // 공유수 기반 점수
        if (news.getShareCount() != null && news.getShareCount() > 0) {
            score += Math.min(0.2, news.getShareCount() / 100.0);
        }
        
        return Math.min(1.0, score);
    }
    
    /**
     * 기본 피드 템플릿 생성
     */
    private FeedTemplate createDefaultFeedTemplate(FeedTemplate.FeedType feedType) {
        List<NewsletterContent.Article> defaultArticles = createDefaultArticles();
        return FeedTemplate.createFromNews(defaultArticles, feedType);
    }
    
    /**
     * 기본 아티클 생성
     */
    private List<NewsletterContent.Article> createDefaultArticles() {
        List<NewsletterContent.Article> articles = new ArrayList<>();
        
        NewsletterContent.Article article1 = NewsletterContent.Article.builder()
                .id(1L)
                .title("오늘의 주요 뉴스")
                .summary("현재 뉴스를 불러올 수 없어 기본 뉴스를 표시합니다.")
                .category("POLITICS")
                .url("https://example.com/news/1")
                .publishedAt(LocalDateTime.now().minusHours(2))
                .imageUrl("https://via.placeholder.com/800x400")
                .viewCount(100L)
                .shareCount(10L)
                .personalizedScore(0.5)
                .trendScore(0.5)
                .isPersonalized(false)
                .build();
        articles.add(article1);
        
        NewsletterContent.Article article2 = NewsletterContent.Article.builder()
                .id(2L)
                .title("경제 동향")
                .summary("경제 관련 뉴스입니다.")
                .category("ECONOMY")
                .url("https://example.com/news/2")
                .publishedAt(LocalDateTime.now().minusHours(4))
                .imageUrl("https://via.placeholder.com/800x400")
                .viewCount(80L)
                .shareCount(5L)
                .personalizedScore(0.5)
                .trendScore(0.5)
                .isPersonalized(false)
                .build();
        articles.add(article2);
        
        return articles;
    }
    
    /**
     * 텍스트 자르기
     */
    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * 한국어 카테고리를 영어 카테고리로 변환
     */
    private String convertCategoryToEnglish(String koreanCategory) {
        if (koreanCategory == null || koreanCategory.trim().isEmpty()) {
            return "POLITICS";
        }

        String normalized = koreanCategory.trim().toLowerCase();
        // "문화"는 "LIFE"와 "ART" 모두에 해당할 수 있으나, 명확히 분리
        return switch (normalized) {
            case "정치", "politics" -> "POLITICS";
            case "경제", "economy" -> "ECONOMY";
            case "사회", "society" -> "SOCIETY";
            case "생활", "life" -> "LIFE";
            case "세계", "international", "국제" -> "INTERNATIONAL";
            case "it/과학", "it_science", "it과학", "과학", "기술" -> "IT_SCIENCE";
            case "자동차/교통", "vehicle", "자동차", "교통" -> "VEHICLE";
            case "여행/음식", "travel_food", "여행", "음식", "맛집" -> "TRAVEL_FOOD";
            case "예술", "art", "문화예술" -> "ART";
            case "문화" -> "LIFE"; // "문화"는 기본적으로 LIFE로 매핑
            default -> {
                log.warn("알 수 없는 카테고리: {}. 기본값 POLITICS 사용", koreanCategory);
                yield "POLITICS";
            }
        };
    }

    /**
     * 영어 카테고리를 한국어 카테고리로 변환
     */
    private String convertCategoryToKorean(String englishCategory) {
        if (englishCategory == null || englishCategory.trim().isEmpty()) {
            return "정치";
        }

        String normalized = englishCategory.trim().toUpperCase();
        return switch (normalized) {
            case "POLITICS" -> "정치";
            case "ECONOMY" -> "경제";
            case "SOCIETY" -> "사회";
            case "LIFE" -> "생활";
            case "INTERNATIONAL" -> "세계";
            case "IT_SCIENCE" -> "IT/과학";
            case "VEHICLE" -> "자동차/교통";
            case "TRAVEL_FOOD" -> "여행/음식";
            case "ART" -> "예술";
            default -> {
                log.warn("알 수 없는 영어 카테고리: {}. 기본값 '정치' 사용", englishCategory);
                yield "정치";
            }
        };
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
