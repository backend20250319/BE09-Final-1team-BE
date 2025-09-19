package com.newsletterservice.service;

import com.newsletterservice.client.*;
import com.newsletterservice.client.dto.*;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.common.exception.NewsletterException;
import com.newsletterservice.dto.*;
import com.newsletterservice.entity.*;
import com.newsletterservice.repository.UserNewsletterSubscriptionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.*;
import java.util.Optional;

/**
 * 뉴스레터 서비스 - 주요 기능만 담당 (SRP 준수)
 * 큰 클래스를 분리하여 각 서비스가 단일 책임을 갖도록 리팩토링
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NewsletterService {

    // ========================================
    // Dependencies - 분리된 서비스들 주입
    // ========================================
    private final NewsletterContentService contentService;
    private final NewsletterDeliveryService deliveryService;
    private final NewsletterAnalyticsService analyticsService;
    private final Optional<EmailService> emailService;
    private final FeedTemplateService feedTemplateService;
    private final Optional<KakaoMessageService> kakaoMessageService;
    
    private final NewsServiceClient newsServiceClient;
    private final UserServiceClient userServiceClient;
    private final UserNewsletterSubscriptionRepository subscriptionRepository;

    // ========================================
    // 1. 뉴스레터 발송 관리
    // ========================================

    // 구독 확인 기능은 user-service에서 처리됩니다.
    
    /**
     * 뉴스레터 구독 생성 (트랜잭션 처리)
     */
    @Transactional
    public UserNewsletterSubscription createSubscription(Long userId, String category, 
            String frequency, String sendTime, Boolean isPersonalized, String keywords) {
        UserNewsletterSubscription subscription = UserNewsletterSubscription.builder()
                .userId(userId)
                .category(category)
                .isActive(true)
                .frequency(frequency)
                .sendTime(sendTime)
                .isPersonalized(isPersonalized)
                .keywords(keywords)
                .build();
        
        return subscriptionRepository.save(subscription);
    }

    // ========================================
    // 2. 콘텐츠 생성 (위임)
    // ========================================

    public NewsletterContent buildPersonalizedContent(Long userId, Long newsletterId) {
        return contentService.buildPersonalizedContent(userId, newsletterId);
    }

    public NewsletterPreview generateNewsletterPreview(Long userId) {
        return contentService.generateNewsletterPreview(userId);
    }

    public String generatePersonalizedNewsletter(String userId) {
        return contentService.generatePersonalizedNewsletter(userId);
    }

    public String generatePreviewHtml(Long id) {
        return contentService.generatePreviewHtml(id);
    }

    public Map<String, Object> getPersonalizationInfo(Long userId) {
        return contentService.getPersonalizationInfo(userId);
    }

    // ========================================
    // 3. 발송 관리 (위임)
    // ========================================

    public DeliveryStats sendNewsletterNow(NewsletterDeliveryRequest request, Long senderId) {
        return deliveryService.sendNewsletterNow(request, senderId);
    }

    public DeliveryStats scheduleNewsletter(NewsletterDeliveryRequest request, Long userId) {
        return deliveryService.scheduleNewsletter(request, userId);
    }

    public void cancelDelivery(Long deliveryId, Long userId) {
        deliveryService.cancelDelivery(deliveryId, userId);
    }

    public void retryDelivery(Long deliveryId, Long userId) {
        deliveryService.retryDelivery(deliveryId, userId);
    }

    // ========================================
    // 4. 분석 및 추천 (위임)
    // ========================================

    public List<NewsletterContent.Article> getPersonalizedRecommendations(Long userId, int limit) {
        return analyticsService.getPersonalizedRecommendations(userId, limit);
    }

    public UserEngagement analyzeUserEngagement(Long userId, int days) {
        return analyticsService.analyzeUserEngagement(userId, days);
    }

    public ShareStatsResponse recordShareStats(ShareStatsRequest request, String userId) {
        return analyticsService.recordShareStats(request, userId);
    }

    // ========================================
    // 5. 뉴스 검색 및 조회 (간소화)
    // ========================================

    public Page<NewsResponse> searchNews(NewsSearchRequest request, Pageable pageable) {
        try {
            ApiResponse<Page<NewsResponse>> response = newsServiceClient.searchNews(
                    request.getKeyword(), 
                    pageable.getPageNumber(), 
                    pageable.getPageSize()
            );
            
            return handleNewsServiceResponse(response, pageable);
            
        } catch (Exception e) {
            log.error("뉴스 검색 실패: keyword={}", request.getKeyword(), e);
            throw new NewsletterException("뉴스 검색 중 오류가 발생했습니다.", "SEARCH_ERROR");
        }
    }

    public List<NewsResponse> getCategoryArticles(String category, int limit) {
        try {
            String englishCategory = convertCategoryToEnglish(category);
            Page<NewsResponse> response = newsServiceClient.getNewsByCategory(englishCategory, 0, limit);
            return response != null ? response.getContent() : new ArrayList<>();
        } catch (Exception e) {
            log.error("카테고리별 기사 조회 실패: category={}", category, e);
            return new ArrayList<>();
        }
    }

    public List<String> getTrendingKeywords(int limit) {
        try {
            ApiResponse<List<TrendingKeywordDto>> response = newsServiceClient.getTrendingKeywordsByCategory("GENERAL", limit, "24h", 24);
            if (response != null && response.isSuccess() && response.getData() != null && !response.getData().isEmpty()) {
                return response.getData().stream()
                        .map(TrendingKeywordDto::getKeyword)
                        .filter(Objects::nonNull)
                        .filter(this::isValidKeywordForNewsletter)
                        .collect(Collectors.toList());
            }
        } catch (feign.RetryableException e) {
            log.error("전체 트렌드 키워드 조회 재시도 실패: error={}", e.getMessage());
            return getFallbackGeneralKeywords(limit);
        } catch (Exception e) {
            log.warn("전체 트렌드 키워드 조회 실패: error={}", e.getMessage());
            return getFallbackGeneralKeywords(limit);
        }
        return new ArrayList<>();
    }

    public List<String> getTrendingKeywordsByCategory(String category, int limit) {
        try {
            String englishCategory = convertCategoryToEnglish(category);
            ApiResponse<List<TrendingKeywordDto>> response = newsServiceClient.getTrendingKeywordsByCategory(englishCategory, limit, "24h", 24);
            if (response != null && response.getData() != null) {
                return response.getData().stream()
                        .map(TrendingKeywordDto::getKeyword)
                        .filter(this::isValidKeywordForNewsletter)
                        .collect(Collectors.toList());
            }
        } catch (feign.RetryableException e) {
            log.error("카테고리별 트렌드 키워드 조회 재시도 실패: category={}, error={}", category, e.getMessage());
            return getFallbackKeywordsForCategory(category, limit);
        } catch (Exception e) {
            log.warn("카테고리별 트렌드 키워드 조회 실패: category={}, error={}", category, e.getMessage());
            return getFallbackKeywordsForCategory(category, limit);
        }
        return new ArrayList<>();
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
            "하는", "되는", "이되는", "되는", "되는", "되는", "되는"
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

    // ========================================
    // 7. 카테고리 및 통계 관리 (위임)
    // ========================================

    public List<NewsletterContent.Article> getCategoryHeadlines(String category, int limit) {
        return contentService.getCategoryHeadlines(category, limit);
    }

    public Map<String, Object> getCategoryArticlesWithTrendingKeywords(String category, int limit) {
        return contentService.getCategoryArticlesWithTrendingKeywords(category, limit);
    }

    public Map<String, Object> getCategorySubscriberStats(String category) {
        return analyticsService.getCategorySubscriberStats(category);
    }

    public Map<String, Object> getAllCategoriesSubscriberStats() {
        return analyticsService.getAllCategoriesSubscriberStats();
    }

    public void syncCategorySubscriberCounts() {
        analyticsService.syncCategorySubscriberCounts();
    }

    // ========================================
    // 8. 기타 관리 기능
    // ========================================

    public List<String> getAvailableCategories() {
        try {
            ApiResponse<List<CategoryDto>> response = newsServiceClient.getCategories();
            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                return response.getData().stream()
                        .map(CategoryDto::getCategoryName)
                        .collect(Collectors.toList());
            }
            return getDefaultCategories();
        } catch (Exception e) {
            log.warn("뉴스 서비스 카테고리 조회 실패, 기본 카테고리 반환", e);
            return getDefaultCategories();
        }
    }

    @Transactional(readOnly = true)
    public Page<NewsletterDelivery> getDeliveriesByUser(Long userId, Pageable pageable) {
        return deliveryService.getDeliveriesByUser(userId, pageable);
    }

    // ========================================
    // 9. 개발/테스트용 메서드
    // ========================================

    public Object getNewsletterById(Long id) {
        return contentService.getNewsletterById(id);
    }

    public Object createSampleNewsletter() {
        return contentService.createSampleNewsletter();
    }

    public Object getNewsletterList(int page, int size) {
        return contentService.getNewsletterList(page, size);
    }

    public Map<String, Object> testNewsletterGeneration(Long userId) {
        return contentService.testNewsletterGeneration(userId);
    }

    // ========================================
    // 10. 이메일 뉴스레터 전송
    // ========================================


    /**
     * 개인화된 이메일 뉴스레터 전송
     * 
     * @param userId 사용자 ID
     * @param newsletterId 뉴스레터 ID
     */
    public void sendPersonalizedEmailNewsletter(Long userId, Long newsletterId) {
        if (emailService.isEmpty()) {
            log.warn("EmailService가 사용할 수 없습니다. 이메일 뉴스레터 전송을 건너뜁니다.");
            return;
        }
        
        try {
            log.info("개인화된 이메일 뉴스레터 전송: userId={}, newsletterId={}", userId, newsletterId);

            // 개인화된 뉴스레터 콘텐츠 생성
            NewsletterContent content = contentService.buildPersonalizedContent(userId, newsletterId);
            
            // 이메일 템플릿 생성
            EmailTemplate template = emailService.get().createNewsletterTemplate(content);
            
            // 사용자 이메일 주소 조회 (user-service에서)
            List<String> userEmails = getUserEmailAddress(userId);
            
            if (userEmails.isEmpty()) {
                log.warn("사용자 이메일 주소를 찾을 수 없습니다: userId={}", userId);
                return;
            }

            // 이메일 전송
            emailService.get().sendBulkEmail(userEmails, template);
            
            log.info("개인화된 이메일 뉴스레터 전송 완료: userId={}, newsletterId={}", userId, newsletterId);

        } catch (Exception e) {
            log.error("개인화된 이메일 뉴스레터 전송 실패: userId={}, newsletterId={}", userId, newsletterId, e);
        }
    }

    /**
     * 테스트 이메일 전송
     * 
     * @param to 테스트 수신자 이메일
     * @param subject 테스트 제목
     * @param content 테스트 내용
     */
    public void sendTestEmail(String to, String subject, String content) {
        if (emailService.isEmpty()) {
            log.warn("EmailService가 사용할 수 없습니다. 테스트 이메일 전송을 건너뜁니다.");
            return;
        }
        emailService.get().sendTestEmail(to, subject, content);
    }

    /**
     * 이메일 뉴스레터 구독자 목록 조회
     * 
     * @return 구독자 이메일 주소 목록
     */
    public List<String> getEmailNewsletterSubscribers() {
        try {
            log.info("이메일 뉴스레터 구독자 목록 조회");
            ApiResponse<List<String>> response = userServiceClient.getEmailNewsletterSubscribers();
            
            if (response != null && response.getData() != null) {
                log.info("이메일 뉴스레터 구독자 수: {}", response.getData().size());
                return response.getData();
            }
            
            log.warn("이메일 뉴스레터 구독자 목록을 가져올 수 없습니다.");
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("이메일 뉴스레터 구독자 목록 조회 실패", e);
            return new ArrayList<>();
        }
    }

    /**
     * 읽기 기록 추가
     * 
     * @param userId 사용자 ID
     * @param newsId 뉴스 ID
     */
    public void addReadHistory(Long userId, Long newsId) {
        try {
            log.info("읽기 기록 추가: userId={}, newsId={}", userId, newsId);
            userServiceClient.addReadHistory(userId, newsId);
        } catch (Exception e) {
            log.error("읽기 기록 추가 실패: userId={}, newsId={}", userId, newsId, e);
        }
    }

    /**
     * 이메일 뉴스레터 전송
     * 
     * @param content 뉴스레터 콘텐츠
     */
    public void sendEmailNewsletter(NewsletterContent content) {
        if (emailService.isEmpty()) {
            log.warn("EmailService가 사용할 수 없습니다. 이메일 뉴스레터 전송을 건너뜁니다.");
            return;
        }
        
        try {
            log.info("이메일 뉴스레터 전송: newsletterId={}", content.getNewsletterId());
            
            // 이메일 템플릿 생성
            EmailTemplate template = emailService.get().createNewsletterTemplate(content);
            
            // 구독자 이메일 목록 조회 (user-service에서)
            List<String> userEmails = getEmailNewsletterSubscribers();
            
            if (userEmails.isEmpty()) {
                log.warn("이메일 뉴스레터 구독자가 없습니다.");
                return;
            }

            // 이메일 전송
            emailService.get().sendBulkEmail(userEmails, template);
            
            log.info("이메일 뉴스레터 전송 완료: newsletterId={}, recipientCount={}", 
                    content.getNewsletterId(), userEmails.size());

        } catch (Exception e) {
            log.error("이메일 뉴스레터 전송 실패: newsletterId={}", content.getNewsletterId(), e);
        }
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    /**
     * 특정 사용자의 이메일 주소 조회
     * 
     * @param userId 사용자 ID
     * @return 사용자 이메일 주소 목록 (단일 사용자이므로 1개 또는 0개)
     */
    private List<String> getUserEmailAddress(Long userId) {
        try {
            log.debug("사용자 이메일 주소 조회: userId={}", userId);
            ApiResponse<String> response = userServiceClient.getUserEmail(userId);
            
            if (response != null && response.getData() != null && !response.getData().trim().isEmpty()) {
                return List.of(response.getData());
            }
            
            log.warn("사용자 이메일 주소를 찾을 수 없습니다: userId={}", userId);
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("사용자 이메일 주소 조회 실패: userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    private Page<NewsResponse> handleNewsServiceResponse(ApiResponse<Page<NewsResponse>> response, Pageable pageable) {
        if (response != null && response.getData() != null) {
            return response.getData();
        }
        return Page.empty(pageable);
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

    private List<String> getDefaultCategories() {
        return Arrays.asList(
                "정치", "경제", "사회", "생활", "세계", 
                "IT/과학", "자동차/교통", "여행/음식", "예술"
        );
    }
    
    // ========================================
    // 5. 피드 B형 뉴스레터 전송 기능
    // ========================================
    
    /**
     * 피드 B형 개인화 뉴스레터 전송
     */
    public void sendPersonalizedFeedBNewsletter(Long userId, String accessToken) {
        if (kakaoMessageService.isEmpty()) {
            log.warn("KakaoMessageService가 사용할 수 없습니다. 피드 B형 개인화 뉴스레터 전송을 건너뜁니다.");
            return;
        }
        
        try {
            log.info("피드 B형 개인화 뉴스레터 전송 시작: userId={}", userId);
            
            // 피드 B형 템플릿 생성 및 전송
            kakaoMessageService.get().sendFeedBMessage(userId, accessToken);
            
            // 전송 기록 저장
            analyticsService.recordNewsletterDelivery(userId, "FEED_B_PERSONALIZED", true);
            
            log.info("피드 B형 개인화 뉴스레터 전송 완료: userId={}", userId);
            
        } catch (Exception e) {
            log.error("피드 B형 개인화 뉴스레터 전송 실패: userId={}", userId, e);
            analyticsService.recordNewsletterDelivery(userId, "FEED_B_PERSONALIZED", false);
            throw new NewsletterException("피드 B형 개인화 뉴스레터 전송에 실패했습니다.", "FEED_B_PERSONALIZED_SEND_ERROR");
        }
    }
    
    /**
     * 피드 B형 카테고리별 뉴스레터 전송
     */
    public void sendCategoryFeedBNewsletter(String category, String accessToken) {
        if (kakaoMessageService.isEmpty()) {
            log.warn("KakaoMessageService가 사용할 수 없습니다. 피드 B형 카테고리별 뉴스레터 전송을 건너뜁니다.");
            return;
        }
        
        try {
            log.info("피드 B형 카테고리별 뉴스레터 전송 시작: category={}", category);
            
            // 피드 B형 템플릿 생성 및 전송
            kakaoMessageService.get().sendCategoryFeedBMessage(category, accessToken);
            
            log.info("피드 B형 카테고리별 뉴스레터 전송 완료: category={}", category);
            
        } catch (Exception e) {
            log.error("피드 B형 카테고리별 뉴스레터 전송 실패: category={}", category, e);
            throw new NewsletterException("피드 B형 카테고리별 뉴스레터 전송에 실패했습니다.", "FEED_B_CATEGORY_SEND_ERROR");
        }
    }
    
    /**
     * 피드 B형 트렌딩 뉴스레터 전송
     */
    public void sendTrendingFeedBNewsletter(String accessToken) {
        if (kakaoMessageService.isEmpty()) {
            log.warn("KakaoMessageService가 사용할 수 없습니다. 피드 B형 트렌딩 뉴스레터 전송을 건너뜁니다.");
            return;
        }
        
        try {
            log.info("피드 B형 트렌딩 뉴스레터 전송 시작");
            
            // 피드 B형 템플릿 생성 및 전송
            kakaoMessageService.get().sendTrendingFeedBMessage(accessToken);
            
            log.info("피드 B형 트렌딩 뉴스레터 전송 완료");
            
        } catch (Exception e) {
            log.error("피드 B형 트렌딩 뉴스레터 전송 실패", e);
            throw new NewsletterException("피드 B형 트렌딩 뉴스레터 전송에 실패했습니다.", "FEED_B_TRENDING_SEND_ERROR");
        }
    }
    
    /**
     * 피드 B형 뉴스레터 미리보기 생성
     */
    public Map<String, Object> createFeedBPreview(String type, String param) {
        try {
            log.info("피드 B형 뉴스레터 미리보기 생성: type={}, param={}", type, param);
            
            FeedTemplate feedTemplate;
            
            switch (type.toLowerCase()) {
                case "personalized":
                    Long userId = Long.valueOf(param);
                    feedTemplate = feedTemplateService.createPersonalizedFeedTemplate(
                            userId, FeedTemplate.FeedType.FEED_B);
                    break;
                case "category":
                    feedTemplate = feedTemplateService.createCategoryFeedTemplate(
                            param, FeedTemplate.FeedType.FEED_B);
                    break;
                case "trending":
                    feedTemplate = feedTemplateService.createTrendingFeedTemplate(
                            FeedTemplate.FeedType.FEED_B);
                    break;
                default:
                    feedTemplate = feedTemplateService.createTrendingFeedTemplate(
                            FeedTemplate.FeedType.FEED_B);
            }
            
            Map<String, Object> previewData = new HashMap<>();
            previewData.put("success", true);
            previewData.put("type", type);
            previewData.put("param", param);
            previewData.put("feedType", feedTemplate.getFeedType().name());
            previewData.put("template", feedTemplate);
            previewData.put("kakaoArgs", feedTemplate.toKakaoTemplateArgs());
            previewData.put("timestamp", System.currentTimeMillis());
            
            log.info("피드 B형 뉴스레터 미리보기 생성 완료: type={}, param={}", type, param);
            return previewData;
            
        } catch (Exception e) {
            log.error("피드 B형 뉴스레터 미리보기 생성 실패: type={}, param={}", type, param, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "미리보기 생성 실패");
            errorResponse.put("timestamp", System.currentTimeMillis());
            return errorResponse;
        }
    }

    /**
     * 카테고리별 트렌드 키워드 조회 실패 시 사용할 fallback 키워드 제공
     */
    private List<String> getFallbackKeywordsForCategory(String category, int limit) {
        log.info("카테고리별 fallback 키워드 사용: category={}, limit={}", category, limit);
        
        // 카테고리별 기본 키워드 매핑
        Map<String, List<String>> fallbackKeywords = Map.of(
            "ECONOMY", List.of("경제", "금리", "인플레이션", "주식", "부동산", "환율", "고용", "성장"),
            "POLITICS", List.of("정치", "선거", "국회", "정부", "정책", "여당", "야당", "대통령"),
            "SOCIETY", List.of("사회", "교육", "복지", "건강", "환경", "안전", "문화", "생활"),
            "WORLD", List.of("국제", "외교", "무역", "글로벌", "유엔", "G7", "G20", "협정"),
            "TECHNOLOGY", List.of("기술", "AI", "반도체", "스마트폰", "인터넷", "디지털", "혁신", "스타트업"),
            "SPORTS", List.of("스포츠", "축구", "야구", "농구", "올림픽", "월드컵", "선수", "경기"),
            "ENTERTAINMENT", List.of("연예", "영화", "드라마", "음악", "가수", "배우", "예능", "방송")
        );
        
        String englishCategory = convertCategoryToEnglish(category);
        List<String> keywords = fallbackKeywords.getOrDefault(englishCategory, 
            List.of("뉴스", "이슈", "화제", "주목", "관심", "핫이슈", "트렌드", "이슈"));
        
        return keywords.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 전체 트렌드 키워드 조회 실패 시 사용할 fallback 키워드 제공
     */
    private List<String> getFallbackGeneralKeywords(int limit) {
        log.info("전체 fallback 키워드 사용: limit={}", limit);
        
        // 일반적인 인기 키워드들
        List<String> generalKeywords = List.of(
            "경제", "정치", "사회", "기술", "스포츠", "연예", "국제", "환경", 
            "교육", "건강", "부동산", "주식", "AI", "반도체", "디지털", "혁신"
        );
        
        return generalKeywords.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}
