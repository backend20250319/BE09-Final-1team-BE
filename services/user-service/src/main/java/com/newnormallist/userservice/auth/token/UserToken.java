package com.newnormallist.userservice.auth.token;

import com.newnormallist.userservice.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자의 OAuth 토큰 정보를 저장하는 엔티티
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_token", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "provider"})
})
public class UserToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenProvider provider;

    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "scope")
    private String scope;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public UserToken(User user, TokenProvider provider, String accessToken, 
                    String refreshToken, LocalDateTime expiresAt, String scope) {
        this.user = user;
        this.provider = provider;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.scope = scope;
    }

    /**
     * 토큰이 만료되었는지 확인
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 액세스 토큰 업데이트
     */
    public void updateAccessToken(String newAccessToken) {
        this.accessToken = newAccessToken;
    }

    /**
     * 액세스 토큰과 만료 시간 업데이트
     */
    public void updateAccessToken(String newAccessToken, LocalDateTime newExpiresAt) {
        this.accessToken = newAccessToken;
        this.expiresAt = newExpiresAt;
    }

    /**
     * 리프레시 토큰 업데이트
     */
    public void updateRefreshToken(String newRefreshToken) {
        this.refreshToken = newRefreshToken;
    }

    /**
     * 토큰 정보 전체 업데이트
     */
    public void updateTokens(String newAccessToken, String newRefreshToken, LocalDateTime newExpiresAt) {
        this.accessToken = newAccessToken;
        this.refreshToken = newRefreshToken;
        this.expiresAt = newExpiresAt;
    }
}
