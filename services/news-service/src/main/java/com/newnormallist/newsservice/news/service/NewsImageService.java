package com.newnormallist.newsservice.news.service;
import com.newnormallist.newsservice.news.dto.TrendingKeywordDto;
import com.newnormallist.newsservice.news.entity.News;
import com.newnormallist.newsservice.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsImageService {
        private final NewsRepository newsRepository;
        private final TrendingService trendingService;

        public String getPersonalizedSectionImage() {
            return newsRepository.findTop1ByImageUrlIsNotNullOrderByPublishedAtDesc()
                    .map(News::getImageUrl)
                    .orElse(getDefaultPersonalizedImage());
        }

        public String getTrendingSectionImage() {
            // 트렌딩 키워드 가져오기
            List<TrendingKeywordDto> trending = trendingService.getTrendingKeywords(24, 1);

            if (!trending.isEmpty()) {
                String keyword = trending.get(0).getKeyword();
                return newsRepository.findByTitleContainingAndImageUrlIsNotNull(keyword)
                        .stream()
                        .findFirst()
                        .map(News::getImageUrl)
                        .orElse(getDefaultTrendingImage());
            }

            return getDefaultTrendingImage();
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