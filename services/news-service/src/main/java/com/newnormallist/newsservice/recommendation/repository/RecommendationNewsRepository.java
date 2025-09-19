package com.newnormallist.newsservice.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Collection;

import com.newnormallist.newsservice.recommendation.entity.NewsEntity;
import com.newnormallist.newsservice.recommendation.entity.RecommendationCategory;


// findLatestIdsByCategory(cat, limit) : 카테고리 최신 뉴스 ID 가져오기.
// findByIdIn(ids) : 메타 정보 일괄 조회.
// findCategoryById(id) : 조회 로그 저장 시 newsId → category 팝업용.
public interface RecommendationNewsRepository extends JpaRepository<NewsEntity, Long> {

    @Query("SELECT n.newsId FROM NewsEntity n WHERE n.categoryName = :cat ORDER BY " +
           "CASE WHEN n.publishedAt LIKE '%T%' THEN STR_TO_DATE(n.publishedAt, '%Y-%m-%dT%H:%i:%s') " +
           "ELSE STR_TO_DATE(n.publishedAt, '%Y-%m-%d %H:%i:%s') END DESC")
    List<Long> findLatestIdsByCategory(@Param("cat") RecommendationCategory category, Pageable pageable);

    @Query("SELECT n FROM NewsEntity n WHERE n.newsId IN :ids")
    List<NewsEntity> findByIdIn(@Param("ids") Collection<Long> ids);

    @Query("SELECT n.categoryName FROM NewsEntity n WHERE n.newsId = :id")
    RecommendationCategory findCategoryById(@Param("id") Long id);
    
    // published_at 기준 최신순 정렬 (전체 뉴스 피드용)
    @Query("SELECT n FROM NewsEntity n ORDER BY " +
           "CASE WHEN n.publishedAt LIKE '%T%' THEN STR_TO_DATE(n.publishedAt, '%Y-%m-%dT%H:%i:%s') " +
           "ELSE STR_TO_DATE(n.publishedAt, '%Y-%m-%d %H:%i:%s') END DESC")
    Page<NewsEntity> findAllByOrderByPublishedAtDesc(Pageable pageable);
}
