package com.newsletterservice.controller;

import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.dto.KakaoUserInfo;
import com.newsletterservice.service.KakaoApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/kakao/login")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Kakao Login", description = "카카오 로그인 관련 API")
public class KakaoLoginController {

    private final KakaoApiService kakaoApiService;

    /**
     * 카카오 로그인 성공 처리
     */
    @GetMapping("/success")
    @Operation(summary = "카카오 로그인 성공", description = "카카오 로그인 성공 후 사용자 정보를 반환합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginSuccess(
            @AuthenticationPrincipal Object principal) {
        
        try {
            if (principal == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("로그인 정보를 찾을 수 없습니다.", "LOGIN_FAILED"));
            }

            Map<String, Object> userInfo = (Map<String, Object>) principal;
            log.info("카카오 로그인 성공: userId={}", userInfo.get("id"));

            return ResponseEntity.ok(ApiResponse.success(userInfo));

        } catch (Exception e) {
            log.error("카카오 로그인 성공 처리 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("로그인 처리 중 오류가 발생했습니다.", "LOGIN_PROCESS_ERROR"));
        }
    }

    /**
     * 카카오 로그인 실패 처리
     */
    @GetMapping("/failure")
    @Operation(summary = "카카오 로그인 실패", description = "카카오 로그인 실패 시 에러 정보를 반환합니다.")
    public ResponseEntity<ApiResponse<String>> loginFailure() {
        log.warn("카카오 로그인 실패");
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("카카오 로그인에 실패했습니다.", "LOGIN_FAILED"));
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    @Operation(summary = "카카오 로그아웃", description = "카카오 로그아웃을 처리합니다.")
    public ResponseEntity<ApiResponse<String>> logout() {
        try {
            // Spring Security의 로그아웃은 SecurityFilterChain에서 처리됨
            log.info("카카오 로그아웃 요청");
            return ResponseEntity.ok(ApiResponse.success("로그아웃되었습니다."));

        } catch (Exception e) {
            log.error("카카오 로그아웃 처리 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("로그아웃 처리 중 오류가 발생했습니다.", "LOGOUT_ERROR"));
        }
    }

    /**
     * 현재 로그인된 사용자 정보 조회
     */
    @GetMapping("/me")
    @Operation(summary = "현재 사용자 정보 조회", description = "현재 로그인된 사용자의 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(
            @AuthenticationPrincipal Object principal) {
        
        try {
            if (principal == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("로그인이 필요합니다.", "LOGIN_REQUIRED"));
            }

            Map<String, Object> userInfo = (Map<String, Object>) principal;
            return ResponseEntity.ok(ApiResponse.success(userInfo));

        } catch (Exception e) {
            log.error("현재 사용자 정보 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("사용자 정보 조회 중 오류가 발생했습니다.", "USER_INFO_ERROR"));
        }
    }
}