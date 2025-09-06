package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.CategoryDto;
import com.newnormallist.newsservice.news.dto.NewsListResponse;
import com.newnormallist.newsservice.news.entity.Category;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Categories", description = "카테고리 / 뉴스 조회")
@RestController
@RequestMapping("/api/news/categories")
@CrossOrigin(origins = "*")
public class CategoryController {
    
    @Autowired
    private NewsService newsService;

    /**
     * 카테고리 목록 조회
     */
    @Operation(
        summary = "카테고리 목록",
        description = "전체 카테고리 목록을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "카테고리 목록 조회 성공")
    })
    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories() {
        List<CategoryDto> categories = newsService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * 카테고리별 뉴스 조회
     */
    @Operation(
        summary = "카테고리별 뉴스 조회",
        description = "특정 카테고리의 뉴스를 페이지로 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "카테고리별 뉴스 조회 성공"),
        @ApiResponse(responseCode = "400", description = "지원하지 않는 카테고리")
    })
    @GetMapping("/{categoryName}/news")
    public ResponseEntity<?> getNewsByCategory(
            @Parameter(
                name = "categoryName", 
                description = "카테고리", 
                schema = @Schema(allowableValues = {"POLITICS","ECONOMY","SOCIETY","LIFE","INTERNATIONAL","IT_SCIENCE","VEHICLE","TRAVEL_FOOD","ART"})
            )
            @PathVariable String categoryName,
            @ParameterObject Pageable pageable) {
        try {
            Category category = Category.valueOf(categoryName.toUpperCase());
            Page<NewsListResponse> news = newsService.getNewsByCategory(category, pageable);
            return ResponseEntity.ok(news);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("지원하지 않는 카테고리입니다: " + categoryName + 
                ". 사용 가능한 카테고리: POLITICS, ECONOMY, SOCIETY, LIFE, INTERNATIONAL, IT_SCIENCE, VEHICLE, TRAVEL_FOOD, ART");
        }
    }

    /**
     * 카테고리별 뉴스 개수 조회
     */
    @Operation(
        summary = "카테고리별 뉴스 개수",
        description = "특정 카테고리의 뉴스 개수를 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "카테고리별 뉴스 개수 조회 성공"),
        @ApiResponse(responseCode = "400", description = "지원하지 않는 카테고리")
    })
    @GetMapping("/{categoryName}/count")
    public ResponseEntity<?> getNewsCountByCategory(
            @Parameter(
                name = "categoryName", 
                description = "카테고리", 
                schema = @Schema(allowableValues = {"POLITICS","ECONOMY","SOCIETY","LIFE","INTERNATIONAL","IT_SCIENCE","VEHICLE","TRAVEL_FOOD","ART"})
            )
            @PathVariable String categoryName) {
        try {
            Category category = Category.valueOf(categoryName.toUpperCase());
            Long count = newsService.getNewsCountByCategory(category);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("지원하지 않는 카테고리입니다: " + categoryName + 
                ". 사용 가능한 카테고리: POLITICS, ECONOMY, SOCIETY, LIFE, INTERNATIONAL, IT_SCIENCE, VEHICLE, TRAVEL_FOOD, ART");
        }
    }
}
