package com.newnormallist.newsservice.recommendation.dto;

import com.newnormallist.newsservice.recommendation.entity.AgeBucket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 유저 메타 (연령, 성별)를 Api로 입출력할 때 사용하는 DTO
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class UserMetaDto {
    private Long userId;
    private AgeBucket ageBucket;
    private String gender;
}