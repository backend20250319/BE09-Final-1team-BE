package com.newnormallist.newsservice.recommendation.service;

import java.util.List;
import com.newnormallist.newsservice.recommendation.dto.FeedItemDto;

// 피드 조립 서비스 인터페이스.
// 첫 페이지: 개인화 추천 (상위 3개 카테고리에서 7/5/3개씩)
// 나머지 페이지: 전체 뉴스 최신순 (published_at 기준)

public interface RecommendationService {
    List<FeedItemDto> getFeed(Long userId);
    List<FeedItemDto> getFeed(Long userId, int page, int size);
}
