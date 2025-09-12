package com.newsletterservice.controller;

import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.common.exception.NewsletterException;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kakao")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Kakao API", description = "카카오 로그인, 친구목록, 메시지 API")
@ConditionalOnProperty(name = "kakao.api.enabled", havingValue = "true", matchIfMissing = false)
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
     * 뉴스레터를 나에게 카카오톡으로 전송 (JavaScript SDK 방식)
     * 
     * JavaScript SDK Kakao.API.request 파라미터 구조:
     * - url: String - "/v2/api/talk/memo/send"로 고정 (필수)
     * - data: Object - API에 전달할 파라미터 (필수)
     *   - template_id: Number (필수) - 메시지 템플릿 도구에서 구성한 사용자 정의 템플릿의 ID
     *   - template_args: Object (선택) - template_id로 지정한 템플릿에 사용자 인자(User argument)가 포함되어 있는 경우 대입할 값, key:value 형식으로 전달
     * - success: Function(Object) - API 호출이 성공할 때 실행되는 콜백 함수 (선택, 서버사이드에서는 로그로 처리)
     * - fail: Function(Object) - API 호출이 실패할 때 실행되는 콜백 함수 (선택, 서버사이드에서는 예외로 처리)
     * - always: Function(Object) - API 호출 성공 여부에 관계없이 항상 호출되는 콜백 함수 (선택, 서버사이드에서는 finally로 처리)
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
     * 권한 확인 후 뉴스레터 전송 (권한 없으면 대체 전송)
     */
    @PostMapping("/message/send-with-fallback")
    @Operation(summary = "권한 확인 후 뉴스레터 전송", description = "카카오톡 메시지 권한을 확인하고, 권한이 없으면 대체 방식으로 전송합니다.")
    public ResponseEntity<ApiResponse<String>> sendNewsletterWithFallback(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken,
            @RequestBody NewsletterContent content) {
        
        String token = accessToken.replace("Bearer ", "");
        kakaoMessageService.sendNewsletterWithFallback(content, token);
        
        return ResponseEntity.ok(ApiResponse.success("뉴스레터가 전송되었습니다. (권한에 따라 카카오톡 또는 대체 방식)"));
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
     * 친구들에게 뉴스레터 전송 (JavaScript SDK Kakao.API.request 방식)
     * 
     * JavaScript SDK Kakao.API.request 파라미터 구조:
     * - url: "/v1/api/talk/friends/message/send" (고정)
     * - data: API에 전달할 파라미터
     *   - receiver_uuids: String[] (필수, 최대 5개)
     *   - template_id: Number (필수)
     *   - template_args: Object (선택, key:value 형식)
     * - success: Function(Object) - 성공 콜백 (서버사이드에서는 로그로 처리)
     * - fail: Function(Object) - 실패 콜백 (서버사이드에서는 예외로 처리)
     * - always: Function(Object) - 항상 실행 콜백 (서버사이드에서는 finally로 처리)
     */
    @PostMapping("/message/send-to-friends-with-uuids")
    @Operation(summary = "친구들에게 뉴스레터 전송 (UUID 방식)", 
               description = "카카오 친구 목록 조회 API로 얻은 UUID 목록으로 뉴스레터를 전송합니다. 최대 5명까지 가능합니다.")
    public ResponseEntity<ApiResponse<String>> sendNewsletterToFriendsWithUuids(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken,
            @Parameter(description = "뉴스레터 제목") @RequestParam String title,
            @Parameter(description = "뉴스레터 요약") @RequestParam String summary,
            @Parameter(description = "뉴스레터 URL") @RequestParam String url,
            @Parameter(description = "받는 사람 UUID 목록 (최대 5개)") @RequestBody List<String> receiverUuids) {
        
        // 카카오 API 스펙 검증
        if (receiverUuids == null || receiverUuids.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("receiver_uuids는 필수입니다.", "INVALID_RECEIVER_UUIDS"));
        }
        
        if (receiverUuids.size() > 5) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("receiver_uuids는 최대 5개까지 가능합니다. 현재: " + receiverUuids.size(), "TOO_MANY_RECEIVERS"));
        }
        
        String token = accessToken.replace("Bearer ", "");
        kakaoMessageService.sendNewsletterToFriendsWithUuids(token, title, summary, url, receiverUuids);
        
        return ResponseEntity.ok(ApiResponse.success("뉴스레터가 " + receiverUuids.size() + "명의 친구들에게 성공적으로 전송되었습니다."));
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
     * 카카오톡 메시지 전송 권한 확인
     */
    @GetMapping("/permissions/talk-message")
    @Operation(summary = "카카오톡 메시지 전송 권한 확인", description = "사용자가 카카오톡 메시지 전송 권한을 가지고 있는지 확인합니다.")
    public ResponseEntity<ApiResponse<Boolean>> checkTalkMessagePermission(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken) {
        
        try {
            String token = accessToken.replace("Bearer ", "");
            boolean hasPermission = kakaoApiService.hasTalkMessagePermission(token);
            
            return ResponseEntity.ok(ApiResponse.success(hasPermission));
            
        } catch (NewsletterException e) {
            log.error("카카오톡 메시지 권한 확인 실패", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("카카오톡 메시지 권한 확인 중 예상치 못한 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "권한 확인 중 오류가 발생했습니다."));
        }
    }

    /**
     * 동의 필요 여부 확인
     */
    @PostMapping("/consent/check-required")
    @Operation(summary = "동의 필요 여부 확인", description = "지정된 스코프들 중 동의가 필요한 항목들을 확인합니다.")
    public ResponseEntity<ApiResponse<List<String>>> checkRequiredConsent(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken,
            @Parameter(description = "확인할 스코프 목록") @RequestBody List<String> scopes) {
        
        String token = accessToken.replace("Bearer ", "");
        List<String> requiredScopes = kakaoApiService.getRequiredConsentScopes(token, scopes);
        
        return ResponseEntity.ok(ApiResponse.success(requiredScopes));
    }

    /**
     * 카카오 동의항목 추가 동의 요청
     */
    @PostMapping("/consent/additional")
    @Operation(summary = "카카오 동의항목 추가 동의", description = "talk_message 등 추가 동의항목에 대한 동의를 요청합니다.")
    public ResponseEntity<ApiResponse<String>> requestAdditionalConsent(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken,
            @Parameter(description = "추가 동의할 스코프 목록") @RequestBody List<String> scopes) {
        
        String token = accessToken.replace("Bearer ", "");
        String consentUrl = kakaoApiService.generateAdditionalConsentUrl(token, scopes);
        
        if (consentUrl == null) {
            return ResponseEntity.ok(ApiResponse.success("모든 동의항목에 이미 동의하셨습니다."));
        }
        
        return ResponseEntity.ok(ApiResponse.success(consentUrl));
    }

    /**
     * 사용자 동의항목 조회
     */
    @GetMapping("/consent/scopes")
    @Operation(summary = "사용자 동의항목 조회", description = "사용자가 동의한 모든 동의항목을 조회합니다.")
    public ResponseEntity<ApiResponse<Object>> getUserScopes(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken) {
        
        String token = accessToken.replace("Bearer ", "");
        var scopes = kakaoApiService.getUserScopes(token);
        
        return ResponseEntity.ok(ApiResponse.success(scopes));
    }

    /**
     * 특정 동의항목 조회
     */
    @GetMapping("/consent/scopes/specific")
    @Operation(summary = "특정 동의항목 조회", description = "지정된 동의항목들의 동의 상태를 조회합니다.")
    public ResponseEntity<ApiResponse<Object>> getSpecificScopes(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken,
            @Parameter(description = "조회할 동의항목 목록") @RequestParam List<String> scopes) {
        
        String token = accessToken.replace("Bearer ", "");
        var scopeInfo = kakaoApiService.getUserScopes(token, scopes);
        
        return ResponseEntity.ok(ApiResponse.success(scopeInfo));
    }

    /**
     * 동의항목 철회
     */
    @PostMapping("/consent/revoke")
    @Operation(summary = "동의항목 철회", description = "사용자가 동의한 특정 동의항목을 철회합니다.")
    public ResponseEntity<ApiResponse<Object>> revokeScopes(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken,
            @Parameter(description = "철회할 동의항목 목록") @RequestBody List<String> scopes) {
        
        String token = accessToken.replace("Bearer ", "");
        var result = kakaoApiService.revokeScopes(token, scopes);
        
        return ResponseEntity.ok(ApiResponse.success(result));
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
    
    /**
     * 피드 A형 템플릿으로 개인화된 뉴스레터 전송
     */
    @PostMapping("/send/feed-a/personalized/{userId}")
    @Operation(summary = "피드 A형 개인화 뉴스레터 전송", description = "사용자의 관심사를 반영한 피드 A형 뉴스레터를 전송합니다.")
    public ResponseEntity<ApiResponse<Object>> sendPersonalizedFeedA(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken) {
        
        String token = accessToken.replace("Bearer ", "");
        kakaoMessageService.sendFeedAMessage(userId, token);
        
        return ResponseEntity.ok(ApiResponse.success("피드 A형 개인화 뉴스레터 전송 완료"));
    }
    
    /**
     * 피드 B형 템플릿으로 개인화된 뉴스레터 전송
     */
    @PostMapping("/send/feed-b/personalized/{userId}")
    @Operation(summary = "피드 B형 개인화 뉴스레터 전송", description = "사용자의 관심사를 반영한 피드 B형 뉴스레터를 전송합니다.")
    public ResponseEntity<ApiResponse<Object>> sendPersonalizedFeedB(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken) {
        
        String token = accessToken.replace("Bearer ", "");
        kakaoMessageService.sendFeedBMessage(userId, token);
        
        return ResponseEntity.ok(ApiResponse.success("피드 B형 개인화 뉴스레터 전송 완료"));
    }
    
    /**
     * 피드 A형 템플릿으로 카테고리별 뉴스레터 전송
     */
    @PostMapping("/send/feed-a/category/{category}")
    @Operation(summary = "피드 A형 카테고리별 뉴스레터 전송", description = "특정 카테고리의 뉴스로 구성된 피드 A형 뉴스레터를 전송합니다.")
    public ResponseEntity<ApiResponse<Object>> sendCategoryFeedA(
            @Parameter(description = "카테고리명") @PathVariable String category,
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken) {
        
        String token = accessToken.replace("Bearer ", "");
        kakaoMessageService.sendCategoryFeedAMessage(category, token);
        
        return ResponseEntity.ok(ApiResponse.success("피드 A형 카테고리별 뉴스레터 전송 완료"));
    }
    
    /**
     * 피드 B형 템플릿으로 카테고리별 뉴스레터 전송
     */
    @PostMapping("/send/feed-b/category/{category}")
    @Operation(summary = "피드 B형 카테고리별 뉴스레터 전송", description = "특정 카테고리의 뉴스로 구성된 피드 B형 뉴스레터를 전송합니다.")
    public ResponseEntity<ApiResponse<Object>> sendCategoryFeedB(
            @Parameter(description = "카테고리명") @PathVariable String category,
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken) {
        
        String token = accessToken.replace("Bearer ", "");
        kakaoMessageService.sendCategoryFeedBMessage(category, token);
        
        return ResponseEntity.ok(ApiResponse.success("피드 B형 카테고리별 뉴스레터 전송 완료"));
    }
    
    /**
     * 피드 A형 템플릿으로 트렌딩 뉴스레터 전송
     */
    @PostMapping("/send/feed-a/trending")
    @Operation(summary = "피드 A형 트렌딩 뉴스레터 전송", description = "트렌딩 뉴스로 구성된 피드 A형 뉴스레터를 전송합니다.")
    public ResponseEntity<ApiResponse<Object>> sendTrendingFeedA(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken) {
        
        String token = accessToken.replace("Bearer ", "");
        kakaoMessageService.sendTrendingFeedAMessage(token);
        
        return ResponseEntity.ok(ApiResponse.success("피드 A형 트렌딩 뉴스레터 전송 완료"));
    }
    
    /**
     * 피드 B형 템플릿으로 트렌딩 뉴스레터 전송
     */
    @PostMapping("/send/feed-b/trending")
    @Operation(summary = "피드 B형 트렌딩 뉴스레터 전송", description = "트렌딩 뉴스로 구성된 피드 B형 뉴스레터를 전송합니다.")
    public ResponseEntity<ApiResponse<Object>> sendTrendingFeedB(
            @Parameter(description = "카카오 액세스 토큰") @RequestHeader("Authorization") String accessToken) {
        
        String token = accessToken.replace("Bearer ", "");
        kakaoMessageService.sendTrendingFeedBMessage(token);
        
        return ResponseEntity.ok(ApiResponse.success("피드 B형 트렌딩 뉴스레터 전송 완료"));
    }
}
