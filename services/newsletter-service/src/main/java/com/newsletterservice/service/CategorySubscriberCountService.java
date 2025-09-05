package com.newsletterservice.service;

import com.newsletterservice.entity.CategorySubscriberCount;
import com.newsletterservice.entity.NewsCategory;
import com.newsletterservice.entity.Subscription;
import com.newsletterservice.entity.SubscriptionStatus;
import com.newsletterservice.repository.CategorySubscriberCountRepository;
import com.newsletterservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategorySubscriberCountService {

    private final CategorySubscriberCountRepository categorySubscriberCountRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * 구독 생성/업데이트 시 카테고리별 구독자 수 증가
     */
    public void incrementCategorySubscriberCounts(Subscription subscription) {
        if (subscription.getPreferredCategories() == null || subscription.getPreferredCategories().isEmpty()) {
            log.warn("구독에 선호 카테고리가 없습니다. subscriptionId: {}", subscription.getId());
            return;
        }

        List<NewsCategory> categories = parseJsonToCategories(subscription.getPreferredCategories());
        
        for (NewsCategory category : categories) {
            try {
                CategorySubscriberCount countEntity = categorySubscriberCountRepository.findByCategory(category)
                        .orElseGet(() -> CategorySubscriberCount.builder()
                                .category(category)
                                .activeSubscriberCount(0L)
                                .totalSubscriberCount(0L)
                                .build());

                if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                    countEntity.incrementActiveCount();
                } else {
                    countEntity.incrementTotalCount();
                }

                categorySubscriberCountRepository.save(countEntity);
                
                log.info("카테고리 구독자 수 증가: category={}, activeCount={}, totalCount={}", 
                        category, countEntity.getActiveSubscriberCount(), countEntity.getTotalSubscriberCount());
                        
            } catch (Exception e) {
                log.error("카테고리 구독자 수 증가 실패: category={}, error={}", category, e.getMessage(), e);
            }
        }
    }

    /**
     * 구독 해제 시 카테고리별 구독자 수 감소
     */
    public void decrementCategorySubscriberCounts(Subscription subscription) {
        if (subscription.getPreferredCategories() == null || subscription.getPreferredCategories().isEmpty()) {
            log.warn("구독에 선호 카테고리가 없습니다. subscriptionId: {}", subscription.getId());
            return;
        }

        List<NewsCategory> categories = parseJsonToCategories(subscription.getPreferredCategories());
        
        for (NewsCategory category : categories) {
            try {
                CategorySubscriberCount countEntity = categorySubscriberCountRepository.findByCategory(category)
                        .orElse(null);

                if (countEntity != null) {
                    if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                        countEntity.decrementActiveCount();
                    } else {
                        countEntity.decrementTotalCount();
                    }

                    categorySubscriberCountRepository.save(countEntity);
                    
                    log.info("카테고리 구독자 수 감소: category={}, activeCount={}, totalCount={}", 
                            category, countEntity.getActiveSubscriberCount(), countEntity.getTotalSubscriberCount());
                } else {
                    log.warn("카테고리 구독자 수 엔티티가 존재하지 않습니다: category={}", category);
                }
                        
            } catch (Exception e) {
                log.error("카테고리 구독자 수 감소 실패: category={}, error={}", category, e.getMessage(), e);
            }
        }
    }

    /**
     * 구독 상태 변경 시 카테고리별 구독자 수 업데이트
     */
    public void updateCategorySubscriberCounts(Subscription oldSubscription, Subscription newSubscription) {
        // 기존 구독의 카테고리들에서 구독자 수 감소
        decrementCategorySubscriberCounts(oldSubscription);
        
        // 새로운 구독의 카테고리들에서 구독자 수 증가
        incrementCategorySubscriberCounts(newSubscription);
    }

    /**
     * 카테고리별 구독자 수 조회
     */
    public CategorySubscriberCount getCategorySubscriberCount(NewsCategory category) {
        return categorySubscriberCountRepository.findByCategory(category)
                .orElse(CategorySubscriberCount.builder()
                        .category(category)
                        .activeSubscriberCount(0L)
                        .totalSubscriberCount(0L)
                        .build());
    }

    /**
     * 모든 카테고리별 구독자 수 조회 (활성 구독자 수 기준 내림차순)
     */
    public List<CategorySubscriberCount> getAllCategorySubscriberCounts() {
        return categorySubscriberCountRepository.findAllOrderByActiveSubscriberCountDesc();
    }

    /**
     * 활성 구독자가 있는 카테고리만 조회
     */
    public List<CategorySubscriberCount> getActiveCategorySubscriberCounts() {
        return categorySubscriberCountRepository.findActiveCategoriesOrderBySubscriberCountDesc();
    }

    /**
     * 전체 활성 구독자 수 조회
     */
    public Long getTotalActiveSubscriberCount() {
        return categorySubscriberCountRepository.getTotalActiveSubscriberCount();
    }

    /**
     * 전체 총 구독자 수 조회
     */
    public Long getTotalSubscriberCount() {
        return categorySubscriberCountRepository.getTotalSubscriberCount();
    }

    /**
     * 카테고리별 구독자 수 통계 조회
     */
    public Map<String, Object> getCategorySubscriberStats() {
        List<CategorySubscriberCount> counts = getAllCategorySubscriberCounts();
        Long totalActive = getTotalActiveSubscriberCount();
        Long totalCount = getTotalSubscriberCount();

        Map<String, Object> stats = Map.of(
                "totalActiveSubscribers", totalActive != null ? totalActive : 0L,
                "totalSubscribers", totalCount != null ? totalCount : 0L,
                "categoryCounts", counts.stream()
                        .collect(Collectors.toMap(
                                count -> count.getCategory().name(),
                                count -> Map.of(
                                        "activeSubscribers", count.getActiveSubscriberCount(),
                                        "totalSubscribers", count.getTotalSubscriberCount(),
                                        "lastUpdated", count.getLastUpdated()
                                )
                        ))
        );

        return stats;
    }

    /**
     * 카테고리별 구독자 수 동기화 (기존 구독 데이터 기반으로 카운트 재계산)
     */
    @Transactional
    public void syncCategorySubscriberCounts() {
        log.info("카테고리별 구독자 수 동기화 시작");
        
        // 모든 카테고리에 대해 카운트 초기화
        for (NewsCategory category : NewsCategory.values()) {
            CategorySubscriberCount countEntity = CategorySubscriberCount.builder()
                    .category(category)
                    .activeSubscriberCount(0L)
                    .totalSubscriberCount(0L)
                    .build();
            
            categorySubscriberCountRepository.save(countEntity);
        }

        // 모든 활성 구독을 조회하여 카테고리별 카운트 계산
        List<Subscription> activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
        
        for (Subscription subscription : activeSubscriptions) {
            if (subscription.getPreferredCategories() != null && !subscription.getPreferredCategories().isEmpty()) {
                incrementCategorySubscriberCounts(subscription);
            }
        }

        log.info("카테고리별 구독자 수 동기화 완료");
    }

    /**
     * JSON 문자열을 NewsCategory 리스트로 파싱
     */
    private List<NewsCategory> parseJsonToCategories(String jsonCategories) {
        try {
            // JSON 배열에서 카테고리명 추출
            String[] categoryNames = jsonCategories
                    .replaceAll("[\\[\\]\"]", "")
                    .split(",");
            
            return java.util.Arrays.stream(categoryNames)
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .map(NewsCategory::valueOf)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("카테고리 JSON 파싱 실패: jsonCategories={}, error={}", jsonCategories, e.getMessage());
            return List.of();
        }
    }
}
