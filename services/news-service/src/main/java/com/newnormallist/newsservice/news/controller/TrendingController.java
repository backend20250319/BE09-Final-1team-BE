package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.NewsListResponse;
import com.newnormallist.newsservice.news.dto.NewsResponse;
import com.newnormallist.newsservice.news.dto.TrendingKeywordDto;
import com.newnormallist.newsservice.news.service.NewsService;
import com.newnormallist.newsservice.news.service.TrendingService;
import com.newnormallist.newsservice.recommendation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.newnormallist.newsservice.news.entity.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag(name = "Trending", description = "트렌드/랭킹 API")
@RestController
@RequestMapping("/api/trending")
@CrossOrigin(origins = "*")
public class TrendingController {
    
    private static final Logger log = LoggerFactory.getLogger(TrendingController.class);

    @Autowired
    private NewsService newsService;
    @Autowired
    private TrendingService trendingService;

    /**
     * 트렌딩 뉴스 (페이징)
     */
    @Operation(
        summary = "트렌딩 뉴스",
        description = "트렌딩 뉴스를 페이지로 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "트렌딩 뉴스 조회 성공")
    @GetMapping
    public ResponseEntity<Page<NewsListResponse>> getTrendingNews(@ParameterObject Pageable pageable) {
        Page<NewsListResponse> news = newsService.getTrendingNews(pageable);
        return ResponseEntity.ok(news);
    }

    /**
     * 트렌딩 뉴스 (리스트)
     */
    @Operation(
        summary = "트렌딩 뉴스 리스트",
        description = "트렌딩 뉴스 목록을 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "트렌딩 뉴스 리스트 조회 성공")
    @GetMapping("/list")
    public ResponseEntity<List<NewsResponse>> getTrendingNewsList() {
        List<NewsResponse> news = newsService.getTrendingNews();
        return ResponseEntity.ok(news);
    }

    /**
     * 실시간 인기 키워드 조회
     */
    @Operation(
        summary = "실시간 인기 키워드",
        description = "지정된 기간 내 인기 키워드를 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인기 키워드 조회 성공")
    @GetMapping("/trending-keywords")
    public ResponseEntity<ApiResponse<List<TrendingKeywordDto>>> getTrendingKeywords(
            @Parameter(description = "반환 개수", schema = @Schema(defaultValue = "10")) 
            @RequestParam(defaultValue = "10") int limit,
            
            @Parameter(description = "집계 기간", schema = @Schema(defaultValue = "24h", allowableValues = {"1h", "6h", "12h", "24h"})) 
            @RequestParam(defaultValue = "24h") String period,
            
            @Parameter(description = "집계 시간(시간)", schema = @Schema(example = "24")) 
            @RequestParam(required = false) Integer hours
    ) {
        int windowHours = (hours != null) ? hours : parsePeriodToHours(period);
        List<TrendingKeywordDto> result = trendingService.getTrendingKeywords(windowHours, limit);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 카테고리별 트렌딩 키워드 조회
     */
    @Operation(
        summary = "카테고리별 트렌딩 키워드",
        description = "특정 카테고리의 트렌딩 키워드를 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "카테고리별 트렌딩 키워드 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 카테고리")
    @GetMapping("/trending-keywords/category/{categoryName}")
    public ResponseEntity<ApiResponse<List<TrendingKeywordDto>>> getTrendingKeywordsByCategory(
            @Parameter(
                name = "categoryName", 
                description = "카테고리", 
                schema = @Schema(allowableValues = {
                    "POLITICS","ECONOMY","SOCIETY","LIFE","INTERNATIONAL",
                    "IT_SCIENCE","VEHICLE","TRAVEL_FOOD","ART"
                })
            )
            @PathVariable("categoryName") String categoryName,
            
            @Parameter(description = "반환 개수", schema = @Schema(defaultValue = "8")) 
            @RequestParam(defaultValue = "8") int limit,
            
            @Parameter(description = "집계 시간(시간)", schema = @Schema(defaultValue = "24")) 
            @RequestParam(defaultValue = "24") int hours
    ) {
        log.info("카테고리별 트렌딩 키워드 조회 요청: category={}, limit={}, hours={}", categoryName, limit, hours);
        
        try {
            Category category = Category.valueOf(categoryName.toUpperCase());
            log.info("카테고리 변환 성공: {} -> {}", categoryName, category);
            
            List<TrendingKeywordDto> result = newsService.getTrendingKeywordsByCategory(category, limit);
            log.info("트렌딩 키워드 조회 결과: category={}, resultSize={}", category, result.size());
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IllegalArgumentException e) {
            log.error("유효하지 않은 카테고리: {}", categoryName);
            return ResponseEntity.badRequest()
                .body(ApiResponse.fail("유효하지 않은 카테고리입니다: " + categoryName));
        } catch (Exception e) {
            log.error("카테고리별 트렌딩 키워드 조회 실패: category={}", categoryName, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("트렌딩 키워드 조회 중 오류가 발생했습니다."));
        }
    }

    private int parsePeriodToHours(String period) {
        try {
            String p = period.trim().toLowerCase();
            if (p.endsWith("h")) return Integer.parseInt(p.substring(0, p.length()-1));
            if (p.endsWith("m")) return Math.max(1, Integer.parseInt(p.substring(0, p.length()-1)) / 60);
            return Integer.parseInt(p);
        } catch (Exception e) {
            return 24;
        }
    }

    /**
     * 인기 뉴스 (조회수 기반)
     */
    @Operation(
        summary = "인기 뉴스",
        description = "조회수 기반 인기 뉴스를 페이지로 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인기 뉴스 조회 성공")
    @GetMapping("/popular")
    public ResponseEntity<Page<NewsListResponse>> getPopularNews(@ParameterObject Pageable pageable) {
        Page<NewsListResponse> news = newsService.getPopularNews(pageable);
        return ResponseEntity.ok(news);
    }

    /**
     * 최신 뉴스
     */
    @Operation(
        summary = "최신 뉴스",
        description = "최신 뉴스를 페이지로 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "최신 뉴스 조회 성공")
    @GetMapping("/latest")
    public ResponseEntity<Page<NewsListResponse>> getLatestNews(@ParameterObject Pageable pageable) {
        Page<NewsListResponse> news = newsService.getLatestNews(pageable);
        return ResponseEntity.ok(news);
    }

    /**
     * 트렌딩 키워드 조회
     */
    @Operation(
        summary = "트렌딩 키워드",
        description = "트렌딩 키워드를 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "트렌딩 키워드 조회 성공")
    @GetMapping("/keywords")
    public ResponseEntity<List<TrendingKeywordDto>> getTrendingKeywords(
            @Parameter(description = "반환 개수", schema = @Schema(defaultValue = "10")) 
            @RequestParam(defaultValue = "10") int limit) {
        List<TrendingKeywordDto> keywords = newsService.getTrendingKeywords(limit);
        return ResponseEntity.ok(keywords);
    }

    /**
     * 인기 키워드 조회
     */
    @Operation(
        summary = "인기 키워드",
        description = "인기 키워드를 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인기 키워드 조회 성공")
    @GetMapping("/keywords/popular")
    public ResponseEntity<List<TrendingKeywordDto>> getPopularKeywords(
            @Parameter(description = "반환 개수", schema = @Schema(defaultValue = "10")) 
            @RequestParam(defaultValue = "10") int limit) {
        List<TrendingKeywordDto> keywords = newsService.getPopularKeywords(limit);
        return ResponseEntity.ok(keywords);
    }
}
