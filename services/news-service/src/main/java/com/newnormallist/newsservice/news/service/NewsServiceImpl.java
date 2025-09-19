package com.newnormallist.newsservice.news.service;

import com.newnormallist.newsservice.news.dto.*;
import com.newnormallist.newsservice.news.entity.*;
import com.newnormallist.newsservice.news.exception.*;
import com.newnormallist.newsservice.news.repository.*;
import com.newnormallist.newsservice.tooltip.client.TooltipServiceClient;
import com.newnormallist.newsservice.tooltip.dto.*;
import com.newnormallist.newsservice.news.client.UserServiceClient;
import com.newnormallist.newsservice.news.client.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
public class NewsServiceImpl implements NewsService {

    @Autowired
    private NewsCrawlRepository newsCrawlRepository;

    @Autowired
    private NewsRepository newsRepository;
    
    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private TooltipServiceClient tooltipServiceClient;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    // 비동기 처리를 위한 Executor (커넥션 풀과 균형 맞춤)
    private final Executor asyncExecutor = Executors.newFixedThreadPool(3); // 5 → 3으로 감소

    @Autowired
    private KeywordSubscriptionRepository keywordSubscriptionRepository;

    @Autowired
    private NewsScrapRepository newsScrapRepository;

    @Autowired
    private ScrapStorageRepository scrapStorageRepository;

    @Autowired
    private NewsComplaintRepository newsComplaintRepository;



    // 크롤링 관련 메서드들
    @Override
    @Transactional
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

