package com.newnormallist.userservice.auth.service;

import com.newnormallist.userservice.auth.dto.*;
import com.newnormallist.userservice.auth.entity.RefreshToken;
import com.newnormallist.userservice.auth.event.OnPasswordResetRequestEvent;
import com.newnormallist.userservice.auth.jwt.JwtTokenProvider;
import com.newnormallist.userservice.auth.repository.RefreshTokenRepository;
import com.newnormallist.userservice.auth.token.PasswordResetToken;
import com.newnormallist.userservice.auth.token.PasswordResetTokenRepostitory;
import com.newnormallist.userservice.common.ErrorCode;
import com.newnormallist.userservice.user.entity.User;
import com.newnormallist.userservice.common.exception.UserException;
import com.newnormallist.userservice.user.repository.UserRepository;
import com.newnormallist.userservice.user.dto.MyPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordResetTokenRepostitory passwordResetTokenRepostitory;
    private final ApplicationEventPublisher eventPublisher;

    private static final int PASSWORD_RESET_TOKEN_EXPIRY_MINUTES = 30;
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 로그인 로직
     * */
    @Transactional
    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        // 1. 사용자 조회 및 비밀번호 검증
        User user = userRepository.findByEmail(loginRequestDto.getEmail())
                .filter(u -> passwordEncoder.matches(loginRequestDto.getPassword(), u.getPassword()))
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND)); // 로그인 실패 시 USER_NOT_FOUND 사
        // 2. 사용자 인증 후 토큰 생성 및 저장하는 메소드 호출
        return issueTokensAndBuildResponse(user, loginRequestDto.getDeviceId());
    }
    /**
     * 토큰 갱신 로직
     * */
    @Transactional
    public AccessTokenResponseDto refreshToken(RefreshTokenRequestDto request) {
        // 1. Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new UserException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        // 2. Refresh Token 조회
        RefreshToken refreshToken = refreshTokenRepository.findByTokenValue(request.getRefreshToken())
                .orElseThrow(() -> new UserException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
        // 3. Refresh Token의 사용자 정보 조회 및 새로운 Access Token 생성
        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name(), user.getId());
        // 4. 새로운 Access Token을 DTO에 담아 반환
        return new AccessTokenResponseDto(newAccessToken);
    }
    /**
     * 로그아웃 로직
     * */
    @Transactional
    public void logout(RefreshTokenRequestDto request) {
        // 클라이언트로부터 받은 Refresh Token 값으로 DB에서 직접 삭제를 시도합니다.
        // 해당 토큰이 DB에 없으면 아무 일도 일어나지 않고, 있으면 삭제됩니다.
        // 이것만으로 로그아웃의 목적은 완벽하게 달성됩니다.
        refreshTokenRepository.deleteByTokenValue(request.getRefreshToken());
    }
    /**
     * 비밀번호 재설정 요청 로직
     */
    @Transactional
    public void requestPasswordReset(PasswordFindRequest request) {
        // 1. 이메일로 사용자가 존재하는지 확인
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 입력된 이름과 DB의 사용자 이름이 일치하는지 확인
        if (!user.getName().equals(request.getName())) {
            // 이름이 일치하지 않아도 동일한 에러를 반환하여 보안 강화
            throw new UserException(ErrorCode.USER_NOT_FOUND);
        }

        // 3. 임시 재설정 토큰 생성
        String token = UUID.randomUUID().toString();
        // 4. 토큰 만료 시간 설정 (30분 후)
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(PASSWORD_RESET_TOKEN_EXPIRY_MINUTES);

        // 5. DB에 토큰 정보 저장
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
        passwordResetTokenRepostitory.save(resetToken);

        // 6. 트랜잭션 완료 후 이메일 발송 이벤트 발행
        eventPublisher.publishEvent(new OnPasswordResetRequestEvent(user.getEmail(), token));
    }
    /**
     * 비밀번호 재설정 로직
     */
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        // 1. 기본 입력값 검증 (가장 빠른 실패)
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new UserException(ErrorCode.PASSWORD_MISMATCH);
        }
        // 2. 토큰 조회 및 유효성 검증
        PasswordResetToken resetToken = passwordResetTokenRepostitory.findByToken(request.getToken())
                .orElseThrow(() -> new UserException(ErrorCode.INVALID_RESET_TOKEN));
        // 3. 토큰 만료 확인
        if (resetToken.isExpired()) {
            passwordResetTokenRepostitory.delete(resetToken);
            throw new UserException(ErrorCode.EXPIRED_RESET_TOKEN);
        }
        // 4. 사용자 정보 조회
        User user = resetToken.getUser();
        // 5. 비밀번호 보안 검증 (사용자 정보가 필요한 검증)
        validatePasswordSecurity(request.getNewPassword(), user.getEmail(), user.getName());
        // 6. 비밀번호 업데이트
        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        // 7. 사용된 토큰 삭제
        passwordResetTokenRepostitory.delete(resetToken);
    }
    /**
     * 추가 정보 입력 로직 (소셜 로그인 후)
     */
    @Transactional
    public LoginResponseDto provideAdditionalInfo(String tempToken, AdditionalInfoRequestDto requestDto) {
        //1. Bearer 접두어 제거
        if (tempToken != null && tempToken.startsWith(BEARER_PREFIX)) {
            tempToken = tempToken.substring(BEARER_PREFIX.length());
        }
        // 2. 임시 토큰 유효성 검증 및 사용자 ID 추출
        if (!jwtTokenProvider.validateToken(tempToken)) {
            throw new UserException(ErrorCode.INVALID_TOKEN);
        }
        Long userId = jwtTokenProvider.getUserIdFromJWT(tempToken);
        // 3. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
        // 4. 사용자 정보 업데이트
        user.updateAdditionalInfo(
                requestDto.getBirthYear(),
                requestDto.getGender(),
                requestDto.getHobbies()// hobbies를 처리하는 로직은 User 엔티티에 맞게 구현
        );
        userRepository.save(user);
        // 5. 토큰 발급 및 응답 생성
        return issueTokensAndBuildResponse(user, requestDto.getDeviceId());
    }
    /**
     * 비밀번호에 개인정보가 들어가 있는지 검사하는 메서드
     * */
    private void validatePasswordSecurity(String password, String email, String name) {
        String lowerPassword = password.toLowerCase();
        String emailId = email.split("@")[0].toLowerCase();
        String lowerName = name.toLowerCase();
        // 사용자 이름이 비밀번호에 포함되어 있는지 검사
        if (lowerPassword.contains(lowerName)) {
            throw new UserException(ErrorCode.PASSWORD_CONTAINS_NAME);
        }
        // 이메일 아이디가 비밀번호에 포함되어 있는지 검사
        if (lowerPassword.contains(emailId)) {
            throw new UserException(ErrorCode.PASSWORD_CONTAINS_EMAIL);
        }
    }
    /**
     * 사용자 정보와 기기 ID를 받아 토큰 발급, 저장 후 최종 응답 DTO 생성 공통 메서드
     * */
    private LoginResponseDto issueTokensAndBuildResponse(User user, String deviceId) {
        // 1. Access Token 및 Refresh Token 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name(), user.getId());
        String refreshTokenValue = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getRole().name(), user.getId(), deviceId);
        // 2. Refresh Token 엔티티 생성 및 저장
        refreshTokenRepository.findByUserId(user.getId())
                .ifPresentOrElse(
                        refreshToken -> refreshToken.updateTokenValue(refreshTokenValue),
                        () -> refreshTokenRepository.save(new RefreshToken(user, refreshTokenValue))
                );
        // 3. 응답에 포함될 사용자 정보 DTO 생성
        LoginResponseDto.UserInfoDto userInfo = new LoginResponseDto.UserInfoDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
        // 4. 최종 응답 DTO 생성 및 반환
        return new LoginResponseDto(accessToken, refreshTokenValue, userInfo);
    }

    /**
     * 사용자 정보 조회 로직
     * @param userId 사용자 ID
     * @return MyPageResponse 사용자 정보
     */
    @Transactional(readOnly = true)
    public MyPageResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
        return new MyPageResponse(user);
    }

}
