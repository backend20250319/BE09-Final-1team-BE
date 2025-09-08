package com.newnormallist.userservice.analytics.service;

import com.newnormallist.userservice.history.dto.*;
import com.newnormallist.userservice.history.entity.UserReadHistory;
import com.newnormallist.userservice.history.repository.UserReadHistoryRepository;
import com.newnormallist.userservice.user.repository.UserRepository;
import com.newnormallist.userservice.user.entity.User;
import com.newnormallist.userservice.common.exception.UserException;
import com.newnormallist.userservice.common.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAnalyticsService {

    private final UserRepository userRepository;
    private final UserReadHistoryRepository userReadHistoryRepository;

    /**
     * 사용자 행동 분석 조회
     */
    public UserBehaviorAnalysis getUserBehaviorAnalysis(Long userId) {
        log.info("사용자 행동 분석 시작: userId={}", userId);
        
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
        
        // 최근 30일간의 읽기 기록 분석
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        var recentHistory = userReadHistoryRepository.findByUserIdAndUpdatedAtAfter(userId, since);
        
        // 카테고리별 읽기 횟수 계산
        Map<String, Long> categoryReadCounts = recentHistory.stream()
                .filter(history -> history.getCategoryName() != null)
                .collect(Collectors.groupingBy(
                        history -> history.getCategoryName().name(),
                        Collectors.counting()
                ));
        
        // 카테고리 선호도 계산 (비율)
        Map<String, Double> categoryPreferences = calculateCategoryPreferences(categoryReadCounts);
        
        // 상위 카테고리 결정
        String topCategory = categoryPreferences.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        
        // 참여도 점수 계산
        double engagementScore = calculateEngagementScore(recentHistory);
        
        // 분석 요약 생성
        String analysisSummary = generateAnalysisSummary(categoryPreferences, engagementScore);
        
        return UserBehaviorAnalysis.builder()
                .userId(userId)
                .categoryReadCounts(categoryReadCounts)
                .categoryPreferences(categoryPreferences)
                .topCategory(topCategory)
                .totalReadCount((long) recentHistory.size())
                .engagementScore(engagementScore)
                .analysisSummary(analysisSummary)
                .build();
    }

    /**
     * 사용자 카테고리 선호도 조회
     */
    public CategoryPreferenceResponse getCategoryPreferences(Long userId) {
        UserBehaviorAnalysis behaviorAnalysis = getUserBehaviorAnalysis(userId);
        
        return CategoryPreferenceResponse.builder()
                .userId(userId)
                .categoryReadCounts(behaviorAnalysis.getCategoryReadCounts())
                .categoryPreferences(behaviorAnalysis.getCategoryPreferences())
                .topCategory(behaviorAnalysis.getTopCategory())
                .totalReadCount(behaviorAnalysis.getTotalReadCount())
                .build();
    }

    /**
     * 사용자 관심사 분석 조회
     */
    public UserInterestResponse getUserInterests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
        
        UserBehaviorAnalysis behaviorAnalysis = getUserBehaviorAnalysis(userId);
        
        // 사용자 가입 시 설정한 관심사 추출
        Set<String> signupInterests = extractSignupInterests(user);
        
        // 행동 기반 상위 관심사 추출 (최대 5개)
        List<String> topInterests = behaviorAnalysis.getCategoryPreferences().entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        // 관심사 분석 요약 생성
        String analysisSummary = generateInterestSummary(signupInterests, topInterests);
        
        return UserInterestResponse.builder()
                .userId(userId)
                .topInterests(topInterests)
                .interestScores(behaviorAnalysis.getCategoryPreferences())
                .analysisSummary(analysisSummary)
                .build();
    }

    /**
     * 사용자 관심사 점수 맵 조회
     */
    public Map<String, Double> getInterestScores(Long userId) {
        return getUserBehaviorAnalysis(userId).getCategoryPreferences();
    }

    /**
     * 사용자 상위 관심사 목록 조회
     */
    public List<String> getTopInterests(Long userId) {
        return getUserInterests(userId).getTopInterests();
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    private Map<String, Double> calculateCategoryPreferences(Map<String, Long> categoryReadCounts) {
        if (categoryReadCounts.isEmpty()) {
            return new HashMap<>();
        }
        
        long totalReads = categoryReadCounts.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Double> preferences = new HashMap<>();
        
        for (Map.Entry<String, Long> entry : categoryReadCounts.entrySet()) {
            double preference = (double) entry.getValue() / totalReads;
            preferences.put(entry.getKey(), preference);
        }
        
        return preferences;
    }

    private double calculateEngagementScore(List<UserReadHistory> recentHistory) {
        if (recentHistory.isEmpty()) {
            return 0.0;
        }
        
        // 기본 참여도: 읽은 뉴스 수 기반 (최대 1.0)
        double baseScore = Math.min(1.0, recentHistory.size() / 100.0);
        
        // 최신성 보너스: 최근 읽은 기록일수록 높은 점수
        LocalDateTime now = LocalDateTime.now();
        double recencyBonus = recentHistory.stream()
                .mapToDouble(history -> {
                    long daysAgo = java.time.temporal.ChronoUnit.DAYS.between(history.getUpdatedAt(), now);
                    return Math.max(0, 1.0 - (daysAgo / 30.0)); // 30일로 정규화
                })
                .average()
                .orElse(0.0);
        
        return Math.min(1.0, (baseScore * 0.7) + (recencyBonus * 0.3));
    }

    private String generateAnalysisSummary(Map<String, Double> categoryPreferences, double engagementScore) {
        if (categoryPreferences.isEmpty()) {
            return "읽기 기록이 부족하여 분석할 수 없습니다.";
        }
        
        String topCategory = categoryPreferences.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("없음");
        
        String engagementLevel = engagementScore > 0.7 ? "높음" : 
                                engagementScore > 0.4 ? "보통" : "낮음";
        
        return String.format("주요 관심 분야: %s, 참여도: %s", topCategory, engagementLevel);
    }

    private Set<String> extractSignupInterests(User user) {
        // 사용자 가입 시 설정한 취미/관심사에서 뉴스 카테고리 관련 항목 추출
        if (user.getHobbies() == null) {
            return new HashSet<>();
        }
        
        return user.getHobbies().stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private String generateInterestSummary(Set<String> signupInterests, List<String> behaviorInterests) {
        StringBuilder summary = new StringBuilder();
        
        if (!signupInterests.isEmpty()) {
            summary.append("가입 시 관심사: ").append(String.join(", ", signupInterests));
        }
        
        if (!behaviorInterests.isEmpty()) {
            if (summary.length() > 0) {
                summary.append(" | ");
            }
            List<String> topThree = behaviorInterests.subList(0, Math.min(3, behaviorInterests.size()));
            summary.append("행동 기반 관심사: ").append(String.join(", ", topThree));
        }
        
        return summary.length() > 0 ? summary.toString() : "관심사 정보가 부족합니다.";
    }
}
