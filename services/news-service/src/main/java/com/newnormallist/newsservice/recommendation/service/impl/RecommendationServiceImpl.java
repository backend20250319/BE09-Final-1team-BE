package com.newnormallist.newsservice.recommendation.service.impl;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.newnormallist.newsservice.recommendation.service.RecommendationService;
import com.newnormallist.newsservice.recommendation.service.VectorBatchService;
import com.newnormallist.newsservice.recommendation.entity.*;
import com.newnormallist.newsservice.recommendation.dto.FeedItemDto;
import com.newnormallist.newsservice.recommendation.repository.*;
import com.newnormallist.newsservice.recommendation.mapper.FeedMapper;
import com.newnormallist.newsservice.recommendation.config.RecommendationProperties;


import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;

// 피드 조립 서비스 구현체.
// UserPrefVectorRepository.findTop3ByUserId(userId)로 top3 카테고리 확보
// 각 카테고리에서 최신 7/5/3 ID 수집
// findByIds로 뉴스 메타 일괄 조회
// DTO로 매핑해 반환
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final VectorBatchService vectorBatchService;
    private final UserPrefVectorRepository userPrefVectorRepository;
    private final RecommendationNewsRepository newsRepository;
    private final RecommendationProperties properties;

    @Override
    public List<FeedItemDto> getFeed(Long id) {
        return getFeed(id, 0, 20); // 기본값으로 첫 페이지
    }
    
    @Override
    public List<FeedItemDto> getFeed(Long id, int page, int size) {
        
        // 첫 페이지 (page=0): 개인화 추천
        if (page == 0) {
            return getPersonalizedFeed(id);
        }
        
        // 두 번째 페이지부터: 전체 뉴스 최신순
        return getLatestNewsFeed(page, size);
    }
    
    private List<FeedItemDto> getPersonalizedFeed(Long id) {
        // 1. 사용자 벡터 업데이트 (필요시)
        vectorBatchService.upsert(id);
        
        // 2. 상위 3개 카테고리 조회
        List<UserPrefVector> top3Vectors = userPrefVectorRepository
            .findTopByUserIdOrderByScoreDesc(id, PageRequest.of(0, 3));
        
        if (top3Vectors.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 3. 각 카테고리별 최신 뉴스 ID 수집
        List<Long> newsIds = new ArrayList<>();
        List<Integer> quotas = properties.getQuotas();
        
        for (int i = 0; i < top3Vectors.size() && i < quotas.size(); i++) {
            RecommendationCategory category = top3Vectors.get(i).getCategory();
            int quota = quotas.get(i);
            
            List<Long> categoryNewsIds = newsRepository.findLatestIdsByCategory(
                category, PageRequest.of(0, quota)
            );
            newsIds.addAll(categoryNewsIds);
        }
        
        if (newsIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 4. 뉴스 메타 정보 일괄 조회
        List<NewsEntity> newsList = newsRepository.findByIdIn(newsIds);
        
        // 5. ID 순서대로 정렬 (원래 요청 순서 유지)
        Map<Long, NewsEntity> newsMap = newsList.stream()
            .collect(Collectors.toMap(NewsEntity::getNewsId, news -> news));
        
        List<FeedItemDto> feedItems = newsIds.stream()
            .map(newsId -> newsMap.get(newsId))
            .filter(Objects::nonNull)
            .map(FeedMapper::toDto)
            .collect(Collectors.toList());
        
        return feedItems;
    }
    
    private List<FeedItemDto> getLatestNewsFeed(int page, int size) {
        // 1. 전체 뉴스를 최신순으로 조회 (published_at DESC)
        Page<NewsEntity> newsPage = newsRepository.findAllByOrderByPublishedAtDesc(PageRequest.of(page, size));
        
        // 2. 카테고리별로 그룹화
        Map<RecommendationCategory, List<NewsEntity>> categoryGroups = newsPage.getContent().stream()
            .collect(Collectors.groupingBy(NewsEntity::getCategoryName));
        
        // 3. 카테고리를 랜덤하게 섞기
        List<RecommendationCategory> shuffledCategories = new ArrayList<>(categoryGroups.keySet());
        Collections.shuffle(shuffledCategories);
        
        // 4. 카테고리 순서대로 뉴스들을 번갈아가며 배치
        List<FeedItemDto> result = new ArrayList<>();
        int maxSize = categoryGroups.values().stream()
            .mapToInt(List::size)
            .max()
            .orElse(0);
        
        for (int i = 0; i < maxSize; i++) {
            for (RecommendationCategory category : shuffledCategories) {
                List<NewsEntity> categoryNews = categoryGroups.get(category);
                if (i < categoryNews.size()) {
                    result.add(FeedMapper.toDto(categoryNews.get(i)));
                }
            }
        }
        
        return result;
    }
}
