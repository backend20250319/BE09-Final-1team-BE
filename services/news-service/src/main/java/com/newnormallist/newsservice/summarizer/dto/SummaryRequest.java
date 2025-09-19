package com.newnormallist.newsservice.summarizer.dto;

import lombok.*;

/** 상세페이지에서 요청하는 요약(뉴스ID 기반) */
@Data
public class SummaryRequest {
    private String type;      // 예: DEFAULT, POLITICS ...
    private Integer lines;    // 기본 3
    private String prompt;    // (옵션) 프롬프트 오버라이드
    private Boolean force;    // true면 기존 캐시 무시하고 재생성
}