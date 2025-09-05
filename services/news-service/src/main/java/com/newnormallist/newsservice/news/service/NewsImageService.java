package com.newsletterservice.service;

import com.newnormallist.newsservice.news.dto.TrendingKeywordDto;
import com.newnormallist.newsservice.news.entity.News;
import com.newnormallist.newsservice.news.repository.NewsRepository;
import com.newnormallist.newsservice.news.service.TrendingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsImageService {
    private final NewsRepository newsRepository;
    private final TrendingService trendingService;

    public String getPersonalizedSectionImage() {
        // 개인화 뉴스 관련 이미지 또는 기본 이미지
        return newsRepository.findTop1ByImageUrlIsNotNullOrderByPublishedAtDesc()
                .map(News::getImageUrl)
                .orElse(getDefaultPersonalizedImage());
    }

    public String getTrendingNewsImage(String keyword) {
        return newsRepository.findByTitleContainingAndImageUrlIsNotNull(keyword)
                .stream()
                .findFirst()
                .map(News::getImageUrl)
                .orElse(getDefaultTrendingImage());
    }

    public String getLatestNewsImage() {
        return newsRepository.findTop1ByImageUrlIsNotNullOrderByPublishedAtDesc()
                .map(News::getImageUrl)
                .orElse(getDefaultPersonalizedImage());
    }

    private String getDefaultPersonalizedImage() {
        return "http://be09-final-1team-fe-env.eba-92qhhhzz.ap-northeast-2.elasticbeanstalk.com/images/personalized-default.jpg";
    }

    private String getDefaultTrendingImage() {
        return "http://be09-final-1team-fe-env.eba-92qhhhzz.ap-northeast-2.elasticbeanstalk.com/images/trending-default.jpg";
    }
}