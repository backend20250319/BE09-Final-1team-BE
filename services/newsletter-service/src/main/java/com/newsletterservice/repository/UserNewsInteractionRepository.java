package com.newsletterservice.repository;

import com.newsletterservice.entity.UserNewsInteraction;
import com.newsletterservice.entity.InteractionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserNewsInteractionRepository extends JpaRepository<UserNewsInteraction, Long> {

    // 사용자별, 상호작용 타입별, 기간별 조회
    List<UserNewsInteraction> findByUserIdAndTypeAndCreatedAtAfter(
            Long userId, InteractionType type, LocalDateTime after);

    // 사용자별, 기간별 조회 (모든 상호작용 타입)
    List<UserNewsInteraction> findByUserIdAndCreatedAtAfter(
            Long userId, LocalDateTime after);

    // 카테고리별 상호작용 수 조회
    long countByUserIdAndCategoryAndTypeAndCreatedAtAfter(
            Long userId, String category, InteractionType type, LocalDateTime after);

    // 사용자 전체 상호작용 수 조회
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    // 사용자의 최근 상호작용 뉴스 ID 목록
    @Query("SELECT DISTINCT ui.newsId FROM UserNewsInteraction ui " +
            "WHERE ui.userId = :userId AND ui.type = :type " +
            "AND ui.createdAt >= :after ORDER BY ui.createdAt DESC")
    List<Long> findRecentInteractedNewsIds(
            @Param("userId") Long userId,
            @Param("type") InteractionType type,
            @Param("after") LocalDateTime after);

    // 카테고리별 평균 읽기 시간
    @Query("SELECT ui.category, AVG(ui.readingDuration) FROM UserNewsInteraction ui " +
            "WHERE ui.userId = :userId AND ui.readingDuration IS NOT NULL " +
            "AND ui.createdAt >= :after GROUP BY ui.category")
    List<Object[]> getAverageReadingTimeByCategory(
            @Param("userId") Long userId,
            @Param("after") LocalDateTime after);
}