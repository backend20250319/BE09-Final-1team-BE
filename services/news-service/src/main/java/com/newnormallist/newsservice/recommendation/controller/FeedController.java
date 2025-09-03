package com.newnormallist.newsservice.recommendation.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import com.newnormallist.newsservice.recommendation.service.RecommendationService;
import com.newnormallist.newsservice.recommendation.dto.FeedItemDto;
import com.newnormallist.newsservice.recommendation.dto.FeedResponseDto;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

// RecommendationService를 호출해 최종 피드(뉴스 리스트) DTO로 반환
// 첫 페이지: 개인화 추천, 나머지 페이지: 전체 뉴스 최신순
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/news/feed")
@Tag(name = "News Feed", description = "개인화된 뉴스 피드 API")
public class FeedController {

    private final RecommendationService recommendationService;

    /**
     * 인증된 사용자의 개인화 피드 조회
     */
    @GetMapping
    @Operation(summary = "개인화 뉴스 피드 조회", description = "로그인한 사용자의 개인화된 뉴스 피드를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @SecurityRequirement(name = "bearerAuth")
    public FeedResponseDto getUserFeed(
            @AuthenticationPrincipal String userIdStr,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "21")
            @RequestParam(defaultValue = "21") int size) {
        Long userId = Long.parseLong(userIdStr);
        List<FeedItemDto> feedItems = recommendationService.getFeed(userId, page, size);
        return FeedResponseDto.builder()
                .content(feedItems)
                .build();
    }

    /**
     * 관리자용: 특정 사용자의 피드 조회 (개발/테스트/관리 목적)
     */
    @GetMapping("/{id}")
    @Operation(summary = "특정 사용자 피드 조회 (관리자용)", description = "특정 사용자의 개인화된 뉴스 피드를 조회합니다. (개발/테스트/관리 목적)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public FeedResponseDto getUserFeedById(
            @Parameter(description = "사용자 ID", example = "17")
            @PathVariable Long id,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "21")
            @RequestParam(defaultValue = "21") int size) {
        List<FeedItemDto> feedItems = recommendationService.getFeed(id, page, size);
        return FeedResponseDto.builder()
                .content(feedItems)
                .build();
    }
}