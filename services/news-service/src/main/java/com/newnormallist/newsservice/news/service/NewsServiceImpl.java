package com.newnormallist.newsservice.news.service;

import com.newnormallist.newsservice.news.dto.*;
import com.newnormallist.newsservice.news.entity.*;
import com.newnormallist.newsservice.news.exception.NewsNotFoundException;

import com.newnormallist.newsservice.news.repository.KeywordSubscriptionRepository;
import com.newnormallist.newsservice.news.repository.NewsCrawlRepository;
import com.newnormallist.newsservice.news.repository.NewsRepository;
import com.newnormallist.newsservice.tooltip.client.TooltipServiceClient;
import com.newnormallist.newsservice.tooltip.dto.ProcessContentRequest;
import com.newnormallist.newsservice.tooltip.dto.ProcessContentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.Map;

@Service
@Transactional
@Slf4j
public class NewsServiceImpl implements NewsService {

    @Autowired
    private NewsCrawlRepository newsCrawlRepository;
    
    @Autowired
    private NewsRepository newsRepository;
    
    @Autowired
    private TooltipServiceClient tooltipServiceClient;
    
    @Autowired
    private KeywordSubscriptionRepository keywordSubscriptionRepository;
    


    // 크롤링 관련 메서드들
    @Override
    public NewsCrawl saveCrawledNews(NewsCrawlDto dto) {
        // 중복 체크
        if (newsCrawlRepository.existsByLinkId(dto.getLinkId())) {
            throw new RuntimeException("이미 존재하는 뉴스입니다: " + dto.getLinkId());
        }

        // Category enum 사용
        Category category = dto.getCategory();

        // NewsCrawl 엔티티 생성
        NewsCrawl newsCrawl = NewsCrawl.builder()
                .linkId(dto.getLinkId())
                .title(dto.getTitle())
                .press(dto.getPress())
                .content(dto.getContent())
                .reporterName(dto.getReporterName())
                .publishedAt(dto.getPublishedAt())
                .category(category)
                .createdAt(LocalDateTime.now())
                .build();

        return newsCrawlRepository.save(newsCrawl);
    }

    @Override
    public NewsCrawlDto previewCrawledNews(NewsCrawlDto dto) {
        // 미리보기용으로는 단순히 DTO를 반환 (DB 저장하지 않음)
        return dto;
    }

