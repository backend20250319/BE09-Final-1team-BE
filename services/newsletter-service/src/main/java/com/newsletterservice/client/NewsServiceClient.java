package com.newsletterservice.client;

import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.client.dto.NewsResponse;
import com.newsletterservice.client.dto.CategoryDto;
import com.newsletterservice.config.FeignTimeoutConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.newsletterservice.client.dto.TrendingKeywordDto;

@FeignClient(
        name = "news-service",
        url  = "${news.base-url:http://localhost:8082}",
        contextId = "newsletterNewsServiceClient",
        configuration = FeignTimeoutConfig.class
)
public interface NewsServiceClient {

    /**
     * 최신 뉴스 조회
     */
    @GetMapping("/api/trending/latest")
    Page<NewsResponse> getLatestNews(
            @RequestParam(required = false) List<String> categoryName,
            @RequestParam(defaultValue = "10") int limit
    );

    /**
     * 카테고리별 뉴스 조회
     */
    @GetMapping("/api/categories/{categoryName}/news")
    Page<NewsResponse> getNewsByCategory(
            @PathVariable("categoryName") String categoryName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );

    /**
     * 트렌딩 뉴스 조회
     */
    @GetMapping("/api/trending")
    Page<NewsResponse> getTrendingNews(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "5") int limit
    );

    /**
     * 키워드 기반 뉴스 검색
     */
    @GetMapping("/api/search")
    Page<NewsResponse> searchNews(
            @RequestParam("keyword") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );

    /**
     * 뉴스 상세 정보 조회
     */
    @GetMapping("/api/news/{newsId}")
    NewsResponse getNewsById(@PathVariable("newsId") Long newsId);

    /**
     * 여러 뉴스 일괄 조회
     */
    @PostMapping("/api/news/batch")
    List<NewsResponse> getNewsByIds(@RequestBody List<Long> newsIds);

    /**
     * 카테고리별 뉴스 개수 조회
     */
    @GetMapping("/api/categories/{categoryName}/count")
    Long getNewsCountByCategory(
            @PathVariable("categoryName") String categoryName
    );

    @GetMapping("/api/categories")
    List<CategoryDto> getCategories();

    /**
     * 인기 뉴스 조회 (퍼스널라이즈 로직용)
     */
    @GetMapping("/api/trending/popular")
    Page<NewsResponse> getPopularNews(
            @RequestParam(defaultValue = "8") int size
    );

    /**
     * 카테고리별 최신 뉴스 조회 (퍼스널라이즈 로직용)
     */
    @GetMapping("/api/news/by-category")
    Page<NewsResponse> getLatestByCategory(
            @RequestParam("category") String categoryName,
            @RequestParam(defaultValue = "3") int size
    );

    /**
     * 트렌딩 키워드 조회
     */
    @GetMapping("/api/trending/trending-keywords")
    ApiResponse<List<TrendingKeywordDto>> getTrendingKeywords(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "24") int hours
    );

    /**
     * 카테고리별 트렌딩 키워드 조회
     */
    @GetMapping("/api/trending/trending-keywords/category/{categoryName}")
    ApiResponse<List<TrendingKeywordDto>> getTrendingKeywordsByCategory(
            @PathVariable("categoryName") String categoryName,
            @RequestParam(defaultValue = "8") int limit,
            @RequestParam(defaultValue = "24") int hours
    );

    @GetMapping("/images/personalized-section")
    String getPersonalizedSectionImage();

    @GetMapping("/images/trending-section")
    String getTrendingSectionImage();

    @GetMapping("/images/latest-news")
    String getLatestNewsImage();
}