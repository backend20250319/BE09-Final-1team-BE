package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.RelatedNewsResponseDto;
import com.newnormallist.newsservice.news.service.RelatedNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Related News", description = "연관 뉴스")
@RestController
@RequestMapping("/api/news/related")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RelatedNewsController {

    private final RelatedNewsService relatedNewsService;

    /**
     * 뉴스 ID로 연관뉴스를 조회합니다.
     * @param newsId 조회할 뉴스 ID
     * @return 연관뉴스 목록 (최대 4개)
     */
    @Operation(
        summary = "연관 뉴스 조회",
        description = "특정 뉴스와 연관된 뉴스 목록을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "연관 뉴스 조회 성공"),
        @ApiResponse(responseCode = "404", description = "뉴스를 찾을 수 없음")
    })
    @GetMapping("/{newsId}")
    public ResponseEntity<List<RelatedNewsResponseDto>> getRelatedNews(
            @Parameter(name = "newsId", description = "기준 뉴스 ID", example = "123") 
            @PathVariable Long newsId) {
        log.info("연관뉴스 조회 요청: newsId = {}", newsId);
        
        List<RelatedNewsResponseDto> relatedNews = relatedNewsService.getRelatedNews(newsId);
        
        log.info("연관뉴스 조회 완료: newsId = {}, 연관뉴스 개수 = {}", newsId, relatedNews.size());
        
        return ResponseEntity.ok(relatedNews);
    }
}
