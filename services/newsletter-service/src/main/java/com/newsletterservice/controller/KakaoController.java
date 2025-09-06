package com.newsletterservice.controller;

import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.dto.KakaoFriend;
import com.newsletterservice.dto.KakaoTokenInfo;
import com.newsletterservice.dto.KakaoUserInfo;
import com.newsletterservice.service.KakaoApiService;
import com.newsletterservice.service.KakaoMessageService;
import com.newsletterservice.dto.NewsletterContent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kakao")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Kakao API", description = "카카오 로그인, 친구목록, 메시지 API")
public class KakaoController {

    private final KakaoApiService kakaoApiService;
    private final KakaoMessageService kakaoMessageService;

    /**
     * 카카오 사용자 정보 조회
     */
    @GetMapping("/user/me")
    @Operation(summary = "카카오 사용자 정보 조회", description = "액세스 토큰으로 카카오 사용자 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<KakaoUserInfo>> getUserInfo(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken) {
        
        String token = accessToken.replace("Bearer ", "");
        KakaoUserInfo userInfo = kakaoApiService.getUserInfo(token);
        
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    /**
     * 카카오 친구 목록 조회
     */
    @GetMapping("/friends")
    @Operation(summary = "카카오 친구 목록 조회", description = "액세스 토큰으로 카카오 친구 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<KakaoFriend>> getFriendList(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken,
            @Parameter(description = "친구 목록 시작 지점") @RequestParam(required = false) Integer offset,
            @Parameter(description = "한 페이지에 가져올 친구 최대 수 (최대 100)") @RequestParam(required = false) Integer limit,
            @Parameter(description = "친구 목록 정렬 순서 (asc/desc)") @RequestParam(required = false) String order,
            @Parameter(description = "친구 목록 정렬 기준 (favorite/nickname)") @RequestParam(required = false) String friendOrder) {
        
        String token = accessToken.replace("Bearer ", "");
        KakaoFriend friendList = kakaoApiService.getFriendList(token, offset, limit, order, friendOrder);
        
        return ResponseEntity.ok(ApiResponse.success(friendList));
    }

    /**
     * 즐겨찾기 친구 목록 조회
     */
    @GetMapping("/friends/favorite")
    @Operation(summary = "즐겨찾기 친구 목록 조회", description = "즐겨찾기로 설정된 친구 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<Object>> getFavoriteFriends(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken) {
        
        String token = accessToken.replace("Bearer ", "");
        var favoriteFriends = kakaoApiService.getFavoriteFriends(token);
        
        return ResponseEntity.ok(ApiResponse.success(favoriteFriends));
    }

    /**
     * 닉네임 순 친구 목록 조회
     */
    @GetMapping("/friends/by-nickname")
    @Operation(summary = "닉네임 순 친구 목록 조회", description = "닉네임 순으로 정렬된 친구 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<KakaoFriend>> getFriendsByNickname(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken,
            @Parameter(description = "친구 목록 시작 지점") @RequestParam(required = false) Integer offset,
            @Parameter(description = "한 페이지에 가져올 친구 최대 수") @RequestParam(required = false) Integer limit) {
        
        String token = accessToken.replace("Bearer ", "");
        KakaoFriend friendList = kakaoApiService.getFriendsByNickname(token, offset, limit);
        
        return ResponseEntity.ok(ApiResponse.success(friendList));
    }

    /**
     * 카카오 토큰 정보 조회
     */
    @GetMapping("/token/info")
    @Operation(summary = "카카오 토큰 정보 조회", description = "액세스 토큰의 유효성과 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<KakaoTokenInfo>> getTokenInfo(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken) {
        
        String token = accessToken.replace("Bearer ", "");
        KakaoTokenInfo tokenInfo = kakaoApiService.getTokenInfo(token);
        
        return ResponseEntity.ok(ApiResponse.success(tokenInfo));
    }

    /**
     * 토큰 유효성 검증
     */
    @GetMapping("/token/validate")
    @Operation(summary = "토큰 유효성 검증", description = "액세스 토큰의 유효성을 검증합니다.")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken) {
        
        String token = accessToken.replace("Bearer ", "");
        boolean isValid = kakaoApiService.isTokenValid(token);
        
        return ResponseEntity.ok(ApiResponse.success(isValid));
    }

    /**
     * 메시지 전송 가능한 친구 목록 조회
     */
    @GetMapping("/friends/messageable")
    @Operation(summary = "메시지 전송 가능한 친구 목록", description = "메시지 전송이 가능한 친구 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<Object>> getMessageableFriends(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken) {
        
        String token = accessToken.replace("Bearer ", "");
        var messageableFriends = kakaoApiService.getMessageableFriends(token);
        
        return ResponseEntity.ok(ApiResponse.success(messageableFriends));
    }

    /**
     * 뉴스레터를 나에게 카카오톡으로 전송
     */
    @PostMapping("/message/send-to-me")
    @Operation(summary = "나에게 뉴스레터 전송", description = "뉴스레터를 카카오톡 나에게 보내기로 전송합니다.")
    public ResponseEntity<ApiResponse<String>> sendNewsletterToMe(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken,
            @RequestBody NewsletterContent content) {
        
        String token = accessToken.replace("Bearer ", "");
        kakaoMessageService.sendNewsletterToMe(content, token);
        
        return ResponseEntity.ok(ApiResponse.success("뉴스레터가 성공적으로 전송되었습니다."));
    }

    /**
     * 뉴스레터를 친구들에게 카카오톡으로 전송
     */
    @PostMapping("/message/send-to-friends")
    @Operation(summary = "친구들에게 뉴스레터 전송", description = "뉴스레터를 카카오톡 친구들에게 전송합니다.")
    public ResponseEntity<ApiResponse<String>> sendNewsletterToFriends(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken,
            @RequestBody NewsletterContent content) {
        
        String token = accessToken.replace("Bearer ", "");
        kakaoMessageService.sendNewsletterToFriends(content, token);
        
        return ResponseEntity.ok(ApiResponse.success("뉴스레터가 친구들에게 성공적으로 전송되었습니다."));
    }

    /**
     * 토큰 갱신
     */
    @PostMapping("/token/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 액세스 토큰을 갱신합니다.")
    public ResponseEntity<ApiResponse<Object>> refreshToken(
            @Parameter(description = "리프레시 토큰") @RequestParam String refreshToken) {
        
        var response = kakaoApiService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    @Operation(summary = "카카오 로그아웃", description = "카카오 로그아웃을 처리합니다.")
    public ResponseEntity<ApiResponse<Object>> logout(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken) {
        
        String token = accessToken.replace("Bearer ", "");
        var response = kakaoApiService.logout(token);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 연결 해제
     */
    @PostMapping("/unlink")
    @Operation(summary = "카카오 연결 해제", description = "카카오 계정과의 연결을 해제합니다.")
    public ResponseEntity<ApiResponse<Object>> unlink(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken) {
        
        String token = accessToken.replace("Bearer ", "");
        var response = kakaoApiService.unlink(token);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
