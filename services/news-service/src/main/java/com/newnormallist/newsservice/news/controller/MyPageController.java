package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.NewsListResponse;
import com.newnormallist.newsservice.news.exception.UnauthenticatedUserException;
import com.newnormallist.newsservice.news.service.MyPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "MyPage", description = "마이페이지(스크랩/이력)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/news/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @Operation(
        summary = "내 스크랩 목록",
        description = "사용자의 스크랩한 뉴스 목록을 페이지로 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "스크랩 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/scraps")
    public ResponseEntity<Page<NewsListResponse>> getMyScraps(
            @Parameter(hidden = true) @AuthenticationPrincipal String userIdString,
            @RequestParam(required = false) String category,
            @ParameterObject Pageable pageable) {
        Long userId = getUserIdOrThrow(userIdString);

        Pageable fixedPageable = PageRequest.of(pageable.getPageNumber(), 10, pageable.getSort());

        Page<NewsListResponse> scraps = myPageService.getScrappedNews(userId, category, fixedPageable);
        return ResponseEntity.ok(scraps);
    }

    @Operation(
        summary = "스크랩 해제",
        description = "특정 뉴스의 스크랩을 해제합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "스크랩 해제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @DeleteMapping("/scraps/{newsId}")
    public ResponseEntity<Void> deleteScrap(
            @Parameter(hidden = true) @AuthenticationPrincipal String userIdString,
            @Parameter(name = "newsId", description = "스크랩 해제할 뉴스 ID", example = "123") 
            @PathVariable Long newsId) {
        Long userId = getUserIdOrThrow(userIdString);
        myPageService.deleteScrap(userId, newsId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 인증 정보에서 사용자 ID를 가져오거나, 정보가 없으면 예외를 발생시킵니다.
     * @param userIdString @AuthenticationPrincipal로 주입된 사용자 ID 문자열
     * @return Long 타입의 사용자 ID
     * @throws UnauthenticatedUserException 사용자 인증 정보가 없을 경우 발생
     */
    private Long getUserIdOrThrow(String userIdString) {
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        try {
            return Long.parseLong(userIdString);
        } catch (NumberFormatException e) {
            throw new UnauthenticatedUserException("유효하지 않은 사용자 ID 형식입니다: " + userIdString);
        }
    }
}