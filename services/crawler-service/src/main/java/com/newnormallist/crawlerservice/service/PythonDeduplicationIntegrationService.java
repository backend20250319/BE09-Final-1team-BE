package com.newnormallist.crawlerservice.service;

import com.newnormallist.crawlerservice.client.PythonDeduplicationClient;
import com.newnormallist.crawlerservice.client.dto.DeduplicationResponse;
import com.newnormallist.crawlerservice.dto.NewsDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Python 중복제거 서비스 연동 서비스
 * 
 * 역할:
 * - Java MSA와 Python FastAPI 중복제거 서비스 간 통신 브리지
 * - 파일서버 기반 중복제거 프로세스 관리
 * - 마이크로서비스 간 데이터 플로우 조율
 * 
 * 기능:
 * - Python 서비스 호출: HTTP 클라이언트를 통한 중복제거 API 호출
 * - 배치 처리: 전체 카테고리에 대한 일괄 중복제거 실행
 * - 오류 처리: 네트워크 오류, 타임아웃, 서비스 장애 대응
 * - 로깅 및 모니터링: 중복제거 프로세스 상태 추적
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PythonDeduplicationIntegrationService {

    private final PythonDeduplicationClient pythonClient;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * 전체 카테고리 중복제거 실행
     * 기존 runDeduplication() 메서드를 대체
     */
    public Map<String, DeduplicationResponse> runFileServerDeduplication() {
        try {
            log.info("🐍 Python 기반 중복제거 시작");
            
            // Python 서비스 상태 확인
            if (!pythonClient.isHealthy()) {
                log.warn("⚠️ Python 중복제거 서비스 상태 불량, 계속 진행...");
            }
            
            String[] categories = {"POLITICS", "ECONOMY", "SOCIETY", "LIFE", "INTERNATIONAL", 
                                    "IT_SCIENCE", "VEHICLE", "TRAVEL_FOOD", "ART"};
            
            Map<String, DeduplicationResponse> results = new HashMap<>();
            
            // 각 카테고리별 중복제거 실행
            for (String category : categories) {
                try {
                    DeduplicationResponse response = pythonClient.runDeduplication(category);
                    results.put(category, response);
                    
                    log.info("✅ {} 카테고리 중복제거 완료: {}개 → {}개 (제거율: {:.1f}%)", 
                        category, 
                        response.getOriginalCount(), 
                        response.getDeduplicatedCount(),
                        response.getRemovalRate() * 100);
                        
                } catch (Exception e) {
                    log.error("❌ {} 카테고리 중복제거 실패: {}", category, e.getMessage());
                    
                    // 실패 시 기본 응답 생성
                    DeduplicationResponse failureResponse = DeduplicationResponse.builder()
                        .category(category)
                        .originalCount(0)
                        .deduplicatedCount(0)
                        .relatedCount(0)
                        .removedCount(0)
                        .processingTimeSeconds(0.0)
                        .statistics(Map.of("error", e.getMessage()))
                        .message("처리 실패: " + e.getMessage())
                        .build();
                    
                    results.put(category, failureResponse);
                }
            }
            
            // 전체 통계 로깅
            logOverallStatistics(results);
            
            log.info("🎉 Python 기반 중복제거 완료");
            return results;
            
        } catch (Exception e) {
            log.error("❌ Python 중복제거 전체 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Python 중복제거 실패", e);
        }
    }

    /**
     * 비동기 전체 카테고리 중복제거
     */
    public CompletableFuture<Map<String, DeduplicationResponse>> runDeduplicationAsync() {
        return CompletableFuture.supplyAsync(this::runFileServerDeduplication, executorService);
    }

    /**
     * 단일 카테고리 중복제거
     */
    public DeduplicationResponse runDeduplicationForCategory(String category) {
        try {
            log.info("🔍 {} 카테고리 중복제거 시작", category);
            
            DeduplicationResponse response = pythonClient.runDeduplication(category);
            
            log.info("✅ {} 카테고리 중복제거 완료: {}개 → {}개", 
                category, response.getOriginalCount(), response.getDeduplicatedCount());
            
            return response;
            
        } catch (Exception e) {
            log.error("❌ {} 카테고리 중복제거 실패: {}", category, e.getMessage());
            throw new RuntimeException(String.format("%s 카테고리 중복제거 실패", category), e);
        }
    }

    /**
     * 중복제거된 뉴스 조회 (기존 getDeduplicatedNews 대체)
     */
    public List<Object> getDeduplicatedNews(String category) {
        try {
            return pythonClient.getDeduplicatedNews(category);
        } catch (Exception e) {
            log.error("중복제거된 뉴스 조회 실패: 카테고리={}, 오류={}", category, e.getMessage());
            return List.of();
        }
    }

    /**
     * 연관뉴스 조회 (기존 getRelatedNews 대체)
     */
    public List<Object> getRelatedNews(String category) {
        try {
            return pythonClient.getRelatedNews(category);
        } catch (Exception e) {
            log.error("연관뉴스 조회 실패: 카테고리={}, 오류={}", category, e.getMessage());
            return List.of();
        }
    }

    /**
     * Python 서비스 상태 확인
     */
    public boolean isPythonServiceHealthy() {
        return pythonClient.isHealthy();
    }

    /**
     * Python 서비스 통계 조회
     */
    public Map<String, Object> getPythonServiceStats() {
        return pythonClient.getStats();
    }

    /**
     * 일괄 중복제거 백그라운드 실행
     */
    public void runBatchDeduplicationInBackground() {
        CompletableFuture.runAsync(() -> {
            try {
                pythonClient.runBatchDeduplication();
            } catch (Exception e) {
                log.error("백그라운드 일괄 중복제거 실패: {}", e.getMessage());
            }
        }, executorService);
    }

    /**
     * 전체 통계 로깅
     */
    private void logOverallStatistics(Map<String, DeduplicationResponse> results) {
        int totalOriginal = 0;
        int totalDeduplicated = 0;
        int totalRelated = 0;
        int totalRemoved = 0;
        double totalProcessingTime = 0.0;
        int successfulCategories = 0;

        for (DeduplicationResponse response : results.values()) {
            if (response.isSuccessful()) {
                totalOriginal += response.getOriginalCount();
                totalDeduplicated += response.getDeduplicatedCount();
                totalRelated += response.getRelatedCount();
                totalRemoved += response.getRemovedCount();
                totalProcessingTime += response.getProcessingTimeSeconds();
                successfulCategories++;
            }
        }

        double overallRemovalRate = totalOriginal > 0 ? (double) totalRemoved / totalOriginal * 100 : 0.0;
        double avgProcessingTime = successfulCategories > 0 ? totalProcessingTime / successfulCategories : 0.0;

        log.info("📊 중복제거 전체 통계:");
        log.info("   ✅ 성공한 카테고리: {}/{}", successfulCategories, results.size());
        log.info("   📄 전체 원본: {}개", totalOriginal);
        log.info("   🎯 중복제거 후: {}개", totalDeduplicated);
        log.info("   🔗 연관뉴스: {}개", totalRelated);
        log.info("   🗑️ 제거된 뉴스: {}개 ({:.1f}%)", totalRemoved, overallRemovalRate);
        log.info("   ⏱️ 평균 처리시간: {:.2f}초", avgProcessingTime);
    }

    /**
     * 서비스 종료 시 리소스 정리
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            log.info("PythonDeduplicationIntegrationService ExecutorService 종료");
        }
    }
}
