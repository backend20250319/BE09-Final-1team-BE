package com.newsletterservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsletterservice.common.exception.NewsletterException;
import com.newsletterservice.dto.KakaoFriend;
import com.newsletterservice.dto.KakaoTokenInfo;
import com.newsletterservice.dto.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoApiService {

    @Qualifier("kakaoWebClient")
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${kakao.app-key:your-app-key}")
    private String kakaoAppKey;

    /**
     * 카카오 사용자 정보 조회
     * GET https://kapi.kakao.com/v2/user/me
     */
    public KakaoUserInfo getUserInfo(String accessToken) {
        try {
            KakaoUserInfo userInfo = webClient
                .get()
                .uri("/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoUserInfo.class)
                .timeout(Duration.ofSeconds(30))
                .block();

            log.info("카카오 사용자 정보 조회 성공: userId={}", userInfo.getId());
            return userInfo;

        } catch (WebClientResponseException e) {
            log.error("카카오 사용자 정보 조회 실패: statusCode={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            handleKakaoApiError(e);
            return null;
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패", e);
            throw new NewsletterException("카카오 사용자 정보 조회에 실패했습니다.", "KAKAO_USER_INFO_ERROR");
        }
    }

    /**
     * 카카오 친구 목록 조회
     * GET https://kapi.kakao.com/v1/api/talk/friends
     */
    public KakaoFriend getFriendList(String accessToken) {
        return getFriendList(accessToken, null, null, null, null);
    }

    /**
     * 카카오 친구 목록 조회 (파라미터 포함)
     * GET https://kapi.kakao.com/v1/api/talk/friends
     */
    public KakaoFriend getFriendList(String accessToken, Integer offset, Integer limit, 
                                   String order, String friendOrder) {
        try {
            StringBuilder uriBuilder = new StringBuilder("/v1/api/talk/friends");
            boolean hasParams = false;

            if (offset != null) {
                uriBuilder.append(hasParams ? "&" : "?").append("offset=").append(offset);
                hasParams = true;
            }
            if (limit != null) {
                uriBuilder.append(hasParams ? "&" : "?").append("limit=").append(limit);
                hasParams = true;
            }
            if (order != null) {
                uriBuilder.append(hasParams ? "&" : "?").append("order=").append(order);
                hasParams = true;
            }
            if (friendOrder != null) {
                uriBuilder.append(hasParams ? "&" : "?").append("friend_order=").append(friendOrder);
                hasParams = true;
            }

            KakaoFriend friendList = webClient
                .get()
                .uri(uriBuilder.toString())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoFriend.class)
                .timeout(Duration.ofSeconds(30))
                .block();

            log.info("카카오 친구 목록 조회 성공: 친구 수={}, 즐겨찾기 수={}", 
                    friendList.getTotalCount(), friendList.getFavoriteCount());
            return friendList;

        } catch (WebClientResponseException e) {
            log.error("카카오 친구 목록 조회 실패: statusCode={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            handleKakaoApiError(e);
            return null;
        } catch (Exception e) {
            log.error("카카오 친구 목록 조회 실패", e);
            throw new NewsletterException("카카오 친구 목록 조회에 실패했습니다.", "KAKAO_FRIEND_LIST_ERROR");
        }
    }

    /**
     * 카카오 토큰 정보 조회
     * GET https://kapi.kakao.com/v1/user/access_token_info
     */
    public KakaoTokenInfo getTokenInfo(String accessToken) {
        try {
            KakaoTokenInfo tokenInfo = webClient
                .get()
                .uri("/v1/user/access_token_info")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoTokenInfo.class)
                .timeout(Duration.ofSeconds(30))
                .block();

            log.info("카카오 토큰 정보 조회 성공: userId={}, expiresIn={}", 
                    tokenInfo.getId(), tokenInfo.getExpiresIn());
            return tokenInfo;

        } catch (WebClientResponseException e) {
            log.error("카카오 토큰 정보 조회 실패: statusCode={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            handleKakaoApiError(e);
            return null;
        } catch (Exception e) {
            log.error("카카오 토큰 정보 조회 실패", e);
            throw new NewsletterException("카카오 토큰 정보 조회에 실패했습니다.", "KAKAO_TOKEN_INFO_ERROR");
        }
    }

    /**
     * 카카오 API 에러 처리
     */
    private void handleKakaoApiError(WebClientResponseException e) {
        String errorBody = e.getResponseBodyAsString();
        int statusCode = e.getStatusCode().value();

        switch (statusCode) {
            case 400:
                if (errorBody.contains("KOE006")) {
                    throw new NewsletterException("인증되지 않은 사용자입니다. 카카오 로그인이 필요합니다.", "KAKAO_AUTH_ERROR");
                } else if (errorBody.contains("KOE101")) {
                    throw new NewsletterException("앱키가 유효하지 않습니다.", "KAKAO_APP_KEY_ERROR");
                } else if (errorBody.contains("KOE102")) {
                    throw new NewsletterException("사용자 토큰이 유효하지 않습니다.", "KAKAO_TOKEN_ERROR");
                } else {
                    throw new NewsletterException("잘못된 요청입니다: " + errorBody, "KAKAO_BAD_REQUEST");
                }
            case 401:
                throw new NewsletterException("인증이 필요합니다. 토큰을 확인해주세요.", "KAKAO_UNAUTHORIZED");
            case 403:
                if (errorBody.contains("KOE005")) {
                    throw new NewsletterException("친구 API 사용 권한이 없습니다.", "KAKAO_FRIEND_PERMISSION_ERROR");
                } else if (errorBody.contains("KOE007")) {
                    throw new NewsletterException("메시지 API 사용 권한이 없습니다.", "KAKAO_MESSAGE_PERMISSION_ERROR");
                } else if (errorBody.contains("-402")) {
                    // -402 에러: insufficient scopes - 추가 동의 필요
                    throw new NewsletterException("추가 동의가 필요합니다.", "KAKAO_INSUFFICIENT_SCOPES", errorBody);
                } else {
                    throw new NewsletterException("API 사용 권한이 없습니다.", "KAKAO_FORBIDDEN");
                }
            case 429:
                throw new NewsletterException("API 호출 한도를 초과했습니다. 잠시 후 다시 시도해주세요.", "KAKAO_RATE_LIMIT");
            case 500:
                throw new NewsletterException("카카오 서버 오류가 발생했습니다.", "KAKAO_SERVER_ERROR");
            default:
                throw new NewsletterException("카카오 API 호출 실패: " + statusCode + " - " + errorBody, "KAKAO_API_ERROR");
        }
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean isTokenValid(String accessToken) {
        try {
            KakaoTokenInfo tokenInfo = getTokenInfo(accessToken);
            return tokenInfo != null && tokenInfo.getId() != null;
        } catch (Exception e) {
            log.warn("토큰 유효성 검증 실패", e);
            return false;
        }
    }

    /**
     * 친구에게 메시지 전송 가능 여부 확인
     * Note: allowed_msg는 deprecated되었으므로, 실제 메시지 전송 시 에러를 통해 확인
     */
    public List<KakaoFriend.Friend> getMessageableFriends(String accessToken) {
        KakaoFriend friendList = getFriendList(accessToken);
        if (friendList == null || friendList.getElements() == null) {
            return List.of();
        }

        // allowed_msg가 deprecated되었으므로 모든 친구를 반환
        // 실제 메시지 전송 시 에러를 통해 메시지 전송 불가능한 친구를 확인
        return friendList.getElements();
    }

    /**
     * 즐겨찾기 친구 목록 조회
     */
    public List<KakaoFriend.Friend> getFavoriteFriends(String accessToken) {
        KakaoFriend friendList = getFriendList(accessToken, 0, 100, "asc", "favorite");
        if (friendList == null || friendList.getElements() == null) {
            return List.of();
        }

        return friendList.getElements().stream()
                .filter(friend -> friend.getFavorite() != null && friend.getFavorite())
                .toList();
    }

    /**
     * 닉네임 순으로 친구 목록 조회
     */
    public KakaoFriend getFriendsByNickname(String accessToken, Integer offset, Integer limit) {
        return getFriendList(accessToken, offset, limit, "asc", "nickname");
    }

    /**
     * 토큰 갱신
     * POST https://kauth.kakao.com/oauth/token
     */
    public Map<String, Object> refreshToken(String refreshToken) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("client_id", kakaoAppKey);
            params.add("refresh_token", refreshToken);

            Map<String, Object> response = webClient
                .post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(params)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

            log.info("카카오 토큰 갱신 성공");
            return response;

        } catch (WebClientResponseException e) {
            log.error("카카오 토큰 갱신 실패: statusCode={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            handleKakaoApiError(e);
            return null;
        } catch (Exception e) {
            log.error("카카오 토큰 갱신 실패", e);
            throw new NewsletterException("카카오 토큰 갱신에 실패했습니다.", "KAKAO_TOKEN_REFRESH_ERROR");
        }
    }

    /**
     * 로그아웃
     * POST https://kapi.kakao.com/v1/user/logout
     */
    public Map<String, Object> logout(String accessToken) {
        try {
            Map<String, Object> response = webClient
                .post()
                .uri("/v1/user/logout")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

            log.info("카카오 로그아웃 성공: userId={}", response.get("id"));
            return response;

        } catch (WebClientResponseException e) {
            log.error("카카오 로그아웃 실패: statusCode={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            handleKakaoApiError(e);
            return null;
        } catch (Exception e) {
            log.error("카카오 로그아웃 실패", e);
            throw new NewsletterException("카카오 로그아웃에 실패했습니다.", "KAKAO_LOGOUT_ERROR");
        }
    }

    /**
     * 연결 해제
     * POST https://kapi.kakao.com/v1/user/unlink
     */
    public Map<String, Object> unlink(String accessToken) {
        try {
            Map<String, Object> response = webClient
                .post()
                .uri("/v1/user/unlink")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

            log.info("카카오 연결 해제 성공: userId={}", response.get("id"));
            return response;

        } catch (WebClientResponseException e) {
            log.error("카카오 연결 해제 실패: statusCode={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            handleKakaoApiError(e);
            return null;
        } catch (Exception e) {
            log.error("카카오 연결 해제 실패", e);
            throw new NewsletterException("카카오 연결 해제에 실패했습니다.", "KAKAO_UNLINK_ERROR");
        }
    }

    /**
     * 카카오톡 메시지 전송 권한 확인
     * 확GET https://kapi.kakao.com/v2/user/scopes
     */
    public boolean hasTalkMessagePermission(String accessToken) {
        try {
            Map<String, Object> response = webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/v2/user/scopes")
                    .queryParam("scopes", "talk_message")
                    .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

            if (response != null && response.containsKey("scopes")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> scopes = (List<Map<String, Object>>) response.get("scopes");
                
                return scopes.stream()
                    .anyMatch(scope -> "talk_message".equals(scope.get("id")) && 
                             Boolean.TRUE.equals(scope.get("agreed")));
            }
            
            return false;

        } catch (WebClientResponseException e) {
            log.error("카카오톡 메시지 권한 확인 실패: statusCode={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            
            // 401, 403 에러는 권한이 없다는 의미로 false 반환
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                return false;
            }
            
            // 다른 에러는 예외로 처리
            throw new NewsletterException("카카오톡 메시지 권한 확인 중 오류가 발생했습니다.", "KAKAO_PERMISSION_CHECK_ERROR");
        } catch (Exception e) {
            log.error("카카오톡 메시지 권한 확인 실패", e);
            throw new NewsletterException("카카오톡 메시지 권한 확인 중 오류가 발생했습니다.", "KAKAO_PERMISSION_CHECK_ERROR");
        }
    }

    /**
     * 사용자 동의항목 조회
     * GET https://kapi.kakao.com/v2/user/scopes
     */
    public Map<String, Object> getUserScopes(String accessToken) {
        try {
            Map<String, Object> response = webClient
                .get()
                .uri("/v2/user/scopes")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

            log.info("카카오 사용자 동의항목 조회 성공");
            return response;

        } catch (WebClientResponseException e) {
            log.error("카카오 사용자 동의항목 조회 실패: statusCode={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            handleKakaoApiError(e);
            return null;
        } catch (Exception e) {
            log.error("카카오 사용자 동의항목 조회 실패", e);
            throw new NewsletterException("카카오 사용자 동의항목 조회에 실패했습니다.", "KAKAO_SCOPES_ERROR");
        }
    }

    /**
     * 특정 동의항목 조회
     * GET https://kapi.kakao.com/v2/user/scopes?scopes=["scope1","scope2"]
     */
    public Map<String, Object> getUserScopes(String accessToken, List<String> scopes) {
        try {
            Map<String, Object> response = webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/v2/user/scopes")
                    .queryParam("scopes", "[" + String.join(",", scopes.stream().map(s -> "\"" + s + "\"").toList()) + "]")
                    .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

            log.info("카카오 특정 동의항목 조회 성공: scopes={}", scopes);
            return response;

        } catch (WebClientResponseException e) {
            log.error("카카오 특정 동의항목 조회 실패: statusCode={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            handleKakaoApiError(e);
            return null;
        } catch (Exception e) {
            log.error("카카오 특정 동의항목 조회 실패", e);
            throw new NewsletterException("카카오 특정 동의항목 조회에 실패했습니다.", "KAKAO_SCOPES_ERROR");
        }
    }

    /**
     * 동의 필요 여부 확인
     * 동의항목 동의 내역 조회 API 응답에서 agreed=false인 항목들을 찾아 반환
     */
    public List<String> getRequiredConsentScopes(String accessToken, List<String> requiredScopes) {
        try {
            Map<String, Object> scopesResponse = getUserScopes(accessToken, requiredScopes);
            if (scopesResponse == null || !scopesResponse.containsKey("scopes")) {
                return requiredScopes; // 조회 실패 시 모든 스코프를 필요로 간주
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scopes = (List<Map<String, Object>>) scopesResponse.get("scopes");
            
            return scopes.stream()
                .filter(scope -> !Boolean.TRUE.equals(scope.get("agreed")))
                .map(scope -> (String) scope.get("id"))
                .toList();

        } catch (Exception e) {
            log.error("동의 필요 스코프 확인 실패", e);
            return requiredScopes; // 에러 시 모든 스코프를 필요로 간주
        }
    }

    /**
     * 추가 동의 URL 생성
     * https://kauth.kakao.com/oauth/authorize?client_id={REST_API_KEY}&redirect_uri={REDIRECT_URI}&response_type=code&scope={SCOPE}
     */
    public String generateAdditionalConsentUrl(String accessToken, List<String> scopes) {
        try {
            // 현재 사용자 정보 조회
            KakaoUserInfo userInfo = getUserInfo(accessToken);
            if (userInfo == null) {
                throw new NewsletterException("사용자 정보를 조회할 수 없습니다.", "KAKAO_USER_INFO_ERROR");
            }

            // 실제로 동의가 필요한 스코프만 필터링
            List<String> requiredScopes = getRequiredConsentScopes(accessToken, scopes);
            if (requiredScopes.isEmpty()) {
                log.info("추가 동의가 필요한 스코프가 없습니다: userId={}", userInfo.getId());
                return null; // 모든 스코프에 이미 동의함
            }

            // OpenID Connect 활성화 앱의 경우 openid 스코프 추가
            List<String> finalScopes = new ArrayList<>(requiredScopes);
            if (!finalScopes.contains("openid")) {
                finalScopes.add("openid");
            }

            // 추가 동의 URL 생성
            String scopeParam = String.join(",", finalScopes);
            String consentUrl = String.format(
                "https://kauth.kakao.com/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code&scope=%s",
                kakaoAppKey,
                "https://your-app-domain.com/auth/kakao/callback", // 실제 리다이렉트 URI로 변경 필요
                scopeParam
            );

            log.info("카카오 추가 동의 URL 생성 성공: userId={}, requiredScopes={}", userInfo.getId(), requiredScopes);
            return consentUrl;

        } catch (Exception e) {
            log.error("카카오 추가 동의 URL 생성 실패", e);
            throw new NewsletterException("카카오 추가 동의 URL 생성에 실패했습니다.", "KAKAO_CONSENT_URL_ERROR");
        }
    }

    /**
     * -402 에러 응답에서 required_scopes 추출
     */
    public List<String> extractRequiredScopesFromError(String errorResponse) {
        try {
            // JSON 파싱하여 required_scopes 추출
            @SuppressWarnings("unchecked")
            Map<String, Object> errorMap = objectMapper.readValue(errorResponse, Map.class);
            
            @SuppressWarnings("unchecked")
            List<String> requiredScopes = (List<String>) errorMap.get("required_scopes");
            
            return requiredScopes != null ? requiredScopes : List.of();
            
        } catch (Exception e) {
            log.error("에러 응답에서 required_scopes 추출 실패", e);
            return List.of();
        }
    }

    /**
     * 동의항목 철회
     * POST https://kapi.kakao.com/v1/user/revoke/scopes
     */
    public Map<String, Object> revokeScopes(String accessToken, List<String> scopes) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            scopes.forEach(scope -> params.add("scopes", scope));

            Map<String, Object> response = webClient
                .post()
                .uri("/v1/user/revoke/scopes")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(params)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();

            log.info("카카오 동의항목 철회 성공: scopes={}", scopes);
            return response;

        } catch (WebClientResponseException e) {
            log.error("카카오 동의항목 철회 실패: statusCode={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            handleKakaoApiError(e);
            return null;
        } catch (Exception e) {
            log.error("카카오 동의항목 철회 실패", e);
            throw new NewsletterException("카카오 동의항목 철회에 실패했습니다.", "KAKAO_SCOPE_REVOKE_ERROR");
        }
    }
}
