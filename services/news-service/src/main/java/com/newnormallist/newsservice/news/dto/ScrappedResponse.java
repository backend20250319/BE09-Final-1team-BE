package com.newnormallist.newsservice.news.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ScrappedResponse {
    private Long newsId;
    private String title;
    private String press;
    private String reporterName;
    private LocalDateTime createdAt;
    private String imageUrl;
}
