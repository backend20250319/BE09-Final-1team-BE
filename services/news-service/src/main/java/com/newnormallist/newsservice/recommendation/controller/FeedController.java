package com.newnormallist.newsservice.recommendation.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import com.newnormallist.newsservice.recommendation.service.RecommendationService;
import com.newnormallist.newsservice.recommendation.dto.ApiResponse;
import com.newnormallist.newsservice.recommendation.dto.FeedItemDto;
import java.util.List;

// RecommendationService를 호출해 최종 피드(뉴스 리스트) DTO로 반환
// 첫 페이지: 개인화 추천, 나머지 페이지: 전체 뉴스 최신순
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/news/feed")
public class FeedController {

    private final RecommendationService recommendationService;

    /**
     * 인증된 사용자의 개인화 피드 조회
     */
    @GetMapping
    public ApiResponse<List<FeedItemDto>> getUserFeed(
            @AuthenticationPrincipal String userIdStr,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "21") int size) {
        Long userId = Long.parseLong(userIdStr);
        return ApiResponse.success(recommendationService.getFeed(userId, page, size));
    }

    /**
     * 관리자용: 특정 사용자의 피드 조회 (개발/테스트/관리 목적)
     */
    @GetMapping("/{id}")
    public ApiResponse<List<FeedItemDto>> getUserFeedById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "21") int size) {
        return ApiResponse.success(recommendationService.getFeed(id, page, size));
    }
}