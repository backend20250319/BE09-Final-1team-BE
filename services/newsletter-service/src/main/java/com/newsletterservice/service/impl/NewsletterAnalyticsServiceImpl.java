package com.newsletterservice.service.impl;

import com.newsletterservice.client.NewsServiceClient;
import com.newsletterservice.client.UserServiceClient;
import com.newsletterservice.client.dto.*;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.common.exception.NewsletterException;
import com.newsletterservice.dto.NewsletterContent;
import com.newsletterservice.dto.ShareStatsRequest;
import com.newsletterservice.dto.ShareStatsResponse;
import com.newsletterservice.dto.UserEngagement;
import com.newsletterservice.entity.NewsCategory;
import com.newsletterservice.entity.NewsletterDelivery;
import com.newsletterservice.repository.NewsletterDeliveryRepository;
import com.newsletterservice.service.NewsletterAnalyticsService;

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
 * 분석 및 통계 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NewsletterAnalyticsServiceImpl implements NewsletterAnalyticsService {

    private final NewsServiceClient newsServiceClient;
    private final UserServiceClient userServiceClient;
    private final NewsletterDeliveryRepository deliveryRepository;

    @Override
    public List<NewsletterContent.Article> getPersonalizedRecommendations(Long userId, int limit) {
        try {
            List<CategoryResponse> preferences = getUserPreferences(userId);
            // 최근 읽기 기록 조회
            ApiResponse<Page<ReadHistoryResponse>> historyResponse = userServiceClient.getReadHistory(userId, 0, 100, "updatedAt,desc");
            List<ReadHistoryResponse> recentReadHistory = new ArrayList<>();
            
            if (historyResponse != null && historyResponse.getData() != null) {
                List<ReadHistoryResponse> allHistory = historyResponse.getData().getContent();
                // 최근 30일간의 기록만 필터링
                LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
                recentReadHistory = allHistory.stream()
                        .filter(history -> history.getUpdatedAt().isAfter(cutoff))
                        .collect(Collectors.toList());
            }
            
            Map<String, Double> categoryScores = calculateCategoryScores(preferences, recentReadHistory);
            List<NewsResponse> candidateNews = fetchRecommendationCandidates(categoryScores, limit * 2);
            
            // 읽은 뉴스 ID 조회
            ApiResponse<List<Long>> readNewsResponse = userServiceClient.getReadNewsIds(userId, 0, 1000);
            Set<Long> readNewsIds = new HashSet<>();
            
            if (readNewsResponse != null && readNewsResponse.getData() != null) {
                readNewsIds.addAll(readNewsResponse.getData());
            }
            
            return candidateNews.stream()
                    .filter(news -> !readNewsIds.contains(news.getNewsId()))
                    .map(this::toContentArticle)
                    .limit(limit)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("맞춤 추천 생성 실패: userId={}", userId, e);
            throw new NewsletterException("맞춤 추천 생성 중 오류가 발생했습니다.", "RECOMMENDATION_ERROR");
        }
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public void recordNewsletterDelivery(Long userId, String newsletterType, boolean success) {
        log.info("뉴스레터 발송 통계 기록: userId={}, type={}, success={}", userId, newsletterType, success);
        
        try {
            // 뉴스레터 발송 통계 기록 로직
            // 실제 구현에서는 데이터베이스에 발송 통계를 저장하거나
            // 외부 분석 서비스에 데이터를 전송할 수 있습니다.
            
            // 임시 구현 - 실제로는 NewsletterDeliveryStats 엔티티와 리포지토리를 사용해야 함
            if (success) {
                log.info("뉴스레터 발송 성공 통계 기록: userId={}, type={}", userId, newsletterType);
                // 성공 통계 기록
            } else {
                log.warn("뉴스레터 발송 실패 통계 기록: userId={}, type={}", userId, newsletterType);
                // 실패 통계 기록
            }
            
            log.info("뉴스레터 발송 통계 기록 완료: userId={}, type={}, success={}", userId, newsletterType, success);
            
        } catch (Exception e) {
            log.error("뉴스레터 발송 통계 기록 실패: userId={}, type={}, success={}", userId, newsletterType, success, e);
            // 통계 기록 실패는 전체 프로세스를 중단시키지 않도록 예외를 던지지 않음
        }
    }

    @Override
    public void syncNewsletterDeliveryStats() {
        log.info("뉴스레터 발송 통계 동기화 시작");
        
        try {
            // 뉴스레터 발송 통계 동기화 로직
            // 실제 구현에서는 데이터베이스의 발송 통계를 정리하거나
            // 외부 분석 서비스와 동기화할 수 있습니다.
            
            // 임시 구현 - 실제로는 NewsletterDeliveryStats 엔티티와 리포지토리를 사용해야 함
            log.info("뉴스레터 발송 통계 동기화 완료");
            
        } catch (Exception e) {
            log.error("뉴스레터 발송 통계 동기화 실패", e);
            throw new NewsletterException("뉴스레터 발송 통계 동기화 중 오류가 발생했습니다.", "SYNC_DELIVERY_STATS_ERROR");
        }
    }

    // Private Helper Methods
    private List<CategoryResponse> getUserPreferences(Long userId) {
        try {
            log.info("사용자 선호도 조회 시작: userId={}", userId);
            ApiResponse<List<CategoryResponse>> response = userServiceClient.getUserPreferences(userId);
            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                log.info("사용자 선호도 조회 성공: userId={}, categories={}", userId, response.getData().size());
                return response.getData();
            }
            log.warn("사용자 선호도가 비어있음: userId={}", userId);
        } catch (Exception e) {
            log.warn("사용자 선호 카테고리 조회 실패: userId={}, error={}", userId, e.getMessage());
        }
        
        // 선호도가 없는 경우 기본 카테고리들로 뉴스 제공
        List<CategoryResponse> defaultPreferences = createDefaultPreferences();
        log.info("기본 선호도 사용: userId={}, categories={}", userId, defaultPreferences.size());
        return defaultPreferences;
    }

    private List<CategoryResponse> createDefaultPreferences() {
        List<CategoryResponse> defaults = new ArrayList<>();
        
        CategoryResponse politics = new CategoryResponse();
        politics.setName("POLITICS");
        defaults.add(politics);
        
        CategoryResponse economy = new CategoryResponse();
        economy.setName("ECONOMY");
        defaults.add(economy);
        
        CategoryResponse society = new CategoryResponse();
        society.setName("SOCIETY");
        defaults.add(society);
        
        return defaults;
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
                String englishCategory = convertCategoryToEnglish(category);
                Page<NewsResponse> response = newsServiceClient.getNewsByCategory(englishCategory, 0, limit / categoryScores.size() + 1);
                Page<NewsResponse> newsPage = response;
                
                List<NewsResponse> categoryNews = newsPage != null && newsPage.getContent() != null ? 
                    newsPage.getContent() : new ArrayList<>();
                candidates.addAll(categoryNews);
            } catch (Exception e) {
                log.warn("추천 후보 수집 실패: category={}", category, e);
            }
        }
        
        return candidates;
    }


    private NewsletterContent.Article toContentArticle(NewsResponse news) {
        return NewsletterContent.Article.builder()
                .id(news.getNewsId())
                .title(news.getTitle())
                .summary(news.getSummary())
                .category(news.getCategoryName())
                .url(news.getLink())
                .publishedAt(parsePublishedAt(news.getPublishedAt()))
                .imageUrl(news.getImageUrl())
                .personalizedScore(1.0)
                .build();
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
