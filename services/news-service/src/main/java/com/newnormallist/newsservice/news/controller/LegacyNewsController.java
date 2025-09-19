package com.newnormallist.newsservice.news.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "*")
public class LegacyNewsController {
    
    @Autowired
    private SystemController systemController;
    
    @Autowired
    private CategoryController categoryController;
    
    @Autowired
    private TrendingController trendingController;
    
    @Autowired
    private PersonalizationController personalizationController;
    
    @Autowired
    private SearchController searchController;
    
    @Autowired
    private AdminController adminController;

    // 기존 URL들을 새 컨트롤러로 위임 (Deprecated 경고와 함께)
    
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return systemController.healthCheck();
    }

    @GetMapping("/test-db")
    public ResponseEntity<String> databaseTest() {
        return systemController.databaseTest();
    }



    @GetMapping("/category/{categoryName}/articles")
    public ResponseEntity<?> getNewsByCategoryArticles(@PathVariable String categoryName, Pageable pageable) {
        return categoryController.getNewsByCategory(categoryName, pageable);
    }

    @GetMapping("/category/{categoryName}")
    public ResponseEntity<?> getNewsByCategory(@PathVariable String categoryName, Pageable pageable) {
        return categoryController.getNewsByCategory(categoryName, pageable);
    }

    @GetMapping("/trending")
    public ResponseEntity<?> getTrendingNews(Pageable pageable) {
        return trendingController.getTrendingNews(pageable);
    }

    @GetMapping("/trending/list")
    public ResponseEntity<?> getTrendingNewsList() {
        return trendingController.getTrendingNewsList();
    }

    @GetMapping("/popular")
    public ResponseEntity<?> getPopularNews(Pageable pageable) {
        return trendingController.getPopularNews(pageable);
    }

    @GetMapping("/latest")
    public ResponseEntity<?> getLatestNews(Pageable pageable) {
        return trendingController.getLatestNews(pageable);
    }

    @GetMapping("/personalized")
    public ResponseEntity<?> getPersonalizedNews(@RequestHeader("X-User-Id") String userId) {
        return personalizationController.getPersonalizedNews(userId);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendedNews(@RequestParam(required = false) Long userId, Pageable pageable) {
        return personalizationController.getRecommendedNews(userId, pageable);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchNews(@RequestParam String query, Pageable pageable) {
        return searchController.searchNews(query, null, null, null, null, null, null, pageable);
    }

    @GetMapping("/press/{press}")
    public ResponseEntity<?> getNewsByPress(@PathVariable String press, Pageable pageable) {
        return searchController.getNewsByPress(press, pageable);
    }

    @PostMapping("/promote/{newsCrawlId}")
    public ResponseEntity<String> promoteNews(@PathVariable Long newsCrawlId) {
        return adminController.promoteNews(newsCrawlId);
    }
}
