package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.AddNewsToCollectionRequest;
import com.newnormallist.newsservice.news.dto.CollectionCreateRequest;
import com.newnormallist.newsservice.news.dto.NewsResponse;
import com.newnormallist.newsservice.news.dto.ScrapStorageResponse;
import com.newnormallist.newsservice.news.entity.Category;
import com.newnormallist.newsservice.news.exception.UnauthenticatedUserException;
import com.newnormallist.newsservice.news.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "News", description = "뉴스 API")
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
    @Operation(
        summary = "뉴스 총 개수",
        description = "전체 뉴스 개수를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "뉴스 개수 조회 성공")
    })
    @GetMapping("/count")
    public ResponseEntity<Long> getNewsCount() {
        return ResponseEntity.ok(newsService.getNewsCount());
    }

    /**
     * 뉴스 조회수 증가
     */
    @Operation(
        summary = "조회수 증가",
        description = "특정 뉴스의 조회수를 증가시킵니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회수 증가 성공")
    })
    @PostMapping("/{newsId}/view")
    public ResponseEntity<Void> incrementViewCount(@PathVariable Long newsId) {
        newsService.incrementViewCount(newsId);
        return ResponseEntity.ok().build();
    }

    /**
     * 뉴스 목록 조회(페이징 지원)
     */
    @Operation(
        summary = "뉴스 목록 조회",
        description = "카테고리와 키워드로 뉴스를 필터링하여 페이지로 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "뉴스 목록 조회 성공")
    })
    @GetMapping
    public ResponseEntity<Page<NewsResponse>> getNews(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @ParameterObject Pageable pageable) {

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
    @Operation(
        summary = "뉴스 상세 조회",
        description = "특정 뉴스의 상세 정보를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "뉴스 상세 조회 성공"),
        @ApiResponse(responseCode = "404", description = "뉴스를 찾을 수 없음")
    })
    @GetMapping("/{newsId:[0-9]+}")
    public ResponseEntity<NewsResponse> getNewsById(
            @Parameter(name = "newsId", description = "뉴스 ID", example = "123") 
            @PathVariable Long newsId) {
        return ResponseEntity.ok(newsService.getNewsById(newsId));
    }

    @Operation(
        summary = "뉴스 신고",
        description = "특정 뉴스를 신고합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "뉴스 신고 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{newsId}/report")
    public ResponseEntity<Void> reportNews(
            @Parameter(name = "newsId", description = "신고할 뉴스 ID", example = "123") 
            @PathVariable Long newsId, 
            @Parameter(hidden = true) @AuthenticationPrincipal String userIdString) {
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        newsService.reportNews(newsId, Long.parseLong(userIdString));
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "뉴스 스크랩",
        description = "특정 뉴스를 스크랩합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "뉴스 스크랩 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{newsId}/scrap")
    public ResponseEntity<Void> scrapNews(
            @Parameter(name = "newsId", description = "스크랩할 뉴스 ID", example = "123") 
            @PathVariable Long newsId, 
            @Parameter(hidden = true) @AuthenticationPrincipal String userIdString) {
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        newsService.scrapNews(newsId, Long.parseLong(userIdString));
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "내 스크랩 보관함 목록",
        description = "사용자의 스크랩 보관함 목록을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "보관함 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/collections")
    public ResponseEntity<List<ScrapStorageResponse>> getUserCollections(
            @Parameter(hidden = true) @AuthenticationPrincipal String userIdString) {
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        return ResponseEntity.ok(newsService.getUserScrapStorages(Long.parseLong(userIdString)));
    }

    @Operation(
        summary = "보관함 생성",
        description = "새로운 스크랩 보관함을 생성합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "보관함 생성 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/collections")
    public ResponseEntity<ScrapStorageResponse> createCollection(
            @Parameter(hidden = true) @AuthenticationPrincipal String userIdString,
            @Valid @RequestBody CollectionCreateRequest request) {
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        ScrapStorageResponse newCollection = newsService.createCollection(Long.parseLong(userIdString), request.getStorageName());
        return new ResponseEntity<>(newCollection, HttpStatus.CREATED);
    }

    @Operation(
        summary = "보관함에 뉴스 추가",
        description = "특정 보관함에 뉴스를 추가합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "뉴스 추가 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/collections/{collectionId}/news")
    public ResponseEntity<Void> addNewsToCollection(
            @Parameter(hidden = true) @AuthenticationPrincipal String userIdString,
            @Parameter(name = "collectionId", description = "보관함 ID", example = "1") 
            @PathVariable Integer collectionId,
            @Valid @RequestBody AddNewsToCollectionRequest request) {
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        newsService.addNewsToCollection(Long.parseLong(userIdString), collectionId, request.getNewsId());
        return ResponseEntity.ok().build();
    }
}
