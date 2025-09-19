package com.newnormallist.crawlerservice.client;

import com.newnormallist.crawlerservice.client.dto.DeduplicationRequest;
import com.newnormallist.crawlerservice.client.dto.DeduplicationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Python 중복제거 서비스 HTTP 클라이언트
 * 
 * 역할:
 * - Java MSA와 Python FastAPI 서비스 간 HTTP 통신
 * - WebClient 기반 비동기 요청 처리
 * - 마이크로서비스 간 데이터 전송 및 응답 처리
 * 
 * 기능:
 * - 중복제거 API 호출: 카테고리별/일괄 중복제거 요청
 * - 데이터 조회: 중복제거된 뉴스 및 연관뉴스 조회
 * - 헬스체크: Python 서비스 상태 모니터링
 * - 통계 조회: 중복제거 처리 통계 수집
 * - 오류 처리: 네트워크 오류, 타임아웃, 재시도 로직
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PythonDeduplicationClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.dedup.url:http://localhost:8084}")
    private String dedupServiceUrl;

    @Value("${services.dedup.timeout:300}")
    private int timeoutSeconds;

    private WebClient webClient;

    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder
                .baseUrl(dedupServiceUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
        }
        return webClient;
    }

    /**
     * 단일 카테고리 중복제거 실행 (기본 메서드)
     */
    public DeduplicationResponse runDeduplication(String category) {
        return runDeduplication(category, null);
    }
    
    /**
     * 단일 카테고리 중복제거 실행 (타임스탬프 지정)
     */
    public DeduplicationResponse runDeduplication(String category, String fileTimestamp) {
        try {
            log.info("🐍 Python 중복제거 서비스 호출: 카테고리={}", category);
            
            DeduplicationRequest request = DeduplicationRequest.builder()
                .category(category) // 이미 문자열이어야 함
                .forceRefresh(false)
                .build();

            DeduplicationResponse response = getWebClient()
                .post()
                .uri("/api/v1/deduplicate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DeduplicationResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                    .filter(throwable -> !(throwable instanceof WebClientResponseException.BadRequest)))
                .block();

            if (response != null) {
                log.info("✅ Python 중복제거 완료: 카테고리={}, 원본={}개 → 결과={}개, 연관뉴스={}개, 처리시간={}초",
                    category, response.getOriginalCount(), response.getDeduplicatedCount(), 
                    response.getRelatedCount(), String.format("%.2f", response.getProcessingTimeSeconds()));
                return response;
            } else {
                throw new RuntimeException("Python 서비스 응답 없음");
            }

        } catch (WebClientResponseException e) {
            log.error("❌ Python 중복제거 서비스 HTTP 오류: 카테고리={}, 상태코드={}, 응답={}",
                category, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(String.format("Python 서비스 HTTP 오류 [%s]: %s", 
                e.getStatusCode(), e.getResponseBodyAsString()));
        } catch (Exception e) {
            log.error("❌ Python 중복제거 서비스 호출 실패: 카테고리={}, 오류={}", category, e.getMessage());
            throw new RuntimeException("Python 서비스 호출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 전체 카테고리 일괄 중복제거 실행 (백그라운드)
     */
    public void runBatchDeduplication() {
        try {
            log.info("🔄 Python 일괄 중복제거 서비스 호출");

            ResponseEntity<Map> response = getWebClient()
                .post()
                .uri("/api/v1/deduplicate/batch")
                .retrieve()
                .toEntity(Map.class)
                .timeout(Duration.ofSeconds(30)) // 일괄 처리는 짧은 타임아웃
                .block();

            if (response != null && response.getStatusCode() == HttpStatus.OK) {
                log.info("✅ Python 일괄 중복제거 시작됨: {}", response.getBody());
            } else {
                log.warn("⚠️ Python 일괄 중복제거 응답 이상: {}", response);
            }

        } catch (Exception e) {
            log.error("❌ Python 일괄 중복제거 호출 실패: {}", e.getMessage());
            // 일괄 처리는 실패해도 전체 플로우를 중단하지 않음
        }
    }

    /**
     * 중복제거된 뉴스 조회
     */
    public List<Object> getDeduplicatedNews(String category) {
        try {
            log.debug("중복제거된 뉴스 조회: 카테고리={}", category);

            Map<String, Object> response = getWebClient()
                .get()
                .uri("/api/v1/categories/{category}/deduplicated", category)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

            if (response != null && response.containsKey("news")) {
                @SuppressWarnings("unchecked")
                List<Object> newsList = (List<Object>) response.get("news");
                log.debug("중복제거된 뉴스 조회 완료: 카테고리={}, 개수={}", category, newsList.size());
                return newsList;
            }

            return List.of();

        } catch (Exception e) {
            log.error("중복제거된 뉴스 조회 실패: 카테고리={}, 오류={}", category, e.getMessage());
            return List.of();
        }
    }

    /**
     * 연관뉴스 조회
     */
    public List<Object> getRelatedNews(String category) {
        try {
            log.debug("연관뉴스 조회: 카테고리={}", category);

            Map<String, Object> response = getWebClient()
                .get()
                .uri("/api/v1/categories/{category}/related", category)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

            if (response != null && response.containsKey("related_news")) {
                @SuppressWarnings("unchecked")
                List<Object> relatedList = (List<Object>) response.get("related_news");
                log.debug("연관뉴스 조회 완료: 카테고리={}, 개수={}", category, relatedList.size());
                return relatedList;
            }

            return List.of();

        } catch (Exception e) {
            log.error("연관뉴스 조회 실패: 카테고리={}, 오류={}", category, e.getMessage());
            return List.of();
        }
    }

    /**
     * Python 서비스 헬스체크
     */
    public boolean isHealthy() {
        try {
            Map<String, Object> response = getWebClient()
                .get()
                .uri("/health")
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(10))
                .block();

            if (response != null && "healthy".equals(response.get("status"))) {
                Boolean sbertLoaded = (Boolean) response.get("sbert_loaded");
                
                log.debug("Python 서비스 상태: SBERT={}", sbertLoaded);
                return Boolean.TRUE.equals(sbertLoaded);
            }

            return false;

        } catch (Exception e) {
            log.warn("Python 서비스 헬스체크 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Python 서비스 통계 조회
     */
    public Map<String, Object> getStats() {
        try {
            return getWebClient()
                .get()
                .uri("/stats")
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(10))
                .block();

        } catch (Exception e) {
            log.error("Python 서비스 통계 조회 실패: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 서비스 URL 동적 변경 (개발/테스트용)
     */
    public void setServiceUrl(String url) {
        this.dedupServiceUrl = url;
        this.webClient = null; // 재생성 강제
        log.info("Python 서비스 URL 변경: {}", url);
    }
}
