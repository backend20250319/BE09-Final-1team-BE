package com.newnormallist.crawlerservice.controller;

import com.newnormallist.crawlerservice.service.DeploymentOptimizedCrawlerService;
import com.newnormallist.crawlerservice.service.FileServerDatabaseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.HttpStatus;

/**
 * 크롤링 컨트롤러
 * 
 * 역할:
 * - 뉴스 크롤링 API 엔드포인트 제공
 * - 크롤링 상태 및 설정 정보 조회
 * - 배포 환경에 최적화된 크롤링 프로세스 관리
 * 
 * 기능:
 * - POST /api/crawler/start: 크롤링 시작 (비동기)
 * - POST /api/crawler/save-fileserver: 파일서버 데이터 DB 저장
 * - GET /api/crawler/status: 크롤링 상태 확인
 * - GET /api/crawler/config: 크롤러 설정 조회
 * - GET /api/crawler/health: 헬스체크
 */
@Tag(name = "Crawler", description = "뉴스 크롤링 및 데이터 수집 API")
@Slf4j
@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
public class CrawlerController {

    private final DeploymentOptimizedCrawlerService deploymentOptimizedCrawlerService;
    private final FileServerDatabaseService fileServerDatabaseService;

    @Operation(summary = "크롤링 시작", description = "배포 환경에 최적화된 뉴스 크롤링을 시작합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "크롤링 시작 성공"),
            @ApiResponse(responseCode = "500", description = "크롤링 시작 실패")
    })
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startCrawling() {
        try {
            log.info("배포 환경 최적화 크롤링 시작 요청");
            
            // 비동기로 실행
            CompletableFuture.runAsync(() -> {
                try {
                    deploymentOptimizedCrawlerService.runDeploymentOptimizedCrawling();
                } catch (Exception e) {
                    log.error("배포 환경 최적화 크롤링 실패: {}", e.getMessage(), e);
                }
            });
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "배포 환경 최적화 크롤링이 시작되었습니다.");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("배포 환경 최적화 크롤링 시작 실패: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "크롤링 시작 실패: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Operation(summary = "파일서버 데이터 저장", description = "파일서버에 있는 뉴스 데이터를 DB에 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 시작 성공"),
            @ApiResponse(responseCode = "500", description = "저장 시작 실패")
    })
    @PostMapping("/save-fileserver")
    public ResponseEntity<Map<String, Object>> saveFileserverData() {
        try {
            log.info("파일서버 데이터 DB 저장 시작 요청");
            
            // 비동기로 실행
            CompletableFuture.runAsync(() -> {
                try {
                    fileServerDatabaseService.saveLatestDataToDatabase();
                } catch (Exception e) {
                    log.error("파일서버 데이터 DB 저장 실패: {}", e.getMessage(), e);
                }
            });
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "파일서버 데이터 DB 저장이 시작되었습니다.");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("파일서버 데이터 DB 저장 시작 실패: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "파일서버 데이터 DB 저장 시작 실패: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Operation(summary = "크롤링 상태 확인", description = "현재 크롤러 서비스의 상태를 확인합니다.")
    @ApiResponse(responseCode = "200", description = "상태 조회 성공")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCrawlingStatus() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("status", "running");
        response.put("message", "크롤러 서비스가 실행 중입니다.");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "crawler-service");
        response.put("port", "8083");
        response.put("deployment-optimized", true);
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "크롤러 설정 조회", description = "크롤러의 현재 설정값들을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "설정 조회 성공")
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getCrawlerConfig() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("targetCount", 100);
        response.put("batchSize", 10);
        response.put("maxConcurrentRequests", 5);
        response.put("retryAttempts", 3);
        response.put("retryDelay", 3000);
        response.put("requestDelay", 1000);
        response.put("categories", new String[]{"POLITICS", "ECONOMY", "SOCIETY", "LIFE", "INTERNATIONAL", "IT_SCIENCE", "VEHICLE", "TRAVEL_FOOD", "ART"});
        response.put("deployment-optimized", true);
        response.put("fileserver-based-deduplication", true);
        response.put("scheduling-enabled", true);
        response.put("schedule", "09:00, 19:00 (Asia/Seoul)");
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "헬스 체크", description = "크롤러 서비스의 헬스 상태를 확인합니다.")
    @ApiResponse(responseCode = "200", description = "헬스 체크 성공")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("status", "UP");
        response.put("service", "crawler-service");
        response.put("deployment-optimized", true);
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
}
