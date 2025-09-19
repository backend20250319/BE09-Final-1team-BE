package com.newnormallist.userservice.user.entity;

import com.newnormallist.userservice.common.ErrorCode;
import com.newnormallist.userservice.common.exception.UserException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "gender")
    private String gender;

    @Column(name = "letter_ok", nullable = false)
    @Builder.Default
    private Boolean letterOk = false;

    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_categories", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "category")
    @Builder.Default
    private Set<NewsCategory> hobbies = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "provider")
    private String provider; // 예: "google", "kakao"

    @Column(name = "provider_id")
    private String providerId; // "google" 또는 "kakao"에서 제공하는 고유 ID

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateProfile(@NotNull(message = "뉴스레터 수신 동의 여부는 필수입니다.") Boolean letterOk, Set<String> hobbies) {
        this.letterOk = letterOk;
        this.hobbies = convertHobbiesToEnumSet(hobbies);
    }

    public void changeStatus(UserStatus userStatus) {
        this.status = userStatus;
    }

    // 소셜 로그인 사용자의 정보 업데이트를 위한 메소드
    public User updateSocialInfo(String name, String provider, String providerId) {
        this.name = name;
        this.provider = provider;
        this.providerId = providerId;
        return this;
    }

    public void updateAdditionalInfo(
            @NotNull(message = "출생연도는 필수입니다.") Integer birthYear,
            @NotNull(message = "성별은 필수입니다.") String gender, List<String> hobbies) {
        this.birthYear = birthYear;
        this.gender = gender;
        this.hobbies = convertHobbiesToEnumSet(hobbies);
    }

    private Set<NewsCategory> convertHobbiesToEnumSet(Collection<String> hobbies) {
        if (hobbies == null || hobbies.isEmpty()) {
            return new HashSet<>(); // 비어있는 Set 반환
        }
        return hobbies.stream()
                .map(hobby -> {
                    try {
                        // 문자열을 대문자로 바꿔서 NewsCategory Enum으로 변환
                        return NewsCategory.valueOf(hobby.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // 유효하지 않은 카테고리면 예외 발생
                        throw new UserException(ErrorCode.INVALID_CATEGORY);
                    }
                })
                .collect(Collectors.toSet());
    }
}