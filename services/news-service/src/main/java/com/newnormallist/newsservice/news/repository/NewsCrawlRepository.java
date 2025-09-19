package com.newnormallist.newsservice.news.repository;

import com.newnormallist.newsservice.news.entity.Category;
import com.newnormallist.newsservice.news.entity.NewsCrawl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface  NewsCrawlRepository extends JpaRepository<NewsCrawl, Long> {
    
    // 링크 ID로 중복 체크
    boolean existsByLinkId(Long linkId);
    
    // 제목으로 검색
    boolean existsByTitle(String title);
    
    // 카테고리별 크롤링 뉴스 조회
    Page<NewsCrawl> findByCategory(Category category, Pageable pageable);
    
    // 언론사별 크롤링 뉴스 조회
    Page<NewsCrawl> findByPress(String press, Pageable pageable);
    
    // 기자별 크롤링 뉴스 조회
    Page<NewsCrawl> findByReporterName(String reporterName, Pageable pageable);
    
    // 특정 기간 내 크롤링 뉴스 조회
    @Query("SELECT nc FROM NewsCrawl nc WHERE nc.createdAt BETWEEN :startDate AND :endDate")
    Page<NewsCrawl> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate, 
                                          Pageable pageable);
    
    // 발행일 기준 최신 크롤링 뉴스 조회
    @Query("SELECT nc FROM NewsCrawl nc ORDER BY nc.publishedAt DESC")
    Page<NewsCrawl> findLatestCrawledNews(Pageable pageable);
    
    // 키워드 검색 (제목, 내용에서 검색)
    @Query("SELECT nc FROM NewsCrawl nc WHERE nc.title LIKE %:keyword% OR nc.content LIKE %:keyword%")
    Page<NewsCrawl> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    // 승격되지 않은 크롤링 뉴스 조회 (News 테이블에 없는 것들)
//    @Query("SELECT nc FROM NewsCrawl nc WHERE nc.rawId NOT IN " +
//           "(SELECT n.originalNewsId FROM News n WHERE n.originalNewsId IS NOT NULL)")
//    Page<NewsCrawl> findUnpromotedNews(Pageable pageable);
    
    // 승격된 크롤링 뉴스 조회 (News 테이블에 있는 것들)
//    @Query("SELECT nc FROM NewsCrawl nc WHERE nc.rawId IN " +
//           "(SELECT n.originalNewsId FROM News n WHERE n.originalNewsId IS NOT NULL)")
//    Page<NewsCrawl> findPromotedNews(Pageable pageable);
    
    // 특정 링크 ID로 조회
    NewsCrawl findByLinkId(Long linkId);
    
    // 제목으로 조회
    List<NewsCrawl> findByTitle(String title);
    
    // 내용이 긴 뉴스 조회 (내용 길이 기준)
    @Query("SELECT nc FROM NewsCrawl nc WHERE LENGTH(nc.content) > :minLength")
    Page<NewsCrawl> findByContentLengthGreaterThan(@Param("minLength") Integer minLength, Pageable pageable);
} 