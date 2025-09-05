package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.entity.NewsCrawl;
import com.newnormallist.newsservice.news.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin", description = "관리자 전용 뉴스 관리")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    
    @Autowired
    private NewsService newsService;

    /**
     * 관리자용: 크롤링된 뉴스 전체 목록 조회
     */
    @Operation(
        summary = "크롤링된 뉴스 목록 조회",
        description = "관리자가 크롤링된 뉴스 목록을 페이지로 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "크롤링된 뉴스 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/crawled-news")
    public ResponseEntity<Page<NewsCrawl>> getCrawledNews(@ParameterObject Pageable pageable) {
        Page<NewsCrawl> newsList = newsService.getCrawledNews(pageable);
        return ResponseEntity.ok(newsList);
    }

    /**
     * 관리자용: 크롤링된 뉴스를 승격하여 노출용 뉴스로 전환
     */
    @Operation(
        summary = "크롤링 뉴스 승격",
        description = "크롤링된 뉴스를 승격하여 노출용 뉴스로 전환합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "뉴스 승격 성공"),
        @ApiResponse(responseCode = "400", description = "승격 실패"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/promote/{newsCrawlId}")
    public ResponseEntity<String> promoteNews(
        @Parameter(name = "newsCrawlId", description = "크롤링 뉴스 ID", example = "123") 
        @PathVariable Long newsCrawlId) {
        try {
            newsService.promoteToNews(newsCrawlId);
            return ResponseEntity.ok("뉴스가 성공적으로 승격되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("승격 실패: " + e.getMessage());
        }
    }
}
