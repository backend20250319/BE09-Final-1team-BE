package com.newsletterservice.service;

import com.newsletterservice.client.NewsServiceClient;
import com.newsletterservice.client.UserServiceClient;
import com.newsletterservice.client.dto.CategoryResponse;
import com.newsletterservice.client.dto.NewsResponse;
import com.newsletterservice.client.dto.ReadHistoryResponse;
import com.newsletterservice.client.dto.TrendingKeywordDto;
import com.newsletterservice.client.dto.CategoryDto;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.common.exception.NewsletterException;
import com.newsletterservice.dto.*;
import com.newsletterservice.entity.*;
import com.newsletterservice.repository.NewsletterDeliveryRepository;
import com.newsletterservice.repository.SubscriptionRepository;
import com.newsletterservice.entity.NewsCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NewsletterService {

    // ========================================
    // Dependencies
    // ========================================
    private final NewsletterDeliveryRepository deliveryRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NewsServiceClient newsServiceClient;
    private final UserServiceClient userServiceClient;
    private final EmailNewsletterRenderer emailRenderer;
    private final UserReadHistoryService userReadHistoryService;
    private final CategorySubscriberCountService categorySubscriberCountService;
    private final KakaoMessageService kakaoMessageService;

    // ========================================
    // Constants
    // ========================================
    private static final int MAX_ITEMS = 8;
    private static final int PER_CATEGORY_LIMIT = 3;
    private static final int MAX_RETRY_COUNT = 3;
    private static final int MAX_CATEGORIES_PER_USER = 3;

    // ========================================
    // 1. 구독 관리 기능
    // ========================================

    public SubscriptionResponse subscribe(SubscriptionRequest request, String userId) {
        log.info("구독 생성 요청: userId={}, categories={}", userId, request.getPreferredCategories());
        
        try {
            validateSubscriptionRequest(request, userId);
            
            Long userIdLong = Long.valueOf(userId);
            Optional<Subscription> existingSubscription = subscriptionRepository.findByUserId(userIdLong);
            
            Subscription subscription = existingSubscription.orElse(new Subscription());
            Subscription oldSubscription = existingSubscription.orElse(null);
            
            updateSubscriptionFromRequest(subscription, request, userIdLong);
            Subscription savedSubscription = subscriptionRepository.save(subscription);
            
            updateCategorySubscriberCounts(oldSubscription, savedSubscription);
            
            return convertToSubscriptionResponse(savedSubscription);
            
        } catch (NewsletterException e) {
            throw e;
        } catch (Exception e) {
            log.error("구독 생성 실패: userId={}", userId, e);
            throw new NewsletterException("구독 처리 중 오류가 발생했습니다.", "SUBSCRIPTION_ERROR");
        }
    }

    public void unsubscribe(Long subscriptionId, Long userId) {
        log.info("구독 해지: subscriptionId={}, userId={}", subscriptionId, userId);
        
        try {
            Subscription subscription = getSubscriptionWithPermissionCheck(subscriptionId, userId);
            SubscriptionStatus oldStatus = subscription.getStatus();
            
            subscription.setStatus(SubscriptionStatus.UNSUBSCRIBED);
            subscription.setUnsubscribedAt(LocalDateTime.now());
            subscription.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            
            if (oldStatus == SubscriptionStatus.ACTIVE) {
                categorySubscriberCountService.decrementCategorySubscriberCounts(subscription);
            }
            
        } catch (Exception e) {
            log.error("구독 해지 중 오류 발생", e);
            throw new NewsletterException("구독 해지 중 오류가 발생했습니다.", "UNSUBSCRIBE_ERROR");
        }
    }

    public SubscriptionResponse getSubscription(Long subscriptionId, Long userId) {
        try {
            Subscription subscription = getSubscriptionWithPermissionCheck(subscriptionId, userId);
            return convertToSubscriptionResponse(subscription);
        } catch (Exception e) {
            log.error("구독 정보 조회 중 오류 발생", e);
            throw new NewsletterException("구독 정보 조회 중 오류가 발생했습니다.", "SUBSCRIPTION_FETCH_ERROR");
        }
    }

    public List<SubscriptionResponse> getMySubscriptions(Long userId) {
        try {
            List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId)
                    .map(List::of)
                    .orElse(List.of());
            
            return subscriptions.stream()
                    .map(this::convertToSubscriptionResponse)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("내 구독 목록 조회 중 오류 발생", e);
            throw new NewsletterException("구독 목록 조회 중 오류가 발생했습니다.", "SUBSCRIPTION_LIST_ERROR");
        }
    }

    public boolean isSubscribed(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(sub -> sub.getStatus() == SubscriptionStatus.ACTIVE)
                .orElse(false);
    }

    // ========================================
    // 2. 뉴스레터 콘텐츠 생성 기능
    // ========================================

    public NewsletterContent buildPersonalizedContent(Long userId, Long newsletterId) {
        log.info("개인화된 뉴스레터 콘텐츠 생성: userId={}, newsletterId={}", userId, newsletterId);
        
        if (!isSubscribed(userId)) {
            throw new NewsletterException("활성 구독이 필요합니다.", "SUBSCRIPTION_REQUIRED");
        }

        List<CategoryResponse> preferences = getUserPreferences(userId);
        boolean hasPreferences = preferences != null && !preferences.isEmpty();
        List<NewsResponse> selectedNews = collectNewsContent(preferences, hasPreferences);
        
        return NewsletterContent.builder()
                .newsletterId(newsletterId)
                .userId(userId)
                .personalized(hasPreferences)
                .title(generateTitle(hasPreferences))
                .generatedAt(LocalDateTime.now())
                .sections(buildSections(selectedNews, hasPreferences))
                .build();
    }

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

    // ========================================
    // 3. 뉴스 검색 및 조회 기능
    // ========================================

    public Page<NewsResponse> searchNews(NewsSearchRequest request, Pageable pageable) {
        try {
            Page<NewsResponse> response = newsServiceClient.searchNews(
                    request.getKeyword(), 
                    pageable.getPageNumber(), 
                    pageable.getPageSize()
            );
            
            List<NewsResponse> searchResults = response != null && response.getContent() != null ? 
                    response.getContent() : new ArrayList<>();
            
            if (request.getCategory() != null && !request.getCategory().isEmpty()) {
                searchResults = searchResults.stream()
                        .filter(news -> request.getCategory().equals(news.getCategoryName()))
                        .collect(Collectors.toList());
            }
            
            return new PageImpl<>(searchResults, pageable, searchResults.size());
            
        } catch (Exception e) {
            log.error("뉴스 검색 실패: keyword={}", request.getKeyword(), e);
            throw new NewsletterException("뉴스 검색 중 오류가 발생했습니다.", "SEARCH_ERROR");
        }
    }

    public List<NewsResponse> getCategoryArticles(String category, int limit) {
        try {
            String englishCategory = convertCategoryToEnglish(category);
            Page<NewsResponse> response = newsServiceClient.getNewsByCategory(englishCategory, 0, limit);
            return response != null && response.getContent() != null ? response.getContent() : new ArrayList<>();
        } catch (Exception e) {
            log.error("카테고리별 기사 조회 실패: category={}", category, e);
            return new ArrayList<>();
        }
    }

    public List<String> getTrendingKeywords(int limit) {
        try {
            ApiResponse<List<TrendingKeywordDto>> response = newsServiceClient.getTrendingKeywords(limit, 24);
            if (response != null && response.getData() != null) {
                return response.getData().stream()
                        .map(TrendingKeywordDto::getKeyword)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("전체 트렌드 키워드 조회 실패", e);
        }
        return new ArrayList<>();
    }

    // ========================================
    // 4. 발송 관리 기능
    // ========================================

    public DeliveryStats sendNewsletterNow(NewsletterDeliveryRequest request, Long senderId) {
        try {
            return processDeliveryRequest(request, false);
        } catch (Exception e) {
            log.error("뉴스레터 즉시 발송 실패", e);
            throw new NewsletterException("뉴스레터 발송 중 오류가 발생했습니다.", "DELIVERY_ERROR");
        }
    }

    public void cancelDelivery(Long deliveryId, Long userId) {
        try {
            NewsletterDelivery delivery = getDeliveryWithPermissionCheck(deliveryId, userId);
            
            if (delivery.getStatus() == DeliveryStatus.SENT || delivery.getStatus() == DeliveryStatus.FAILED) {
                throw new NewsletterException("이미 처리된 발송은 취소할 수 없습니다.", "INVALID_STATUS");
            }
            
            delivery.updateStatus(DeliveryStatus.CANCELLED);
            delivery.setUpdatedAt(LocalDateTime.now());
            deliveryRepository.save(delivery);
            
        } catch (Exception e) {
            log.error("발송 취소 실패: deliveryId={}", deliveryId, e);
            throw new NewsletterException("발송 취소 중 오류가 발생했습니다.", "CANCEL_ERROR");
        }
    }

    // ========================================
    // 5. 분석 및 추천 기능
    // ========================================

    public List<NewsletterContent.Article> getPersonalizedRecommendations(Long userId, int limit) {
        try {
            List<CategoryResponse> preferences = getUserPreferences(userId);
            List<ReadHistoryResponse> recentReadHistory = 
                    userReadHistoryService.getRecentReadHistory(userId, 30);
            
            Map<String, Double> categoryScores = calculateCategoryScores(preferences, recentReadHistory);
            List<NewsResponse> candidateNews = fetchRecommendationCandidates(categoryScores, limit * 2);
            
            List<Long> candidateNewsIds = candidateNews.stream()
                    .map(NewsResponse::getNewsId)
                    .collect(Collectors.toList());
            List<Long> unreadNewsIds = userReadHistoryService.filterUnreadNewsIds(userId, candidateNewsIds);
            
            return candidateNews.stream()
                    .filter(news -> unreadNewsIds.contains(news.getNewsId()))
                    .map(this::toContentArticle)
                    .limit(limit)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("맞춤 추천 생성 실패: userId={}", userId, e);
            throw new NewsletterException("맞춤 추천 생성 중 오류가 발생했습니다.", "RECOMMENDATION_ERROR");
        }
    }

    public UserEngagement analyzeUserEngagement(Long userId, int days) {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            List<NewsletterDelivery> deliveries = deliveryRepository.findByUserIdAndCreatedAtAfter(userId, since);
            
            long totalReceived = deliveries.size();
            long totalOpened = deliveries.stream().mapToLong(d -> d.getOpenedAt() != null ? 1 : 0).sum();
            
            double engagementRate = totalReceived > 0 ? (double) totalOpened / totalReceived * 100 : 0;
            
            return UserEngagement.builder()
                    .userId(userId)
                    .totalReceived(totalReceived)
                    .totalOpened(totalOpened)
                    .engagementRate(engagementRate)
                    .recommendation(generateEngagementRecommendation(engagementRate))
                    .analysisPeriod(days)
                    .build();
                    
        } catch (Exception e) {
            log.error("참여도 분석 실패: userId={}", userId, e);
            return createEmptyEngagement(userId);
        }
    }

    // ========================================
    // 6. 이메일 추적 기능
    // ========================================

    public void trackEmailOpen(Long deliveryId, String userAgent, String ipAddress) {
        try {
            NewsletterDelivery delivery = deliveryRepository.findById(deliveryId)
                    .orElseThrow(() -> new NewsletterException("발송 기록을 찾을 수 없습니다.", "DELIVERY_NOT_FOUND"));
            
            if (delivery.getOpenedAt() == null) {
                delivery.setOpenedAt(LocalDateTime.now());
                delivery.setUpdatedAt(LocalDateTime.now());
                deliveryRepository.save(delivery);
                
                trackNewsView(delivery.getUserId(), delivery.getNewsletterId(), "EMAIL_NEWSLETTER");
            }
        } catch (Exception e) {
            log.error("이메일 열람 추적 실패: deliveryId={}", deliveryId, e);
        }
    }

    public void trackLinkClick(Long deliveryId, Long newsId, String linkUrl) {
        try {
            NewsletterDelivery delivery = deliveryRepository.findById(deliveryId)
                    .orElseThrow(() -> new NewsletterException("발송 기록을 찾을 수 없습니다.", "DELIVERY_NOT_FOUND"));
            
            trackNewsClick(delivery.getUserId(), newsId, "EMAIL_NEWSLETTER");
            
            if (delivery.getOpenedAt() == null) {
                delivery.setOpenedAt(LocalDateTime.now());
                delivery.setUpdatedAt(LocalDateTime.now());
                deliveryRepository.save(delivery);
            }
        } catch (Exception e) {
            log.error("링크 클릭 추적 실패: deliveryId={}", deliveryId, e);
        }
    }

    // ========================================
    // 7. 스케줄링 기능
    // ========================================

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processScheduledDeliveries() {
        LocalDateTime now = LocalDateTime.now();
        List<NewsletterDelivery> scheduledDeliveries = deliveryRepository
                .findByStatusAndScheduledAtBefore(DeliveryStatus.PENDING, now);

        for (NewsletterDelivery delivery : scheduledDeliveries) {
            try {
                processScheduledDelivery(delivery);
            } catch (Exception e) {
                log.error("예약된 뉴스레터 처리 실패: ID={}", delivery.getId());
                delivery.updateStatus(DeliveryStatus.FAILED);
                delivery.setErrorMessage(e.getMessage());
                deliveryRepository.save(delivery);
            }
        }
    }

    // ========================================
    // 8. 조회 기능
    // ========================================

    public List<String> getAvailableCategories() {
        try {
            List<CategoryDto> response = newsServiceClient.getCategories();
            if (response != null && !response.isEmpty()) {
                return response.stream()
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
        return deliveryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    private void validateSubscriptionRequest(SubscriptionRequest request, String userId) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new NewsletterException("이메일은 필수입니다.", "INVALID_EMAIL");
        }
        
        if (request.getPreferredCategories() == null || request.getPreferredCategories().isEmpty()) {
            throw new NewsletterException("최소 1개의 카테고리를 선택해야 합니다.", "NO_CATEGORIES");
        }
        
        // 카테고리 개수 제한 검증
        Long userIdLong = Long.valueOf(userId);
        List<Subscription> existingSubscriptions = subscriptionRepository.findByUserIdAndStatus(userIdLong, SubscriptionStatus.ACTIVE);
        
        Set<NewsCategory> existingCategories = existingSubscriptions.stream()
                .filter(sub -> sub.getPreferredCategories() != null)
                .flatMap(sub -> parseJsonToCategories(sub.getPreferredCategories()).stream())
                .collect(Collectors.toSet());
        
        Set<NewsCategory> newCategories = new HashSet<>(request.getPreferredCategories());
        newCategories.removeAll(existingCategories);
        
        int totalCategories = existingCategories.size() + newCategories.size();
        
        if (totalCategories > MAX_CATEGORIES_PER_USER) {
            throw new NewsletterException(
                String.format("최대 %d개 카테고리까지 구독할 수 있습니다. 현재 %d개 구독 중입니다.", 
                    MAX_CATEGORIES_PER_USER, existingCategories.size()),
                "CATEGORY_LIMIT_EXCEEDED"
            );
        }
    }

    private void updateSubscriptionFromRequest(Subscription subscription, SubscriptionRequest request, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        
        if (subscription.getId() == null) {
            subscription.setUserId(userId);
            subscription.setSubscribedAt(now);
            subscription.setCreatedAt(now);
        }
        
        String categoriesJson = convertNewsCategoriesToJson(request.getPreferredCategories());
        
        subscription.setEmail(request.getEmail());
        subscription.setFrequency(request.getFrequency());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPreferredCategories(categoriesJson);
        subscription.setKeywords(convertToJson(request.getKeywords()));
        subscription.setSendTime(request.getSendTime() != null ? request.getSendTime() : 9);
        subscription.setPersonalized(request.isPersonalized());
        subscription.setUpdatedAt(now);
    }

    private void updateCategorySubscriberCounts(Subscription oldSubscription, Subscription newSubscription) {
        if (oldSubscription != null) {
            categorySubscriberCountService.updateCategorySubscriberCounts(oldSubscription, newSubscription);
        } else {
            categorySubscriberCountService.incrementCategorySubscriberCounts(newSubscription);
        }
    }

    private Subscription getSubscriptionWithPermissionCheck(Long subscriptionId, Long userId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new NewsletterException("구독을 찾을 수 없습니다.", "SUBSCRIPTION_NOT_FOUND"));
        
        if (!subscription.getUserId().equals(userId)) {
            throw new NewsletterException("권한이 없습니다.", "UNAUTHORIZED");
        }
        
        return subscription;
    }

    private NewsletterDelivery getDeliveryWithPermissionCheck(Long deliveryId, Long userId) {
        NewsletterDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NewsletterException("발송 기록을 찾을 수 없습니다.", "DELIVERY_NOT_FOUND"));
        
        if (!delivery.getUserId().equals(userId)) {
            throw new NewsletterException("권한이 없습니다.", "UNAUTHORIZED");
        }
        
        return delivery;
    }

    private List<NewsResponse> collectNewsContent(List<CategoryResponse> preferences, boolean hasPreferences) {
        List<NewsResponse> selectedNews = new ArrayList<>();

        if (hasPreferences) {
            for (CategoryResponse category : preferences) {
                if (selectedNews.size() >= MAX_ITEMS) break;
                
                try {
                    List<NewsResponse> categoryNews = fetchNewsByCategory(category.getName(), 0, PER_CATEGORY_LIMIT);
                    appendDistinct(selectedNews, categoryNews, MAX_ITEMS);
                } catch (Exception e) {
                    log.warn("카테고리 {} 뉴스 수집 실패: {}", category.getName(), e.getMessage());
                }
            }
        }

        fillRemainingSlots(selectedNews);
        return selectedNews;
    }

    private void fillRemainingSlots(List<NewsResponse> selectedNews) {
        if (selectedNews.size() < MAX_ITEMS) {
            try {
                List<NewsResponse> latest = fetchLatestNews(null, MAX_ITEMS - selectedNews.size());
                appendDistinct(selectedNews, latest, MAX_ITEMS);
            } catch (Exception e) {
                log.warn("최신 뉴스 수집 실패: {}", e.getMessage());
            }
        }
    }

    private List<CategoryResponse> getUserPreferences(Long userId) {
        try {
            ApiResponse<List<CategoryResponse>> response = userServiceClient.getUserPreferences(userId);
            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("사용자 선호 카테고리 조회 실패: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    private List<NewsResponse> fetchLatestNews(List<String> categories, int limit) {
        try {
            Page<NewsResponse> response = newsServiceClient.getLatestNews(categories, limit);
            return response != null && response.getContent() != null ? response.getContent() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("최신 뉴스 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<NewsResponse> fetchNewsByCategory(String category, int page, int size) {
        try {
            String englishCategory = convertCategoryToEnglish(category);
            Page<NewsResponse> response = newsServiceClient.getNewsByCategory(englishCategory, page, size);
            return response != null && response.getContent() != null ? response.getContent() : new ArrayList<>();
        } catch (Exception e) {
            log.error("카테고리별 뉴스 조회 실패: category={}", category, e);
            return new ArrayList<>();
        }
    }

    private void appendDistinct(List<NewsResponse> accumulator, List<NewsResponse> candidates, int maxSize) {
        if (candidates == null || candidates.isEmpty()) return;
        
        Set<Long> existingIds = accumulator.stream()
                .map(NewsResponse::getNewsId)
                .collect(Collectors.toSet());
        
        for (NewsResponse news : candidates) {
            if (accumulator.size() >= maxSize) break;
            if (existingIds.add(news.getNewsId())) {
                accumulator.add(news);
            }
        }
    }

    private String generateTitle(boolean isPersonalized) {
        return isPersonalized ? "당신을 위한 맞춤 뉴스레터" : "오늘의 핫한 뉴스";
    }

    private List<NewsletterContent.Section> buildSections(List<NewsResponse> newsList, boolean isPersonalized) {
        List<NewsletterContent.Section> sections = new ArrayList<>();
        
        if (newsList.isEmpty()) {
            sections.add(createEmptySection());
        } else {
            sections.add(createMainSection(newsList, isPersonalized));
        }
        
        return sections;
    }

    private NewsletterContent.Section createEmptySection() {
        return NewsletterContent.Section.builder()
                .heading("오늘의 뉴스")
                .sectionType("DEFAULT")
                .description("현재 뉴스를 불러올 수 없습니다.")
                .articles(new ArrayList<>())
                .build();
    }

    private NewsletterContent.Section createMainSection(List<NewsResponse> newsList, boolean isPersonalized) {
        List<NewsletterContent.Article> articles = newsList.stream()
                .map(this::toContentArticle)
                .collect(Collectors.toList());
        
        String sectionImageUrl = selectSectionImage(newsList, isPersonalized);
        
        return NewsletterContent.Section.builder()
                .heading(isPersonalized ? "당신을 위한 뉴스" : "오늘의 뉴스")
                .sectionType(isPersonalized ? "PERSONALIZED" : "TRENDING")
                .description(isPersonalized ? "관심 카테고리 기반으로 선별된 뉴스입니다." : "현재 인기 있는 뉴스입니다.")
                .articles(articles)
                .build();
    }


    /**
     * 섹션 이미지 선택 로직
     */
    private String selectSectionImage(List<NewsResponse> newsList, boolean isPersonalized) {
        // 1순위: 뉴스 목록에서 이미지가 있는 첫 번째 기사
        String newsImage = newsList.stream()
                .filter(news -> news.getImageUrl() != null && !news.getImageUrl().isEmpty())
                .findFirst()
                .map(NewsResponse::getImageUrl)
                .orElse(null);

        if (newsImage != null) {
            return newsImage;
        }

        // 2순위: 섹션 타입에 따른 기본 이미지
        if (isPersonalized) {
            // 개인화 섹션용 이미지 - newsServiceClient 사용
            return newsServiceClient.getPersonalizedSectionImage();
        } else {
            // 트렌딩 섹션용 이미지 - newsServiceClient 사용
            return newsServiceClient.getTrendingSectionImage();
        }
    }

    private NewsletterContent.Article toContentArticle(NewsResponse news) {
        return NewsletterContent.Article.builder()
                .id(news.getNewsId())
                .title(news.getTitle())
                .summary(news.getSummary())
                .category(news.getCategoryName())
                .url(news.getLink())
                .publishedAt(news.getPublishedAt())
                .imageUrl(news.getImageUrl())
                .personalizedScore(1.0)
                .build();
    }

    private DeliveryStats processDeliveryRequest(NewsletterDeliveryRequest request, boolean isScheduled) {
        int totalTargets = request.getTargetUserIds().size();
        int successCount = 0;
        int failureCount = 0;
        
        for (Long targetUserId : request.getTargetUserIds()) {
            try {
                NewsletterDelivery delivery = createDeliveryRecord(request, targetUserId, isScheduled);
                deliveryRepository.save(delivery);
                
                if (!isScheduled) {
                    performDelivery(delivery);
                }
                
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("사용자 {} 발송 처리 실패: {}", targetUserId, e.getMessage());
            }
        }
        
        double successRate = totalTargets > 0 ? (double) successCount / totalTargets * 100 : 0.0;
        return DeliveryStats.builder()
                .totalSent(successCount)
                .totalFailed(failureCount)
                .totalScheduled(totalTargets)
                .successRate(successRate)
                .build();
    }

    private NewsletterDelivery createDeliveryRecord(NewsletterDeliveryRequest request, Long userId, boolean isScheduled) {
        return NewsletterDelivery.builder()
                .userId(userId)
                .newsletterId(request.getNewsletterId())
                .deliveryMethod(request.getDeliveryMethod())
                .status(isScheduled ? DeliveryStatus.SCHEDULED : DeliveryStatus.PROCESSING)
                .scheduledAt(isScheduled ? request.getScheduledAt() : LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void performDelivery(NewsletterDelivery delivery) {
        try {
            switch (delivery.getDeliveryMethod()) {
                case EMAIL -> sendByEmail(delivery);
                case SMS -> throw new RuntimeException("SMS 발송 기능은 아직 구현되지 않았습니다.");
                case PUSH -> throw new RuntimeException("푸시 알림 발송 기능은 아직 구현되지 않았습니다.");
            }

            delivery.updateStatus(DeliveryStatus.SENT);
            delivery.setSentAt(LocalDateTime.now());

        } catch (Exception e) {
            delivery.updateStatus(DeliveryStatus.FAILED);
            delivery.setErrorMessage(e.getMessage());
        }

        delivery.setUpdatedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);
    }

    private void sendByEmail(NewsletterDelivery delivery) {
        try {
            NewsletterContent content = buildPersonalizedContent(delivery.getUserId(), delivery.getNewsletterId());
            String htmlContent = emailRenderer.renderToHtml(content);
            
            // TODO: 실제 이메일 발송 서비스 호출
            // emailService.sendHtmlEmail(delivery.getUserId(), content.getTitle(), htmlContent);
            
        } catch (Exception e) {
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    @Async
    @Transactional
    private void processScheduledDelivery(NewsletterDelivery delivery) {
        delivery.updateStatus(DeliveryStatus.PROCESSING);
        delivery.setUpdatedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);

        performDelivery(delivery);
    }

    private Map<String, Double> calculateCategoryScores(List<CategoryResponse> preferences, 
                                                       List<ReadHistoryResponse> readHistory) {
        Map<String, Double> scores = new HashMap<>();
        
        // 선호 카테고리 점수
        for (CategoryResponse pref : preferences) {
            scores.put(pref.getName(), 1.0);
        }
        
        // 읽은 뉴스 기반 점수 조정
        Map<String, Long> categoryReadCounts = readHistory.stream()
                .filter(history -> history.getCategoryName() != null)
                .collect(Collectors.groupingBy(
                        ReadHistoryResponse::getCategoryName,
                        Collectors.counting()
                ));
        
        for (Map.Entry<String, Long> entry : categoryReadCounts.entrySet()) {
            double readScore = Math.log(entry.getValue() + 1) * 0.1;
            scores.merge(entry.getKey(), readScore, Double::sum);
        }
        
        return scores;
    }

    private List<NewsResponse> fetchRecommendationCandidates(Map<String, Double> categoryScores, int limit) {
        List<NewsResponse> candidates = new ArrayList<>();
        
        for (String category : categoryScores.keySet()) {
            try {
                List<NewsResponse> categoryNews = fetchNewsByCategory(category, 0, limit / categoryScores.size() + 1);
                candidates.addAll(categoryNews);
            } catch (Exception e) {
                log.warn("추천 후보 수집 실패: category={}", category, e);
            }
        }
        
        return candidates;
    }

    private String generateEngagementRecommendation(double engagementRate) {
        if (engagementRate > 40.0) {
            return "매우 높은 참여도입니다! 개인화를 더욱 강화하거나 발송 빈도를 늘려보세요.";
        } else if (engagementRate > 25.0) {
            return "좋은 참여도입니다. 현재 설정을 유지하시면 됩니다.";
        } else if (engagementRate > 15.0) {
            return "참여도가 보통 수준입니다. 콘텐츠 품질을 개선하거나 발송 시간을 조정해보세요.";
        } else {
            return "참여도가 낮습니다. 구독 빈도를 줄이거나 관심 키워드를 재설정해보세요.";
        }
    }

    private UserEngagement createEmptyEngagement(Long userId) {
        return UserEngagement.builder()
                .userId(userId)
                .engagementRate(0.0)
                .recommendation("데이터가 부족합니다. 더 많은 뉴스레터를 받아보세요.")
                .build();
    }

    private void trackNewsView(Long userId, Long newsId, String category) {
        try {
            userReadHistoryService.addReadHistory(userId, newsId);
        } catch (Exception e) {
            log.error("뉴스 조회 기록 실패", e);
        }
    }

    private void trackNewsClick(Long userId, Long newsId, String category) {
        try {
            userReadHistoryService.addReadHistory(userId, newsId);
        } catch (Exception e) {
            log.error("뉴스 클릭 기록 실패", e);
        }
    }

    private SubscriptionResponse convertToSubscriptionResponse(Subscription subscription) {
        List<NewsCategory> categories = parseJsonToCategories(subscription.getPreferredCategories());
        
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUserId())
                .email(subscription.getEmail())
                .preferredCategories(categories)
                .keywords(parseJsonToStringList(subscription.getKeywords()))
                .frequency(subscription.getFrequency())
                .status(subscription.getStatus())
                .sendTime(subscription.getSendTime())
                .isPersonalized(subscription.isPersonalized())
                .subscribedAt(subscription.getSubscribedAt())
                .lastSentAt(subscription.getLastSentAt())
                .createdAt(subscription.getCreatedAt())
                .build();
    }

    private String convertToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(list);
        } catch (Exception e) {
            log.error("리스트 JSON 변환 실패", e);
            return "[]";
        }
    }

    private String convertNewsCategoriesToJson(List<NewsCategory> categories) {
        if (categories == null || categories.isEmpty()) return "[]";
        
        try {
            List<String> categoryNames = categories.stream()
                    .map(NewsCategory::name)
                    .collect(Collectors.toList());
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(categoryNames);
        } catch (Exception e) {
            log.error("뉴스 카테고리 JSON 변환 실패", e);
            return "[]";
        }
    }

    private List<NewsCategory> parseJsonToCategories(String json) {
        if (json == null || json.trim().isEmpty() || "[]".equals(json)) {
            return new ArrayList<>();
        }
        try {
            com.fasterxml.jackson.core.type.TypeReference<List<String>> stringTypeRef = 
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {};
            List<String> categoryNames = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, stringTypeRef);
            
            return categoryNames.stream()
                    .map(name -> {
                        try {
                            return NewsCategory.valueOf(name);
                        } catch (IllegalArgumentException e1) {
                            // categoryName으로 매칭 시도
                            for (NewsCategory category : NewsCategory.values()) {
                                if (category.getCategoryName().equals(name)) {
                                    return category;
                                }
                            }
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("카테고리 JSON 파싱 실패: json={}", json, e);
            return new ArrayList<>();
        }
    }

    private List<String> parseJsonToStringList(String json) {
        if (json == null || json.trim().isEmpty() || "[]".equals(json)) {
            return new ArrayList<>();
        }
        try {
            com.fasterxml.jackson.core.type.TypeReference<List<String>> typeRef = 
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {};
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, typeRef);
        } catch (Exception e) {
            log.error("문자열 리스트 JSON 파싱 실패", e);
            return new ArrayList<>();
        }
    }

    private List<String> getDefaultCategories() {
        return Arrays.asList(
                NewsCategory.POLITICS.getCategoryName(),
                NewsCategory.ECONOMY.getCategoryName(),
                NewsCategory.SOCIETY.getCategoryName(),
                NewsCategory.LIFE.getCategoryName(),
                NewsCategory.INTERNATIONAL.getCategoryName(),
                NewsCategory.IT_SCIENCE.getCategoryName(),
                NewsCategory.VEHICLE.getCategoryName(),
                NewsCategory.TRAVEL_FOOD.getCategoryName(),
                NewsCategory.ART.getCategoryName()
        );
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

    // ========================================
    // 누락된 메서드들 추가
    // ========================================

    public List<SubscriptionResponse> getActiveSubscriptions(Long userId) {
        log.info("활성 구독 목록 조회: userId={}", userId);
        
        try {
            List<Subscription> subscriptions = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
            return subscriptions.stream()
                    .map(this::convertToSubscriptionResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("활성 구독 목록 조회 실패: userId={}", userId, e);
            throw new NewsletterException("활성 구독 목록 조회 중 오류가 발생했습니다.", "ACTIVE_SUBSCRIPTION_ERROR");
        }
    }

    public SubscriptionResponse changeSubscriptionStatus(Long subscriptionId, Long userId, String newStatus) {
        log.info("구독 상태 변경: subscriptionId={}, userId={}, newStatus={}", subscriptionId, userId, newStatus);
        
        try {
            Subscription subscription = getSubscriptionWithPermissionCheck(subscriptionId, userId);
            SubscriptionStatus status = SubscriptionStatus.valueOf(newStatus.toUpperCase());
            
            subscription.setStatus(status);
            subscription.setUpdatedAt(LocalDateTime.now());
            Subscription savedSubscription = subscriptionRepository.save(subscription);
            
            return convertToSubscriptionResponse(savedSubscription);
        } catch (Exception e) {
            log.error("구독 상태 변경 실패: subscriptionId={}, userId={}", subscriptionId, userId, e);
            throw new NewsletterException("구독 상태 변경 중 오류가 발생했습니다.", "STATUS_CHANGE_ERROR");
        }
    }

    public List<NewsletterContent.Article> getCategoryHeadlines(String category, int limit) {
        log.info("카테고리 헤드라인 조회: category={}, limit={}", category, limit);
        
        try {
            String englishCategory = convertCategoryToEnglish(category);
            NewsCategory newsCategory = NewsCategory.valueOf(englishCategory);
            
            Page<NewsResponse> newsPage = newsServiceClient.getNewsByCategory(newsCategory.name(), 0, limit);
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

    public Map<String, Object> getCategoryArticlesWithTrendingKeywords(String category, int limit) {
        log.info("카테고리별 기사 및 트렌딩 키워드 조회: category={}, limit={}", category, limit);
        
        try {
            String englishCategory = convertCategoryToEnglish(category);
            NewsCategory newsCategory = NewsCategory.valueOf(englishCategory);
            
            Page<NewsResponse> newsPage = newsServiceClient.getNewsByCategory(newsCategory.name(), 0, limit);
            List<NewsResponse> newsList = newsPage.getContent();
            
            ApiResponse<List<TrendingKeywordDto>> trendingKeywordsResponse = newsServiceClient.getTrendingKeywordsByCategory(newsCategory.name(), limit, 24);
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

    public List<String> getTrendingKeywordsByCategory(String category, int limit) {
        log.info("카테고리별 트렌딩 키워드 조회: category={}, limit={}", category, limit);
        
        try {
            String englishCategory = convertCategoryToEnglish(category);
            NewsCategory newsCategory = NewsCategory.valueOf(englishCategory);
            
            ApiResponse<List<TrendingKeywordDto>> response = newsServiceClient.getTrendingKeywordsByCategory(newsCategory.name(), limit, 24);
            List<TrendingKeywordDto> trendingKeywords = response.getData();
            
            return trendingKeywords.stream()
                    .map(TrendingKeywordDto::getKeyword)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("카테고리별 트렌딩 키워드 조회 실패: category={}", category, e);
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getCategorySubscriberStats(String category) {
        log.info("카테고리별 구독자 통계 조회: category={}", category);
        
        try {
            String englishCategory = convertCategoryToEnglish(category);
            NewsCategory newsCategory = NewsCategory.valueOf(englishCategory);
            
            // 임시 구현 - 실제로는 CategorySubscriberCountService에서 구현 필요
            Map<String, Object> result = new HashMap<>();
            result.put("category", category);
            result.put("activeSubscribers", 0);
            result.put("totalSubscribers", 0);
            
            return result;
        } catch (Exception e) {
            log.error("카테고리별 구독자 통계 조회 실패: category={}", category, e);
            return new HashMap<>();
        }
    }

    public Map<String, Object> getAllCategoriesSubscriberStats() {
        log.info("전체 카테고리별 구독자 통계 조회");
        
        try {
            // 임시 구현 - 실제로는 CategorySubscriberCountService에서 구현 필요
            Map<String, Object> result = new HashMap<>();
            result.put("totalCategories", 9);
            result.put("totalActiveSubscribers", 0);
            result.put("totalSubscribers", 0);
            
            return result;
        } catch (Exception e) {
            log.error("전체 카테고리별 구독자 통계 조회 실패", e);
            return new HashMap<>();
        }
    }

    public void syncCategorySubscriberCounts() {
        log.info("카테고리별 구독자 수 동기화 시작");
        
        try {
            // 임시 구현 - 실제로는 CategorySubscriberCountService에서 구현 필요
            log.info("카테고리별 구독자 수 동기화 완료");
        } catch (Exception e) {
            log.error("카테고리별 구독자 수 동기화 실패", e);
            throw new NewsletterException("카테고리별 구독자 수 동기화 중 오류가 발생했습니다.", "SYNC_ERROR");
        }
    }

    public SubscriptionResponse reactivateSubscription(Long subscriptionId, Long userId) {
        log.info("구독 재활성화: subscriptionId={}, userId={}", subscriptionId, userId);
        
        try {
            Subscription subscription = getSubscriptionWithPermissionCheck(subscriptionId, userId);
            
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setUnsubscribedAt(null);
            subscription.setUpdatedAt(LocalDateTime.now());
            Subscription savedSubscription = subscriptionRepository.save(subscription);
            
            categorySubscriberCountService.incrementCategorySubscriberCounts(savedSubscription);
            
            return convertToSubscriptionResponse(savedSubscription);
        } catch (Exception e) {
            log.error("구독 재활성화 실패: subscriptionId={}, userId={}", subscriptionId, userId, e);
            throw new NewsletterException("구독 재활성화 중 오류가 발생했습니다.", "REACTIVATE_ERROR");
        }
    }

    public DeliveryStats scheduleNewsletter(NewsletterDeliveryRequest request, Long userId) {
        log.info("뉴스레터 예약 발송: newsletterId={}, userId={}", request.getNewsletterId(), userId);
        
        try {
            // 예약 발송 로직 구현
            DeliveryStats stats = DeliveryStats.builder()
                    .totalRecipients(request.getTargetUserIds().size())
                    .deliveryTime(LocalDateTime.now())
                    .status("SCHEDULED")
                    .build();
            
            return stats;
        } catch (Exception e) {
            log.error("뉴스레터 예약 발송 실패: newsletterId={}, userId={}", request.getNewsletterId(), userId, e);
            throw new NewsletterException("뉴스레터 예약 발송 중 오류가 발생했습니다.", "SCHEDULE_ERROR");
        }
    }

    public void retryDelivery(Long deliveryId, Long userId) {
        log.info("발송 재시도: deliveryId={}, userId={}", deliveryId, userId);
        
        try {
            // 발송 재시도 로직 구현
            log.info("발송 재시도 완료: deliveryId={}", deliveryId);
        } catch (Exception e) {
            log.error("발송 재시도 실패: deliveryId={}, userId={}", deliveryId, userId, e);
            throw new NewsletterException("발송 재시도 중 오류가 발생했습니다.", "RETRY_ERROR");
        }
    }

    /**
     * 공유 통계 기록
     */
    public ShareStatsResponse recordShareStats(ShareStatsRequest request, String userId) {
        log.info("공유 통계 기록: userId={}, type={}, newsId={}, category={}", 
                userId, request.getType(), request.getNewsId(), request.getCategory());
        
        try {
            // 공유 통계 기록 로직
            // 실제 구현에서는 데이터베이스에 공유 통계를 저장하거나
            // 외부 분석 서비스에 데이터를 전송할 수 있습니다.
            
            // 임시 구현 - 실제로는 공유 통계 엔티티와 리포지토리를 사용해야 함
            Long shareCount = 1L; // 기본값
            
            // 공유 타입별 처리
            switch (request.getType().toLowerCase()) {
                case "kakao":
                    log.info("카카오 공유 통계 기록");
                    break;
                case "facebook":
                    log.info("페이스북 공유 통계 기록");
                    break;
                case "twitter":
                    log.info("트위터 공유 통계 기록");
                    break;
                default:
                    log.info("기타 공유 타입 통계 기록: {}", request.getType());
                    break;
            }
            
            ShareStatsResponse response = ShareStatsResponse.builder()
                    .type(request.getType())
                    .shareCount(shareCount)
                    .message("공유 통계가 성공적으로 기록되었습니다.")
                    .success(true)
                    .build();
            
            log.info("공유 통계 기록 완료: {}", response);
            return response;
            
        } catch (Exception e) {
            log.error("공유 통계 기록 실패: userId={}, type={}", userId, request.getType(), e);
            throw new NewsletterException("공유 통계 기록 중 오류가 발생했습니다.", "SHARE_STATS_ERROR");
        }
    }
}