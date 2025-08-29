package com.newnormallist.tooltipservice.controller;

import com.newnormallist.tooltipservice.dto.ProcessContentRequest;
import com.newnormallist.tooltipservice.dto.ProcessContentResponse;
import com.newnormallist.tooltipservice.dto.TermDefinitionResponseDto;
import com.newnormallist.tooltipservice.dto.TermDetailResponseDto;
import com.newnormallist.tooltipservice.service.NewsAnalysisService;
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
import java.util.NoSuchElementException;

@Tag(name = "News Analysis", description = "뉴스 본문 분석 및 단어 정의 조회 API")
@RestController
@RequestMapping("/api/news/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalysisController {

    private final NewsAnalysisService newsAnalysisService;

    @Operation(
            summary = "뉴스 본문 분석 및 마크업", 
            description = "뉴스 본문을 분석하여 어려운 단어에 툴팁 마크업(<span> 태그)을 적용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "분석 완료 (성공 또는 실패 시 원본 반환)"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/process")
    public ResponseEntity<ProcessContentResponse> processNewsContent(
            @Parameter(description = "뉴스 ID와 원본 내용", required = true) @RequestBody ProcessContentRequest request) {
        String processedContent;
        try {
            processedContent = newsAnalysisService.getAnalyzedContent(request.newsId(), request.originalContent());
        } catch (Exception e) {
            log.error("뉴스 ID {} 분석 중 에러 발생. 원본 내용을 반환합니다.", request.newsId(), e);
            processedContent = request.originalContent();
        }
        return ResponseEntity.ok(new ProcessContentResponse(processedContent));
    }


    @Operation(
            summary = "단어 정의 조회", 
            description = "특정 단어의 모든 정의를 displayOrder 순으로 조회합니다. 정확 일치 후 부분 일치로 검색합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "정의 조회 성공"),
            @ApiResponse(responseCode = "404", description = "단어를 찾을 수 없음")
    })
    @GetMapping("/definition/{term}")
    public ResponseEntity<TermDetailResponseDto> getTermDefinition(
            @Parameter(description = "조회할 단어", example = "예산", required = true) @PathVariable String term) {
        try {
            TermDetailResponseDto termDetail = newsAnalysisService.getTermDefinitions(term);
            return ResponseEntity.ok(termDetail);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}