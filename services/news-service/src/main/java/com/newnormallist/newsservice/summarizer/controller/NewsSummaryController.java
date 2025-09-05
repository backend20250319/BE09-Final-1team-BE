package com.newnormallist.newsservice.summarizer.controller;

import com.newnormallist.newsservice.summarizer.dto.AdhocSummaryRequest;
import com.newnormallist.newsservice.summarizer.dto.SummaryRequest;
import com.newnormallist.newsservice.summarizer.dto.SummaryResponse;
import com.newnormallist.newsservice.summarizer.dto.SummaryOptions;
import com.newnormallist.newsservice.summarizer.service.NewsSummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Validated
public class NewsSummaryController {

    private final NewsSummaryService newsSummaryService;

    /** 뉴스ID 기반 요약 (DB 캐시 활용, 저장은 Flask, 자바는 재조회만) */
    @PostMapping(
            path = "/{newsId}/summary",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<SummaryResponse> summarizeByNewsId(
            @PathVariable Long newsId,
            @Valid @RequestBody SummaryRequest req
    ) {
        SummaryOptions opt = SummaryOptions.builder()
                .type(Optional.ofNullable(req.getType()).orElse("DEFAULT"))
                .lines(req.getLines() != null && req.getLines() > 0 ? req.getLines() : 3)
                .prompt(req.getPrompt())
                .force(Boolean.TRUE.equals(req.getForce()))
                .build();

        SummaryResponse res = newsSummaryService.summarize(newsId, opt);
        return ResponseEntity.ok(res);
    }

    /** 자유 텍스트 즉시 요약(저장 없음) */
    @PostMapping(
            path = "/summary",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<String> summarizeAdhoc(
            @Valid @RequestBody AdhocSummaryRequest req
    ) {
        SummaryOptions opt = SummaryOptions.builder()
                .type(Optional.ofNullable(req.getType()).orElse("DEFAULT"))
                .lines(req.getLines() != null && req.getLines() > 0 ? req.getLines() : 3)
                .prompt(req.getPrompt())
                .force(false)
                .build();

        String result = newsSummaryService.summarizeText(req.getText(), opt);
        return ResponseEntity.ok(result);
    }
}
