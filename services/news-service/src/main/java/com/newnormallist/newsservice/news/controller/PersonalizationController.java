package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.NewsListResponse;
import com.newnormallist.newsservice.news.dto.NewsResponse;
import com.newnormallist.newsservice.news.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Personalization", description = "개인화/추천")
@RestController
@RequestMapping("/api/personalization")
@CrossOrigin(origins = "*")
public class PersonalizationController {
    
    @Autowired
    private NewsService newsService;

    /**
     * 개인화 뉴스
     */
    @Operation(
        summary = "사용자 개인화 뉴스",
        description = "사용자별 개인화된 뉴스를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "개인화 뉴스 조회 성공")
    })
    @GetMapping("/news")
    public ResponseEntity<List<NewsResponse>> getPersonalizedNews(
            @Parameter(name = "userId", required = false, description = "사용자 ID") 
            @RequestHeader("X-User-Id") String userId) {
        List<NewsResponse> news = newsService.getPersonalizedNews(Long.parseLong(userId));
        return ResponseEntity.ok(news);
    }

    /**
     * 추천 뉴스 (사용자 기반)
     */
    @Operation(
        summary = "추천 뉴스",
        description = "사용자 기반 추천 뉴스를 페이지로 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "추천 뉴스 조회 성공")
    })
    @GetMapping("/recommendations")
    public ResponseEntity<Page<NewsListResponse>> getRecommendedNews(
            @Parameter(name = "userId", required = false, description = "사용자 ID") 
            @RequestParam(required = false) Long userId,
            @ParameterObject Pageable pageable) {
        if (userId == null) {
            userId = 1L; // 기본 사용자 ID (실제로는 인증된 사용자 ID 사용)
        }
        Page<NewsListResponse> news = newsService.getRecommendedNews(userId, pageable);
        return ResponseEntity.ok(news);
    }
}
