package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.NewsListResponse;
import com.newnormallist.newsservice.news.exception.UnauthenticatedUserException;
import com.newnormallist.newsservice.news.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @GetMapping("/scraps")
    public ResponseEntity<Page<NewsListResponse>> getMyScraps(
            @AuthenticationPrincipal String userIdString,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false, defaultValue = "false") boolean uncollectedOnly,
            Pageable pageable) {
        Long userId = getUserIdOrThrow(userIdString);

        Page<NewsListResponse> scraps = myPageService.getScrappedNews(userId, category, query, uncollectedOnly, pageable);
        return ResponseEntity.ok(scraps);
    }

    @DeleteMapping("/scraps/{newsId}")
    public ResponseEntity<Void> deleteScrap(
            @AuthenticationPrincipal String userIdString,
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