package com.newnormallist.userservice.auth.controller;

import com.newnormallist.userservice.auth.service.KakaoTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 토큰 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/auth/tokens")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Token Management", description = "OAuth 토큰 관리 API")
public class TokenController {

    private final KakaoTokenService kakaoTokenService;

    /**
     * 사용자의 카카오 토큰 조회
     */
    @GetMapping("/kakao/{userId}")
    @Operation(summary = "카카오 토큰 조회", description = "사용자의 카카오 액세스 토큰을 조회합니다. 만료된 경우 자동으로 갱신을 시도합니다.")
    public ResponseEntity<String> getKakaoToken(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable String userId) {
        
        try {
            String token = kakaoTokenService.getUserKakaoToken(userId);
            
            if (token != null) {
                return ResponseEntity.ok(token);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("카카오 토큰 조회 실패: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
