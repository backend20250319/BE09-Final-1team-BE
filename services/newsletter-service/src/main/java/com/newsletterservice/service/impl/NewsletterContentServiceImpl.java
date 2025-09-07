package com.newsletterservice.service.impl;

import com.newsletterservice.client.NewsServiceClient;
import com.newsletterservice.client.UserServiceClient;
import com.newsletterservice.client.dto.*;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.common.exception.NewsletterException;
import com.newsletterservice.dto.NewsletterContent;
import com.newsletterservice.dto.NewsletterPreview;
import com.newsletterservice.client.dto.ReadHistoryResponse;
import com.newsletterservice.entity.NewsCategory;
import com.newsletterservice.service.EmailNewsletterRenderer;
import com.newsletterservice.service.NewsletterContentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 뉴스레터 콘텐츠 생성 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NewsletterContentServiceImpl implements NewsletterContentService {

    private final NewsServiceClient newsServiceClient;
    private final UserServiceClient userServiceClient;
    private final EmailNewsletterRenderer emailRenderer;
    
    private static final int MAX_ITEMS = 8;
    private static final int PER_CATEGORY_LIMIT = 3;

    @Override
    public NewsletterContent buildPersonalizedContent(Long userId, Long newsletterId) {
        log.info("개인화된 뉴스레터 콘텐츠 생성: userId={}, newsletterId={}", userId, newsletterId);
        
        // 사용자 선호도 기반 기사 조회
        List<NewsletterContent.Article> personalizedArticles = getPersonalizedArticles(userId);
        
        NewsletterContent content = new NewsletterContent();
        content.setNewsletterId(newsletterId);
        content.setUserId(userId);
        content.setGeneratedAt(LocalDateTime.now());
        content.setPersonalized(true);
        
        // 개인화 정보 추가
        Map<String, Object> personalizationInfo = buildPersonalizationInfo(userId);
        content.setPersonalizationInfo(personalizationInfo);
        
        // 개인화된 제목 생성
        content.setTitle(generatePersonalizedTitle(personalizationInfo));
        
        // 섹션에 실제 기사들 추가
        NewsletterContent.Section newsSection = new NewsletterContent.Section();
        newsSection.setHeading("오늘의 뉴스");
        newsSection.setSectionType("article");
        newsSection.setArticles(personalizedArticles);
        
        content.setSections(List.of(newsSection));
        return content;
    }

    @Override
    public NewsletterPreview generateNewsletterPreview(Long userId) {
        try {
            NewsletterContent content = buildPersonalizedContent(userId, null);
            String htmlPreview = emailRenderer.renderToHtml(content);

            return NewsletterPreview.builder()
                    .userId(userId)
                    .title(content.getTitle())
                    .htmlContent(htmlPreview)
                    .articleCount(content.getSections().stream()
                            .mapToInt(section -> section.getArticles().size())
                            .sum())
                    .generatedAt(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("뉴스레터 미리보기 생성 실패: userId={}", userId, e);
            throw new NewsletterException("미리보기 생성 중 오류가 발생했습니다.", "PREVIEW_ERROR");
        }
    }

    @Override
    public String generatePersonalizedNewsletter(String userId) {
        log.info("간단한 개인화 뉴스레터 생성: userId={}", userId);
        
        try {
            Long userIdLong = Long.valueOf(userId);
            
            // 1. 사용자 정보 및 선호도 조회
            ApiResponse<UserResponse> userResponse = userServiceClient.getUserById(userIdLong);
            UserResponse user = userResponse != null ? userResponse.getData() : null;
            
            ApiResponse<UserInterestResponse> interestResponse = userServiceClient.getUserInterests(userIdLong);
            UserInterestResponse interests = interestResponse != null ? interestResponse.getData() : null;
            
            // 2. 개인화된 뉴스 조회
            List<NewsResponse> personalizedNews = new ArrayList<>();
            
            if (interests != null && interests.getTopCategories() != null && !interests.getTopCategories().isEmpty()) {
                // 관심사가 있는 경우 - 첫 번째 관심 카테고리로 뉴스 조회
                String topCategory = interests.getTopCategories().get(0);
                ApiResponse<Page<NewsResponse>> newsResponse = newsServiceClient.getLatestByCategory(topCategory, 5);
                Page<NewsResponse> newsPage = newsResponse.getData();
                personalizedNews = newsPage != null ? newsPage.getContent() : new ArrayList<>();
                
                log.info("관심사 기반 뉴스 조회: category={}, count={}", topCategory, personalizedNews.size());
            } else {
                // 관심사가 없는 경우 - 트렌딩 뉴스 조회
                ApiResponse<Page<NewsResponse>> trendingResponse = newsServiceClient.getTrendingNews(24, 5);
                Page<NewsResponse> trendingNews = trendingResponse.getData();
                personalizedNews = trendingNews != null ? trendingNews.getContent() : new ArrayList<>();
                
                log.info("트렌딩 뉴스 조회: count={}", personalizedNews.size());
            }
            
            // 3. HTML 템플릿에 데이터 바인딩
            return buildHtmlTemplate(user, personalizedNews);
            
        } catch (Exception e) {
            log.error("개인화 뉴스레터 생성 실패: userId={}", userId, e);
            return buildErrorHtml("뉴스레터 생성 실패", "뉴스레터를 생성하는 중 오류가 발생했습니다.", "잠시 후 다시 시도해주세요.");
        }
    }

    @Override
    public String generatePreviewHtml(Long id) {
        try {
            // 실제 구현에서는 뉴스레터 내용을 기반으로 HTML을 생성해야 하지만,
            // 현재는 임시로 기본 HTML을 반환
            return "<html><body><h1>뉴스레터 미리보기</h1><p>뉴스레터 내용이 여기에 표시됩니다.</p></body></html>";
        } catch (Exception e) {
            log.error("뉴스레터 미리보기 HTML 생성 실패: id={}", id, e);
            throw new NewsletterException("뉴스레터 미리보기 생성에 실패했습니다.", "PREVIEW_GENERATION_ERROR");
        }
    }

    @Override
    public Map<String, Object> getPersonalizationInfo(Long userId) {
        return buildPersonalizationInfo(userId);
    }

    @Override
    public List<NewsletterContent.Article> getCategoryHeadlines(String category, int limit) {
        log.info("카테고리 헤드라인 조회: category={}, limit={}", category, limit);
        
        try {
            String englishCategory = convertCategoryToEnglish(category);
            NewsCategory newsCategory = NewsCategory.valueOf(englishCategory);
            
            ApiResponse<Page<NewsResponse>> newsResponse = newsServiceClient.getNewsByCategory(newsCategory.name(), 0, limit);
            Page<NewsResponse> newsPage = newsResponse.getData();
            List<NewsResponse> newsList = newsPage.getContent();
            
            return newsList.stream()
                    .map(news -> NewsletterContent.Article.builder()
                            .title(news.getTitle())
                            .summary(news.getContent())
                            .url(news.getLink())
                            .publishedAt(news.getPublishedAt())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("카테고리 헤드라인 조회 실패: category={}", category, e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> getCategoryArticlesWithTrendingKeywords(String category, int limit) {
        log.info("카테고리별 기사 및 트렌딩 키워드 조회: category={}, limit={}", category, limit);
        
        try {
            String englishCategory = convertCategoryToEnglish(category);
            NewsCategory newsCategory = NewsCategory.valueOf(englishCategory);
            
            ApiResponse<Page<NewsResponse>> newsResponse = newsServiceClient.getNewsByCategory(newsCategory.name(), 0, limit);
            Page<NewsResponse> newsPage = newsResponse.getData();
            List<NewsResponse> newsList = newsPage.getContent();
            
            ApiResponse<List<TrendingKeywordDto>> trendingKeywordsResponse = newsServiceClient.getTrendingKeywordsByCategory(newsCategory.name(), limit, "24h", 24);
            List<TrendingKeywordDto> trendingKeywords = trendingKeywordsResponse.getData();
            
            Map<String, Object> result = new HashMap<>();
            result.put("articles", newsList);
            result.put("trendingKeywords", trendingKeywords.stream()
                    .map(TrendingKeywordDto::getKeyword)
                    .collect(Collectors.toList()));
            
            return result;
        } catch (Exception e) {
            log.error("카테고리별 기사 및 트렌딩 키워드 조회 실패: category={}", category, e);
            return new HashMap<>();
        }
    }

    @Override
    public Object getNewsletterById(Long id) {
        try {
            // 실제 구현에서는 뉴스레터 엔티티를 조회해야 하지만,
            // 현재는 임시로 더미 데이터를 반환
            Map<String, Object> newsletter = new HashMap<>();
            newsletter.put("id", id);
            newsletter.put("title", "샘플 뉴스레터");
            newsletter.put("content", "뉴스레터 내용");
            newsletter.put("createdAt", LocalDateTime.now());
            return newsletter;
        } catch (Exception e) {
            log.error("뉴스레터 조회 실패: id={}", id, e);
            throw new NewsletterException("뉴스레터 조회에 실패했습니다.", "NEWSLETTER_NOT_FOUND");
        }
    }

    @Override
    public Object createSampleNewsletter() {
        try {
            Map<String, Object> sampleNewsletter = new HashMap<>();
            sampleNewsletter.put("id", 1L);
            sampleNewsletter.put("title", "샘플 뉴스레터");
            sampleNewsletter.put("content", "이것은 샘플 뉴스레터입니다.");
            sampleNewsletter.put("createdAt", LocalDateTime.now());
            return sampleNewsletter;
        } catch (Exception e) {
            log.error("샘플 뉴스레터 생성 실패", e);
            throw new NewsletterException("샘플 뉴스레터 생성에 실패했습니다.", "SAMPLE_NEWSLETTER_ERROR");
        }
    }

    @Override
    public Object getNewsletterList(int page, int size) {
        try {
            // 실제 구현에서는 페이징된 뉴스레터 목록을 조회해야 하지만,
            // 현재는 임시로 더미 데이터를 반환
            List<Map<String, Object>> newsletters = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                Map<String, Object> newsletter = new HashMap<>();
                newsletter.put("id", (long) (page * size + i + 1));
                newsletter.put("title", "뉴스레터 " + (page * size + i + 1));
                newsletter.put("content", "뉴스레터 내용 " + (page * size + i + 1));
                newsletter.put("createdAt", LocalDateTime.now());
                newsletters.add(newsletter);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("content", newsletters);
            result.put("totalElements", 100L);
            result.put("totalPages", 10);
            result.put("currentPage", page);
            result.put("size", size);
            return result;
        } catch (Exception e) {
            log.error("뉴스레터 목록 조회 실패: page={}, size={}", page, size, e);
            throw new NewsletterException("뉴스레터 목록 조회에 실패했습니다.", "NEWSLETTER_LIST_ERROR");
        }
    }

    @Override
    public Map<String, Object> testNewsletterGeneration(Long userId) {
        log.info("뉴스레터 생성 테스트 시작: userId={}", userId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 간단한 개인화 뉴스레터 생성 테스트
            String htmlContent = generatePersonalizedNewsletter(userId.toString());
            result.put("htmlGenerated", !htmlContent.isEmpty());
            result.put("htmlLength", htmlContent.length());
            
            // 2. API 연동 상태 확인
            try {
                ApiResponse<UserResponse> userResponse = userServiceClient.getUserById(userId);
                result.put("userInfoAvailable", userResponse != null && userResponse.getData() != null);
            } catch (Exception e) {
                result.put("userInfoAvailable", false);
                result.put("userInfoError", e.getMessage());
            }
            
            try {
                ApiResponse<UserInterestResponse> interestResponse = userServiceClient.getUserInterests(userId);
                result.put("userInterestsAvailable", interestResponse != null && interestResponse.getData() != null);
            } catch (Exception e) {
                result.put("userInterestsAvailable", false);
                result.put("userInterestsError", e.getMessage());
            }
            
            try {
                ApiResponse<Page<NewsResponse>> trendingResponse = newsServiceClient.getTrendingNews(24, 5);
                Page<NewsResponse> trendingNews = trendingResponse.getData();
                result.put("trendingNewsAvailable", trendingNews != null && !trendingNews.getContent().isEmpty());
                result.put("trendingNewsCount", trendingNews != null ? trendingNews.getContent().size() : 0);
            } catch (Exception e) {
                result.put("trendingNewsAvailable", false);
                result.put("trendingNewsError", e.getMessage());
            }
            
            result.put("success", true);
            result.put("message", "뉴스레터 생성 테스트 완료");
            
        } catch (Exception e) {
            log.error("뉴스레터 생성 테스트 실패: userId={}", userId, e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    // Private Helper Methods
    private List<NewsletterContent.Article> getPersonalizedArticles(Long userId) {
        log.info("개인화된 기사 조회 시작: userId={}", userId);
        
        try {
            // 1. 사용자 읽기 기록 분석 (UserServiceClient 사용)
            ApiResponse<Page<ReadHistoryResponse>> historyResponse = userServiceClient.getReadHistory(userId, 0, 30, "updatedAt,desc");
            List<ReadHistoryResponse> recentHistory = historyResponse != null && historyResponse.getData() != null ? 
                    historyResponse.getData().getContent() : new ArrayList<>();
            Map<String, Long> categoryReadCounts = new HashMap<>(); // TODO: UserServiceClient에서 카테고리별 읽기 횟수 조회 구현 필요
            
            // 2. 개인화된 뉴스 수집 (관심사 기반)
            List<NewsResponse> personalizedNews = collectPersonalizedNewsWithInterests(userId);
            
            // 3. 뉴스 응답을 뉴스레터 아티클로 변환
            List<NewsletterContent.Article> articles = personalizedNews.stream()
                    .map(news -> convertNewsResponseToArticle(news, categoryReadCounts))
                    .limit(8)
                    .collect(Collectors.toList());
            
            log.info("개인화된 기사 조회 완료: userId={}, count={}", userId, articles.size());
            return articles;
                    
        } catch (Exception e) {
            log.error("개인화된 기사 조회 실패: userId={}", userId, e);
            return createSampleArticles();
        }
    }

    private List<NewsResponse> collectPersonalizedNewsWithInterests(Long userId) {
        List<NewsResponse> allNews = new ArrayList<>();
        
        try {
            // 1. 사용자 관심사 조회
            ApiResponse<UserInterestResponse> interestResponse = userServiceClient.getUserInterests(userId);
            UserInterestResponse userInterests = interestResponse != null ? interestResponse.getData() : null;
            
            // 2. 읽은 뉴스 ID 조회 (UserServiceClient 사용)
            ApiResponse<List<Long>> readNewsIdsResponse = userServiceClient.getReadNewsIds(userId, 0, 100);
            List<Long> readNewsIds = readNewsIdsResponse != null && readNewsIdsResponse.getData() != null ? 
                    readNewsIdsResponse.getData() : new ArrayList<>();
            
            if (userInterests != null && userInterests.getTopCategories() != null && !userInterests.getTopCategories().isEmpty()) {
                // 관심사가 있는 경우 - 관심사 기반 뉴스 수집
                List<String> topCategories = userInterests.getTopCategories();
                log.info("사용자 관심사 기반 뉴스 수집: userId={}, categories={}", userId, topCategories);
                
                allNews = collectPersonalizedNews(userId, topCategories);
            } else {
                // 관심사가 없는 경우 - 기본 뉴스 제공
                log.info("관심사가 없어 기본 뉴스 제공: userId={}", userId);
                allNews = fetchDefaultNews();
            }
            
            // 3. 읽은 뉴스 제외
            allNews = allNews.stream()
                    .filter(news -> !readNewsIds.contains(news.getNewsId()))
                    .collect(Collectors.toList());
            
            log.info("개인화 뉴스 수집 완료: userId={}, count={}", userId, allNews.size());
            
        } catch (Exception e) {
            log.error("개인화 뉴스 수집 실패: userId={}", userId, e);
            // 실패 시 기본 뉴스 제공
            allNews = fetchDefaultNews();
        }
        
        return allNews;
    }

    private List<NewsResponse> collectPersonalizedNews(Long userId, List<String> categories) {
        List<NewsResponse> allNews = new ArrayList<>();
        ApiResponse<List<Long>> readNewsIdsResponse = userServiceClient.getReadNewsIds(userId, 0, 100);
        List<Long> readNewsIds = readNewsIdsResponse != null && readNewsIdsResponse.getData() != null ? 
                readNewsIdsResponse.getData() : new ArrayList<>();
        
        int articlesPerCategory = 8 / Math.max(categories.size(), 1);
        
        for (String category : categories) {
            try {
                String englishCategory = convertCategoryToEnglish(category);
                ApiResponse<Page<NewsResponse>> response = newsServiceClient.getNewsByCategory(englishCategory, 0, articlesPerCategory + 2);
                Page<NewsResponse> newsPage = response.getData();
                List<NewsResponse> categoryNews = newsPage != null && newsPage.getContent() != null ? 
                    newsPage.getContent() : new ArrayList<>();
                
                // 읽은 뉴스 제외
                List<NewsResponse> unreadNews = categoryNews.stream()
                        .filter(news -> !readNewsIds.contains(news.getNewsId()))
                        .limit(articlesPerCategory)
                        .collect(Collectors.toList());
                
                allNews.addAll(unreadNews);
                log.info("카테고리 {} 뉴스 추가: {}개", category, unreadNews.size());
                
            } catch (Exception e) {
                log.warn("카테고리 {} 뉴스 수집 실패", category, e);
            }
        }
        
        // 부족한 경우 트렌딩 뉴스로 보완
        if (allNews.size() < 8) {
            fillWithTrendingNews(allNews, readNewsIds, 8 - allNews.size());
        }
        
        return allNews;
    }

    private void fillWithTrendingNews(List<NewsResponse> currentNews, List<Long> readNewsIds, int needed) {
        try {
            ApiResponse<Page<NewsResponse>> trendingResponse = newsServiceClient.getTrendingNews(24, needed * 2);
            Page<NewsResponse> trendingNews = trendingResponse.getData();
            
            Set<Long> existingIds = currentNews.stream()
                    .map(NewsResponse::getNewsId)
                    .collect(Collectors.toSet());
            
            List<NewsResponse> additionalNews = trendingNews.getContent().stream()
                    .filter(news -> !readNewsIds.contains(news.getNewsId()))
                    .filter(news -> !existingIds.contains(news.getNewsId()))
                    .limit(needed)
                    .collect(Collectors.toList());
            
            currentNews.addAll(additionalNews);
            log.info("트렌딩 뉴스로 {}개 보완", additionalNews.size());
            
        } catch (Exception e) {
            log.warn("트렌딩 뉴스 보완 실패", e);
        }
    }


    private List<NewsResponse> fetchDefaultNews() {
        try {
            // 트렌딩 뉴스 조회
            ApiResponse<Page<NewsResponse>> trendingResponse = newsServiceClient.getTrendingNews(24, 8);
            Page<NewsResponse> trendingNews = trendingResponse.getData();
            if (trendingNews != null && trendingNews.getContent() != null && !trendingNews.getContent().isEmpty()) {
                log.info("트렌딩 뉴스 조회 성공: {}개", trendingNews.getContent().size());
                return trendingNews.getContent();
            }
        } catch (Exception e) {
            log.warn("트렌딩 뉴스 조회 실패, 인기 뉴스로 대체", e);
        }
        
        try {
            // 인기 뉴스로 대체
            ApiResponse<Page<NewsResponse>> popularResponse = newsServiceClient.getPopularNews(8);
            Page<NewsResponse> popularNews = popularResponse.getData();
            if (popularNews != null && popularNews.getContent() != null && !popularNews.getContent().isEmpty()) {
                log.info("인기 뉴스 조회 성공: {}개", popularNews.getContent().size());
                return popularNews.getContent();
            }
        } catch (Exception e) {
            log.warn("인기 뉴스 조회 실패, 최신 뉴스로 대체", e);
        }
        
        try {
            // 최신 뉴스로 대체
            ApiResponse<Page<NewsResponse>> latestResponse = newsServiceClient.getLatestNews(null, 8);
            Page<NewsResponse> latestNews = latestResponse.getData();
            if (latestNews != null && latestNews.getContent() != null && !latestNews.getContent().isEmpty()) {
                log.info("최신 뉴스 조회 성공: {}개", latestNews.getContent().size());
                return latestNews.getContent();
            }
        } catch (Exception e) {
            log.error("모든 뉴스 조회 실패", e);
        }
        
        log.warn("모든 뉴스 조회 실패, 빈 리스트 반환");
        return new ArrayList<>();
    }

    private List<NewsletterContent.Article> createSampleArticles() {
        List<NewsletterContent.Article> sampleArticles = new ArrayList<>();
        
        // 샘플 기사 1
        NewsletterContent.Article article1 = NewsletterContent.Article.builder()
                .id(1L)
                .title("샘플 뉴스 1: 오늘의 주요 뉴스")
                .summary("이것은 샘플 뉴스 기사입니다. 실제 뉴스 데이터를 불러올 수 없을 때 표시됩니다.")
                .category("POLITICS")
                .url("https://example.com/news/1")
                .publishedAt(LocalDateTime.now().minusHours(2))
                .imageUrl("https://via.placeholder.com/300x200")
                .personalizedScore(1.0)
                .build();
        sampleArticles.add(article1);
        
        // 샘플 기사 2
        NewsletterContent.Article article2 = NewsletterContent.Article.builder()
                .id(2L)
                .title("샘플 뉴스 2: 경제 동향")
                .summary("경제 관련 샘플 뉴스입니다.")
                .category("ECONOMY")
                .url("https://example.com/news/2")
                .publishedAt(LocalDateTime.now().minusHours(4))
                .imageUrl("https://via.placeholder.com/300x200")
                .personalizedScore(1.0)
                .build();
        sampleArticles.add(article2);
        
        return sampleArticles;
    }

    private NewsletterContent.Article convertNewsResponseToArticle(NewsResponse news, Map<String, Long> categoryReadCounts) {
        // 개인화 점수 계산
        double personalizedScore = calculateArticlePersonalizedScore(news, categoryReadCounts);
        
        return NewsletterContent.Article.builder()
                .id(news.getNewsId())
                .title(news.getTitle())
                .summary(news.getSummary() != null ? news.getSummary() : news.getContent())
                .category(news.getCategoryName())
                .url(news.getLink())
                .publishedAt(news.getPublishedAt())
                .imageUrl(news.getImageUrl())
                .viewCount(news.getViewCount())
                .shareCount(news.getShareCount())
                .personalizedScore(personalizedScore)
                .trendScore(calculateTrendScore(news))
                .build();
    }

    private double calculateArticlePersonalizedScore(NewsResponse news, Map<String, Long> categoryReadCounts) {
        if (categoryReadCounts == null || categoryReadCounts.isEmpty()) {
            return 0.5; // 기본 점수
        }
        
        String category = news.getCategoryName();
        if (category == null) {
            return 0.5;
        }
        
        Long readCount = categoryReadCounts.get(category);
        if (readCount == null || readCount == 0) {
            return 0.3; // 읽지 않은 카테고리
        }
        
        // 읽은 횟수에 따른 점수 (최대 1.0)
        return Math.min(1.0, 0.3 + (readCount * 0.1));
    }

    private double calculateTrendScore(NewsResponse news) {
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

    private Map<String, Object> buildPersonalizationInfo(Long userId) {
        Map<String, Object> info = new HashMap<>();
        
        try {
            // UserServiceClient를 통해 개인화 정보 조회
            ApiResponse<Map<String, Object>> response = userServiceClient.getPersonalizationInfo(userId);
            if (response != null && response.getData() != null) {
                info = response.getData();
            } else {
                // 기본값 설정
                info.put("signupInterests", List.of());
                info.put("subscriptionCategories", List.of());
                info.put("hasReadingHistory", false);
                info.put("totalReadCount", 0L);
                info.put("preferredCategories", List.of());
                info.put("personalizationScore", 0.0);
            }
            
            log.info("개인화 정보 구성 완료: userId={}, score={}", userId, info.get("personalizationScore"));
            
        } catch (Exception e) {
            log.error("개인화 정보 구성 실패: userId={}", userId, e);
            // 기본값 설정
            info.put("signupInterests", List.of());
            info.put("subscriptionCategories", List.of());
            info.put("hasReadingHistory", false);
            info.put("totalReadCount", 0L);
            info.put("preferredCategories", List.of());
            info.put("personalizationScore", 0.0);
        }
        
        return info;
    }


    private String generatePersonalizedTitle(Map<String, Object> personalizationInfo) {
        try {
            @SuppressWarnings("unchecked")
            List<String> preferredCategories = (List<String>) personalizationInfo.get("preferredCategories");
            Double personalizationScore = (Double) personalizationInfo.get("personalizationScore");
            Boolean hasReadingHistory = (Boolean) personalizationInfo.get("hasReadingHistory");
            
            if (personalizationScore != null && personalizationScore > 0.7) {
                // 높은 개인화 점수 - 구체적인 제목
                if (preferredCategories != null && !preferredCategories.isEmpty()) {
                    String topCategory = preferredCategories.get(0);
                    return String.format("당신이 관심 있어할 %s 뉴스", convertCategoryToKorean(topCategory));
                }
                return "당신을 위한 맞춤 뉴스";
            } else if (hasReadingHistory != null && hasReadingHistory) {
                // 읽기 기록이 있는 경우
                return "당신의 관심사를 반영한 뉴스";
            } else {
                // 기본 제목
                return "오늘의 핫한 뉴스";
            }
        } catch (Exception e) {
            log.warn("개인화 제목 생성 실패, 기본 제목 사용", e);
            return "오늘의 핫한 뉴스";
        }
    }

    private String convertCategoryToKorean(String englishCategory) {
        if (englishCategory == null) return "뉴스";
        
        return switch (englishCategory.toUpperCase()) {
            case "POLITICS" -> "정치";
            case "ECONOMY" -> "경제";
            case "SOCIETY" -> "사회";
            case "LIFE" -> "생활";
            case "INTERNATIONAL" -> "세계";
            case "IT_SCIENCE" -> "IT/과학";
            case "VEHICLE" -> "자동차/교통";
            case "TRAVEL_FOOD" -> "여행/음식";
            case "ART" -> "예술";
            default -> "뉴스";
        };
    }

    private String convertCategoryToEnglish(String koreanCategory) {
        if (koreanCategory == null || koreanCategory.trim().isEmpty()) {
            return "POLITICS";
        }
        
        return switch (koreanCategory.trim().toLowerCase()) {
            case "정치", "politics" -> "POLITICS";
            case "경제", "economy" -> "ECONOMY";
            case "사회", "society" -> "SOCIETY";
            case "생활", "life", "문화" -> "LIFE";
            case "세계", "international", "국제" -> "INTERNATIONAL";
            case "it/과학", "it_science", "it과학", "과학", "기술" -> "IT_SCIENCE";
            case "자동차/교통", "vehicle", "자동차", "교통" -> "VEHICLE";
            case "여행/음식", "travel_food", "여행", "음식", "맛집" -> "TRAVEL_FOOD";
            case "예술", "art", "문화예술" -> "ART";
            default -> {
                log.warn("알 수 없는 카테고리: {}. 기본값 POLITICS 사용", koreanCategory);
                yield "POLITICS";
            }
        };
    }

    private String buildHtmlTemplate(UserResponse user, List<NewsResponse> personalizedNews) {
        StringBuilder html = new StringBuilder();
        
        // HTML 헤더
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='ko'>\n");
        html.append("<head>\n");
        html.append("    <meta charset='UTF-8'>\n");
        html.append("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("    <title>개인화 뉴스레터</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }\n");
        html.append("        .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n");
        html.append("        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }\n");
        html.append("        .header h1 { margin: 0; font-size: 24px; font-weight: 300; }\n");
        html.append("        .content { padding: 30px; }\n");
        html.append("        .article { border: 1px solid #e0e0e0; border-radius: 6px; padding: 15px; margin-bottom: 15px; background-color: #fafafa; }\n");
        html.append("        .article:hover { border-color: #667eea; box-shadow: 0 2px 8px rgba(102, 126, 234, 0.2); }\n");
        html.append("        .article-title { font-size: 16px; font-weight: 600; color: #333; margin: 0 0 8px 0; }\n");
        html.append("        .article-title a { color: #333; text-decoration: none; }\n");
        html.append("        .article-title a:hover { color: #667eea; }\n");
        html.append("        .article-summary { color: #666; font-size: 14px; line-height: 1.5; margin-bottom: 10px; }\n");
        html.append("        .article-meta { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: #999; }\n");
        html.append("        .article-category { background-color: #667eea; color: white; padding: 2px 8px; border-radius: 12px; font-size: 11px; }\n");
        html.append("        .footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        // 컨테이너 시작
        html.append("<div class='container'>\n");
        
        // 헤더
        html.append("    <div class='header'>\n");
        html.append("        <h1>📰 개인화 뉴스레터</h1>\n");
        if (user != null) {
            html.append("        <p>안녕하세요, ").append(user.getNickname() != null ? user.getNickname() : "사용자").append("님!</p>\n");
        }
        html.append("        <p>").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append(" 발행</p>\n");
        html.append("    </div>\n");
        
        // 콘텐츠 시작
        html.append("    <div class='content'>\n");
        
        if (personalizedNews.isEmpty()) {
            html.append("        <p>현재 뉴스를 불러올 수 없습니다.</p>\n");
        } else {
            for (NewsResponse news : personalizedNews) {
                html.append("        <div class='article'>\n");
                html.append("            <h3 class='article-title'>\n");
                html.append("                <a href='").append(news.getLink()).append("' target='_blank'>\n");
                html.append("                    ").append(news.getTitle()).append("\n");
                html.append("                </a>\n");
                html.append("            </h3>\n");
                
                if (news.getSummary() != null && !news.getSummary().isEmpty()) {
                    html.append("            <p class='article-summary'>").append(news.getSummary()).append("</p>\n");
                }
                
                html.append("            <div class='article-meta'>\n");
                html.append("                <span class='article-category'>").append(convertCategoryToKorean(news.getCategoryName())).append("</span>\n");
                if (news.getPublishedAt() != null) {
                    html.append("                <span>").append(news.getPublishedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("</span>\n");
                }
                html.append("            </div>\n");
                html.append("        </div>\n");
            }
        }
        
        html.append("    </div>\n");
        
        // 푸터
        html.append("    <div class='footer'>\n");
        html.append("        <p>이 뉴스레터는 자동으로 생성되었습니다.</p>\n");
        html.append("        <p>구독 해지나 설정 변경은 웹사이트에서 가능합니다.</p>\n");
        html.append("    </div>\n");
        
        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>");
        
        return html.toString();
    }

    private String buildErrorHtml(String title, String message, String suggestion) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 50px auto; padding: 20px; text-align: center; background-color: #f5f5f5; }
                    .error-container { background: white; padding: 40px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .error-icon { font-size: 48px; color: #e74c3c; margin-bottom: 20px; }
                    .error-title { color: #e74c3c; font-size: 24px; margin-bottom: 10px; }
                    .error-message { color: #666; margin-bottom: 20px; line-height: 1.6; }
                    .suggestion { background: #e3f2fd; padding: 15px; border-radius: 5px; color: #1976d2; margin-bottom: 20px; }
                </style>
            </head>
            <body>
                <div class="error-container">
                    <div class="error-icon">⚠️</div>
                    <h1 class="error-title">%s</h1>
                    <p class="error-message">%s</p>
                    <div class="suggestion">💡 %s</div>
                </div>
            </body>
            </html>
            """, title, title, message, suggestion);
    }
}
