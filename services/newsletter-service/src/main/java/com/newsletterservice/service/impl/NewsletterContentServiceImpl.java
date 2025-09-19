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
import java.time.format.DateTimeParseException;
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
                Page<NewsResponse> newsPage = newsResponse != null && newsResponse.isSuccess() ? newsResponse.getData() : null;
                personalizedNews = newsPage != null ? newsPage.getContent() : new ArrayList<>();
                
                log.info("관심사 기반 뉴스 조회: category={}, count={}", topCategory, personalizedNews.size());
            } else {
                // 관심사가 없는 경우 - 트렌딩 뉴스 조회
                ApiResponse<Page<NewsResponse>> trendingResponse = newsServiceClient.getTrendingNews(24, 5);
                Page<NewsResponse> trendingNews = trendingResponse != null && trendingResponse.isSuccess() ? trendingResponse.getData() : null;
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
            log.info("뉴스레터 미리보기 HTML 생성 시작: id={}", id);
            
            // 뉴스 데이터 조회
            List<NewsResponse> latestNews = getLatestNewsForPreview();
            List<NewsResponse> trendingNews = getTrendingNewsForPreview();
            List<NewsResponse> categoryNews = getCategoryNewsForPreview();
            
            // HTML 템플릿 생성
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang='ko'>\n");
            html.append("<head>\n");
            html.append("    <meta charset='UTF-8'>\n");
            html.append("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
            html.append("    <title>뉴스레터 미리보기</title>\n");
            html.append("    <style>\n");
            html.append("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; }\n");
            html.append("        .container { max-width: 800px; margin: 0 auto; background-color: white; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }\n");
            html.append("        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 40px; text-align: center; }\n");
            html.append("        .header h1 { margin: 0; font-size: 32px; font-weight: 300; }\n");
            html.append("        .header p { margin: 10px 0 0 0; font-size: 16px; opacity: 0.9; }\n");
            html.append("        .content { padding: 40px; }\n");
            html.append("        .section { margin-bottom: 40px; }\n");
            html.append("        .section-title { font-size: 24px; color: #333; margin-bottom: 20px; border-bottom: 3px solid #667eea; padding-bottom: 10px; }\n");
            html.append("        .news-item { border: 1px solid #e0e0e0; border-radius: 8px; padding: 20px; margin-bottom: 20px; background-color: #fafafa; transition: all 0.3s ease; }\n");
            html.append("        .news-item:hover { border-color: #667eea; box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3); transform: translateY(-2px); }\n");
            html.append("        .news-title { font-size: 18px; font-weight: 600; color: #333; margin: 0 0 10px 0; }\n");
            html.append("        .news-title a { color: #333; text-decoration: none; }\n");
            html.append("        .news-title a:hover { color: #667eea; }\n");
            html.append("        .news-summary { color: #666; font-size: 16px; line-height: 1.6; margin-bottom: 15px; }\n");
            html.append("        .news-meta { display: flex; justify-content: space-between; align-items: center; font-size: 14px; color: #999; }\n");
            html.append("        .news-category { background-color: #667eea; color: white; padding: 4px 12px; border-radius: 16px; font-size: 12px; font-weight: bold; }\n");
            html.append("        .news-date { color: #999; }\n");
            html.append("        .footer { background-color: #f8f9fa; padding: 30px; text-align: center; color: #666; font-size: 14px; }\n");
            html.append("        .preview-badge { position: absolute; top: 20px; right: 20px; background-color: rgba(255,255,255,0.2); padding: 8px 16px; border-radius: 20px; font-size: 12px; font-weight: bold; }\n");
            html.append("    </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("    <div class='container'>\n");
            html.append("        <div class='header'>\n");
            html.append("            <div class='preview-badge'>미리보기</div>\n");
            html.append("            <h1>뉴스레터 미리보기</h1>\n");
            html.append("            <p>최신 뉴스와 트렌딩 정보를 확인하세요</p>\n");
            html.append("        </div>\n");
            html.append("        <div class='content'>\n");
            
            // 최신 뉴스 섹션
            if (!latestNews.isEmpty()) {
                html.append("            <div class='section'>\n");
                html.append("                <h2 class='section-title'>📰 최신 뉴스</h2>\n");
                for (NewsResponse news : latestNews) {
                    html.append("                <div class='news-item'>\n");
                    html.append("                    <h3 class='news-title'><a href='#'>").append(escapeHtml(news.getTitle())).append("</a></h3>\n");
                    html.append("                    <p class='news-summary'>").append(escapeHtml(news.getSummary())).append("</p>\n");
                    html.append("                    <div class='news-meta'>\n");
                    html.append("                        <span class='news-category'>").append(escapeHtml(news.getCategory())).append("</span>\n");
                    html.append("                        <span class='news-date'>").append(news.getPublishedAt()).append("</span>\n");
                    html.append("                    </div>\n");
                    html.append("                </div>\n");
                }
                html.append("            </div>\n");
            }
            
            // 트렌딩 뉴스 섹션
            if (!trendingNews.isEmpty()) {
                html.append("            <div class='section'>\n");
                html.append("                <h2 class='section-title'>🔥 트렌딩 뉴스</h2>\n");
                for (NewsResponse news : trendingNews) {
                    html.append("                <div class='news-item'>\n");
                    html.append("                    <h3 class='news-title'><a href='#'>").append(escapeHtml(news.getTitle())).append("</a></h3>\n");
                    html.append("                    <p class='news-summary'>").append(escapeHtml(news.getSummary())).append("</p>\n");
                    html.append("                    <div class='news-meta'>\n");
                    html.append("                        <span class='news-category'>").append(escapeHtml(news.getCategory())).append("</span>\n");
                    html.append("                        <span class='news-date'>").append(news.getPublishedAt()).append("</span>\n");
                    html.append("                    </div>\n");
                    html.append("                </div>\n");
                }
                html.append("            </div>\n");
            }
            
            // 카테고리별 뉴스 섹션
            if (!categoryNews.isEmpty()) {
                html.append("            <div class='section'>\n");
                html.append("                <h2 class='section-title'>📋 카테고리별 뉴스</h2>\n");
                for (NewsResponse news : categoryNews) {
                    html.append("                <div class='news-item'>\n");
                    html.append("                    <h3 class='news-title'><a href='#'>").append(escapeHtml(news.getTitle())).append("</a></h3>\n");
                    html.append("                    <p class='news-summary'>").append(escapeHtml(news.getSummary())).append("</p>\n");
                    html.append("                    <div class='news-meta'>\n");
                    html.append("                        <span class='news-category'>").append(escapeHtml(news.getCategory())).append("</span>\n");
                    html.append("                        <span class='news-date'>").append(news.getPublishedAt()).append("</span>\n");
                    html.append("                    </div>\n");
                    html.append("                </div>\n");
                }
                html.append("            </div>\n");
            }
            
            html.append("        </div>\n");
            html.append("        <div class='footer'>\n");
            html.append("            <p>이 뉴스레터는 미리보기입니다. 실제 구독 시 더 많은 뉴스와 개인화된 콘텐츠를 받아보실 수 있습니다.</p>\n");
            html.append("        </div>\n");
            html.append("    </div>\n");
            html.append("</body>\n");
            html.append("</html>");
            
            log.info("뉴스레터 미리보기 HTML 생성 완료: id={}, 뉴스 수={}", id, latestNews.size() + trendingNews.size() + categoryNews.size());
            return html.toString();
            
        } catch (Exception e) {
            log.error("뉴스레터 미리보기 HTML 생성 실패: id={}", id, e);
            return generateErrorPreviewHtml("뉴스레터 미리보기 생성 실패", "뉴스 데이터를 불러오는 중 오류가 발생했습니다.");
        }
    }

    @Override
    public Map<String, Object> getPersonalizationInfo(Long userId) {
        return buildPersonalizationInfo(userId);
    }

    /**
     * 미리보기용 최신 뉴스 조회
     */
    private List<NewsResponse> getLatestNewsForPreview() {
        try {
            log.info("미리보기용 최신 뉴스 조회 시작");
            ApiResponse<Page<NewsResponse>> response = newsServiceClient.getLatestNews(null, 5);
            
            if (response != null && response.isSuccess() && response.getData() != null) {
                List<NewsResponse> news = response.getData().getContent();
                log.info("미리보기용 최신 뉴스 조회 완료: {}개", news.size());
                return news;
            } else {
                log.warn("미리보기용 최신 뉴스 조회 실패: {}", response.getMessage());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("미리보기용 최신 뉴스 조회 중 오류 발생", e);
            return new ArrayList<>();
        }
    }

    /**
     * 미리보기용 트렌딩 뉴스 조회
     */
    private List<NewsResponse> getTrendingNewsForPreview() {
        try {
            log.info("미리보기용 트렌딩 뉴스 조회 시작");
            ApiResponse<Page<NewsResponse>> response = newsServiceClient.getTrendingNews(24, 5);
            
            if (response != null && response.isSuccess() && response.getData() != null) {
                List<NewsResponse> news = response.getData().getContent();
                log.info("미리보기용 트렌딩 뉴스 조회 완료: {}개", news.size());
                return news;
            } else {
                log.warn("미리보기용 트렌딩 뉴스 조회 실패: {}", response.getMessage());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("미리보기용 트렌딩 뉴스 조회 중 오류 발생", e);
            return new ArrayList<>();
        }

    }

    /**
     * 미리보기용 카테고리별 뉴스 조회
     */

    private static final String[] PREVIEW_CATEGORIES = {"정치", "경제", "사회", "IT/과학", "세계", "생활", "자동차/교통", "여행/음식", "예술"};
    private static final int FIRST_PAGE = 0;
    private static final int NEWS_PER_CATEGORY_PREVIEW = 2;

    private List<NewsResponse> getCategoryNewsForPreview() {
        try {
            log.info("미리보기용 카테고리별 뉴스 조회 시작");
            List<NewsResponse> allCategoryNews = new ArrayList<>();

            // 주요 카테고리들에서 뉴스 조회
            String[] categories = PREVIEW_CATEGORIES;

            fetchNewsFromCategories(categories, allCategoryNews);

            log.info("미리보기용 카테고리별 뉴스 조회 완료: {}개", allCategoryNews.size());
            return allCategoryNews;
        } catch (Exception e) {
            log.error("미리보기용 카테고리별 뉴스 조회 중 오류 발생", e);
            return new ArrayList<>();
        }
    }


    private static boolean isValidResponse(Page<NewsResponse> response) {
        // Page<NewsResponse>에는 isSuccess()와 getData() 메서드가 없으므로, 단순히 null 체크만 수행합니다.
        return response != null && response.getContent() != null;
    }

    private void fetchNewsFromCategories(String[] categories, List<NewsResponse> allCategoryNews) {
        Arrays.stream(categories)
                .forEach(category -> fetchNewsFromSingleCategory(category, allCategoryNews));
    }

    private void  fetchNewsFromSingleCategory(String category, List<NewsResponse> allCategoryNews) {
            try {
                String englishCategory = convertToEnglishCategory(category);
                Page<NewsResponse> response = newsServiceClient.getNewsByCategory(englishCategory, FIRST_PAGE, NEWS_PER_CATEGORY_PREVIEW);

                if (isValidResponse(response)) {
                    List<NewsResponse> news = response.getContent();
                    allCategoryNews.addAll(news);
                }
            } catch (Exception e) {
                log.warn("카테고리 {} 뉴스 조회 실패", category, e);
            }

    }

    /**
     * HTML 이스케이프 처리
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    /**
     * 에러 미리보기 HTML 생성
     */
    private String generateErrorPreviewHtml(String title, String message) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='ko'>\n");
        html.append("<head>\n");
        html.append("    <meta charset='UTF-8'>\n");
        html.append("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("    <title>").append(escapeHtml(title)).append("</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; display: flex; justify-content: center; align-items: center; min-height: 100vh; }\n");
        html.append("        .error-container { max-width: 600px; background-color: white; padding: 40px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); text-align: center; }\n");
        html.append("        .error-title { color: #e74c3c; font-size: 24px; margin-bottom: 20px; }\n");
        html.append("        .error-message { color: #666; font-size: 16px; line-height: 1.6; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class='error-container'>\n");
        html.append("        <h1 class='error-title'>").append(escapeHtml(title)).append("</h1>\n");
        html.append("        <p class='error-message'>").append(escapeHtml(message)).append("</p>\n");
        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>");
        return html.toString();
    }

    @Override
    public List<NewsletterContent.Article> getCategoryHeadlines(String category, int limit) {
        log.info("카테고리 헤드라인 조회: category={}, limit={}", category, limit);
        
        try {
            // 1. 카테고리 변환 시도
            String englishCategory = convertToEnglishCategory(category);
            log.info("변환된 영어 카테고리: {} -> {}", category, englishCategory);
            
            // 2. 뉴스 서비스 호출
            Page<NewsResponse> newsResponse = newsServiceClient.getNewsByCategory(englishCategory, 0, limit);
            log.info("뉴스 서비스 응답: success={}, hasData={}", 
                newsResponse != null, 
                newsResponse != null);
            
            if (newsResponse != null) {
                Page<NewsResponse> newsPage = newsResponse;
                List<NewsResponse> newsList = newsPage.getContent();
                log.info("조회된 뉴스 수: {}", newsList.size());
                
                List<NewsletterContent.Article> articles = newsList.stream()
                        .map(news -> NewsletterContent.Article.builder()
                                .id(news.getNewsId())
                                .title(news.getTitle() != null ? news.getTitle() : "제목 없음")
                                .summary(news.getSummary() != null ? news.getSummary() : 
                                        news.getContent() != null ? news.getContent() : "내용 없음")
                                .url(news.getLink() != null ? news.getLink() : "#")
                                .category(news.getCategoryName() != null ? news.getCategoryName() : category)
                                .publishedAt(parsePublishedAt(news.getPublishedAt()))
                                .imageUrl(news.getImageUrl())
                                .viewCount(news.getViewCount() != null ? news.getViewCount().longValue() : 0L)
                                .shareCount(news.getShareCount())
                                .isPersonalized(false)
                                .build())
                        .collect(Collectors.toList());
                
                log.info("변환된 아티클 수: {}", articles.size());
                return articles;
            } else {
                log.warn("뉴스 서비스 응답이 비어있음: response={}", newsResponse);
                return createFallbackArticles(category, limit);
            }
        } catch (feign.FeignException.NotFound e) {
            log.warn("뉴스 서비스에서 404 응답: category={}, error={}", category, e.getMessage());
            return createFallbackArticles(category, limit);
        } catch (feign.FeignException e) {
            log.error("Feign 클라이언트 오류: category={}, status={}, error={}", 
                category, e.status(), e.getMessage());
            return createFallbackArticles(category, limit);
        } catch (Exception e) {
            log.error("카테고리 헤드라인 조회 실패: category={}", category, e);
            return createFallbackArticles(category, limit);
        }
    }

    /**
     * 폴백용 샘플 기사 생성
     */
    private List<NewsletterContent.Article> createFallbackArticles(String category, int limit) {
        log.info("폴백 기사 생성: category={}, limit={}", category, limit);
        
        List<NewsletterContent.Article> fallbackArticles = new ArrayList<>();
        
        // 카테고리별 샘플 제목
        Map<String, List<String>> sampleTitles = Map.of(
            "정치", List.of("정치 관련 주요 이슈", "정부 정책 발표", "국회 동향"),
            "경제", List.of("경제 동향 분석", "주식시장 현황", "부동산 시장 동향"),
            "사회", List.of("사회 이슈 분석", "교육 정책 변화", "복지 제도 개선"),
            "IT/과학", List.of("최신 기술 동향", "AI 발전 현황", "과학 연구 성과"),
            "세계", List.of("국제 정세 분석", "해외 주요 뉴스", "글로벌 경제 동향")
        );
        
        List<String> titles = sampleTitles.getOrDefault(category, 
            List.of("주요 뉴스", "이슈 분석", "최신 동향"));
        
        for (int i = 0; i < Math.min(limit, titles.size()); i++) {
            NewsletterContent.Article article = NewsletterContent.Article.builder()
                    .id((long) (i + 1))
                    .title(titles.get(i))
                    .summary(category + " 관련 주요 내용을 다룬 기사입니다.")
                    .url("https://example.com/news/" + (i + 1))
                    .category(category)
                    .publishedAt(LocalDateTime.now().minusHours(i + 1))
                    .isPersonalized(false)
                    .build();
            fallbackArticles.add(article);
        }
        
        log.info("폴백 기사 생성 완료: count={}", fallbackArticles.size());
        return fallbackArticles;
    }

    @Override
    public Map<String, Object> getCategoryArticlesWithTrendingKeywords(String category, int limit) {
        log.info("카테고리별 기사 및 트렌딩 키워드 조회: category={}, limit={}", category, limit);
        
        try {
            String englishCategory = convertCategoryToEnglish(category);
            NewsCategory newsCategory = NewsCategory.valueOf(englishCategory);
            
            Page<NewsResponse> newsResponse = newsServiceClient.getNewsByCategory(newsCategory.name(), 0, limit);
            Page<NewsResponse> newsPage = newsResponse;
            List<NewsResponse> newsList = newsPage.getContent();
            
            ApiResponse<List<TrendingKeywordDto>> trendingKeywordsResponse = newsServiceClient.getTrendingKeywordsByCategory(newsCategory.name(), limit, "24h", 24);
            List<TrendingKeywordDto> trendingKeywords = trendingKeywordsResponse.getData();
            
            Map<String, Object> result = new HashMap<>();
            result.put("articles", newsList);
            result.put("trendingKeywords", trendingKeywords.stream()
                    .map(TrendingKeywordDto::getKeyword)
                    .filter(this::isValidKeywordForNewsletter)
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
                Page<NewsResponse> response = newsServiceClient.getNewsByCategory(englishCategory, 0, articlesPerCategory + 2);
                Page<NewsResponse> newsPage = response;
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

    /**
     * Newsletter Service에서 사용할 키워드 유효성 검사 (추가 안전장치)
     */
    private boolean isValidKeywordForNewsletter(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        
        // 1. 최소 길이 체크
        if (keyword.length() < 2) {
            return false;
        }
        
        // 2. 추가적인 의미없는 단어들 필터링
        String[] additionalStopWords = {
            "없습니다", "추출할", "내용을", "영화의", "기사의", "뉴스의",
            "관련", "대한", "위해", "통해", "있는", "같은", "이런", "그런",
            "하는", "되는", "이되는"
        };
        
        for (String stopWord : additionalStopWords) {
            if (keyword.contains(stopWord)) {
                return false;
            }
        }
        
        // 3. 특수문자나 숫자만으로 구성된 키워드 제외
        if (keyword.matches("^[^가-힣A-Za-z]*$")) {
            return false;
        }
        
        return true;
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
            Page<NewsResponse> latestNews = latestResponse != null && latestResponse.isSuccess() ? latestResponse.getData() : null;
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
                .publishedAt(parsePublishedAt(news.getPublishedAt()))
                .imageUrl(news.getImageUrl())
                .viewCount(news.getViewCount() != null ? news.getViewCount().longValue() : 0L)
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
                LocalDateTime publishedAt = parsePublishedAt(news.getPublishedAt());
                if (publishedAt != null) {
                    html.append("                <span>").append(publishedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("</span>\n");
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
    
    /**
     * 한국어 카테고리를 영어 카테고리로 변환 (뉴스 서비스 API용)
     * 데이터베이스에서 동적으로 카테고리 정보를 가져옴
     */
    private String convertToEnglishCategory(String koreanCategory) {
        try {
            ApiResponse<List<CategoryDto>> response = newsServiceClient.getCategories();
            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData().stream()
                        .filter(category -> koreanCategory.equals(category.getCategoryName()))
                        .map(CategoryDto::getCategoryCode)
                        .findFirst()
                        .orElse("POLITICS"); // 기본값
            }
        } catch (Exception e) {
            log.warn("카테고리 정보 조회 실패, 기본값 사용: {}", e.getMessage());
        }
        
        // 폴백: 기본값 반환
        return "POLITICS";
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
