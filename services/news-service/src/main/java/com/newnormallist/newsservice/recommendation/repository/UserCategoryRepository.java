package com.newnormallist.newsservice.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

import com.newnormallist.newsservice.recommendation.entity.UserCategories;
import com.newnormallist.newsservice.recommendation.entity.UserCategoryId;
import com.newnormallist.newsservice.recommendation.entity.RecommendationCategory;

// 유저 선호 카테고리 조회 -> P(c) 계산에 사용
public interface UserCategoryRepository extends JpaRepository<UserCategories, UserCategoryId> {

    // user_id로 해당 사용자의 선호 행(1~3개) 모두 조회
    List<UserCategories> findByIdUserId(Long userId);

    // 카테고리 enum만 바로 받고 싶을 때 (벡터 P(c) 만들기 편함)
    @Query("select uc.id.category from UserCategories uc where uc.id.userId = :uid")
    List<RecommendationCategory> findCategoriesByUserId(@Param("uid") Long userId);

    // 존재여부/삭제 편의 메서드 (엔드포인트에서 쓰기 좋아요)
    boolean existsByIdUserIdAndIdCategory(Long userId, RecommendationCategory category);
    long deleteByIdUserIdAndIdCategory(Long userId, RecommendationCategory category);
}