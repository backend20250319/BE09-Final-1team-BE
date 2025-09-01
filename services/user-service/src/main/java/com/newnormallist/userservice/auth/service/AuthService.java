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

    /**
     * 로그인 로직
     * */
    @Transactional
    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        // 1. 사용자 조회 및 비밀번호 검증
        User user = userRepository.findByEmail(loginRequestDto.getEmail())
                .filter(u -> passwordEncoder.matches(loginRequestDto.getPassword(), u.getPassword()))
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND)); // 로그인 실패 시 USER_NOT_FOUND 사용
        // 2. Access Token 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name(), user.getId());
        // 3. Refresh Token 생성
        String refreshTokenValue = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getRole().name(), user.getId(), loginRequestDto.getDeviceId());
        // 4. Refresh Token 저장 또는 업데이트
        refreshTokenRepository.findByUserId(user.getId())
                .ifPresentOrElse(
                        // 이미 Refresh Token이 있으면 값 업데이트
                        refreshToken -> refreshToken.updateTokenValue(refreshTokenValue),
                        // 없으면 새로 생성하여 저장
                        () -> refreshTokenRepository.save(new RefreshToken(user, refreshTokenValue))
                );
        // 5. 사용자 정보 DTO 생성
        LoginResponseDto.UserInfoDto userInfo = new LoginResponseDto.UserInfoDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
        // 6. 토큰과 사용자 정보를 DTO에 담아 반환
        return new LoginResponseDto(accessToken, refreshTokenValue, userInfo);
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

        // ✅ 2. 입력된 이름과 DB의 사용자 이름이 일치하는지 확인
        if (!user.getName().equals(request.getName())) {
            // 이름이 일치하지 않아도 동일한 에러를 반환하여 보안 강화
            throw new UserException(ErrorCode.USER_NOT_FOUND);
        }

        // 3. 임시 재설정 토큰 생성
        String token = UUID.randomUUID().toString();
        // 4. 토큰 만료 시간 설정 (예: 30분 후)
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(30);

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
        // 1. 비밀번호 확인
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new UserException(ErrorCode.PASSWORD_MISMATCH);
        }
        // 2. DB에서 토큰 조회
        PasswordResetToken resetToken = passwordResetTokenRepostitory.findByToken(request.getToken())
                .orElseThrow(() -> new UserException(ErrorCode.INVALID_RESET_TOKEN));
        // 3. 토큰이 만료되었는지 확인
        if (resetToken.isExpired()) {
            passwordResetTokenRepostitory.delete(resetToken);  // 토큰 만료 시 DB에서 삭제
            throw new UserException(ErrorCode.EXPIRED_RESET_TOKEN);
        }
        // 4. 토큰에 연결된 사용자 정보로 비밀번호 변경
        User user = resetToken.getUser();
        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        // 5. 사용된 토큰은 삭제
        passwordResetTokenRepostitory.delete(resetToken);
    }
    /**
     * 추가 정보 입력 로직 (소셜 로그인 후)
     */
    @Transactional
    public LoginResponseDto provideAdditionalInfo(String tempToken, AdditionalInfoRequestDto requestDto) {
        //1. Bearer 접두어 제거
        if (tempToken != null && tempToken.startsWith("Bearer ")) {
            tempToken = tempToken.substring(7);
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

        // 5. 최종 access, refresh 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name(), user.getId());
        String refreshTokenValue = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getRole().name(), user.getId(), requestDto.getDeviceId());

        refreshTokenRepository.findByUserId(user.getId())
                .ifPresentOrElse(
                        refreshToken -> refreshToken.updateTokenValue(refreshTokenValue),
                        () -> refreshTokenRepository.save(new RefreshToken(user, refreshTokenValue))
                );
        LoginResponseDto.UserInfoDto userInfo = new LoginResponseDto.UserInfoDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
        return new LoginResponseDto(accessToken, refreshTokenValue, userInfo);
    }
}
