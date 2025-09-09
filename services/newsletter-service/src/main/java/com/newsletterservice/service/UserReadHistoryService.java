package com.newsletterservice.service;

import com.newsletterservice.client.dto.ReadHistoryResponse;
import com.newsletterservice.client.UserServiceClient;


import com.newsletterservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UserService의 UserReadHistory를 활용하는 서비스
 * NewsletterService에서 UserNewsInteraction 대신 사용
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserReadHistoryService {

    private final UserServiceClient userServiceClient;

    // ========================================
    // 1. 기본 조회 기능
    // ========================================

    /**
     * 사용자가 읽은 뉴스 ID 목록 조회
     */
    public List<Long> getReadNewsIds(Long userId, int page, int size) {
        try {
            ApiResponse<List<Long>> response = userServiceClient.getReadNewsIds(userId, page, size);
            return response != null && response.getData() != null ? response.getData() : List.of();
        } catch (Exception e) {
            log.error("읽은 뉴스 ID 목록 조회 실패: userId={}", userId, e);
            return List.of();
        }
    }

    /**
     * 사용자가 읽은 뉴스 기록 조회 (페이징)
     */
    public Page<ReadHistoryResponse> getReadHistory(Long userId, Pageable pageable) {
        try {
            ApiResponse<Page<ReadHistoryResponse>> response = userServiceClient.getReadHistory(
                    userId, 
                    pageable.getPageNumber(), 
                    pageable.getPageSize(),
                    "updatedAt,desc"
            );
            
            if (response != null && response.getData() != null) {
                return response.getData();
            }
            
            // 빈 페이지 반환
            return Page.empty(pageable);
        } catch (Exception e) {
            log.error("읽은 뉴스 기록 조회 실패: userId={}", userId, e);
            return Page.empty(pageable);
        }
    }

    /**
     * 특정 뉴스를 읽었는지 확인
     */
    public boolean hasReadNews(Long userId, Long newsId) {
        try {
            ApiResponse<Boolean> response = userServiceClient.hasReadNews(userId, newsId);
            return response != null && response.getData() != null ? response.getData() : false;
        } catch (Exception e) {
            log.error("뉴스 읽음 여부 확인 실패: userId={}, newsId={}", userId, newsId, e);
            return false;
        }
    }

    /**
     * 뉴스 읽음 기록 추가
     */
    public void addReadHistory(Long userId, Long newsId) {
        try {
            userServiceClient.addReadHistory(userId, newsId);
            log.debug("뉴스 읽음 기록 추가 완료: userId={}, newsId={}", userId, newsId);
        } catch (Exception e) {
            log.error("뉴스 읽음 기록 추가 실패: userId={}, newsId={}", userId, newsId, e);
            // 읽음 기록 추가 실패는 사용자 경험에 영향을 주지 않도록 예외를 던지지 않음
        }
    }

    // ========================================
    // 2. 분석 및 추천 기능
    // ========================================

    /**
     * 사용자의 최근 읽은 뉴스 기록 조회 (일수 기준)
     */
    public List<ReadHistoryResponse> getRecentReadHistory(Long userId, int days) {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            
            // 충분히 큰 페이지로 조회하여 필터링
            Pageable pageable = PageRequest.of(0, 1000);
            Page<ReadHistoryResponse> historyPage = getReadHistory(userId, pageable);
            
            return historyPage.getContent().stream()
                    .filter(history -> history.getUpdatedAt().isAfter(since))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("최근 읽은 뉴스 기록 조회 실패: userId={}, days={}", userId, days, e);
            return List.of();
        }
    }

    /**
     * 카테고리별 읽은 뉴스 수 계산
     */
    public Map<String, Long> getCategoryReadCounts(Long userId, int days) {
        List<ReadHistoryResponse> recentHistory = getRecentReadHistory(userId, days);
        
        return recentHistory.stream()
                .filter(history -> history.getCategoryName() != null)
                .collect(Collectors.groupingBy(
                        ReadHistoryResponse::getCategoryName,
                        Collectors.counting()
                ));
    }

    /**
     * 사용자의 선호 카테고리 분석 (읽은 뉴스 기반)
     */
    public List<String> getPreferredCategories(Long userId, int days, int limit) {
        Map<String, Long> categoryCounts = getCategoryReadCounts(userId, days);
        
        return categoryCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 사용자가 읽지 않은 뉴스 ID 목록 필터링
     */
    public List<Long> filterUnreadNewsIds(Long userId, List<Long> candidateNewsIds) {
        if (candidateNewsIds.isEmpty()) {
            return List.of();
        }

        try {
            // 사용자가 읽은 뉴스 ID 목록 조회 (충분히 큰 페이지)
            List<Long> readNewsIds = getReadNewsIds(userId, 0, 1000);
            
            // 읽지 않은 뉴스만 필터링
            return candidateNewsIds.stream()
                    .filter(newsId -> !readNewsIds.contains(newsId))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("읽지 않은 뉴스 필터링 실패: userId={}", userId, e);
            return candidateNewsIds; // 필터링 실패 시 원본 반환
        }
    }

    /**
     * 사용자의 읽은 뉴스 통계
     */
    public UserReadStats getUserReadStats(Long userId, int days) {
        List<ReadHistoryResponse> recentHistory = getRecentReadHistory(userId, days);
        
        Map<String, Long> categoryCounts = getCategoryReadCounts(userId, days);
        String mostReadCategory = categoryCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        
        return UserReadStats.builder()
                .userId(userId)
                .totalReadCount(recentHistory.size())
                .categoryReadCounts(categoryCounts)
                .mostReadCategory(mostReadCategory)
                .analysisPeriod(days)
                .build();
    }

    // ========================================
    // 3. 내부 데이터 클래스
    // ========================================

    /**
     * 사용자 읽은 뉴스 통계 DTO
     */
    public static class UserReadStats {
        private final Long userId;
        private final int totalReadCount;
        private final Map<String, Long> categoryReadCounts;
        private final String mostReadCategory;
        private final int analysisPeriod;

        public UserReadStats(Long userId, int totalReadCount, Map<String, Long> categoryReadCounts, 
                           String mostReadCategory, int analysisPeriod) {
            this.userId = userId;
            this.totalReadCount = totalReadCount;
            this.categoryReadCounts = categoryReadCounts;
            this.mostReadCategory = mostReadCategory;
            this.analysisPeriod = analysisPeriod;
        }

        // Builder 패턴
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Long userId;
            private int totalReadCount;
            private Map<String, Long> categoryReadCounts;
            private String mostReadCategory;
            private int analysisPeriod;

            public Builder userId(Long userId) {
                this.userId = userId;
                return this;
            }

            public Builder totalReadCount(int totalReadCount) {
                this.totalReadCount = totalReadCount;
                return this;
            }

            public Builder categoryReadCounts(Map<String, Long> categoryReadCounts) {
                this.categoryReadCounts = categoryReadCounts;
                return this;
            }

            public Builder mostReadCategory(String mostReadCategory) {
                this.mostReadCategory = mostReadCategory;
                return this;
            }

            public Builder analysisPeriod(int analysisPeriod) {
                this.analysisPeriod = analysisPeriod;
                return this;
            }

            public UserReadStats build() {
                return new UserReadStats(userId, totalReadCount, categoryReadCounts, mostReadCategory, analysisPeriod);
            }
        }

        // Getters
        public Long getUserId() { return userId; }
        public int getTotalReadCount() { return totalReadCount; }
        public Map<String, Long> getCategoryReadCounts() { return categoryReadCounts; }
        public String getMostReadCategory() { return mostReadCategory; }
        public int getAnalysisPeriod() { return analysisPeriod; }
    }
}
