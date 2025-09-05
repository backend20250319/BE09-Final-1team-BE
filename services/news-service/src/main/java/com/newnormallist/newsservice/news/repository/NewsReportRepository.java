package com.newnormallist.newsservice.news.repository;

import com.newnormallist.newsservice.news.entity.NewsReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsReportRepository extends JpaRepository<NewsReport, Long> {
    
    // 특정 뉴스에 대한 신고 목록 조회
    List<NewsReport> findByNewsNewsId(Long newsId);
    
    // 특정 사용자가 특정 뉴스를 신고했는지 확인
    Optional<NewsReport> findByUserIdAndNewsNewsId(Long userId, Long newsId);
    
    // 특정 사용자의 신고 목록 조회
    List<NewsReport> findByUserId(Long userId);
    
    // 특정 뉴스의 신고 개수 조회
    long countByNewsNewsId(Long newsId);
}
