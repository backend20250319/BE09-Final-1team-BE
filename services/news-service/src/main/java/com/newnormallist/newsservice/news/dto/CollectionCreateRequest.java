package com.newnormallist.newsservice.news.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CollectionCreateRequest {

    @NotBlank(message = "컬렉션 이름은 필수입니다.")
    private String storageName;
}
