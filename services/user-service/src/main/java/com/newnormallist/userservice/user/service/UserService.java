package com.newnormallist.userservice.user.service;

import com.newnormallist.userservice.analytics.service.UserAnalyticsService;
import com.newnormallist.userservice.auth.repository.RefreshTokenRepository;
import com.newnormallist.userservice.common.ErrorCode;
import com.newnormallist.userservice.history.dto.ReadHistoryResponse;
import com.newnormallist.userservice.history.dto.UserBehaviorAnalysis;
import com.newnormallist.userservice.history.dto.CategoryPreferenceResponse;
import com.newnormallist.userservice.history.dto.UserInterestResponse;
import com.newnormallist.userservice.history.entity.UserReadHistory;
import com.newnormallist.userservice.history.repository.UserReadHistoryRepository;
import com.newnormallist.userservice.user.dto.*;
import com.newnormallist.userservice.user.entity.NewsCategory;
import com.newnormallist.userservice.user.entity.User;
import com.newnormallist.userservice.common.exception.UserException;
import com.newnormallist.userservice.user.entity.UserStatus;
import com.newnormallist.userservice.user.repository.UserRepository;
import com.newnormallist.userservice.user.repository.NewsRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserReadHistoryRepository userReadHistoryRepository;
    private final NewsRepository newsRepository;
    private final UserAnalyticsService userAnalyticsService;

    /**
     * 회원가입 로직
     * @param signupRequest 회원가입 요청 정보
     */
    @Transactional
    public void signup(SignupRequest signupRequest) {
        // 1. 이메일 중복 검사
        validateEmailDuplication(signupRequest.getEmail());
        // 2. 비밀번호에 개인정보 포함 여부 검사
        validatePasswordSecurity(signupRequest.getPassword(), signupRequest.getEmail(), signupRequest.getName());
        // 3. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());
        // 4. User 엔티티 생성
        User user = User.builder()
                .email(signupRequest.getEmail())
                .password(encodedPassword)
                .name(signupRequest.getName())
                .birthYear(signupRequest.getBirthYear())
                .gender(signupRequest.getGender())
                .hobbies(signupRequest.getHobbies() != null ? signupRequest.getHobbies() : new HashSet<>())
                .build();
        // 5. 사용자 저장
        userRepository.save(user);
        log.info("사용자 회원가입 완료 - 이메일: {}", signupRequest.getEmail());
    }
    // 이메일 중복 검사 통합 메서드
    private void validateEmailDuplication(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new UserException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    /**
     * 마이페이지 정보 조회 로직
     * @param userId 현재 인증된 사용자 ID
     * @return MyPageResponse 마이페이지 정보
     */
    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(Long userId) {
        // 1. 사용자 조회
        User user = findByUserId(userId);
        // 2. 조회된 사용자 정보를 DTO로 변환
        return new MyPageResponse(user);
    }
    /**
     * 마이페이지 정보 수정 로직
     * @param userId 현재 인증된 사용자 ID
     * @param request 수정할 정보
     */
    @Transactional
    public void updateMyPage(Long userId, UserUpdateRequest request) {
        // 1. 사용자 조회
        User user = findByUserId(userId);
        // 2. 비밀번호 변경 로직
        updatePasswordIfRequested(user, request);
        // 3. 뉴스레터 수신 여부 및 관심사 업데이트
        user.updateProfile(request.getLetterOk(), request.getHobbies());
        log.info("사용자 마이페이지 정보 수정 완료 - 사용자 ID: {}", userId);
    }

    /**
     * 요청에 새로운 비밀번호가 포함된 경우, 유효성 검사 후 비밀번호를 업데이트하는 헬퍼 메소드
     * */
    private void updatePasswordIfRequested(User user, UserUpdateRequest request) {
        // 1. 요청에 새로운 비밀번호가 없으면 즉시 종료
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return;
        }
        // 2. 현재 비밀번호가 누락되었는지 확인
        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new UserException(ErrorCode.CURRENT_PASSWORD_REQUIRED);
        }
        //3. 현재 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UserException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        }
        // 3-1. 새로운 비밀번호에 개인정보 포함 여부 검사
        validatePasswordSecurity(request.getNewPassword(), user.getEmail(), user.getName());
        // 4. 새로운 비밀번호와 비밀번호 확인 일치 여부 확인
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new UserException(ErrorCode.PASSWORD_MISMATCH);
        }
        // 5. 새로운 비밀번호로 업데이트
        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
        user.updatePassword(encodedNewPassword);
    }

    /**
     * 회원 탈퇴 로직
     * @param userId 현재 인증된 사용자 ID
     */
    @Transactional
    public void deleteUser(Long userId) {
        // 1. 사용자 조회
        User user = findByUserId(userId);

        // 2. 리프레시 토큰 삭제
        refreshTokenRepository.deleteByUserId(userId);

        // 3. 상태를 DELETED로 변경 (Soft Delete)
        user.changeStatus(UserStatus.DELETED);

        log.info("사용자 탈퇴 처리 완료 - 사용자 ID: {}", userId);;
    }
    /**
     * 뉴스 카테고리 목록 조회 로직
     * @return List<CategoryResponse> 뉴스 카테고리 목록
     * */
    public List<CategoryResponse> getNewsCategories() {
        List<CategoryResponse> categories = Arrays.stream(NewsCategory.values())
                .map(category -> new CategoryResponse(
                        category.name(),
                        category.getCategoryName(),
                        category.getIcon()))
                .toList();
        log.info("뉴스 카테고리 목록 조회 완료 - 카테고리 수: {}", categories.size());
        return categories;
    }
    /**
     * 관리자용 사용자 정보 조회 로직
     * @param status 필터링할 회원 상태
     * @param keyword 검색 키워드 (이메일 또는 이름)
     * @param pageable 페이지 정보
     * @return Page<UserAdminResponse> 페이징 처리된 회원 목록
     */
    @Transactional(readOnly = true)
    public Page<UserAdminResponse> getUsersForAdmin(UserStatus status, String keyword, Pageable pageable) {
        // 1. Specification을 사용한 동적 쿼리 생성
        Specification<User> spec = (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction(); // 기본적으로 AND 조건으로 시작
            // 1-1. status 필터링 조건
            if (status != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
            }
            // 1-2. keyword 검색 조건 (이메일 또는 이름)
            if (keyword != null && !keyword.isBlank()) {
                Predicate emailLike = criteriaBuilder.like(root.get("email"), "%" + keyword + "%");
                Predicate nameLike = criteriaBuilder.like(root.get("name"), "%" + keyword + "%");
                predicate= criteriaBuilder.and(predicate, criteriaBuilder.or(emailLike, nameLike));
            }
            return predicate;
        };
        // 2. 페이징과 Specification을 적용하여 데이터 조회
        Page<User> users = userRepository.findAll(spec, pageable);
        // 3. Page<UserAdminResponse>로 변환
        return users.map(UserAdminResponse::new);
    }
    /**
     * 관리자용 사용자 정보 삭제 로직
     * @param userId 삭제할 사용자 ID
     */
    @Transactional
    public void adminHardDeleteUser(Long userId) {
        int affected = userRepository.hardDeleteIFDeleted(userId);
        if (affected == 1) {
            log.info("관리자에 의한 사용자 하드 삭제 완료 - 사용자 ID: {}", userId);
            return;
        }
        // 실패한 경우
        User user = findByUserId(userId);
        if (user.getStatus() != UserStatus.DELETED) {
            throw new UserException(ErrorCode.INVALID_STATUS);
        }
        throw new UserException(ErrorCode.OPERATION_FAILED);
    }
    /**
     * 관리자용 배치 하드 삭제 로직
     * @param before 삭제 기준 날짜 (이 날짜 이전의 사용자)
     * @return int 삭제된 사용자 수
     */
    @Transactional
    public int adminPurgeDeleted(LocalDateTime before) {
        // 1. 상태가 DELETED인 사용자 중, updatedAt이 before보다 이전인 사용자 삭제
        int deletedCount = userRepository.deleteByStatusBefore(UserStatus.DELETED, before);
        log.info("관리자에 의한 배치 하드 삭제 완료 - 삭제된 사용자 수: {}, before = {}", deletedCount, before);
        return deletedCount;
    }

    /**
     * 사용자 조회 공통 메서드
     * */
    private User findByUserId(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 비밀번호에 개인정보가 들어가 있는지 검사하는 메서드
     * */
    private void validatePasswordSecurity(String password, String email, String name) {
        String lowerPassword = password.toLowerCase();
        String emailId = email.split("@")[0];

        if (lowerPassword.contains(name)) {
            throw new UserException(ErrorCode.PASSWORD_CONTAINS_NAME);
        }
        if (lowerPassword.contains(emailId)) {
            throw new UserException(ErrorCode.PASSWORD_CONTAINS_EMAIL);
        }
    }

    /**
     * 사용자 행동 분석 조회 (내부 서비스용) - 위임
     */
    @Transactional(readOnly = true)
    public UserBehaviorAnalysis getUserBehaviorAnalysis(Long userId) {
        return userAnalyticsService.getUserBehaviorAnalysis(userId);
    }

    /**
     * 사용자 카테고리 선호도 조회 (내부 서비스용) - 위임
     */
    @Transactional(readOnly = true)
    public CategoryPreferenceResponse getCategoryPreferences(Long userId) {
        return userAnalyticsService.getCategoryPreferences(userId);
    }

    /**
     * 사용자 관심사 분석 조회 (내부 서비스용) - 위임
     */
    @Transactional(readOnly = true)
    public UserInterestResponse getUserInterests(Long userId) {
        return userAnalyticsService.getUserInterests(userId);
    }

    /**
     * 사용자 관심사 점수 맵 조회 (내부 서비스용) - 위임
     */
    @Transactional(readOnly = true)
    public Map<String, Double> getInterestScores(Long userId) {
        return userAnalyticsService.getInterestScores(userId);
    }

    /**
     * 사용자 상위 관심사 목록 조회 (내부 서비스용) - 위임
     */
    @Transactional(readOnly = true)
    public List<String> getTopInterests(Long userId) {
        return userAnalyticsService.getTopInterests(userId);
    }

    /**
     * 앱 내 알림을 허용한 사용자 목록 조회
     * @return 앱 내 알림을 허용한 사용자 ID 목록
     */
    @Transactional(readOnly = true)
    public List<Long> getInAppNotificationEnabledUsers() {
        log.debug("앱 내 알림을 허용한 사용자 목록 조회");
        
        try {
            // 활성 사용자 중에서 뉴스레터 수신 동의(letterOk = true)한 사용자들을 조회
            // letterOk 필드를 앱 내 알림 허용으로 해석
            List<Long> enabledUsers = userRepository.findInAppNotificationEnabledUserIds();
            
            log.info("앱 내 알림 허용 사용자 수: {}", enabledUsers.size());
            return enabledUsers;
            
        } catch (Exception e) {
            log.error("앱 내 알림 허용 사용자 목록 조회 실패", e);
            // 오류 발생 시 빈 목록 반환
            return new ArrayList<>();
        }
    }

}
