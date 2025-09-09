package com.newnormallist.newsservice.news.repository;
import com.newnormallist.newsservice.news.entity.Category;
import com.newnormallist.newsservice.news.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {


    Optional<News> findTop1ByImageUrlIsNotNullOrderByPublishedAtDesc();

    List<News> findByTitleContainingAndImageUrlIsNotNull(String keyword);

    @Query("SELECT n FROM News n WHERE STR_TO_DATE(n.publishedAt, '%Y-%m-%d %H:%i:%s') > :since")
    List<News> findByPublishedAtAfter(@Param("since") LocalDateTime since);

    // 카테고리별 뉴스 조회 (최신순)
    @Query("SELECT n FROM News n WHERE n.categoryName = :category ORDER BY STR_TO_DATE(n.publishedAt, '%Y-%m-%d %H:%i:%s') DESC")
    Page<News> findByCategory(@Param("category") Category category, Pageable pageable);

    // 키워드 검색 (제목, 내용에서 검색, 최신순)
    @Query("SELECT n FROM News n WHERE " +
           "n.title LIKE %:keyword% OR n.content LIKE %:keyword% ORDER BY STR_TO_DATE(n.publishedAt, '%Y-%m-%d %H:%i:%s') DESC")
    Page<News> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 최신 뉴스 조회 (발행일 기준 내림차순)
    @Query("SELECT n FROM News n ORDER BY n.publishedAt DESC")
    Page<News> findLatestNews(Pageable pageable);

    // 인기 뉴스 조회 (신뢰도 기준 내림차순)
    @Query("SELECT n FROM News n ORDER BY n.trusted DESC")
    Page<News> findPopularNews(Pageable pageable);

    // 트렌딩 뉴스 조회 (신뢰도 + 발행일 기준)
    @Query("SELECT n FROM News n ORDER BY n.trusted DESC, STR_TO_DATE(n.publishedAt, '%Y-%m-%d %H:%i:%s') DESC")
    Page<News> findTrendingNews(Pageable pageable);

    // 특정 기간 내 뉴스 조회 (페이징) - String 비교로 변경
    @Query("SELECT n FROM News n WHERE n.publishedAt BETWEEN :startDate AND :endDate ORDER BY STR_TO_DATE(n.publishedAt, '%Y-%m-%d %H:%i:%s') DESC")
    Page<News> findByPublishedAtBetween(@Param("startDate") String startDate,
                                       @Param("endDate") String endDate,
                                       Pageable pageable);

    // 특정 기간 내 뉴스 조회 (List 반환) - String 비교로 변경
    @Query("SELECT n FROM News n WHERE n.publishedAt BETWEEN :startDate AND :endDate ORDER BY STR_TO_DATE(n.publishedAt, '%Y-%m-%d %H:%i:%s') DESC")
    List<News> findByPublishedAtBetween(@Param("startDate") String startDate,
                                       @Param("endDate") String endDate);

    // 신뢰도가 높은 뉴스 조회 (최신순)
    @Query("SELECT n FROM News n WHERE n.trusted = true ORDER BY STR_TO_DATE(n.publishedAt, '%Y-%m-%d %H:%i:%s') DESC")
    Page<News> findByTrustedTrue(Pageable pageable);

    // 특정 언론사 뉴스 조회 (최신순)
    @Query("SELECT n FROM News n WHERE n.press = :press ORDER BY STR_TO_DATE(n.publishedAt, '%Y-%m-%d %H:%i:%s') DESC")
    Page<News> findByPress(@Param("press") String press, Pageable pageable);

    // 카테고리별 뉴스 개수 조회
    @Query("SELECT COUNT(n) FROM News n WHERE n.categoryName = :category")
    Long countByCategory(@Param("category") Category category);

    // 전체 뉴스 조회 (페이징)
    Page<News> findAll(Pageable pageable);

    // 전체 뉴스 조회 (최신순 정렬)
    @Query("SELECT n FROM News n ORDER BY STR_TO_DATE(n.publishedAt, '%Y-%m-%d %H:%i:%s') DESC")
    Page<News> findAllByOrderByPublishedAtDesc(Pageable pageable);

    // 연관뉴스 조회를 위한 메서드들

    // oid_aid로 뉴스 조회
    @Query("SELECT n FROM News n WHERE n.oidAid = :oidAid")
    List<News> findByOidAid(@Param("oidAid") String oidAid);

    // oid_aid 리스트로 뉴스 조회
    @Query("SELECT n FROM News n WHERE n.oidAid IN :oidAids")
    List<News> findByOidAidIn(@Param("oidAids") List<String> oidAids);

    // 같은 발행일, 같은 카테고리, 특정 뉴스 제외
    @Query("SELECT n FROM News n WHERE n.publishedAt = :publishedAt AND n.categoryName = :categoryName AND n.newsId != :excludeNewsId")
    List<News> findByPublishedAtAndCategoryNameAndNewsIdNot(@Param("publishedAt") String publishedAt,
                                                           @Param("categoryName") Category categoryName,
                                                           @Param("excludeNewsId") Long excludeNewsId);

    // oid_aid 리스트로 뉴스 조회 (특정 뉴스 제외)
    @Query("SELECT n FROM News n WHERE n.oidAid IN :oidAids AND n.newsId != :excludeNewsId")
    List<News> findByOidAidInAndNewsIdNot(@Param("oidAids") List<String> oidAids,
                                          @Param("excludeNewsId") Long excludeNewsId);

    // 특정 기간, 같은 카테고리, 특정 뉴스들 제외
    @Query("SELECT n FROM News n WHERE n.categoryName = :categoryName AND n.publishedAt BETWEEN :startDate AND :endDate AND n.newsId NOT IN :excludeNewsIds")
    List<News> findByCategoryNameAndPublishedAtBetweenAndNewsIdNotIn(@Param("categoryName") Category categoryName,
                                                                     @Param("startDate") String startDate,
                                                                     @Param("endDate") String endDate,
                                                                     @Param("excludeNewsIds") List<Long> excludeNewsIds);

    // 같은 카테고리, 특정 뉴스들 제외 (페이징)
    Page<News> findByCategoryNameAndNewsIdNotIn(Category categoryName, List<Long> excludeNewsIds, Pageable pageable);

    // 개인화된 뉴스 조회 (사용자 선호도 기반)
    @Query("SELECT n FROM News n WHERE " +
           "n.categoryName IN :categories OR " +
           "n.title LIKE %:keyword% OR " +
           "n.content LIKE %:keyword% " +
           "ORDER BY n.trusted DESC, STR_TO_DATE(n.publishedAt, '%Y-%m-%d %H:%i:%s') DESC")
    List<News> findPersonalizedNews(@Param("userId") Long userId,
                                   @Param("categories") List<String> userPreferences,
                                   @Param("keyword") String readingHistory,
                                   Pageable pageable);

    // 개인화된 뉴스 조회 (카테고리 기반)
    @Query("SELECT n FROM News n WHERE n.categoryName IN :categories " +
           "ORDER BY n.trusted DESC, STR_TO_DATE(n.publishedAt, '%Y-%m-%d %H:%i:%s') DESC")
    List<News> findPersonalizedNewsByCategories(@Param("categories") List<Category> categories,
                                               Pageable pageable);

    // 카테고리별 신뢰도 높은 뉴스 조회
    @Query("SELECT n FROM News n WHERE n.categoryName = :category AND n.trusted = true " +
           "ORDER BY STR_TO_DATE(n.publishedAt, '%Y-%m-%d %H:%i:%s') DESC")
    Page<News> findByCategoryAndTrustedTrue(@Param("category") Category category, Pageable pageable);
}