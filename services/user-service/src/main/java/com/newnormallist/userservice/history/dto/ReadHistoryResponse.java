package com.newnormallist.userservice.history.dto;

import com.newnormallist.userservice.history.entity.UserReadHistory;
import com.newnormallist.userservice.user.entity.NewsCategory;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReadHistoryResponse {
    private final Long newsId;
    private final String newsTitle;
    private final LocalDateTime updatedAt;
    private final NewsCategory categoryName;

    public ReadHistoryResponse(UserReadHistory history) {
        this.newsId = history.getNewsId();
        this.newsTitle = history.getNewsTitle();
        this.updatedAt = history.getUpdatedAt();
        this.categoryName = history.getCategoryName();
    }
}
