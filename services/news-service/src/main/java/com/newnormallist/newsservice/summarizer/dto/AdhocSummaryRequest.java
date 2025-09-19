package com.newnormallist.newsservice.summarizer.dto;

import lombok.*;

/** 자유 텍스트 기반 요약(필요 시) */
@Data
public class AdhocSummaryRequest {
    private String text;
    private String type;
    private Integer lines;
    private String prompt;
}