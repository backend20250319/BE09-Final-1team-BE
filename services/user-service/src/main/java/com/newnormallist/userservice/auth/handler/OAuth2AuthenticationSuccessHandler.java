package com.newnormallist.userservice.auth.handler;

import com.newnormallist.userservice.auth.dto.GoogleUserInfo;
import com.newnormallist.userservice.auth.dto.KakaoUserInfo;
import com.newnormallist.userservice.auth.dto.OAuth2UserInfo;
import com.newnormallist.userservice.auth.entity.RefreshToken;
import com.newnormallist.userservice.auth.jwt.JwtTokenProvider;
import com.newnormallist.userservice.auth.repository.CookieOAuth2AuthorizationRequestRepository;
import com.newnormallist.userservice.auth.repository.RefreshTokenRepository;
import com.newnormallist.userservice.common.util.CookieUtil;
import com.newnormallist.userservice.user.entity.User;
import com.newnormallist.userservice.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final CookieOAuth2AuthorizationRequestRepository cookieOAuth2AuthorizationRequestRepository;

    @Value("${oauth2.redirect.frontend-callback-url}")
    private String finalRedirectUrl;

    @Value("${oauth2.redirect.frontend-additional-info-url}")
    private String additionalInfoUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        clearAuthenticationAttributes(request, response);

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        OAuth2UserInfo userInfo;
        if (registrationId.equals("google")) {
            userInfo = new GoogleUserInfo(oAuth2User.getAttributes());
        } else if (registrationId.equals("kakao")) {
            userInfo = new KakaoUserInfo(oAuth2User.getAttributes());
        } else {
            // 다른 provider가 추가될 경우에 대한 예외 처리
            log.error("Unsupported provider: {}", registrationId);
            throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        }

        String email = userInfo.getEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("OAuth2 login succeeded but user not found in DB."));
        // 신규 소셜 로그인 사용자의 경우
        if (user.getBirthYear() == null || user.getGender() == null) {
            log.info("신규 소셜 로그인 사용자입니다. 추가 정보 입력 페이지로 리디렉션합니다. Email: {}", email);
            String tempToken = jwtTokenProvider.createTempToken(user.getEmail(), user.getId());
            // 임시 토큰을 쿠키와 URL 파라미터 모두로 전달 (프론트엔드 호환성을 위해)
            int tempTokenMaxAge = 10 * 60; // 10분
            CookieUtil.addCookie(response, "temp_token", tempToken, tempTokenMaxAge);
            // 사용자 정보와 함께 URL로 리디렉션
            String targetUrl = createAdditionalInfoRedirectUrl(user);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } else {
            // 기존 소셜 로그인 사용자의 경우
            log.info("기존 소셜 로그인 사용자입니다. 최종 로그인 처리를 진행합니다. Email: {}", email);
            String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name(), user.getId());
            // 소셜 로그인에서는 deviceId가 없으므로 email을 대신 사용
            String refreshTokenValue = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getRole().name(), user.getId(), email);
            refreshTokenRepository.findByUserId(user.getId())
                    .ifPresentOrElse(
                            refreshToken -> refreshToken.updateTokenValue(refreshTokenValue),
                            () -> refreshTokenRepository.save(new RefreshToken(user, refreshTokenValue))
                    );
            // 토큰을 쿠키와 URL 파라미터 모두로 전달 (프론트엔드 호환성을 위해)
            int accessTokenMaxAge = (int) (jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000);
            int refreshTokenMaxAge = (int) (jwtTokenProvider.getRefreshTokenValidityInMilliseconds() / 1000);
            CookieUtil.addCookie(response, "access-token", accessToken, accessTokenMaxAge);
            CookieUtil.addCookie(response, "refresh-token", refreshTokenValue, refreshTokenMaxAge);

            String targetUrl = createFinalRedirectUrl(user);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }

    private String createFinalRedirectUrl() {
        return finalRedirectUrl;
    }

    private String createFinalRedirectUrl(User user) {
        return UriComponentsBuilder.fromUriString(finalRedirectUrl)
                .queryParam("success", "true")
                .queryParam("userId", user.getId())
                .queryParam("email", user.getEmail())
                .queryParam("nickname", user.getName())
                .queryParam("role", user.getRole().name())
                .build().toUriString();
    }

    private String createAdditionalInfoRedirectUrl() {
        return additionalInfoUrl;
    }

    private String createAdditionalInfoRedirectUrl(User user) {
        return UriComponentsBuilder.fromUriString(additionalInfoUrl)
                .queryParam("success", "true")
                .queryParam("userId", user.getId())
                .queryParam("email", user.getEmail())
                .queryParam("nickname", user.getName())
                .queryParam("role", user.getRole().name())
                .queryParam("needsAdditionalInfo", "true")
                .build().toUriString();
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        cookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }
}