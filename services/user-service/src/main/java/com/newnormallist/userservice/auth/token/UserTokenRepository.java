package com.newnormallist.userservice.auth.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UserToken 엔티티를 위한 리포지토리 인터페이스
 */
@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    /**
     * 사용자 ID와 제공자로 토큰 조회
     */
    Optional<UserToken> findByUserIdAndProvider(Long userId, TokenProvider provider);

    /**
     * 사용자 ID로 모든 토큰 조회
     */
    List<UserToken> findByUserId(Long userId);

    /**
     * 제공자별 토큰 조회
     */
    List<UserToken> findByProvider(TokenProvider provider);

    /**
     * 만료된 토큰 조회
     */
    @Query("SELECT ut FROM UserToken ut WHERE ut.expiresAt < :now")
    List<UserToken> findExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * 사용자 ID와 제공자로 토큰 삭제
     */
    @Transactional
    @Modifying
    void deleteByUserIdAndProvider(Long userId, TokenProvider provider);

    /**
     * 사용자 ID로 모든 토큰 삭제
     */
    @Transactional
    @Modifying
    void deleteByUserId(Long userId);

    /**
     * 만료된 토큰 삭제
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM UserToken ut WHERE ut.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * 특정 제공자의 만료된 토큰 삭제
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM UserToken ut WHERE ut.provider = :provider AND ut.expiresAt < :now")
    void deleteExpiredTokensByProvider(@Param("provider") TokenProvider provider, @Param("now") LocalDateTime now);
}
