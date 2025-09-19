package com.newnormallist.userservice.user.controller;

import com.newnormallist.userservice.common.ApiResult;
import com.newnormallist.userservice.common.AuthUtils;
import com.newnormallist.userservice.history.dto.ReadHistoryResponse;
import com.newnormallist.userservice.history.dto.UserBehaviorAnalysis;
import com.newnormallist.userservice.history.dto.CategoryPreferenceResponse;
import com.newnormallist.userservice.history.dto.UserInterestResponse;
import com.newnormallist.userservice.user.dto.*;
import com.newnormallist.userservice.user.entity.UserStatus;
import com.newnormallist.userservice.user.service.UserService;

// Swagger 어노테이션 import
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "User", description = "사용자 회원/마이페이지/관리 기능 API")
@SecurityRequirement(name = "bearerAuth") // 모든 API에 전역적으로 인증 요구 설정
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 회원가입 API
     */
    @Operation(
            summary = "회원가입",
            description = "새로운 사용자를 등록합니다.",
            operationId = "signup"
            // requestBody 속성은 springdoc이 자동으로 생성해주므로 제거
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 사용자")
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResult<String>> signup(@Valid @RequestBody SignupRequest signupRequest) {
        userService.signup(signupRequest);
        return ResponseEntity.status(201).body(ApiResult.success("회원가입이 성공적으로 완료되었습니다."));
    }

    /**
     * 특정 사용자 정보 조회 API (내부 서비스용)
     */
    @Operation(
            summary = "사용자 정보 조회",
            description = "특정 사용자의 정보를 조회합니다. (내부 서비스용)",
            operationId = "getUserById"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResult<MyPageResponse>> getUserById(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId
    ) {
        MyPageResponse userResponse = userService.getMyPage(userId);
        return ResponseEntity.ok(ApiResult.success(userResponse));
    }

    /**
     * 마이페이지 (내 정보) 조회 API
     */
    @Operation(
            summary = "마이페이지 조회",
            description = "사용자의 마이페이지 정보를 조회합니다.",
            operationId = "getMyPage"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마이페이지 정보 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/mypage")
    public ResponseEntity<ApiResult<MyPageResponse>> getMyPage(
            @Parameter(hidden = true) @AuthenticationPrincipal String userIdStr
    ) {
        try {
            Long userId = AuthUtils.parseUserId(userIdStr);
            MyPageResponse myPageResponse = userService.getMyPage(userId);
            return ResponseEntity.ok(ApiResult.success(myPageResponse));
        } catch (IllegalArgumentException e) {
            return AuthUtils.unauthorizedResponse();
        }
    }

    /**
     * 마이페이지 정보 수정 API
     */
    @Operation(
            summary = "마이페이지 수정",
            description = "닉네임/관심사 등 마이페이지 정보를 수정합니다.",
            operationId = "updateMyPage"
            // requestBody 속성은 springdoc이 자동으로 생성해주므로 제거
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마이페이지 정보 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PutMapping("/myupdate")
    public ResponseEntity<ApiResult<String>> updateMyPage(
            @Parameter(hidden = true) @AuthenticationPrincipal String userIdStr,
            @Valid @RequestBody UserUpdateRequest userUpdateRequest
    ) {
        try {
            Long userId = AuthUtils.parseUserId(userIdStr);
            userService.updateMyPage(userId, userUpdateRequest);
            return ResponseEntity.ok(ApiResult.success("마이페이지 정보가 성공적으로 수정되었습니다."));
        } catch (IllegalArgumentException e) {
            return AuthUtils.unauthorizedResponse();
        }
    }

    /**
     * 회원 탈퇴 API
     */
    @Operation(
            summary = "회원 탈퇴 요청",
            description = "관리자에게 회원 탈퇴 요청을 보냅니다.",
            operationId = "deleteUser"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 탈퇴 요청 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResult<String>> deleteUser(
            @Parameter(hidden = true) @AuthenticationPrincipal String userIdStr
    ) {
        try {
            Long userId = AuthUtils.parseUserId(userIdStr);
            userService.deleteUser(userId);
            return ResponseEntity.ok(ApiResult.success("회원 탈퇴가 성공적으로 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return AuthUtils.unauthorizedResponse();
        }
    }

    /**
     * 관심사 목록 가져오기 API
     */
    @Operation(
            summary = "관심사 목록 조회",
            description = "전체 관심사 목록을 조회합니다.",
            operationId = "getNewsCategories"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "관심사 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "관심사를 찾을 수 없음")
    })
    @GetMapping("/categories")
    public ResponseEntity<ApiResult<List<CategoryResponse>>> getNewsCategories() {
        List<CategoryResponse> categories = userService.getNewsCategories();
        return ResponseEntity.ok(ApiResult.success(categories));
    }

    /**
     * 사용자 행동 분석 조회 API (내부 서비스용)
     */
    @Operation(
            summary = "사용자 행동 분석 조회",
            description = "특정 사용자의 행동 분석 결과를 조회합니다. (내부 서비스용)",
            operationId = "getUserBehaviorAnalysis"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "행동 분석 조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userId}/behavior-analysis")
    public ResponseEntity<ApiResult<UserBehaviorAnalysis>> getUserBehaviorAnalysis(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId
    ) {
        UserBehaviorAnalysis analysis = userService.getUserBehaviorAnalysis(userId);
        return ResponseEntity.ok(ApiResult.success(analysis));
    }

    /**
     * 사용자 카테고리 선호도 조회 API (내부 서비스용)
     */
    @Operation(
            summary = "사용자 카테고리 선호도 조회",
            description = "특정 사용자의 카테고리 선호도를 조회합니다. (내부 서비스용)",
            operationId = "getCategoryPreferences"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "카테고리 선호도 조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userId}/category-preferences")
    public ResponseEntity<ApiResult<CategoryPreferenceResponse>> getCategoryPreferences(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId
    ) {
        CategoryPreferenceResponse preferences = userService.getCategoryPreferences(userId);
        return ResponseEntity.ok(ApiResult.success(preferences));
    }

    /**
     * 사용자 관심사 분석 조회 API (내부 서비스용)
     */
    @Operation(
            summary = "사용자 관심사 분석 조회",
            description = "특정 사용자의 관심사 분석 결과를 조회합니다. (내부 서비스용)",
            operationId = "getUserInterests"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "관심사 분석 조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userId}/interests")
    public ResponseEntity<ApiResult<UserInterestResponse>> getUserInterests(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId
    ) {
        UserInterestResponse interests = userService.getUserInterests(userId);
        return ResponseEntity.ok(ApiResult.success(interests));
    }

    /**
     * 사용자 관심사 점수 맵 조회 API (내부 서비스용)
     */
    @Operation(
            summary = "사용자 관심사 점수 맵 조회",
            description = "특정 사용자의 관심사 점수 맵을 조회합니다. (내부 서비스용)",
            operationId = "getInterestScores"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "관심사 점수 맵 조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userId}/interest-scores")
    public ResponseEntity<ApiResult<Map<String, Double>>> getInterestScores(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId
    ) {
        Map<String, Double> scores = userService.getInterestScores(userId);
        return ResponseEntity.ok(ApiResult.success(scores));
    }

    /**
     * 사용자 상위 관심사 목록 조회 API (내부 서비스용)
     */
    @Operation(
            summary = "사용자 상위 관심사 목록 조회",
            description = "특정 사용자의 상위 관심사 목록을 조회합니다. (내부 서비스용)",
            operationId = "getTopInterests"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상위 관심사 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userId}/top-interests")
    public ResponseEntity<ApiResult<List<String>>> getTopInterests(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId
    ) {
        List<String> topInterests = userService.getTopInterests(userId);
        return ResponseEntity.ok(ApiResult.success(topInterests));
    }

    /**
     * 관리자용 회원 목록 조회 API
     */
    @Operation(
            summary = "관리자용 회원 목록 조회",
            description = "관리자가 모든 사용자의 목록을 페이지로 조회합니다.",
            operationId = "getUsersForAdmin"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/admin")
    public ResponseEntity<ApiResult<Page<UserAdminResponse>>> getUsers(
            @Parameter(description = "회원 상태 필터", schema = @Schema(implementation = UserStatus.class))
            @RequestParam(required = false) UserStatus status,
            @Parameter(description = "검색 키워드(닉네임/이메일 등)", example = "john")
            @RequestParam(required = false) String keyword,
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<UserAdminResponse> userPage = userService.getUsersForAdmin(status, keyword, pageable);
        return ResponseEntity.ok(ApiResult.success(userPage));
    }

    /**
     * (내부) 특정 사용자 하드 삭제 API
     */
    @Hidden // 문서는 비노출(내부 운영용). 노출하려면 제거하세요.
    @Operation(
            summary = "[내부] 사용자 하드 삭제",
            description = "지정한 사용자 데이터를 영구 삭제합니다.",
            operationId = "adminHardDeleteUser"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "하드 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    // 내부 운영용 API 이므로 관리자 권한 필요
    @DeleteMapping("/internal/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<Void>> hardDelete(
            @Parameter(description = "대상 사용자 ID", example = "123") @PathVariable Long userId
    ) {
        userService.adminHardDeleteUser(userId);
        return ResponseEntity.ok(ApiResult.success(null));
    }

    /**
     * (내부) 배치 하드 삭제 API
     */
    @Hidden // 문서는 비노출(내부 운영용). 노출하려면 제거하세요.
    @Operation(
            summary = "[내부] 배치 하드 삭제",
            description = "지정한 시각 이전에 삭제 처리된 계정을 일괄 영구 삭제합니다.",
            operationId = "adminPurgeDeleted"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배치 하드 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @DeleteMapping("/internal/admin/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<Map<String, Object>>> purge(
            @Parameter(description = "이 시각 이전 삭제 데이터 일괄 영구 삭제 (ISO-8601)", example = "2025-08-01T00:00:00")
            @RequestParam("before")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before
    ) {
        int deleted = userService.adminPurgeDeleted(before);
        return ResponseEntity.ok(ApiResult.success(Map.of("deleted", deleted, "before", before.toString())));
    }

    /**
     * 앱 내 알림을 허용한 사용자 목록 조회 API (내부 서비스용)
     */
    @Hidden // 내부 서비스용 API
    @Operation(
            summary = "앱 내 알림 허용 사용자 목록 조회",
            description = "앱 내 알림을 허용한 사용자 ID 목록을 조회합니다. (내부 서비스용)",
            operationId = "getInAppNotificationEnabledUsers"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "앱 내 알림 허용 사용자 목록 조회 성공")
    })
    @GetMapping("/in-app-notification-enabled")
    public ResponseEntity<ApiResult<List<Long>>> getInAppNotificationEnabledUsers() {
        List<Long> enabledUsers = userService.getInAppNotificationEnabledUsers();
        return ResponseEntity.ok(ApiResult.success(enabledUsers));
    }
}
