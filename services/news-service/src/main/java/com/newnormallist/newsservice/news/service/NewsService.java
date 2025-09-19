package com.newnormallist.newsservice.news.service;

import com.newnormallist.newsservice.news.dto.CategoryDto;
import com.newnormallist.newsservice.news.dto.KeywordSubscriptionDto;
import com.newnormallist.newsservice.news.dto.NewsCrawlDto;
import com.newnormallist.newsservice.news.dto.NewsListResponse;
import com.newnormallist.newsservice.news.dto.NewsResponse;
import com.newnormallist.newsservice.news.dto.TrendingKeywordDto;
import com.newnormallist.newsservice.news.entity.Category;
import com.newnormallist.newsservice.news.entity.NewsCrawl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.newnormallist.newsservice.news.dto.ScrapStorageResponse;
import com.newnormallist.newsservice.news.dto.ScrappedNewsResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface NewsService {

    // 크롤링된 뉴스 데이터를 저장
    NewsCrawl saveCrawledNews(NewsCrawlDto dto);

    // 크롤링된 뉴스 미리보기 (저장하지 않고 미리보기만)
    NewsCrawlDto previewCrawledNews(NewsCrawlDto dto);

    // 뉴스 조회 관련 메서드들
    Page<NewsResponse> getNews(Category category, String keyword, Pageable pageable);
    NewsResponse getNewsById(Long newsId);
    List<NewsResponse> getPersonalizedNews(Long userId);
    List<NewsResponse> getTrendingNews();
    void incrementViewCount(Long newsId);
    Long getViewCount(Long newsId);
    Long getDailyViewCount(Long newsId);

    // 새로운 API 엔드포인트들을 위한 메서드들
    Page<NewsListResponse> getTrendingNews(Pageable pageable);
    Page<NewsListResponse> getRecommendedNews(Long userId, Pageable pageable);
    Page<NewsListResponse> getNewsByCategory(Category category, Pageable pageable);
    Page<NewsListResponse> searchNews(String query, Pageable pageable);
    Page<NewsListResponse> searchNewsWithFilters(String query, String sortBy, String sortOrder,
                                                String category, String press, String startDate,
                                                String endDate, Pageable pageable);
    Page<NewsListResponse> getPopularNews(Pageable pageable);
    Page<NewsListResponse> getLatestNews(Pageable pageable);
    List<CategoryDto> getAllCategories();

    // 새로 추가된 메서드들
    Page<NewsListResponse> getNewsByPress(String press, Pageable pageable);
    List<NewsListResponse> getNewsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    Long getNewsCount();
    Long getNewsCountByCategory(Category category);

    // 관리자용: 크롤링된 뉴스를 승격하여 노출용 뉴스로 전환
    void promoteToNews(Long newsCrawlId);

    // 관리자용: 크롤링된 뉴스 목록 조회
    Page<NewsCrawl> getCrawledNews(Pageable pageable);

    // 키워드 구독 관련 메서드들
    KeywordSubscriptionDto subscribeKeyword(Long userId, String keyword);
    void unsubscribeKeyword(Long userId, String keyword);
    List<KeywordSubscriptionDto> getUserKeywordSubscriptions(Long userId);

    // 트렌딩 키워드 관련 메서드들
    List<TrendingKeywordDto> getTrendingKeywords(int limit);
    List<TrendingKeywordDto> getPopularKeywords(int limit);
    List<TrendingKeywordDto> getTrendingKeywordsByCategory(Category category, int limit);

    // 신고 및 스크랩
    void reportNews(Long newsId, Long userId);
    void scrapNews(Long newsId, Long userId); // 기본 스크랩

    // 컬렉션 (스크랩 보관함)
    List<ScrapStorageResponse> getUserScrapStorages(Long userId);
    ScrapStorageResponse getCollectionDetails(Long userId, Integer collectionId);
    ScrapStorageResponse createCollection(Long userId, String storageName);
    ScrapStorageResponse updateCollection(Long userId, Integer collectionId, String newName);
    void addNewsToCollection(Long userId, Integer collectionId, Long newsId);
    Page<ScrappedNewsResponse> getNewsInCollection(Long userId, Integer collectionId, String category, String query, Pageable pageable);
    void deleteCollection(Long userId, Integer collectionId);
    void deleteNewsFromCollection(Long userId, Integer collectionId, Long newsId);

    void assignScrapToStorage(Long userId, Integer newsScrapId, Integer targetStorageId);
}