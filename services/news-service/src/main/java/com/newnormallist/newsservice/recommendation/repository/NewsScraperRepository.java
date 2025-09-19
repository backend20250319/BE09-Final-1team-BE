package com.newnormallist.newsservice.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

import com.newnormallist.newsservice.recommendation.entity.NewsScraper;

// 최근 30일 스크랩 기록 조회 -> S(c) 계산에 사용
public interface NewsScraperRepository extends JpaRepository<NewsScraper, Long> {
    @Query("SELECT ns FROM NewsScraper ns JOIN ScrapStorages ss ON ns.storageId = ss.storageId WHERE ss.userId = :uid AND ns.createdAt >= :since")
    List<NewsScraper> findRecentScrapsByUserId(@Param("uid") Long userId, @Param("since") LocalDateTime since);

    // 날짜 파싱 오류 방지를 위한 대체 메서드
    @Query(value = "SELECT ns.created_at, n.category FROM news_scrap ns " +
                   "JOIN scrap_storage ss ON ns.storage_id = ss.storage_id " +
                   "JOIN news n ON ns.news_id = n.news_id " +
                   "WHERE ss.user_id = :uid AND ns.created_at >= :since",
           nativeQuery = true)
    List<Object[]> findRecentScrapsByUserIdNative(@Param("uid") Long userId, @Param("since") String since);
}