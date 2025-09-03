package com.newnormallist.userservice.user.dto;

import com.newnormallist.userservice.user.entity.NewsCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NewsInfo {
    private final String title;
    private final NewsCategory categoryName;
}
