package com.newnormallist.newsservice.recommendation.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 피드 응답 래퍼 - content 배열로 뉴스 목록을 감싸는 형태

@Getter 
@Setter 
@NoArgsConstructor
@AllArgsConstructor 
@Builder
public class FeedResponseDto {
    private List<FeedItemDto> content;
}
