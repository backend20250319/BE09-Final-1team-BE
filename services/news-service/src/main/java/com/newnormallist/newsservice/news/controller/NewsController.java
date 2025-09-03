package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.*;
import com.newnormallist.newsservice.news.entity.Category;
import com.newnormallist.newsservice.news.exception.UnauthenticatedUserException;
import com.newnormallist.newsservice.news.service.NewsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "*")
public class NewsController {

    @Autowired
    private NewsService newsService;

    /**
     * 뉴스 개수 조회 API
     * @return 총 뉴스 개수
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getNewsCount() {
        return ResponseEntity.ok(newsService.getNewsCount());
    }

    /**
     * 뉴스 조회수 증가
     */
    @PostMapping("/{newsId}/view")
    public ResponseEntity<Void> incrementViewCount(@PathVariable Long newsId) {
        newsService.incrementViewCount(newsId);
        return ResponseEntity.ok().build();
    }

    /**
     * 뉴스 목록 조회(페이징 지원)
     */
    @GetMapping
    public ResponseEntity<Page<NewsResponse>> getNews(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {

        Category categoryEntity = null;
        if (category != null && !category.equalsIgnoreCase("전체") && !category.isEmpty()) {
            try {
                categoryEntity = Category.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("지원하지 않는 카테고리입니다: " + category);
            }
        }

        Page<NewsResponse> newsList = newsService.getNews(categoryEntity, keyword, pageable);
        return ResponseEntity.ok(newsList);
    }

    /**
     * 특정(단건) 뉴스 상세 조회
     */
    @GetMapping("/{newsId:[0-9]+}")
    public ResponseEntity<NewsResponse> getNewsById(@PathVariable Long newsId) {
        return ResponseEntity.ok(newsService.getNewsById(newsId));
    }

    @PostMapping("/{newsId}/report")
    public ResponseEntity<Void> reportNews(@PathVariable Long newsId, @AuthenticationPrincipal String userIdString) {
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        newsService.reportNews(newsId, Long.parseLong(userIdString));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{newsId}/scrap")
    public ResponseEntity<Void> scrapNews(@PathVariable Long newsId, @AuthenticationPrincipal String userIdString) {
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        newsService.scrapNews(newsId, Long.parseLong(userIdString));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/collections")
    public ResponseEntity<List<ScrapStorageResponse>> getUserCollections(@AuthenticationPrincipal String userIdString) {
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        return ResponseEntity.ok(newsService.getUserScrapStorages(Long.parseLong(userIdString)));
    }

    @GetMapping("/collections/{collectionId}")
    public ResponseEntity<ScrapStorageResponse> getCollectionDetails(
            @AuthenticationPrincipal String userIdString,
            @PathVariable Integer collectionId) {
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        ScrapStorageResponse collection = newsService.getCollectionDetails(Long.parseLong(userIdString), collectionId);
        return ResponseEntity.ok(collection);
    }

    @PostMapping("/collections")
    public ResponseEntity<ScrapStorageResponse> createCollection(
            @AuthenticationPrincipal String userIdString,
            @Valid @RequestBody CollectionCreateRequest request) {
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        ScrapStorageResponse newCollection = newsService.createCollection(Long.parseLong(userIdString), request.getStorageName());
        return new ResponseEntity<>(newCollection, HttpStatus.CREATED);
    }

    @PutMapping("/collections/{collectionId}")
    public ResponseEntity<ScrapStorageResponse> updateCollection(
            @AuthenticationPrincipal String userIdString,
            @PathVariable Integer collectionId,
            @Valid @RequestBody CollectionUpdateRequest request) {
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        ScrapStorageResponse updatedCollection = newsService.updateCollection(Long.parseLong(userIdString), collectionId, request.getNewName());
        return ResponseEntity.ok(updatedCollection);
    }

    @PostMapping("/collections/{collectionId}/news")
    public ResponseEntity<Void> addNewsToCollection(
            @AuthenticationPrincipal String userIdString,
            @PathVariable Integer collectionId,
            @Valid @RequestBody AddNewsToCollectionRequest request) {
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        newsService.addNewsToCollection(Long.parseLong(userIdString), collectionId, request.getNewsId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/collections/{collectionId}/news")
    public ResponseEntity<Page<ScrappedNewsResponse>> getNewsInCollection(
            @AuthenticationPrincipal String userIdString,
            @PathVariable Integer collectionId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String query,
            Pageable pageable) {
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        Page<ScrappedNewsResponse> newsPage = newsService.getNewsInCollection(Long.parseLong(userIdString), collectionId, category, query, pageable);
        return ResponseEntity.ok(newsPage);
    }

    @DeleteMapping("/collections/{collectionId}")
    public ResponseEntity<Void> deleteCollection(
            @AuthenticationPrincipal String userIdString,
            @PathVariable Integer collectionId) {
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        newsService.deleteCollection(Long.parseLong(userIdString), collectionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/collections/{collectionId}/news/{newsId}")
    public ResponseEntity<Void> deleteNewsFromCollection(
            @AuthenticationPrincipal String userIdString,
            @PathVariable Integer collectionId,
            @PathVariable Long newsId) {
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        newsService.deleteNewsFromCollection(Long.parseLong(userIdString), collectionId, newsId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/scraps/{newsScrapId}/assign-to-storage")
    public ResponseEntity<Void> assignScrapToStorage(
            @AuthenticationPrincipal String userIdString,
            @PathVariable Integer newsScrapId,
            @RequestParam Integer targetStorageId) {
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        newsService.assignScrapToStorage(Long.parseLong(userIdString), newsScrapId, targetStorageId);
        return ResponseEntity.ok().build();
    }
}
