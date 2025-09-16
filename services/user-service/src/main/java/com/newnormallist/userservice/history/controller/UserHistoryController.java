package com.newnormallist.userservice.history.controller;

import com.newnormallist.userservice.common.ApiResult;
import com.newnormallist.userservice.common.AuthUtils;
import com.newnormallist.userservice.history.dto.ReadHistoryResponse;
import com.newnormallist.userservice.history.service.UserHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "User History", description = "사용자 뉴스 읽은 기록 관리 API")
@SecurityRequirement(name = "bearerAuth") // 모든 API에 전역적으로 인증 요구 설정
@RequestMapping("/api/users/mypage/history")
@RequiredArgsConstructor
public class UserHistoryController {
    private final UserHistoryService userHistoryService;
    /**
     * 마이페이지 - 읽은 뉴스 목록 저장 API
     */
    @Operation(
            summary = "읽은 뉴스 저장",
            description = "사용자의 읽은 뉴스 이력을 저장합니다.",
            operationId = "addReadHistory"
    )
    @PostMapping("/{newsId}") // ✅ 핵심: 상대 경로로 단순화
    public ResponseEntity<ApiResult<String>> addReadHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal String userIdStr,
            @Parameter(description = "뉴스 ID", example = "98765") @PathVariable Long newsId
    ) {
        try {
            Long userId = AuthUtils.parseUserId(userIdStr);
            userHistoryService.addReadHistory(userId, newsId);
            return ResponseEntity.ok(ApiResult.success("읽은 뉴스가 저장되었습니다."));
        } catch (IllegalArgumentException e) {
            return AuthUtils.unauthorizedResponse();
        }
    }

    /**
     * 마이페이지 - 읽은 뉴스 목록 조회 API
     */
    @Operation(
            summary = "읽은 뉴스 목록 조회",
            description = "사용자가 읽은 뉴스 이력을 최신순으로 페이지 조회합니다.",
            operationId = "getReadHistory"
    )
    @GetMapping("/index") // ✅ 핵심: 상대 경로로 단순화
    public ResponseEntity<ApiResult<Page<ReadHistoryResponse>>> getReadHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal String userIdStr,
            @ParameterObject
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        try {
            Long userId = AuthUtils.parseUserId(userIdStr);
            Page<ReadHistoryResponse> historyPage = userHistoryService.getReadHistory(userId, pageable);
            return ResponseEntity.ok(ApiResult.success(historyPage));
        } catch (IllegalArgumentException e) {
            return AuthUtils.unauthorizedResponse();
        }
    }
}
