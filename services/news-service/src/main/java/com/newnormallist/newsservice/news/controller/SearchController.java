package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.NewsListResponse;
import com.newnormallist.newsservice.news.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Search", description = "검색 API")
@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class SearchController {
    
    @Autowired
    private NewsService newsService;

    /**
     * 키워드 검색 (정렬 및 필터링 지원)
     */
    @Operation(
        summary = "뉴스 검색",
        description = "전문/메타데이터 기반 뉴스 검색. 카테고리/기간/정렬을 지원합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "검색 성공")
    })
    @GetMapping
    public ResponseEntity<Page<NewsListResponse>> searchNews(
            @Parameter(description = "검색어", schema = @Schema(example = "AI 반도체")) 
            @RequestParam String query,
            
            @Parameter(description = "정렬 기준", schema = @Schema(allowableValues = {"publishedAt", "views", "relevance"})) 
            @RequestParam(required = false) String sortBy,
            
            @Parameter(description = "정렬 순서", schema = @Schema(allowableValues = {"asc", "desc"})) 
            @RequestParam(required = false) String sortOrder,
            
            @Parameter(
                description = "카테고리",
                schema = @Schema(allowableValues = {
                    "POLITICS","ECONOMY","SOCIETY","LIFE","INTERNATIONAL",
                    "IT_SCIENCE","VEHICLE","TRAVEL_FOOD","ART"
                })
            )
            @RequestParam(required = false) String category,
            
            @Parameter(description = "언론사", schema = @Schema(example = "조선일보")) 
            @RequestParam(required = false) String press,
            
            @Parameter(description = "시작일(YYYY-MM-DD)", schema = @Schema(example = "2025-08-01")) 
            @RequestParam(required = false) String startDate,
            
            @Parameter(description = "종료일(YYYY-MM-DD)", schema = @Schema(example = "2025-08-28")) 
            @RequestParam(required = false) String endDate,
            
            @ParameterObject Pageable pageable) {
        Page<NewsListResponse> news = newsService.searchNewsWithFilters(
                query, sortBy, sortOrder, category, press, startDate, endDate, pageable);
        return ResponseEntity.ok(news);
    }

    /**
     * 언론사별 뉴스 조회
     */
    @Operation(
        summary = "언론사별 뉴스",
        description = "특정 언론사의 뉴스를 페이지로 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "언론사별 뉴스 조회 성공")
    })
    @GetMapping("/press/{press}")
    public ResponseEntity<Page<NewsListResponse>> getNewsByPress(
            @Parameter(name = "press", description = "언론사명", example = "조선일보") 
            @PathVariable String press,
            @ParameterObject Pageable pageable) {
        Page<NewsListResponse> news = newsService.getNewsByPress(press, pageable);
        return ResponseEntity.ok(news);
    }

    /**
     * 기간별 뉴스 조회
     */
    @Operation(
        summary = "기간별 뉴스",
        description = "특정 기간의 뉴스를 조회합니다. 기본값은 최근 7일입니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "기간별 뉴스 조회 성공")
    })
    @GetMapping("/date-range")
    public ResponseEntity<List<NewsListResponse>> getNewsByDateRange(
            @Parameter(description = "시작일시", schema = @Schema(example = "2025-08-01T00:00:00")) 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(description = "종료일시", schema = @Schema(example = "2025-08-28T23:59:59")) 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        // 기본값 설정: startDate가 없으면 7일 전, endDate가 없으면 현재 시간
        LocalDateTime defaultStartDate = startDate != null ? startDate : LocalDateTime.now().minusDays(7);
        LocalDateTime defaultEndDate = endDate != null ? endDate : LocalDateTime.now();
        
        List<NewsListResponse> newsList = newsService.getNewsByDateRange(defaultStartDate, defaultEndDate);
        return ResponseEntity.ok(newsList);
    }
}
