package com.newnormallist.newsservice.news.repository;

import com.newnormallist.newsservice.news.entity.Category;
import com.newnormallist.newsservice.news.entity.NewsScrap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface NewsScrapRepository extends JpaRepository<NewsScrap, Integer> {

    @Query(value = "SELECT ns FROM NewsScrap ns LEFT JOIN FETCH ns.news n WHERE ns.storageId = :storageId",
            countQuery = "SELECT count(ns) FROM NewsScrap ns WHERE ns.storageId = :storageId")
    Page<NewsScrap> findByStorageIdWithNews(@Param("storageId") Integer storageId, Pageable pageable);

    Optional<NewsScrap> findByStorageIdAndNewsNewsId(Integer storageId, Long newsId);

    long countByStorageId(Integer storageId);

    @Modifying
    @Transactional
    void deleteByStorageId(Integer storageId);

    Page<NewsScrap> findByUserId(Long userId, Pageable pageable);

    List<NewsScrap> findByUserIdAndNewsNewsId(Long userId, Long newsId);

    @Query("SELECT ns FROM NewsScrap ns JOIN ns.news n WHERE ns.userId = :userId AND n.title LIKE %:query%")
    Page<NewsScrap> findByUserIdAndNewsTitleContaining(@Param("userId") Long userId, @Param("query") String query, Pageable pageable);

    Page<NewsScrap> findByUserIdAndNews_CategoryName(Long userId, Category category, Pageable pageable);

    Page<NewsScrap> findByStorageIdAndNews_CategoryName(Integer storageId, Category category, Pageable pageable);

    @Query("SELECT ns FROM NewsScrap ns JOIN ns.news n WHERE ns.storageId = :storageId AND n.title LIKE %:query%")
    Page<NewsScrap> findByStorageIdAndNews_TitleContaining(@Param("storageId") Integer storageId, @Param("query") String query, Pageable pageable);

    // 컬렉션에 속하지 않은 스크랩 조회
    Page<NewsScrap> findByUserIdAndStorageIdIsNull(Long userId, Pageable pageable);

    // 컬렉션에 속하지 않은 스크랩을 카테고리별로 조회
    Page<NewsScrap> findByUserIdAndStorageIdIsNullAndNews_CategoryName(Long userId, Category category, Pageable pageable);

    // 컬렉션에 속하지 않은 스크랩을 제목으로 검색
    @Query("SELECT ns FROM NewsScrap ns JOIN ns.news n WHERE ns.userId = :userId AND ns.storageId IS NULL AND n.title LIKE %:query%")
    Page<NewsScrap> findByUserIdAndStorageIdIsNullAndNewsTitleContaining(@Param("userId") Long userId, @Param("query") String query, Pageable pageable);

    // 특정 뉴스의 스크랩 수 조회 (인기도 점수 계산용)
    long countByNewsNewsId(Long newsId);
}
