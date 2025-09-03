package com.newnormallist.newsservice.news.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CollectionUpdateRequest {

    @NotBlank(message = "새 컬렉션 이름은 비워둘 수 없습니다.")
    private String newName;
}