    // 뉴스 조회 관련 메서드들
    @Override
    public Page<NewsResponse> getNews(Category category, String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 키워드 검색
            return newsRepository.searchByKeyword(keyword, pageable)
                    .map(this::convertToNewsResponse);
        } else if (category != null) {
            // 카테고리별 검색
            return newsRepository.findByCategory(category, pageable)
                    .map(this::convertToNewsResponse);
        } else {
            // 전체 뉴스 (최신순 정렬)
            return newsRepository.findAllByOrderByPublishedAtDesc(pageable)
                    .map(this::convertToNewsResponse);
        }
    }
    
    @Override
    public NewsResponse getNewsById(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsNotFoundException("존재하지 않는 뉴스입니다: " + newsId));
        // return convertToNewsResponse(news);
        // ----- 툴팁 기능을 위한 코드 시작 -----
        // 툴팁 서비스를 호출하여 마크업된 본문 가져오기
        String processedContent = getProcessedContent(newsId, news.getContent());
        
        return convertToNewsResponseWithTooltip(news, processedContent);
    }
    
    /**
     * 툴팁 서비스를 호출하여 마크업된 본문을 가져옵니다.
     * 실패 시 원본 본문을 반환합니다.
     */
    private String getProcessedContent(Long newsId, String originalContent) {
        try {
            log.info("🟡 뉴스 ID {}에 대해 툴팁 서비스 호출을 시작합니다.", newsId);
            ProcessContentRequest request = new ProcessContentRequest(newsId, originalContent);
            ProcessContentResponse response = tooltipServiceClient.processContent(request);
            log.info("🟢 뉴스 ID {} 툴팁 마크업 완료!", newsId);
            return response.processedContent();
        } catch (Exception e) {
            log.warn("⚠️ 뉴스 ID {} 툴팁 서비스 호출 실패, 원본 텍스트 사용: {}", newsId, e.getMessage());
            return originalContent;
        }
    }
    
    /**
     * 툴팁이 적용된 NewsResponse 생성
     */
    private NewsResponse convertToNewsResponseWithTooltip(News news, String processedContent) {
        return NewsResponse.builder()
                .newsId(news.getNewsId())
                .title(news.getTitle())
                .content(processedContent) // 👈 마크업된 본문
                .press(news.getPress())
                .publishedAt(parsePublishedAt(news.getPublishedAt()))
                .reporterName(news.getReporter())
                .createdAt(news.getCreatedAt())
                .updatedAt(news.getUpdatedAt())
                .trusted(news.getTrusted() ? 1 : 0)
                .imageUrl(news.getImageUrl())
                .oidAid(news.getOidAid())
                .categoryName(news.getCategoryName().name())
                .build();
                // ----- 툴팁 기능을 위한 코드 끝 -----
    }
    
    @Override
    public List<NewsResponse> getPersonalizedNews(Long userId) {
        // TODO: 사용자 선호도 기반 개인화 로직 구현
        // 현재는 신뢰도가 높은 뉴스 10개 반환
        return newsRepository.findByTrustedTrue(Pageable.ofSize(10))
                .getContent()
                .stream()
                .map(this::convertToNewsResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<NewsResponse> getTrendingNews() {
        // 신뢰도가 높은 뉴스 10개 반환
        return newsRepository.findByTrustedTrue(Pageable.ofSize(10))
                .getContent()
                .stream()
                .map(this::convertToNewsResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public void incrementViewCount(Long newsId) {
        // TODO: 조회수 증가 로직 구현
        // 현재는 view count 필드가 없으므로 나중에 구현
    }


    @Override
    public Page<NewsListResponse> getTrendingNews(Pageable pageable) {
        return newsRepository.findTrendingNews(pageable)
                .map(this::convertToNewsListResponse);
    }
    
    @Override
    public Page<NewsListResponse> getRecommendedNews(Long userId, Pageable pageable) {
        // TODO: 사용자 기반 추천 로직 구현
        // 현재는 신뢰도가 높은 뉴스 반환
        return newsRepository.findByTrustedTrue(pageable)
                .map(this::convertToNewsListResponse);
    }
    
    @Override
    public Page<NewsListResponse> getNewsByCategory(Category category, Pageable pageable) {
        return newsRepository.findByCategory(category, pageable)
                .map(this::convertToNewsListResponse);
    }
    
    @Override
    public Page<NewsListResponse> searchNews(String query, Pageable pageable) {
        return newsRepository.searchByKeyword(query, pageable)
                .map(this::convertToNewsListResponse);
    }
    
    @Override
    public Page<NewsListResponse> searchNewsWithFilters(String query, String sortBy, String sortOrder, 
                                                       String category, String press, String startDate, 
                                                       String endDate, Pageable pageable) {
        // 기본 검색 결과 가져오기
        Page<News> newsPage = newsRepository.searchByKeyword(query, pageable);
        
        // 필터링 적용
        List<News> filteredNews = newsPage.getContent().stream()
                .filter(news -> {
                    // 카테고리 필터
                    if (category != null && !category.isEmpty()) {
                        try {
                            Category categoryEnum = Category.valueOf(category.toUpperCase());
                            if (!news.getCategoryName().equals(categoryEnum)) {
                                return false;
                            }
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    }
                    
                    // 언론사 필터
                    if (press != null && !press.isEmpty()) {
                        if (!news.getPress().toLowerCase().contains(press.toLowerCase())) {
                            return false;
                        }
                    }
                    
                    // 날짜 필터
                    if (startDate != null && !startDate.isEmpty()) {
                        LocalDateTime start = parsePublishedAt(startDate);
                        if (news.getCreatedAt().isBefore(start)) {
                            return false;
                        }
                    }
                    
                    if (endDate != null && !endDate.isEmpty()) {
                        LocalDateTime end = parsePublishedAt(endDate);
                        if (news.getCreatedAt().isAfter(end)) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
        
        // 정렬 적용
        if (sortBy != null && !sortBy.isEmpty()) {
            String order = (sortOrder != null && sortOrder.equalsIgnoreCase("desc")) ? "desc" : "asc";
            
            switch (sortBy.toLowerCase()) {
                case "date":
                case "publishedat":
                    if (order.equals("desc")) {
                        filteredNews.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                    } else {
                        filteredNews.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
                    }
                    break;
//                case "viewcount":
//                    if (order.equals("desc")) {
//                        filteredNews.sort((a, b) -> Integer.compare(b.getViewCount() != null ? b.getViewCount() : 0,
//                                                                   a.getViewCount() != null ? a.getViewCount() : 0));
//                    } else {
//                        filteredNews.sort((a, b) -> Integer.compare(a.getViewCount() != null ? a.getViewCount() : 0,
//                                                                   b.getViewCount() != null ? b.getViewCount() : 0));
//                    }
//                    break;
                case "title":
                    if (order.equals("desc")) {
                        filteredNews.sort((a, b) -> b.getTitle().compareTo(a.getTitle()));
                    } else {
                        filteredNews.sort((a, b) -> a.getTitle().compareTo(b.getTitle()));
                    }
                    break;
                case "press":
                    if (order.equals("desc")) {
                        filteredNews.sort((a, b) -> b.getPress().compareTo(a.getPress()));
                    } else {
                        filteredNews.sort((a, b) -> a.getPress().compareTo(b.getPress()));
                    }
                    break;
                default:
                    // 기본 정렬: 최신순
                    filteredNews.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            }
        }
        
        // 페이징 적용
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        int start = pageNumber * pageSize;
        int end = Math.min(start + pageSize, filteredNews.size());
        
        List<News> pagedNews = filteredNews.subList(start, end);
        List<NewsListResponse> responseList = pagedNews.stream()
                .map(this::convertToNewsListResponse)
                .collect(Collectors.toList());
        
        // Page 객체 생성
        return new org.springframework.data.domain.PageImpl<>(
                responseList, pageable, filteredNews.size());
    }
    
    @Override
    public Page<NewsListResponse> getPopularNews(Pageable pageable) {
        return newsRepository.findPopularNews(pageable)
                .map(this::convertToNewsListResponse);
    }
    
    @Override
    public Page<NewsListResponse> getLatestNews(Pageable pageable) {
        return newsRepository.findLatestNews(pageable)
                .map(this::convertToNewsListResponse);
    }
    
    @Override
    public List<CategoryDto> getAllCategories() {
        return List.of(Category.values())
                .stream()
                .map(this::convertToCategoryDto)
                .collect(Collectors.toList());
    }
    
    // 새로 추가된 메서드들의 구현
    @Override
    public Page<NewsListResponse> getNewsByPress(String press, Pageable pageable) {
        return newsRepository.findByPress(press, pageable)
                .map(this::convertToNewsListResponse);
    }
    
    @Override
    public List<NewsListResponse> getNewsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        // LocalDateTime을 String으로 변환하여 전달
        String startDateStr = startDate.toString();
        String endDateStr = endDate.toString();
        return newsRepository.findByPublishedAtBetween(startDateStr, endDateStr)
                .stream()
                .map(this::convertToNewsListResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public Long getNewsCount() {
        return newsRepository.count();
    }
    
    @Override
    public Long getNewsCountByCategory(Category category) {
        return newsRepository.countByCategory(category);
    }
    
    @Override
    public void promoteToNews(Long newsCrawlId) {
        // 크롤링된 뉴스를 승격하여 노출용 뉴스로 전환
        NewsCrawl newsCrawl = newsCrawlRepository.findById(newsCrawlId)
                .orElseThrow(() -> new NewsNotFoundException("NewsCrawl not found with id: " + newsCrawlId));
        
        // 이미 승격된 뉴스인지 확인
//        List<News> existingNews = newsRepository.findByOriginalNewsId(newsCrawl.getRawId());
//        if (!existingNews.isEmpty()) {
//            throw new RuntimeException("이미 승격된 뉴스입니다: " + newsCrawlId);
//        }
//
        // News 엔티티 생성 및 저장
        News news = News.builder()
                .title(newsCrawl.getTitle())
                .content(newsCrawl.getContent())
                .press(newsCrawl.getPress())
                .reporter(newsCrawl.getReporterName())
                .publishedAt(newsCrawl.getPublishedAt().toString())
                .trusted(calculateTrusted(newsCrawl)) // 신뢰도 계산
                .categoryName(newsCrawl.getCategory()) // 카테고리 설정
                .dedupState(DedupState.KEPT) // 기본값
                .build();
        
        newsRepository.save(news);
    }
    
    @Override
    public Page<NewsCrawl> getCrawledNews(Pageable pageable) {
        return newsCrawlRepository.findAll(pageable);
    }

    // DTO 변환 메서드들
    private NewsResponse convertToNewsResponse(News news) {
        return NewsResponse.builder()
                .newsId(news.getNewsId())
                .title(news.getTitle())
                .content(news.getContent())
                .press(news.getPress())
                .link(null) // TODO: link 필드 추가 필요
                .trusted(news.getTrusted() ? 1 : 0)
                .publishedAt(parsePublishedAt(news.getPublishedAt()))
                .createdAt(news.getCreatedAt())
                .reporterName(news.getReporter())
                .categoryName(news.getCategoryName().name())
                .dedupState(news.getDedupState().name())
                .dedupStateDescription(news.getDedupState().getDescription())
                .imageUrl(news.getImageUrl())
                .oidAid(news.getOidAid())
                .build();
    }
    
    private NewsListResponse convertToNewsListResponse(News news) {
        return NewsListResponse.builder()
                .newsId(news.getNewsId())
                .title(news.getTitle())
                .content(news.getContent())
                .press(news.getPress())
                .link(null) // TODO: link 필드 추가 필요
                .trusted(news.getTrusted() ? 1 : 0)
                .publishedAt(parsePublishedAt(news.getPublishedAt()))
                .createdAt(news.getCreatedAt())
                .reporterName(news.getReporter())
                .viewCount(0) // TODO: view count 필드 추가 필요
                .categoryName(news.getCategoryName().name())
                .dedupState(news.getDedupState().name())
                .dedupStateDescription(news.getDedupState().getDescription())
                .imageUrl(news.getImageUrl())
                .oidAid(news.getOidAid())
                .build();
    }
    
    private CategoryDto convertToCategoryDto(Category category) {
        return CategoryDto.builder()
                .categoryCode(category.name())
                .categoryName(category.getCategoryName())
                .icon("📰") // 기본 아이콘
                .build();
    }
    
    // 요약 생성 메서드 (간단한 구현)
    private String generateSummary(String content) {
        if (content == null || content.length() <= 200) {
            return content;
        }
        return content.substring(0, 200) + "...";
    }
    
    // 신뢰도 계산 메서드 (간단한 구현)
    private Boolean calculateTrusted(NewsCrawl newsCrawl) {
        int trusted = 50; // 기본값
        
        // 내용 길이에 따른 신뢰도 조정
        if (newsCrawl.getContent() != null) {
            if (newsCrawl.getContent().length() > 1000) {
                trusted += 20;
            } else if (newsCrawl.getContent().length() > 500) {
                trusted += 10;
            }
        }
        
        // 기자명이 있는 경우 신뢰도 증가
        if (newsCrawl.getReporterName() != null && !newsCrawl.getReporterName().trim().isEmpty()) {
            trusted += 10;
        }
        
        // 언론사에 따른 신뢰도 조정
        if (newsCrawl.getPress() != null) {
            String press = newsCrawl.getPress().toLowerCase();
            if (press.contains("조선일보") || press.contains("중앙일보") || press.contains("동아일보")) {
                trusted += 15;
            } else if (press.contains("한겨레") || press.contains("경향신문")) {
                trusted += 10;
            }
        }
        
        return trusted >= 70; // 70 이상이면 true
    }

    // 안전한 날짜 파싱 메서드
    private LocalDateTime parsePublishedAt(String publishedAt) {
        if (publishedAt == null || publishedAt.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        
        try {
            // MySQL의 DATETIME 형식 (2025-08-07 11:50:01.000000) 처리
            if (publishedAt.contains(".")) {
                // 마이크로초 부분 제거
                String withoutMicroseconds = publishedAt.substring(0, publishedAt.lastIndexOf("."));
                return LocalDateTime.parse(withoutMicroseconds, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                // 일반적인 형식
                return LocalDateTime.parse(publishedAt, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception e) {
            System.err.println("날짜 파싱 실패: " + publishedAt + ", 에러: " + e.getMessage());
            return LocalDateTime.now();
        }
    }
    
    // 키워드 구독 관련 메서드들
    @Override
    public KeywordSubscriptionDto subscribeKeyword(Long userId, String keyword) {
        // 이미 구독 중인지 확인
        if (keywordSubscriptionRepository.existsByUserIdAndKeywordAndIsActiveTrue(userId, keyword)) {
            throw new RuntimeException("이미 구독 중인 키워드입니다: " + keyword);
        }
        
        KeywordSubscription subscription = KeywordSubscription.builder()
                .userId(userId)
                .keyword(keyword)
                .isActive(true)
                .build();
        
        KeywordSubscription saved = keywordSubscriptionRepository.save(subscription);
        return convertToKeywordSubscriptionDto(saved);
    }
    
    @Override
    public void unsubscribeKeyword(Long userId, String keyword) {
        KeywordSubscription subscription = keywordSubscriptionRepository
                .findByUserIdAndKeywordAndIsActiveTrue(userId, keyword)
                .orElseThrow(() -> new RuntimeException("구독하지 않은 키워드입니다: " + keyword));
        
        subscription.setIsActive(false);
        keywordSubscriptionRepository.save(subscription);
    }
    
    @Override
    public List<KeywordSubscriptionDto> getUserKeywordSubscriptions(Long userId) {
        return keywordSubscriptionRepository.findByUserIdAndIsActiveTrue(userId)
                .stream()
                .map(this::convertToKeywordSubscriptionDto)
                .collect(Collectors.toList());
    }

    // 트렌딩 키워드 관련 메서드들
    @Override
    public List<TrendingKeywordDto> getTrendingKeywords(int limit) {
        // 최근 7일간의 뉴스에서 키워드 추출 및 트렌딩 점수 계산
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        // 실제 구현에서는 뉴스 내용에서 키워드를 추출하고 트렌딩 점수를 계산해야 함
        // 여기서는 간단한 예시로 인기 키워드를 반환
        return getPopularKeywords(limit);
    }
    
    @Override
    public List<TrendingKeywordDto> getPopularKeywords(int limit) {
        List<Object[]> popularKeywords = keywordSubscriptionRepository.findPopularKeywords();
        
        return popularKeywords.stream()
                .limit(limit)
                .map(result -> TrendingKeywordDto.builder()
                        .keyword((String) result[0])
                        .count((Long) result[1])
                        .trendScore((double) result[1]) // 간단히 구독 수를 트렌딩 점수로 사용
                        .build())
                .collect(Collectors.toList());
    }
    
    @Override
    public List<TrendingKeywordDto> getTrendingKeywordsByCategory(Category category, int limit) {
        log.info("카테고리별 트렌딩 키워드 조회 시작: category={}, limit={}", category, limit);
        
        // 해당 카테고리의 최근 뉴스에서 키워드 추출 (기간을 30일로 확장)
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        log.info("조회 기간: {} ~ {}", monthAgo, LocalDateTime.now());
        
        try {
            // 해당 카테고리의 최근 뉴스 조회 (개수를 500개로 증가)
            Page<News> categoryNews = newsRepository.findByCategory(category, Pageable.ofSize(500));
            log.info("카테고리 {} 전체 뉴스 수: {}", category, categoryNews.getTotalElements());
            
            List<News> recentNews = categoryNews.getContent().stream()
                    .filter(news -> {
                        try {
                            LocalDateTime publishedAt = LocalDateTime.parse(news.getPublishedAt());
                            return publishedAt.isAfter(monthAgo);
                        } catch (Exception e) {
                            log.debug("날짜 파싱 실패: newsId={}, publishedAt={}", news.getNewsId(), news.getPublishedAt());
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
            
            log.info("카테고리 {}의 최근 뉴스 수: {}", category, recentNews.size());
            
            if (recentNews.isEmpty()) {
                log.warn("최근 뉴스가 없어 기본 키워드를 반환합니다: category={}", category);
                return getDefaultKeywordsByCategory(category, limit);
            }
            
            // 키워드 추출 및 빈도 계산
            Map<String, Long> keywordCounts = recentNews.stream()
                    .flatMap(news -> extractKeywordsFromNews(news).stream())
                    .collect(Collectors.groupingBy(keyword -> keyword, Collectors.counting()));
            
            log.info("추출된 키워드 수: {}", keywordCounts.size());
            log.debug("키워드 빈도: {}", keywordCounts);
            
            List<TrendingKeywordDto> result = keywordCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(limit)
                    .map(entry -> TrendingKeywordDto.builder()
                            .keyword(entry.getKey())
                            .count(entry.getValue())
                            .trendScore(entry.getValue().doubleValue())
                            .build())
                    .collect(Collectors.toList());
            
            log.info("카테고리별 트렌드 키워드 결과: category={}, resultSize={}", category, result.size());
            
            // 결과가 비어있으면 기본 키워드 반환
            if (result.isEmpty()) {
                log.info("추출된 키워드가 없어 기본 키워드를 반환합니다: category={}", category);
                return getDefaultKeywordsByCategory(category, limit);
            }
            
            return result;
                    
        } catch (Exception e) {
            log.error("카테고리별 트렌딩 키워드 조회 실패: category={}, error={}", category, e.getMessage(), e);
            return getDefaultKeywordsByCategory(category, limit);
        }
    }

    @Override
    public void reportNews(Long newsId, Long userId) {

    }

    @Override
    public void scrapNews(Long newsId, Long userId) {

    }

    @Override
    public List<ScrapStorageResponse> getUserScrapStorages(Long userId) {
        return List.of();
    }

    @Override
    public ScrapStorageResponse createCollection(Long userId, String storageName) {
        return null;
    }

    @Override
    public void addNewsToCollection(Long userId, Integer collectionId, Long newsId) {

    }

    @Override
    public Page<ScrappedNewsResponse> getNewsInCollection(Long userId, Integer collectionId, Pageable pageable) {
        return null;
    }

    /**
     * 뉴스에서 키워드 추출
     */
    private List<String> extractKeywordsFromNews(News news) {
        List<String> keywords = new ArrayList<>();
        
        // 제목에서 키워드 추출
        if (news.getTitle() != null) {
            List<String> titleKeywords = extractKeywordsFromText(news.getTitle());
            log.debug("제목에서 추출된 키워드: {}", titleKeywords);
            keywords.addAll(titleKeywords);
        }
        
        // 내용에서 키워드 추출 (내용이 너무 길면 앞부분만 사용, 길이를 1000자로 증가)
        if (news.getContent() != null) {
            String content = news.getContent();
            if (content.length() > 1000) {
                content = content.substring(0, 1000);
            }
            List<String> contentKeywords = extractKeywordsFromText(content);
            log.debug("내용에서 추출된 키워드 수: {}", contentKeywords.size());
            keywords.addAll(contentKeywords);
        }
        
        log.debug("전체 추출된 키워드: {}", keywords);
        return keywords;
    }
    
    /**
     * 텍스트에서 키워드 추출
     */
    private List<String> extractKeywordsFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> keywords = new ArrayList<>();
        
        // 1. 공백으로 분할
        String[] words = text.split("\\s+");
        
        for (String word : words) {
            if (word == null || word.trim().isEmpty()) {
                continue;
            }
            
            // 2. 특수문자 제거 (한글, 영문, 숫자만 남김)
            String cleanedWord = word.replaceAll("[^가-힣0-9A-Za-z]", "");
            
            // 3. 더 관대한 키워드 필터링 조건
            if (cleanedWord.length() >= 2 && 
                !STOPWORDS.contains(cleanedWord) &&
                !cleanedWord.equals("있다") && 
                !cleanedWord.equals("없다") && 
                !cleanedWord.equals("하다") && 
                !cleanedWord.equals("되다") && 
                !cleanedWord.equals("이다") &&
                !cleanedWord.equals("것") &&
                !cleanedWord.equals("수") &&
                !cleanedWord.equals("등") &&
                !cleanedWord.equals("및") &&
                !cleanedWord.equals("또는") &&
                !cleanedWord.equals("그리고") &&
                !cleanedWord.equals("이번") &&
                !cleanedWord.equals("지난") &&
                !cleanedWord.equals("현재") &&
                !cleanedWord.equals("최대") &&
                !cleanedWord.equals("최소") &&
                !cleanedWord.equals("현장") &&
                !cleanedWord.equals("관련") &&
                !cleanedWord.equals("기자") &&
                !cleanedWord.equals("사진") &&
                !cleanedWord.equals("영상") &&
                !cleanedWord.equals("단독") &&
                !cleanedWord.equals("인터뷰") &&
                !cleanedWord.equals("종합") &&
                !cleanedWord.equals("오늘") &&
                !cleanedWord.equals("내일") &&
                !cleanedWord.equals("정부") &&
                !cleanedWord.equals("대통령") &&
                !cleanedWord.equals("국회") &&
                !cleanedWord.equals("한국") &&
                !cleanedWord.equals("대한민국") &&
                !cleanedWord.equals("뉴스") &&
                !cleanedWord.equals("기사") &&
                !cleanedWord.equals("외신")) {
                
                keywords.add(cleanedWord);
                log.debug("추출된 키워드: '{}' (원본: '{}')", cleanedWord, word);
            }
        }
        
        log.debug("텍스트에서 추출된 키워드 수: {}", keywords.size());
        return keywords;
    }
    
    /**
     * 기본 키워드 반환
     */
    private List<TrendingKeywordDto> getDefaultKeywords(int limit) {
        List<String> defaultKeywords = Arrays.asList(
            "주요뉴스", "핫이슈", "트렌드", "분석", "전망", "동향", "소식", "업데이트"
        );
        
        return defaultKeywords.stream()
                .limit(limit)
                .map(keyword -> TrendingKeywordDto.builder()
                        .keyword(keyword)
                        .count(1L)
                        .trendScore(1.0)
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * 카테고리별 기본 키워드 반환
     */
    private List<TrendingKeywordDto> getDefaultKeywordsByCategory(Category category, int limit) {
        List<String> defaultKeywords = switch (category) {
            case VEHICLE -> Arrays.asList(
                "전기차", "자율주행", "대중교통", "도로교통", "친환경", "모빌리티", "자동차시장", "교통정책"
            );
            case ECONOMY -> Arrays.asList(
                "주식", "부동산", "금리", "환율", "투자", "경제정책", "기업실적", "시장동향"
            );
            case POLITICS -> Arrays.asList(
                "정치", "국회", "정부", "외교", "정책", "선거", "여야", "국정감사"
            );
            case SOCIETY -> Arrays.asList(
                "사회", "교육", "복지", "의료", "환경", "안전", "범죄", "사회문제"
            );
            case IT_SCIENCE -> Arrays.asList(
                "AI", "빅데이터", "클라우드", "블록체인", "5G", "반도체", "소프트웨어", "디지털전환"
            );
            case INTERNATIONAL -> Arrays.asList(
                "국제", "외교", "무역", "글로벌", "외국", "국제정세", "외교정책", "국제협력"
            );
            case LIFE -> Arrays.asList(
                "생활", "문화", "건강", "요리", "패션", "여행", "취미", "라이프스타일"
            );
            case TRAVEL_FOOD -> Arrays.asList(
                "여행", "음식", "맛집", "관광", "호텔", "레스토랑", "카페", "여행지"
            );
            case ART -> Arrays.asList(
                "예술", "영화", "음악", "미술", "공연", "문화", "창작", "아트"
            );
            default -> Arrays.asList(
                "주요뉴스", "핫이슈", "트렌드", "분석", "전망", "동향", "소식", "업데이트"
            );
        };
        
        return defaultKeywords.stream()
                .limit(limit)
                .map(keyword -> TrendingKeywordDto.builder()
                        .keyword(keyword)
                        .count(1L)
                        .trendScore(1.0)
                        .build())
                .collect(Collectors.toList());
    }
    
    // 너무 일반적인 단어는 제외
    private static final Set<String> STOPWORDS = Set.of(
        "속보", "영상", "단독", "인터뷰", "기자", "사진", "종합", "오늘", "내일",
        "정부", "대통령", "국회", "한국", "대한민국", "뉴스", "기사", "외신",
        "관련", "이번", "지난", "현재", "최대", "최소", "현장", "및", "또는", "그리고",
        "있다", "없다", "하다", "되다", "이다"
    );
    
    private KeywordSubscriptionDto convertToKeywordSubscriptionDto(KeywordSubscription subscription) {
        return KeywordSubscriptionDto.builder()
                .subscriptionId(subscription.getSubscriptionId())
                .userId(subscription.getUserId())
                .keyword(subscription.getKeyword())
                .isActive(subscription.getIsActive())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }
} 