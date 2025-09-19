package com.newnormallist.newsservice.news.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddNewsToCollectionRequest {

    @NotNull(message = "뉴스 ID는 필수입니다.")
    private Long newsId;
}
