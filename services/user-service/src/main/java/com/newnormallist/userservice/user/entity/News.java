package com.newnormallist.userservice.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "news")
@Getter
@NoArgsConstructor
public class News {
    
    @Id
    @Column(name = "news_id")
    private Long newsId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category_name")
    private NewsCategory categoryName;
    
    @Column(name = "title")
    private String title;
}
