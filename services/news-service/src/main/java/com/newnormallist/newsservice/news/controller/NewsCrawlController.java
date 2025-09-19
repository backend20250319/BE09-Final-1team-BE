package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.NewsCrawlDto;
import com.newnormallist.newsservice.news.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Crawl", description = "크롤링 결과 저장/미리보기")
@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "*")
public class NewsCrawlController {

    @Autowired
    private NewsService newsService;

    // 크롤러에서 전송된 뉴스 데이터를 받아 저장
    @Operation(
        summary = "크롤링 뉴스 저장",
        description = "크롤러에서 전송된 뉴스 데이터를 저장합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "뉴스 저장 성공"),
        @ApiResponse(responseCode = "400", description = "저장 실패")
    })
    @PostMapping("/crawl")
    public ResponseEntity<String> saveCrawledNews(@RequestBody NewsCrawlDto dto) {
        try {
            newsService.saveCrawledNews(dto);
            return ResponseEntity.ok("뉴스가 성공적으로 저장되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("저장 실패: " + e.getMessage());
        }
    }
    
    // 크롤링된 뉴스 미리보기 (저장하지 않고 미리보기만)
    @Operation(
        summary = "크롤링 뉴스 미리보기",
        description = "크롤링된 뉴스를 저장하지 않고 미리보기만 제공합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "미리보기 성공")
    })
    @PostMapping("/crawl/preview")
    public ResponseEntity<NewsCrawlDto> previewCrawledNews(@RequestBody NewsCrawlDto dto) {
        NewsCrawlDto preview = newsService.previewCrawledNews(dto);
        return ResponseEntity.ok(preview);
    }
}
