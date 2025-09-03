package com.newnormallist.newsservice.news.dto;

import com.newnormallist.newsservice.news.entity.News;
import com.newnormallist.newsservice.news.entity.NewsScrap;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ScrappedNewsResponse {
    private Long newsId;
    private String title;
    private String press;
    private String imageUrl;
    private String categoryName; // 추가
    private String publishedAt; // 추가
    private LocalDateTime scrappedAt;

    public static ScrappedNewsResponse from(NewsScrap newsScrap) {
        News news = newsScrap.getNews();
        if (news == null) {
            // 혹시 모를 null 상황에 대비
            return ScrappedNewsResponse.builder()
                    .newsId(0L)
                    .title("[삭제된 뉴스]")
                    .press("-")
                    .categoryName("기타")
                    .publishedAt(null)
                    .scrappedAt(newsScrap.getCreatedAt())
                    .build();
        }

        return ScrappedNewsResponse.builder()
                .newsId(news.getNewsId())
                .title(news.getTitle())
                .press(news.getPress())
                .imageUrl(news.getImageUrl())
                .categoryName(news.getCategoryName() != null ? news.getCategoryName().getCategoryName() : "기타")
                .publishedAt(news.getPublishedAt()) // 기사 발행일 매핑
                .scrappedAt(newsScrap.getCreatedAt()) // 스크랩된 시각
                .build();
    }
}
