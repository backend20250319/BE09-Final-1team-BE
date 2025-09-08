package com.newnormallist.newsservice.news.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "news")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_id")
    private Long newsId;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_name", nullable = false)
    private Category categoryName;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "press", nullable = false, columnDefinition = "TEXT")
    private String press;

    @Column(name = "published_at", length = 100)
    private String publishedAt;

    @Column(name = "reporter", nullable = false, columnDefinition = "TEXT")
    private String reporter;

    @CreationTimestamp
    @Column(name = "created_at", nullable = true)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "dedup_state", nullable = false)
    private DedupState dedupState;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "trusted", columnDefinition="bit(1)", nullable = true)
    private Boolean trusted;

    @Column(name = "oid_aid", length=255, unique=true)
    private String oidAid;

    @Column(name = "link", nullable = false, columnDefinition = "TEXT")
    private String link;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    // 뉴스레터와의 N:N 연결
    @OneToMany(mappedBy = "news")
    private List<NewsletterNews> newsletterNewsList;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private NewsStatus status = NewsStatus.PUBLISHED;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        News news = (News) o;
        return Objects.equals(newsId, news.newsId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(newsId);
    }
}