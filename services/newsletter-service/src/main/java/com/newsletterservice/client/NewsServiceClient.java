// ========================================
// 2. 개선된 NewsServiceClient
// ========================================
package com.newsletterservice.client;

import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.client.dto.*;
import com.newsletterservice.config.FeignTimeoutConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "news-service",
        url = "http://localhost:8082",  // 직접 URL 지정
        contextId = "newsletterNewsServiceClient",
        configuration = FeignTimeoutConfig.class
)
public interface NewsServiceClient {

    // ========================================
    // 뉴스 조회
    // ========================================

    @GetMapping("/api/trending/latest")
    ApiResponse<Page<NewsResponse>> getLatestNews(
            @RequestParam(required = false) List<String> categoryName,
            @RequestParam(defaultValue = "10") int limit
    );

    @GetMapping("/api/news")
    Page<NewsResponse> getNewsByCategory(
            @RequestParam("category") String categoryName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );

    @GetMapping("/api/trending")
    ApiResponse<Page<NewsResponse>> getTrendingNews(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "5") int limit
    );

    @GetMapping("/api/trending/popular")
    ApiResponse<Page<NewsResponse>> getPopularNews(
            @RequestParam(defaultValue = "8") int size
    );

    @GetMapping("/api/news/by-category")
    ApiResponse<Page<NewsResponse>> getLatestByCategory(
            @RequestParam("category") String categoryName,
            @RequestParam(defaultValue = "3") int size
    );

    @GetMapping("/api/search")
    ApiResponse<Page<NewsResponse>> searchNews(
            @RequestParam("keyword") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );

    @GetMapping("/api/news/{newsId}")   
    ApiResponse<NewsResponse> getNewsById(@PathVariable("newsId") Long newsId);

    @PostMapping("/api/news/batch")
    ApiResponse<List<NewsResponse>> getNewsByIds(@RequestBody List<Long> newsIds);

    // ========================================
    // 카테고리 및 통계
    // ========================================

    @GetMapping("/api/news/categories")
    ApiResponse<List<CategoryDto>> getCategories();

    @GetMapping("/api/news/categories/{categoryName}/count")
    ApiResponse<Long> getNewsCountByCategory(@PathVariable("categoryName") String categoryName);

    // ========================================
    // 트렌딩 키워드
    // ========================================

    @GetMapping("/api/news/trending")
    Page<NewsResponse> getTrendingKeywords(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "24h") String period,
            @RequestParam(required = false) Integer hours
    );

    @GetMapping("/api/trending/trending-keywords/category/{categoryName}")
    ApiResponse<List<TrendingKeywordDto>> getTrendingKeywordsByCategory(
            @PathVariable("categoryName") String categoryName,
            @RequestParam(defaultValue = "8") int limit,
            @RequestParam(defaultValue = "24h") String period,
            @RequestParam(required = false) Integer hours
    );

    // ========================================
    // 이미지 리소스
    // ========================================

    @GetMapping("/api/images/personalized-section")
    ApiResponse<String> getPersonalizedSectionImage();

    @GetMapping("/api/images/trending-section")
    ApiResponse<String> getTrendingSectionImage();

    @GetMapping("/api/images/latest-news")
    ApiResponse<String> getLatestNewsImage();
}