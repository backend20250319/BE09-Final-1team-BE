package com.newnormallist.newsservice.news.dto;

import com.newnormallist.newsservice.news.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsCrawlDto {
    
    private Long linkId;
    private String title;
    private String press;
    private String content;
    private String reporterName;
    private LocalDateTime publishedAt;
    private Category category;

}