        // 신고 건수 확인
        long complaintCount = newsComplaintRepository.countByNewsNewsId(newsId);
        if (complaintCount >= 10) {
            throw new NewsForbiddenException("많은 신고가 접수되어 접근이 제한된 뉴스입니다.");
        }

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
                .content(processedContent) 
                .press(news.getPress())
                .link(news.getLink())
                .publishedAt(parsePublishedAt(news.getPublishedAt()))
                .reporterName(news.getReporter())
                .createdAt(news.getCreatedAt())
                .updatedAt(news.getUpdatedAt())
                .trusted(news.getTrusted() ? 1 : 0)
                .imageUrl(news.getImageUrl())
                .oidAid(news.getOidAid())
                .categoryName(news.getCategoryName().name())
                .viewCount(news.getViewCount())
                .build();
                // ----- 툴팁 기능을 위한 코드 끝 -----
    }

    @Override
    public List<NewsResponse> getPersonalizedNews(Long userId) {
        try {
            log.info("개인화 뉴스 조회 시작: userId={}", userId);
            
            // 1. 사용자 선호도 조회 (UserServiceClient 사용)
            List<String> userPreferences = getUserPreferences(userId);
            List<String> readingHistory = getUserReadingHistory(userId);
            
            if (userPreferences.isEmpty() && readingHistory.isEmpty()) {
                log.info("신규 사용자 또는 데이터 부족: userId={}, 신뢰도 높은 뉴스 반환", userId);
                // 신규 사용자의 경우 신뢰도 높은 뉴스 반환
                return newsRepository.findByTrustedTrue(Pageable.ofSize(10))
                        .getContent()
                        .stream()
                        .map(this::convertToNewsResponse)
                        .collect(Collectors.toList());
            }
            
            // 2. 개인화된 뉴스 조회
            List<News> personalizedNews;
            if (!userPreferences.isEmpty()) {
                // String 카테고리를 Category enum으로 변환
                List<Category> categories = userPreferences.stream()
                        .map(this::stringToCategory)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                
                if (!categories.isEmpty()) {
                    // 카테고리 기반 개인화 뉴스 조회
                    personalizedNews = newsRepository.findPersonalizedNewsByCategories(
                        categories, Pageable.ofSize(15)
                    );
                } else {
                    // 변환 실패 시 신뢰도 높은 뉴스 조회
                    personalizedNews = newsRepository.findByTrustedTrue(Pageable.ofSize(15))
                            .getContent();
                }
            } else {
                // 읽기 기록 기반 뉴스 조회 (신뢰도 높은 뉴스)
                personalizedNews = newsRepository.findByTrustedTrue(Pageable.ofSize(15))
                        .getContent();
            }
            
            // 3. 다양성을 위해 카테고리별로 균형있게 선택
            List<NewsResponse> diversifiedNews = diversifyNews(personalizedNews)
                    .stream()
                    .limit(10)
                    .map(this::convertToNewsResponse)
                    .collect(Collectors.toList());
            
            log.info("개인화 뉴스 조회 완료: userId={}, count={}", userId, diversifiedNews.size());
            return diversifiedNews;
                    
        } catch (Exception e) {
            log.error("개인화 뉴스 조회 실패: userId={}", userId, e);
            // 폴백: 신뢰도 높은 뉴스 반환
            return newsRepository.findByTrustedTrue(Pageable.ofSize(10))
                    .getContent()
                    .stream()
                    .map(this::convertToNewsResponse)
                    .collect(Collectors.toList());
        }
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
    @Transactional(readOnly = false)
    public void incrementViewCount(Long newsId) {
        try {
            String key = "news:viewcount:" + newsId;
            String dailyKey = "news:daily:viewcount:" + newsId + ":" + LocalDate.now();
            
            // Redis에서 조회수 증가 (원자적 연산)
            redisTemplate.opsForValue().increment(key, 1);
            redisTemplate.opsForValue().increment(dailyKey, 1);
            
            // 일일 조회수는 자정에 만료
            redisTemplate.expire(dailyKey, Duration.ofDays(1));
            
            // 비동기로 DB 업데이트 (배치 처리)
            CompletableFuture.runAsync(() -> updateViewCountInDB(newsId), asyncExecutor);
            
            log.debug("뉴스 조회수 증가: newsId={}", newsId);
            
        } catch (Exception e) {
            log.error("조회수 증가 실패: newsId={}", newsId, e);
        }
    }


    @Override
    public Page<NewsListResponse> getTrendingNews(Pageable pageable) {
        return newsRepository.findTrendingNews(pageable)
                .map(this::convertToNewsListResponse);
    }

    @Override
    public Page<NewsListResponse> getRecommendedNews(Long userId, Pageable pageable) {
        try {
            // 사용자 프로필 기반 추천
            UserProfile userProfile = getUserProfile(userId);
            
            if (userProfile.isNewUser()) {
                // 신규 사용자: 인기 뉴스 + 다양한 카테고리
                return getPopularNewsForNewUser(pageable);
            }
            
            // 기존 사용자: 협업 필터링 + 콘텐츠 기반 필터링
            List<News> recommendedNews = getHybridRecommendations(userId, userProfile);
            
            // 페이징 처리
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), recommendedNews.size());
            
            List<NewsListResponse> pageContent = recommendedNews
                    .subList(start, end)
                    .stream()
                    .map(this::convertToNewsListResponse)
                    .collect(Collectors.toList());
            
            return new PageImpl<>(pageContent, pageable, recommendedNews.size());
            
        } catch (Exception e) {
            log.error("추천 뉴스 조회 실패: userId={}", userId, e);
            // 폴백: 신뢰도 높은 뉴스 반환
            return newsRepository.findByTrustedTrue(pageable)
                    .map(this::convertToNewsListResponse);
        }
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
    // 4. DTO 변환 메서드 개선 (link 필드 및 viewCount 추가)
    private NewsResponse convertToNewsResponse(News news) {
        return NewsResponse.builder()
                .newsId(news.getNewsId())
                .title(news.getTitle())
                .content(news.getContent())
                .press(news.getPress())
                .link(buildNewsLink(news)) // 뉴스 링크 생성
                .trusted(news.getTrusted() ? 1 : 0)
                .publishedAt(parsePublishedAt(news.getPublishedAt()))
                .createdAt(news.getCreatedAt())
                .reporterName(news.getReporter())
                .categoryName(news.getCategoryName().name())
                .dedupState(news.getDedupState().name())
                .dedupStateDescription(news.getDedupState().getDescription())
                .imageUrl(news.getImageUrl())
                .oidAid(news.getOidAid())
                .viewCount(news.getViewCount())
                .build();
    }

    private NewsListResponse convertToNewsListResponse(News news) {
        return NewsListResponse.builder()
                .newsId(news.getNewsId())
                .title(news.getTitle())
                .content(news.getContent())
                .press(news.getPress())
                .link(buildNewsLink(news)) // 뉴스 링크 생성
                .trusted(news.getTrusted() ? 1 : 0)
                .publishedAt(parsePublishedAt(news.getPublishedAt()))
                .createdAt(news.getCreatedAt())
                .reporterName(news.getReporter())
                .viewCount(getViewCount(news.getNewsId()).intValue()) // 실제 조회수 조회
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
                .icon("📰") 
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
    @Transactional(readOnly = false)
    public void reportNews(Long newsId, Long userId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsNotFoundException("뉴스를 찾을 수 없습니다: " + newsId));

        // TODO: 이미 신고한 사용자인지 체크하는 로직을 추가하면 좋습니다. (중복 신고 방지)

        NewsComplaint complaint = NewsComplaint.builder()
                .userId(userId)
                .news(news)
                .build();

        newsComplaintRepository.save(complaint);
        log.info("사용자 {}가 뉴스 {}를 신고했습니다. DB 저장 완료.", userId, newsId);

        // 신고 건수 확인
        long complaintCount = newsComplaintRepository.countByNewsNewsId(newsId);
        log.info("뉴스 {}의 총 신고 건수: {}", newsId, complaintCount);

        if (complaintCount >= 20) {
            log.warn("뉴스 {}의 신고 건수가 {}건에 도달하여 상태를 HIDDEN으로 변경합니다.", newsId, complaintCount);
            news.setStatus(NewsStatus.HIDDEN);
            newsRepository.save(news);
        }
    }

    @Override
    @Transactional(readOnly = false)
    public void scrapNews(Long newsId, Long userId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsNotFoundException("뉴스를 찾을 수 없습니다: " + newsId));

        // 이미 스크랩되었는지 확인합니다. (storageId가 null인 경우 포함)
        // 동일 사용자가 이미 스크랩했는지 확인 (userId로 검사)
        boolean alreadyScrapped = !newsScrapRepository.findByUserIdAndNewsNewsId(userId, newsId).isEmpty();

        if (alreadyScrapped) {
            throw new IllegalStateException("이미 스크랩된 뉴스입니다.");
        }

        // NewsScrap 엔티티를 생성하고 저장. storageId는 초기에는 null로 설정
        NewsScrap newsScrap = NewsScrap.builder()
                .news(news)
                .userId(userId) // userId 설정
                .storageId(null) // 초기에는 storageId를 null로 설정
                .build();

        newsScrapRepository.save(newsScrap);
        log.info("뉴스 스크랩 완료 (임시 저장): userId={}, newsId={}", userId, newsId);
    }

    @Override
    public List<ScrapStorageResponse> getUserScrapStorages(Long userId) {
        return scrapStorageRepository.findByUserId(userId)
                .stream()
                .map(this::convertToScrapStorageResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ScrapStorageResponse getCollectionDetails(Long userId, Integer collectionId) {
        ScrapStorage scrapStorage = scrapStorageRepository.findById(collectionId)
                .filter(storage -> storage.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalStateException("조회 권한이 없거나 존재하지 않는 컬렉션입니다: " + collectionId));
        return convertToScrapStorageResponse(scrapStorage);
    }

    @Override
    public ScrapStorageResponse createCollection(Long userId, String storageName) {
        // 보관함 이름 중복 체크
        scrapStorageRepository.findByUserId(userId).stream()
                .filter(storage -> storage.getStorageName().equals(storageName))
                .findAny()
                .ifPresent(storage -> {
                    throw new IllegalStateException("이미 존재하는 보관함 이름입니다: " + storageName);
                });

        ScrapStorage newStorage = ScrapStorage.builder()
                .userId(userId)
                .storageName(storageName)
                .build();
        ScrapStorage savedStorage = scrapStorageRepository.save(newStorage);
        log.info("새 스크랩 보관함 생성: userId={}, storageName={}", userId, storageName);
        return convertToScrapStorageResponse(savedStorage);
    }

    @Override
    public ScrapStorageResponse updateCollection(Long userId, Integer collectionId, String newName) {
        // 1. 컬렉션 조회 및 소유권 확인
        ScrapStorage scrapStorage = scrapStorageRepository.findById(collectionId)
                .filter(storage -> storage.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalStateException("수정 권한이 없거나 존재하지 않는 컬렉션입니다: " + collectionId));

        // 2. 새로운 이름이 현재 이름과 동일한지 확인
        if (scrapStorage.getStorageName().equals(newName)) {
            return convertToScrapStorageResponse(scrapStorage);
        }

        // 3. 새로운 이름이 해당 사용자의 다른 컬렉션과 중복되는지 확인
        scrapStorageRepository.findByUserId(userId).stream()
                .filter(storage -> storage.getStorageName().equals(newName))
                .findAny()
                .ifPresent(storage -> {
                    throw new IllegalStateException("이미 존재하는 컬렉션 이름입니다: " + newName);
                });

        // 4. 이름 변경 및 저장
        scrapStorage.setStorageName(newName);
        ScrapStorage updatedStorage = scrapStorageRepository.save(scrapStorage);
        log.info("컬렉션 이름 변경 완료: userId={}, collectionId={}, newName={}", userId, collectionId, newName);

        // 5. DTO로 변환하여 반환
        return convertToScrapStorageResponse(updatedStorage);
    }

    @Override
    @Transactional(readOnly = false)
    public void addNewsToCollection(Long userId, Integer collectionId, Long newsId) {
        // 1. 사용자의 보관함이 맞는지 확인
        scrapStorageRepository.findById(collectionId)
                .filter(storage -> storage.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalStateException("유효하지 않은 스크랩 보관함입니다: " + collectionId));

        // 2. 사용자가 해당 뉴스를 이미 스크랩했는지 확인
        List<NewsScrap> existingScraps = newsScrapRepository.findByUserIdAndNewsNewsId(userId, newsId);

        if (!existingScraps.isEmpty()) {
            // 3. 이미 스크랩한 경우: 기존 스크랩의 storageId를 업데이트
            NewsScrap scrapToUpdate = existingScraps.get(0); // 중복 스크랩이 없다고 가정

            // 이미 해당 컬렉션에 속해 있는지 확인
            if (collectionId.equals(scrapToUpdate.getStorageId())) {
                throw new IllegalStateException("이미 해당 컬렉션에 추가된 뉴스입니다.");
            }

            scrapToUpdate.setStorageId(collectionId);
            newsScrapRepository.save(scrapToUpdate);
            log.info("기존 스크랩을 컬렉션에 추가: userId={}, newsId={}, collectionId={}", userId, newsId, collectionId);

        } else {
            // 4. 스크랩하지 않은 경우: 새로운 스크랩을 생성하고 컬렉션에 추가
            News news = newsRepository.findById(newsId)
                    .orElseThrow(() -> new NewsNotFoundException("뉴스를 찾을 수 없습니다: " + newsId));

            NewsScrap newScrap = NewsScrap.builder()
                    .storageId(collectionId)
                    .news(news)
                    .userId(userId)
                    .build();

            newsScrapRepository.save(newScrap);
            log.info("새로운 스크랩을 생성하여 컬렉션에 추가: userId={}, newsId={}, collectionId={}", userId, newsId, collectionId);
        }
    }

    @Override
    @Transactional(readOnly = false)
    public void assignScrapToStorage(Long userId, Integer newsScrapId, Integer targetStorageId) {
        NewsScrap newsScrap = newsScrapRepository.findById(newsScrapId)
                .orElseThrow(() -> new IllegalStateException("스크랩을 찾을 수 없습니다: " + newsScrapId));

        // 스크랩이 현재 사용자에게 속하는지 확인
        if (!newsScrap.getUserId().equals(userId)) {
            throw new IllegalStateException("해당 스크랩에 대한 권한이 없습니다: " + newsScrapId);
        }

        // 대상 보관함이 사용자의 것인지 확인
        ScrapStorage targetStorage = scrapStorageRepository.findById(targetStorageId)
                .filter(storage -> storage.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalStateException("유효하지 않은 대상 보관함입니다: " + targetStorageId));

        // 스크랩의 storageId를 업데이트
        newsScrap.setStorageId(targetStorageId);
        newsScrapRepository.save(newsScrap);
        log.info("스크랩 이동 완료: newsScrapId={}, targetStorageId={}", newsScrapId, targetStorageId);
    }

    private ScrapStorageResponse convertToScrapStorageResponse(ScrapStorage storage) {
        long newsCount = newsScrapRepository.countByStorageId(storage.getStorageId());
        return ScrapStorageResponse.builder()
                .storageId(storage.getStorageId())
                .storageName(storage.getStorageName())
                .newsCount(newsCount)
                .createdAt(storage.getCreatedAt())
                .updatedAt(storage.getUpdatedAt())
                .build();
    }

    @Override
    public Page<ScrappedNewsResponse> getNewsInCollection(Long userId, Integer collectionId, String category, String query, Pageable pageable) {
        // 1. 사용자의 보관함이 맞는지 확인
        scrapStorageRepository.findById(collectionId)
                .filter(storage -> storage.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalStateException("유효하지 않은 스크랩 보관함입니다: " + collectionId));

        Page<NewsScrap> scrapsPage;

        // 2. 검색어(query)가 있을 경우, 제목으로 검색
        if (query != null && !query.trim().isEmpty()) {
            scrapsPage = newsScrapRepository.findByStorageIdAndNews_TitleContaining(collectionId, query, pageable);
        } else if (category != null && !category.isEmpty() && !category.equalsIgnoreCase("전체")) {
            // 3. 검색어가 없을 경우, 카테고리로 필터링
            Category categoryEnum = Arrays.stream(Category.values())
                    .filter(c -> c.getCategoryName().equalsIgnoreCase(category))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No enum constant for category name: " + category));
            scrapsPage = newsScrapRepository.findByStorageIdAndNews_CategoryName(collectionId, categoryEnum, pageable);
        } else {
            // 4. 검색어와 카테고리 필터가 모두 없을 경우, 전체 조회
            scrapsPage = newsScrapRepository.findByStorageIdWithNews(collectionId, pageable);
        }

        // 5. ScrappedNewsResponse DTO로 변환
        return scrapsPage.map(ScrappedNewsResponse::from);
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteCollection(Long userId, Integer collectionId) {
        // 1. 보관함이 사용자의 소유인지 확인
        ScrapStorage scrapStorage = scrapStorageRepository.findById(collectionId)
                .filter(storage -> storage.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalStateException("삭제 권한이 없거나 존재하지 않는 컬렉션입니다: " + collectionId));

        // 2. 해당 보관함에 속한 모든 스크랩(news_scrap)을 삭제
        newsScrapRepository.deleteByStorageId(collectionId);
        log.info("컬렉션에 포함된 뉴스 스크랩 삭제 완료: storageId={}", collectionId);

        // 3. 보관함 자체를 삭제
        scrapStorageRepository.delete(scrapStorage);
        log.info("컬렉션 삭제 완료: userId={}, storageId={}", userId, collectionId);
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteNewsFromCollection(Long userId, Integer collectionId, Long newsId) {
        // 1. 보관함이 사용자의 소유인지 확인
        scrapStorageRepository.findById(collectionId)
                .filter(storage -> storage.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalStateException("삭제 권한이 없거나 존재하지 않는 컬렉션입니다: " + collectionId));

        // 2. 해당 보관함에 속한 특정 뉴스 스크랩을 찾음
        NewsScrap newsScrap = newsScrapRepository.findByStorageIdAndNewsNewsId(collectionId, newsId)
                .orElseThrow(() -> new IllegalStateException("컬렉션에 해당 뉴스가 존재하지 않습니다."));

        // 3. 스크랩 삭제
        newsScrapRepository.delete(newsScrap);
        log.info("컬렉션에서 뉴스 삭제 완료: userId={}, collectionId={}, newsId={}", userId, collectionId, newsId);
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
     * 텍스트에서 키워드 추출 - 개선된 필터링 로직
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
            
            // 3. 개선된 키워드 필터링 조건
            if (isValidKeyword(cleanedWord)) {
                keywords.add(cleanedWord);
                log.debug("추출된 키워드: '{}' (원본: '{}')", cleanedWord, word);
            }
        }
        
        log.debug("텍스트에서 추출된 키워드 수: {}", keywords.size());
        return keywords;
    }
    
    /**
     * 키워드 유효성 검사 - 체계적인 필터링
     */
    private boolean isValidKeyword(String word) {
        if (word == null || word.length() < 2) {
            return false;
        }
        
        // 1. 불용어 목록에 포함된 단어 제외
        if (STOPWORDS.contains(word)) {
            return false;
        }
        
        // 2. 숫자만으로 구성된 단어 제외 (연도, 날짜 등)
        if (word.matches("^\\d+$")) {
            return false;
        }
        
        // 3. 특수 패턴 제외
        if (word.matches(".*[#@$%^&*()].*")) {
            return false;
        }
        
        // 4. 너무 짧은 영문 단어 제외 (2글자 이하)
        if (word.matches("^[A-Za-z]{1,2}$")) {
            return false;
        }
        
        // 5. 반복 문자 패턴 제외 (예: "ㅋㅋㅋ", "ㅎㅎㅎ")
        if (word.matches("(.)\\1{2,}")) {
            return false;
        }
        
        // 6. 의미없는 조합어 제외
        if (isMeaninglessCombination(word)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 의미없는 조합어 판별
     */
    private boolean isMeaninglessCombination(String word) {
        // 의미없는 조합어 패턴들
        String[] meaninglessPatterns = {
            "영화의", "기사의", "뉴스의", "사진의", "영상의", "내용의", "정보의",
            "추출할", "분석할", "조사할", "확인할", "검토할", "검증할",
            "관련된", "대한", "위한", "통한", "통해", "대해", "관해",
            "있는", "없는", "같은", "다른", "이런", "그런", "저런",
            "하는", "되는", "되는", "이되는", "되는", "되는", "되는"
        };
        
        for (String pattern : meaninglessPatterns) {
            if (word.contains(pattern)) {
                return true;
            }
        }
        
        return false;
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
    
    // 확장된 불용어 목록 - 의미없는 단어들을 체계적으로 필터링
    private static final Set<String> STOPWORDS = Set.of(
            // 뉴스 관련 일반 용어
            "속보", "영상", "단독", "인터뷰", "기자", "사진", "종합", "뉴스", "기사", "외신",
            "현장", "보도", "취재", "논평", "사설", "칼럼", "특집", "기획", "리포트",
            
            // 시간 관련
            "오늘", "내일", "어제", "이번", "지난", "현재", "최근", "곧", "이제",
            "년", "월", "일", "시", "분", "초", "주", "달", "년도",
            
            // 일반적인 조사/어미
            "것", "수", "등", "및", "또는", "그리고", "하지만", "그러나", "따라서",
            "있다", "없다", "하다", "되다", "이다", "아니다", "같다", "다르다",
            "위해", "통해", "대해", "관해", "대한", "관련", "위한", "통한",
            
            // 정부/기관 관련
            "정부", "대통령", "국회", "한국", "대한민국", "국가", "정부기관", "공공기관",
            "시청", "구청", "군청", "도청", "청", "부", "처", "원",
            
            // 일반적인 형용사/부사
            "최대", "최소", "매우", "정말", "진짜", "완전", "엄청", "너무", "아주",
            "많이", "조금", "약간", "좀", "더", "가장", "제일", "특히", "특별히",
            
            // 기타 의미없는 단어들
            "내용", "정보", "자료", "데이터", "결과", "상황", "문제", "이슈", "사건",
            "분석", "전망", "동향", "소식", "업데이트", "변화", "발전", "진전",
            "영향", "효과", "원인", "이유", "목적", "방법", "과정"
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

    // ========================================
    // 개인화 뉴스 관련 헬퍼 메서드들
    // ========================================

    /**
     * 사용자 선호도 조회 (UserServiceClient 사용)
     */
    private List<String> getUserPreferences(Long userId) {
        try {
            UserInterestResponse interestResponse = userServiceClient.getUserInterests(userId);
            if (interestResponse != null && interestResponse.getTopCategories() != null) {
                return interestResponse.getTopCategories();
            }
        } catch (Exception e) {
            log.warn("사용자 관심사 조회 실패: userId={}", userId, e);
        }
        return Collections.emptyList();
    }

    /**
     * 사용자 읽기 기록 조회 (UserServiceClient 사용)
     */
    private List<String> getUserReadingHistory(Long userId) {
        try {
            UserBehaviorAnalysis behaviorAnalysis = userServiceClient.getUserBehaviorAnalysis(userId);
            if (behaviorAnalysis != null && behaviorAnalysis.getCategoryReadCounts() != null) {
                // 카테고리별 읽기 횟수가 높은 순으로 정렬하여 반환
                return behaviorAnalysis.getCategoryReadCounts().entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("사용자 읽기 기록 조회 실패: userId={}", userId, e);
        }
        return Collections.emptyList();
    }


    /**
     * String을 Category enum으로 변환하는 헬퍼 메서드
     */
    private Category stringToCategory(String categoryName) {
        try {
            return Category.valueOf(categoryName.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("유효하지 않은 카테고리명: {}", categoryName);
        return null;
        }
    }

    // ========================================
    // 조회수 관련 헬퍼 메서드들
    // ========================================

    /**
     * 비동기로 DB의 조회수 업데이트
     */
    private void updateViewCountInDB(Long newsId) {
        try {
            // Redis에서 현재 조회수 조회
            String key = "news:viewcount:" + newsId;
            String viewCountStr = redisTemplate.opsForValue().get(key);
            
            if (viewCountStr != null) {
                Long viewCount = Long.parseLong(viewCountStr);
                
                // DB 업데이트
                newsRepository.findById(newsId).ifPresent(news -> {
                    news.setViewCount(viewCount);
                    newsRepository.save(news);
                });
                
                log.debug("DB 조회수 업데이트 완료: newsId={}, viewCount={}", newsId, viewCount);
            }
            
        } catch (Exception e) {
            log.error("DB 조회수 업데이트 실패: newsId={}", newsId, e);
        }
    }


    /**
     * 일일 조회수 조회 (Redis에서)
     */
    public Long getDailyViewCount(Long newsId) {
        try {
            String dailyKey = "news:daily:viewcount:" + newsId + ":" + LocalDate.now();
            String viewCountStr = redisTemplate.opsForValue().get(dailyKey);
            return viewCountStr != null ? Long.parseLong(viewCountStr) : 0L;
        } catch (Exception e) {
            log.error("일일 조회수 조회 실패: newsId={}", newsId, e);
            return 0L;
        }
    }


    // ========================================
    // 카카오 토큰 관련 메서드들
    // ========================================

    /**
     * 사용자의 카카오 토큰 조회 (사용자가 제공한 코드 예시)
     * 실제로는 UserServiceClient를 통해 user-service의 API를 호출
     */
    private String getUserKakaoToken(String userId) {
        try {
            log.debug("사용자 카카오 토큰 조회: userId={}", userId);
            
            // UserServiceClient를 통해 user-service의 카카오 토큰 조회 API 호출
            String kakaoToken = userServiceClient.getKakaoToken(userId);
            
            if (kakaoToken != null && !kakaoToken.trim().isEmpty()) {
                log.debug("카카오 토큰 조회 성공: userId={}", userId);
                return kakaoToken;
            } else {
                log.warn("카카오 토큰이 없음: userId={}", userId);
                return null;
            }
            
        } catch (Exception e) {
            log.error("카카오 토큰 조회 실패: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 카카오 토큰을 사용한 예시 메서드
     * 뉴스 공유 시 카카오 메시지 전송 등에 사용할 수 있음
     */
    public void shareNewsToKakao(Long userId, Long newsId) {
        try {
            // 1. 사용자의 카카오 토큰 조회
            String kakaoToken = getUserKakaoToken(String.valueOf(userId));
            
            if (kakaoToken == null) {
                log.warn("카카오 토큰이 없어 뉴스 공유를 할 수 없습니다: userId={}, newsId={}", userId, newsId);
                return;
            }
            
            // 2. 뉴스 정보 조회
            News news = newsRepository.findById(newsId)
                    .orElseThrow(() -> new NewsNotFoundException("뉴스를 찾을 수 없습니다: " + newsId));
            
            // 3. 카카오 메시지 전송 로직 (실제 구현은 KakaoMessageService 등에서)
            log.info("카카오로 뉴스 공유: userId={}, newsId={}, title={}", userId, newsId, news.getTitle());
            
            // TODO: 실제 카카오 메시지 전송 로직 구현
            // kakaoMessageService.sendMessage(kakaoToken, templateId, templateArgs);
            
        } catch (Exception e) {
            log.error("카카오 뉴스 공유 실패: userId={}, newsId={}", userId, newsId, e);
        }
    }

    // ========================================
    // 인기도 점수 계산 관련 메서드들
    // ========================================

    /**
     * 인기도 점수 계산 구현
     */
    private double calculatePopularityScore(NewsResponse news) {
        try {
            // 여러 지표를 종합하여 인기도 점수 계산
            double viewScore = calculateViewScore(news.getNewsId());
            double shareScore = calculateShareScore(news.getNewsId());
            double timeScore = calculateTimeScore(news.getPublishedAt());
            double trustScore = news.getTrusted() == 1 ? 1.0 : 0.0;
            
            // 가중평균으로 최종 점수 계산
            double popularityScore = (viewScore * 0.4) + (shareScore * 0.3) + 
                                    (timeScore * 0.2) + (trustScore * 0.1);
            
            return Math.min(1.0, popularityScore); // 최대값 1.0으로 제한
            
        } catch (Exception e) {
            log.error("인기도 점수 계산 실패: newsId={}", news.getNewsId(), e);
            return 0.5; // 기본값
        }
    }

    /**
     * 조회수 점수 계산 (0.0 ~ 1.0)
     */
    private double calculateViewScore(Long newsId) {
        try {
            Long viewCount = getViewCount(newsId);
            if (viewCount == null || viewCount == 0) {
                return 0.0;
            }
            
            // 로그 스케일링을 사용하여 조회수 점수 계산
            // 최대 조회수를 100,000으로 가정하고 로그 스케일 적용
            double normalizedScore = Math.log10(viewCount + 1) / Math.log10(100001);
            return Math.min(1.0, normalizedScore);
            
        } catch (Exception e) {
            log.error("조회수 점수 계산 실패: newsId={}", newsId, e);
            return 0.0;
        }
    }

    /**
     * 공유 점수 계산 (0.0 ~ 1.0)
     * 현재는 스크랩 수를 공유 지표로 사용
     */
    private double calculateShareScore(Long newsId) {
        try {
            // 스크랩 수를 공유 지표로 사용
            long scrapCount = newsScrapRepository.countByNewsNewsId(newsId);
            if (scrapCount == 0) {
                return 0.0;
            }
            
            // 로그 스케일링을 사용하여 공유 점수 계산
            // 최대 공유 수를 1,000으로 가정
            double normalizedScore = Math.log10(scrapCount + 1) / Math.log10(1001);
            return Math.min(1.0, normalizedScore);
            
        } catch (Exception e) {
            log.error("공유 점수 계산 실패: newsId={}", newsId, e);
            return 0.0;
        }
    }

    /**
     * 시간 점수 계산 (0.0 ~ 1.0)
     * 최신 뉴스일수록 높은 점수
     */
    private double calculateTimeScore(LocalDateTime publishedAt) {
        try {
            if (publishedAt == null) {
                return 0.0;
            }
            
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(publishedAt, now);
            long hoursAgo = duration.toHours();
            
            // 시간이 지날수록 점수가 감소
            // 24시간 이내: 1.0
            // 48시간 이내: 0.8
            // 72시간 이내: 0.6
            // 168시간(7일) 이내: 0.4
            // 그 이후: 0.2
            
            if (hoursAgo <= 24) {
                return 1.0;
            } else if (hoursAgo <= 48) {
                return 0.8;
            } else if (hoursAgo <= 72) {
                return 0.6;
            } else if (hoursAgo <= 168) { // 7일
                return 0.4;
            } else {
                return 0.2;
            }
            
        } catch (Exception e) {
            log.error("시간 점수 계산 실패: publishedAt={}", publishedAt, e);
            return 0.5; // 기본값
        }
    }

    // ========================================
    // 개인화 정보 구성 관련 메서드들
    // ========================================

    /**
     * 개인화 정보 구성 개선
     */
    private Map<String, Object> buildPersonalizationInfo(Long userId) {
        Map<String, Object> info = new HashMap<>();
        
        try {
            List<String> signupInterests = getUserSignupInterests(userId);
            List<String> subscriptionCategories = getUserSubscriptionCategories(userId);
            
            // UserServiceClient에서 카테고리별 읽기 횟수 조회
            Map<String, Long> readingHistory = userServiceClient.getCategoryReadingHistory(userId);
            
            info.put("signupInterests", signupInterests);
            info.put("subscriptionCategories", subscriptionCategories);
            info.put("hasReadingHistory", !readingHistory.isEmpty());
            info.put("totalReadCount", readingHistory.values().stream().mapToLong(Long::longValue).sum());
            info.put("preferredCategories", getTopPreferredCategories(readingHistory, 3));
            info.put("personalizationScore", calculatePersonalizationScore(signupInterests, subscriptionCategories, readingHistory));
            
            // 추가 개인화 지표
            info.put("readingTimePreference", userServiceClient.getReadingTimePreference(userId));
            info.put("devicePreference", userServiceClient.getDevicePreference(userId));
            info.put("contentLengthPreference", userServiceClient.getContentLengthPreference(userId));
            
            log.info("개인화 정보 구성 완료: userId={}, score={}", userId, info.get("personalizationScore"));
            
        } catch (Exception e) {
            log.error("개인화 정보 구성 실패: userId={}", userId, e);
            // 기본값 설정
            info.put("signupInterests", List.of());
            info.put("subscriptionCategories", List.of());
            info.put("hasReadingHistory", false);
            info.put("totalReadCount", 0L);
            info.put("preferredCategories", List.of());
            info.put("personalizationScore", 0.0);
            info.put("readingTimePreference", "MORNING");
            info.put("devicePreference", "MOBILE");
            info.put("contentLengthPreference", "MEDIUM");
        }
        
        return info;
    }

    /**
     * 사용자 가입 시 관심사 조회
     */
    private List<String> getUserSignupInterests(Long userId) {
        try {
            UserInterestResponse interestResponse = userServiceClient.getUserInterests(userId);
            if (interestResponse != null && interestResponse.getSignupInterests() != null) {
                return interestResponse.getSignupInterests();
            }
        } catch (Exception e) {
            log.warn("가입 관심사 조회 실패: userId={}", userId, e);
        }
        return Collections.emptyList();
    }

    /**
     * 사용자 구독 카테고리 조회
     */
    private List<String> getUserSubscriptionCategories(Long userId) {
        try {
            // 키워드 구독에서 카테고리 정보 추출
            List<KeywordSubscription> subscriptions = keywordSubscriptionRepository.findByUserIdAndIsActiveTrue(userId);
            return subscriptions.stream()
                    .map(KeywordSubscription::getKeyword)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("구독 카테고리 조회 실패: userId={}", userId, e);
        }
        return Collections.emptyList();
    }

    /**
     * 상위 선호 카테고리 조회
     */
    private List<String> getTopPreferredCategories(Map<String, Long> readingHistory, int limit) {
        if (readingHistory == null || readingHistory.isEmpty()) {
            return Collections.emptyList();
        }
        
        return readingHistory.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 개인화 점수 계산
     */
    private double calculatePersonalizationScore(List<String> signupInterests, 
                                               List<String> subscriptionCategories, 
                                               Map<String, Long> readingHistory) {
        try {
            double score = 0.0;
            
            // 1. 가입 관심사 점수 (30%)
            if (!signupInterests.isEmpty()) {
                score += 0.3;
            }
            
            // 2. 구독 카테고리 점수 (25%)
            if (!subscriptionCategories.isEmpty()) {
                score += Math.min(0.25, subscriptionCategories.size() * 0.05);
            }
            
            // 3. 읽기 기록 점수 (45%)
            if (readingHistory != null && !readingHistory.isEmpty()) {
                long totalReadCount = readingHistory.values().stream().mapToLong(Long::longValue).sum();
                if (totalReadCount > 0) {
                    // 읽기 횟수에 따른 점수 (최대 0.45)
                    double readingScore = Math.min(0.45, Math.log10(totalReadCount + 1) / Math.log10(101));
                    score += readingScore;
                }
            }
            
            return Math.min(1.0, score);
            
        } catch (Exception e) {
            log.error("개인화 점수 계산 실패", e);
            return 0.0;
        }
    }


    // ========================================
    // 헬퍼 메서드들
    // ========================================

    /**
     * 뉴스 링크 생성 (개선된 버전)
     */
    private String buildNewsLink(News news) {
        if (StringUtils.hasText(news.getOidAid())) {
            return news.getOidAid();
        } else if (StringUtils.hasText(news.getLink())) {
            return news.getLink();
        } else {
            return "/news/" + news.getNewsId(); // 내부 뉴스 페이지 URL
        }
    }

    /**
     * 조회수 조회 (개선된 버전)
     */
    public Long getViewCount(Long newsId) {
        try {
            String key = "news:viewcount:" + newsId;
            String viewCountStr = redisTemplate.opsForValue().get(key);
            return viewCountStr != null ? Long.parseLong(viewCountStr) : 0L;
        } catch (Exception e) {
            log.error("조회수 조회 실패: newsId={}", newsId, e);
            return 0L;
        }
    }

    /**
     * 뉴스 다양성 확보를 위한 메서드 (개선된 버전)
     */
    private List<News> diversifyNews(List<News> news) {
        if (news == null || news.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 카테고리별로 그룹화하여 다양성 확보
        Map<Category, List<News>> categoryGroups = news.stream()
                .collect(Collectors.groupingBy(News::getCategoryName));
        
        List<News> diversified = new ArrayList<>();
        int maxPerCategory = Math.max(1, 10 / categoryGroups.size());
        
        for (Map.Entry<Category, List<News>> entry : categoryGroups.entrySet()) {
            List<News> categoryNews = entry.getValue();
            int takeCount = Math.min(maxPerCategory, categoryNews.size());
            
            // 신뢰도가 높은 순으로 정렬하여 선택
            categoryNews.stream()
                    .sorted((n1, n2) -> Boolean.compare(n2.getTrusted(), n1.getTrusted()))
                    .limit(takeCount)
                    .forEach(diversified::add);
        }
        
        // 남은 자리가 있으면 전체에서 신뢰도 높은 순으로 추가
        if (diversified.size() < 10) {
            int remaining = 10 - diversified.size();
            news.stream()
                    .filter(newsItem -> !diversified.contains(newsItem))
                    .sorted((n1, n2) -> Boolean.compare(n2.getTrusted(), n1.getTrusted()))
                    .limit(remaining)
                    .forEach(diversified::add);
        }
        
        return diversified;
    }

    /**
     * 텍스트 자르기 메서드
     */
    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * 뉴스 제목 요약 생성
     */
    private String generateNewsSummary(String content, int maxLength) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        
        // HTML 태그 제거
        String cleanContent = content.replaceAll("<[^>]*>", "");
        
        // 공백 정리
        cleanContent = cleanContent.replaceAll("\\s+", " ").trim();
        
        return truncateText(cleanContent, maxLength);
    }

    /**
     * 뉴스 우선순위 계산
     */
    private double calculateNewsPriority(News news) {
        try {
            double priority = 0.0;
            
            // 1. 신뢰도 점수 (40%)
            if (news.getTrusted()) {
                priority += 0.4;
            }
            
            // 2. 조회수 점수 (30%)
            Long viewCount = getViewCount(news.getNewsId());
            if (viewCount > 0) {
                double viewScore = Math.min(0.3, Math.log10(viewCount + 1) / Math.log10(1001));
                priority += viewScore;
            }
            
            // 3. 시간 점수 (30%)
            LocalDateTime publishedAt = parsePublishedAt(news.getPublishedAt());
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(publishedAt, now);
            long hoursAgo = duration.toHours();
            
            if (hoursAgo <= 24) {
                priority += 0.3;
            } else if (hoursAgo <= 48) {
                priority += 0.2;
            } else if (hoursAgo <= 72) {
                priority += 0.1;
            }
            
            return Math.min(1.0, priority);
            
        } catch (Exception e) {
            log.error("뉴스 우선순위 계산 실패: newsId={}", news.getNewsId(), e);
            return 0.5; // 기본값
        }
    }

    /**
     * 뉴스 관련도 계산
     */
    private double calculateNewsRelevance(News news, List<String> userInterests) {
        if (userInterests == null || userInterests.isEmpty()) {
            return 0.5; // 기본값
        }
        
        try {
            double relevance = 0.0;
            String categoryName = news.getCategoryName().name();
            
            // 카테고리 매칭 점수
            if (userInterests.contains(categoryName)) {
                relevance += 0.6;
            }
            
            // 제목 키워드 매칭 점수
            if (news.getTitle() != null) {
                String title = news.getTitle().toLowerCase();
                long matchingKeywords = userInterests.stream()
                        .map(String::toLowerCase)
                        .filter(title::contains)
                        .count();
                
                if (matchingKeywords > 0) {
                    relevance += Math.min(0.4, matchingKeywords * 0.1);
                }
            }
            
            return Math.min(1.0, relevance);
            
        } catch (Exception e) {
            log.error("뉴스 관련도 계산 실패: newsId={}", news.getNewsId(), e);
            return 0.5; // 기본값
        }
    }

    // ========================================
    // 누락된 메서드들 추가
    // ========================================

    /**
     * 사용자 프로필 조회
     */
    private UserProfile getUserProfile(Long userId) {
        try {
            // 사용자 관심사 조회
            UserInterestResponse interestResponse = userServiceClient.getUserInterests(userId);
            List<String> preferredCategories = interestResponse != null && interestResponse.getTopCategories() != null 
                ? interestResponse.getTopCategories() : Collections.emptyList();
            
            // 사용자 행동 분석 조회
            UserBehaviorAnalysis behaviorAnalysis = userServiceClient.getUserBehaviorAnalysis(userId);
            Map<String, Long> categoryReadCounts = behaviorAnalysis != null && behaviorAnalysis.getCategoryReadCounts() != null
                ? behaviorAnalysis.getCategoryReadCounts() : Collections.emptyMap();
            
            // 구독 키워드 조회
            List<KeywordSubscription> subscriptions = keywordSubscriptionRepository.findByUserIdAndIsActiveTrue(userId);
            List<String> subscribedKeywords = subscriptions.stream()
                .map(KeywordSubscription::getKeyword)
                .collect(Collectors.toList());
            
            // 총 읽기 횟수 계산
            int totalReadCount = categoryReadCounts.values().stream()
                .mapToInt(Long::intValue)
                .sum();
            
            // 개인화 점수 계산
            double personalizationScore = calculatePersonalizationScore(
                preferredCategories, subscribedKeywords, categoryReadCounts);
            
            return UserProfile.builder()
                .userId(userId)
                .preferredCategories(preferredCategories)
                .categoryReadCounts(categoryReadCounts)
                .subscribedKeywords(subscribedKeywords)
                .totalReadCount(totalReadCount)
                .personalizationScore(personalizationScore)
                .build();
                
        } catch (Exception e) {
            log.error("사용자 프로필 조회 실패: userId={}", userId, e);
            // 기본 프로필 반환
            return UserProfile.builder()
                .userId(userId)
                .preferredCategories(Collections.emptyList())
                .categoryReadCounts(Collections.emptyMap())
                .subscribedKeywords(Collections.emptyList())
                .totalReadCount(0)
                .personalizationScore(0.0)
                .build();
        }
    }

    /**
     * 신규 사용자를 위한 인기 뉴스 조회
     */
    private Page<NewsListResponse> getPopularNewsForNewUser(Pageable pageable) {
        try {
            // 신뢰도가 높은 뉴스 중에서 다양한 카테고리로 구성
            List<News> trustedNews = newsRepository.findByTrustedTrue(Pageable.ofSize(50))
                .getContent();
            
            // 카테고리별로 균형있게 선택
            List<News> diversifiedNews = diversifyNews(trustedNews);
            
            // 페이징 처리
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), diversifiedNews.size());
            
            List<NewsListResponse> pageContent = diversifiedNews
                .subList(start, end)
                .stream()
                .map(this::convertToNewsListResponse)
                .collect(Collectors.toList());
            
            return new PageImpl<>(pageContent, pageable, diversifiedNews.size());
            
        } catch (Exception e) {
            log.error("신규 사용자 인기 뉴스 조회 실패", e);
            // 폴백: 기본 인기 뉴스
            return newsRepository.findPopularNews(pageable)
                .map(this::convertToNewsListResponse);
        }
    }

    /**
     * 하이브리드 추천 시스템 (협업 필터링 + 콘텐츠 기반 필터링)
     */
    private List<News> getHybridRecommendations(Long userId, UserProfile userProfile) {
        try {
            List<News> recommendations = new ArrayList<>();
            
            // 1. 콘텐츠 기반 필터링 (사용자 선호 카테고리 기반)
            if (!userProfile.getPreferredCategories().isEmpty()) {
                List<Category> categories = userProfile.getPreferredCategories().stream()
                    .map(this::stringToCategory)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                
                if (!categories.isEmpty()) {
                    List<News> contentBasedNews = newsRepository.findPersonalizedNewsByCategories(
                        categories, Pageable.ofSize(20));
                    recommendations.addAll(contentBasedNews);
                }
            }
            
            // 2. 협업 필터링 (비슷한 사용자들이 읽은 뉴스)
            List<News> collaborativeNews = getCollaborativeRecommendations(userId, userProfile);
            recommendations.addAll(collaborativeNews);
            
            // 3. 인기도 기반 보완
            if (recommendations.size() < 15) {
                List<News> popularNews = newsRepository.findPopularNews(Pageable.ofSize(10))
                    .getContent();
                recommendations.addAll(popularNews);
            }
            
            // 4. 중복 제거 및 정렬
            return recommendations.stream()
                .distinct()
                .sorted((n1, n2) -> Double.compare(
                    calculateNewsRelevance(n2, userProfile.getPreferredCategories()),
                    calculateNewsRelevance(n1, userProfile.getPreferredCategories())))
                .limit(20)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("하이브리드 추천 실패: userId={}", userId, e);
            // 폴백: 신뢰도 높은 뉴스
            return newsRepository.findByTrustedTrue(Pageable.ofSize(15))
                .getContent();
        }
    }

    /**
     * 협업 필터링 기반 추천
     */
    private List<News> getCollaborativeRecommendations(Long userId, UserProfile userProfile) {
        try {
            // 간단한 협업 필터링 구현
            // 실제로는 더 복잡한 알고리즘이 필요하지만, 여기서는 기본적인 구현만 제공
            
            // 사용자와 비슷한 관심사를 가진 사용자들이 읽은 뉴스 조회
            List<String> userInterests = userProfile.getPreferredCategories();
            if (userInterests.isEmpty()) {
                return Collections.emptyList();
            }
            
            // 관심사 기반 뉴스 조회
            List<Category> categories = userInterests.stream()
                .map(this::stringToCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            if (categories.isEmpty()) {
                return Collections.emptyList();
            }
            
            return newsRepository.findPersonalizedNewsByCategories(
                categories, Pageable.ofSize(10));
                
        } catch (Exception e) {
            log.error("협업 필터링 추천 실패: userId={}", userId, e);
            return Collections.emptyList();
        }
    }
}
