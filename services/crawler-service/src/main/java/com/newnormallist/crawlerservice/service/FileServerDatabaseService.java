package com.newnormallist.crawlerservice.service;

import com.newnormallist.crawlerservice.dto.NewsDetail;
import com.newnormallist.crawlerservice.dto.RelatedNewsDetail;
import com.newnormallist.crawlerservice.entity.News;
import com.newnormallist.crawlerservice.entity.RelatedNews;
import com.newnormallist.crawlerservice.repository.NewsRepository;
import com.newnormallist.crawlerservice.repository.RelatedNewsRepository;
import com.newnormallist.crawlerservice.enums.DedupState;
import com.newnormallist.crawlerservice.enums.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;


/**
 * 파일서버 기반 데이터베이스 저장 서비스
 * 
 * 역할:
 * - 파일서버에 저장된 중복제거 완료 데이터를 MySQL DB에 저장
 * - JPA 기반 엔티티 변환 및 트랜잭션 관리
 * - 데이터 무결성 및 중복 방지
 * 
 * 기능:
 * - CSV 파싱: 파일서버의 CSV 데이터를 JPA 엔티티로 변환
 * - 뉴스 저장: 중복제거된 뉴스를 news 테이블에 저장
 * - 연관뉴스 저장: 유사도 기반 연관뉴스를 related_news 테이블에 저장
 * - 배치 처리: 대량 데이터를 효율적으로 일괄 저장
 * - 통계 제공: 저장된 데이터의 현황 요약
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServerDatabaseService {

    private final NewsRepository newsRepository;
    private final RelatedNewsRepository relatedNewsRepository;
    private final FileServerService fileServerService;

    /**
     * 파일서버 최신 데이터를 DB에 저장
     */
    @Transactional
    public void saveLatestDataToDatabase() {
        log.info("📁 파일서버 최신 데이터 DB 저장 시작");
        
        try {
            String latestTimePath = fileServerService.getLatestTimePath();
            log.info("📁 최신 시간대 경로: {}", latestTimePath);
            
            String[] categories = {"POLITICS", "ECONOMY", "SOCIETY", "LIFE", "INTERNATIONAL", 
                                 "IT_SCIENCE", "VEHICLE", "TRAVEL_FOOD", "ART"};
            
            for (String category : categories) {
                try {
                    // 1. 중복제거된 뉴스 저장
                    saveDeduplicatedNewsFromFile(category, latestTimePath);
                    
                    // 2. 연관뉴스 저장
                    saveRelatedNewsFromFile(category, latestTimePath);
                    
                } catch (Exception e) {
                    log.error("📁 {} 카테고리 DB 저장 실패: {}", category, e.getMessage(), e);
                    throw new RuntimeException(category + " 카테고리 DB 저장 실패", e);
                }
            }
            
            log.info("📁 파일서버 최신 데이터 DB 저장 완료");
            
        } catch (Exception e) {
            log.error("📁 파일서버 DB 저장 전체 프로세스 실패: {}", e.getMessage(), e);
            throw new RuntimeException("파일서버 DB 저장 실패", e);
        }
    }
    
    /**
     * 중복제거된 뉴스를 파일에서 읽어서 DB에 저장
     */
    private void saveDeduplicatedNewsFromFile(String category, String timePath) {
        log.info("📁 {} 카테고리 중복제거된 뉴스 DB 저장 시작", category);
        
        try {
            // FileServerService의 일관된 방식으로 뉴스 조회
            List<NewsDetail> newsDetailList = fileServerService.getNewsListFromCsv(category, "deduplicated", timePath);
            
            if (newsDetailList.isEmpty()) {
                log.info("📁 중복제거된 뉴스 데이터가 없음: {}/{}", category, "deduplicated");
                return;
            }
            
            List<News> newsEntities = new ArrayList<>();
            int savedCount = 0;
            int skippedCount = 0;
            
            for (NewsDetail newsDetail : newsDetailList) {
                if (newsDetail != null) {
                    // 중복 체크
                    if (newsRepository.existsByOidAid(newsDetail.getOidAid())) {
                        log.debug("📁 중복된 뉴스 건너뜀: {}", newsDetail.getOidAid());
                        skippedCount++;
                        continue;
                    }
                    
                    News newsEntity = convertToNewsEntity(newsDetail);
                    newsEntities.add(newsEntity);
                }
            }
            
            if (!newsEntities.isEmpty()) {
                newsRepository.saveAll(newsEntities);
                savedCount = newsEntities.size();
                log.info("📁 {} 카테고리 중복제거된 뉴스 DB 저장 완료: {}개 저장, {}개 건너뜀", category, savedCount, skippedCount);
            } else {
                log.info("📁 {} 카테고리 저장할 뉴스 없음 (모두 중복)", category);
            }
            
        } catch (Exception e) {
            log.error("📁 {} 카테고리 중복제거된 뉴스 DB 저장 실패: {}", category, e.getMessage());
            throw new RuntimeException("중복제거된 뉴스 DB 저장 실패", e);
        }
    }
    
    /**
     * 연관뉴스를 파일에서 읽어서 DB에 저장
     */
    private void saveRelatedNewsFromFile(String category, String timePath) {
        log.info("📁 {} 카테고리 연관뉴스 DB 저장 시작", category);
        
        try {
            // FileServerService의 일관된 방식으로 연관뉴스 조회
            List<RelatedNewsDetail> relatedNewsDetailList = fileServerService.getRelatedNewsFromCsv(category, timePath);
            
            if (relatedNewsDetailList.isEmpty()) {
                log.info("📁 연관뉴스 데이터가 없음: {}/{}", category, "related");
                return;
            }
            
            List<RelatedNews> relatedEntities = new ArrayList<>();
            
            for (RelatedNewsDetail relatedDetail : relatedNewsDetailList) {
                if (relatedDetail != null) {
                    RelatedNews relatedNews = convertToRelatedNewsEntity(relatedDetail);
                    relatedEntities.add(relatedNews);
                }
            }
            
            if (!relatedEntities.isEmpty()) {
                relatedNewsRepository.saveAll(relatedEntities);
                log.info("📁 {} 카테고리 연관뉴스 DB 저장 완료: {}개", category, relatedEntities.size());
            }
            
        } catch (Exception e) {
            log.error("📁 {} 카테고리 연관뉴스 DB 저장 실패: {}", category, e.getMessage());
            throw new RuntimeException("연관뉴스 DB 저장 실패", e);
        }
    }
    

    
    /**
     * RelatedNewsDetail을 RelatedNews 엔티티로 변환
     */
    private RelatedNews convertToRelatedNewsEntity(RelatedNewsDetail relatedDetail) {
        return RelatedNews.builder()
            .repOidAid(relatedDetail.getRepOidAid())
            .relatedOidAid(relatedDetail.getRelatedOidAid())
            .similarity(relatedDetail.getSimilarity())
            .createdAt(relatedDetail.getCreatedAt() != null ? relatedDetail.getCreatedAt() : LocalDateTime.now())
            .updatedAt(null) // 명시적으로 null 설정
            .build();
    }
    
    /**
     * NewsDetail을 News Entity로 변환
     */
    private News convertToNewsEntity(NewsDetail newsDetail) {
        News news = News.builder()
            .oidAid(newsDetail.getOidAid())
            .title(newsDetail.getTitle())
            .content(newsDetail.getContent())
            .reporter(newsDetail.getReporter())
            .publishedAt(newsDetail.getDate())
            .imageUrl(newsDetail.getImageUrl())
            .link(newsDetail.getLink())
            .press(newsDetail.getPress())
            .trusted(newsDetail.getTrusted() == 1)
            .dedupState(convertDedupState(newsDetail.getDedupState()))
            .category(convertCategory(newsDetail.getCategoryName())) // 카테고리 변환 추가
            .viewCount(0) // 조회수 기본값 0으로 설정
            .createdAt(LocalDateTime.now())
            .build();
        
        // viewCount가 null인 경우 명시적으로 0으로 설정
        if (news.getViewCount() == null) {
            news.setViewCount(0);
        }
        
        return news;
    }
    
    /**
     * DedupState 문자열을 Enum으로 변환
     */
    private DedupState convertDedupState(String dedupStateStr) {
        if (dedupStateStr == null || dedupStateStr.isEmpty()) {
            return DedupState.KEPT;
        }
        
        try {
            return DedupState.valueOf(dedupStateStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("📁 알 수 없는 DedupState: {}, KEPT으로 설정", dedupStateStr);
            return DedupState.KEPT;
        }
    }
    
    /**
     * Category 문자열을 Enum으로 변환
     */
    private Category convertCategory(String categoryStr) {
        if (categoryStr == null || categoryStr.isEmpty()) {
            return Category.POLITICS; // 기본값
        }
        
        try {
            return Category.valueOf(categoryStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("📁 알 수 없는 Category: {}, POLITICS로 설정", categoryStr);
            return Category.POLITICS;
        }
    }
    

    
    /**
     * 최신 데이터 현황 요약
     */
    public void summarizeLatestData() {
        log.info("📁 파일서버 최신 데이터 현황 요약");
        
        try {
            String latestTimePath = fileServerService.getLatestTimePath();
            
            String[] categories = {"POLITICS", "ECONOMY", "SOCIETY", "LIFE", "INTERNATIONAL", 
                                 "IT_SCIENCE", "VEHICLE", "TRAVEL_FOOD", "ART"};
            
            for (String category : categories) {
                // 중복제거된 뉴스 개수
                List<NewsDetail> dedupNews = fileServerService.getNewsListFromCsv(category, "deduplicated", latestTimePath);
                
                // 연관뉴스 개수 (FileServerService를 통해 조회)
                List<RelatedNewsDetail> relatedNews = fileServerService.getRelatedNewsFromCsv(category, latestTimePath);
                int relatedCount = relatedNews.size();
                
                log.info("📁 {} 카테고리: 중복제거 {}개, 연관뉴스 {}개", 
                    category, dedupNews.size(), relatedCount);
            }
            
        } catch (Exception e) {
            log.error("📁 최신 데이터 현황 요약 실패: {}", e.getMessage());
        }
    }
    

    

    

}
