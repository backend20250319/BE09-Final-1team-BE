package com.newnormallist.userservice.auth.service;

import com.newnormallist.userservice.auth.dto.GoogleUserInfo;
import com.newnormallist.userservice.auth.dto.KakaoUserInfo;
import com.newnormallist.userservice.auth.dto.OAuth2UserInfo;
import com.newnormallist.userservice.user.entity.User;
import com.newnormallist.userservice.user.entity.UserRole;
import com.newnormallist.userservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 기본 OAuth2UserService 객체 생성
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        // 2. userRequest를 통해 카카오/구글에서 사용자 정보 로드 준비
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        // 3. 카카오/구글에 따라 적절한 UserInfo 객체 생성
        OAuth2UserInfo userInfo;
        if (registrationId.equals("google")) {
            userInfo = new GoogleUserInfo(oAuth2User.getAttributes());
        } else if (registrationId.equals("kakao")) {
            userInfo = new KakaoUserInfo(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 공급자입니다: " + registrationId);
        }
        // 4. User 엔티티로 저장 또는 업데이트
        User user = saveOrUpdate(userInfo);
        // 5. DefaultOAuth2User 객체 반환
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
                oAuth2User.getAttributes(),
                userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName()
            );
        }

        // User 엔티티 저장 또는 업데이트를 위한 공통 메소드
        private User saveOrUpdate(OAuth2UserInfo userInfo) {
            User user = userRepository.findByEmail(userInfo.getEmail())
                    .map(entity -> entity.updateSocialInfo(userInfo.getName(), userInfo.getProvider(), userInfo.getProviderId()))
                    .orElseGet(() -> User.builder()
                            .email(userInfo.getEmail())
                            .name(userInfo.getName())
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .role(UserRole.USER)
                            .provider(userInfo.getProvider())
                            .providerId(userInfo.getProviderId())
                            .build());

            return userRepository.save(user);
        }
    }
