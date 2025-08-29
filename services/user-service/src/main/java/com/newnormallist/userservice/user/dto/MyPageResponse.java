package com.newnormallist.userservice.user.dto;

import com.newnormallist.userservice.user.entity.NewsCategory;
import com.newnormallist.userservice.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
public class MyPageResponse {
    private final String email;
    private final String name;
    private final Boolean letterOk;
    private final Set<NewsCategory> hobbies;
    private final LocalDateTime createdAt;
    private final String provider;

    @Builder
    public MyPageResponse(User user) {
        this.email = user.getEmail();
        this.name = user.getName();
        this.letterOk = user.getLetterOk();
        this.hobbies = user.getHobbies();
        this.createdAt = user.getCreatedAt();
        this.provider = user.getProvider();
    }

}
