package com.newsletterservice.service;

import com.newsletterservice.dto.NewsletterContent;
import com.newsletterservice.dto.NewsletterPreview;

import java.util.List;
import java.util.Map;

/**
 * 뉴스레터 콘텐츠 생성 전용 서비스 인터페이스
 */
public interface NewsletterContentService {
    
    /**
     * 개인화된 콘텐츠 생성
     */
    NewsletterContent buildPersonalizedContent(Long userId, Long newsletterId);
    
    /**
     * 뉴스레터 미리보기 생성
     */
    NewsletterPreview generateNewsletterPreview(Long userId);
    
    /**
     * 개인화된 뉴스레터 생성 (HTML)
     */
    String generatePersonalizedNewsletter(String userId);
    
    /**
     * 미리보기 HTML 생성
     */
    String generatePreviewHtml(Long id);
    
    /**
     * 개인화 정보 조회
     */
    Map<String, Object> getPersonalizationInfo(Long userId);
    
    /**
     * 카테고리별 헤드라인 조회
     */
    List<NewsletterContent.Article> getCategoryHeadlines(String category, int limit);
    
    /**
     * 카테고리별 기사 및 트렌딩 키워드 조회
     */
    Map<String, Object> getCategoryArticlesWithTrendingKeywords(String category, int limit);
    
    // 개발/테스트용 메서드들
    /**
     * 뉴스레터 ID로 조회
     */
    Object getNewsletterById(Long id);
    
    /**
     * 샘플 뉴스레터 생성
     */
    Object createSampleNewsletter();
    
    /**
     * 뉴스레터 목록 조회
     */
    Object getNewsletterList(int page, int size);
    
    /**
     * 뉴스레터 생성 테스트
     */
    Map<String, Object> testNewsletterGeneration(Long userId);
}
