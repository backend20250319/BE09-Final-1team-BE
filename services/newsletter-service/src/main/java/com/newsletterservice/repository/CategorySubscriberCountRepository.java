package com.newsletterservice.repository;

import com.newsletterservice.entity.CategorySubscriberCount;
import com.newsletterservice.entity.NewsCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategorySubscriberCountRepository extends JpaRepository<CategorySubscriberCount, Long> {

    /**
     * 카테고리별 구독자 수 조회
     */
    Optional<CategorySubscriberCount> findByCategory(NewsCategory category);

    /**
     * 모든 카테고리별 구독자 수 조회 (활성 구독자 수 기준 내림차순)
     */
    @Query("SELECT c FROM CategorySubscriberCount c ORDER BY c.activeSubscriberCount DESC")
    List<CategorySubscriberCount> findAllOrderByActiveSubscriberCountDesc();

    /**
     * 활성 구독자 수가 0보다 큰 카테고리만 조회
     */
    @Query("SELECT c FROM CategorySubscriberCount c WHERE c.activeSubscriberCount > 0 ORDER BY c.activeSubscriberCount DESC")
    List<CategorySubscriberCount> findActiveCategoriesOrderBySubscriberCountDesc();

    /**
     * 특정 카테고리의 활성 구독자 수 증가
     */
    @Modifying
    @Query("UPDATE CategorySubscriberCount c SET c.activeSubscriberCount = c.activeSubscriberCount + 1, c.totalSubscriberCount = c.totalSubscriberCount + 1 WHERE c.category = :category")
    void incrementSubscriberCount(@Param("category") NewsCategory category);

    /**
     * 특정 카테고리의 활성 구독자 수 감소
     */
    @Modifying
    @Query("UPDATE CategorySubscriberCount c SET c.activeSubscriberCount = CASE WHEN c.activeSubscriberCount > 0 THEN c.activeSubscriberCount - 1 ELSE 0 END WHERE c.category = :category")
    void decrementActiveSubscriberCount(@Param("category") NewsCategory category);

    /**
     * 특정 카테고리의 총 구독자 수 감소
     */
    @Modifying
    @Query("UPDATE CategorySubscriberCount c SET c.totalSubscriberCount = CASE WHEN c.totalSubscriberCount > 0 THEN c.totalSubscriberCount - 1 ELSE 0 END WHERE c.category = :category")
    void decrementTotalSubscriberCount(@Param("category") NewsCategory category);

    /**
     * 전체 활성 구독자 수 조회
     */
    @Query("SELECT SUM(c.activeSubscriberCount) FROM CategorySubscriberCount c")
    Long getTotalActiveSubscriberCount();

    /**
     * 전체 총 구독자 수 조회
     */
    @Query("SELECT SUM(c.totalSubscriberCount) FROM CategorySubscriberCount c")
    Long getTotalSubscriberCount();
}
