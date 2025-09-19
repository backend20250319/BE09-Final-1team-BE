package com.newnormallist.newsservice.news.service;

import com.newnormallist.newsservice.news.dto.RelatedNewsResponseDto;

import java.util.List;

public interface RelatedNewsService {
    
    /**
     * 뉴스 ID로 연관뉴스를 조회합니다.
     * @param newsId 조회할 뉴스 ID
     * @return 연관뉴스 목록 (최대 4개)
     */
    List<RelatedNewsResponseDto> getRelatedNews(Long newsId);
}
