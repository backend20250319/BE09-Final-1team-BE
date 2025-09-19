package com.newnormallist.userservice.history.entity;

import com.newnormallist.userservice.user.entity.NewsCategory;
import com.newnormallist.userservice.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_read_history",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "news_id"}))
public class UserReadHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "news_id", nullable = false)
    private Long newsId;

    @Column(name = "news_title", nullable = false)
    private String newsTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_name")
    private NewsCategory categoryName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public UserReadHistory(User user, Long newsId, String newsTitle, NewsCategory categoryName) {
        this.user = user;
        this.newsId = newsId;
        this.newsTitle = newsTitle;
        this.categoryName = categoryName;
    }

    public void updateReadTime() {
        this.updatedAt = LocalDateTime.now();
    }


}
