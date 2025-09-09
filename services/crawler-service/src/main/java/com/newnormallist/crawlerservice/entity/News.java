package com.newnormallist.crawlerservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.newnormallist.crawlerservice.enums.Category;
import com.newnormallist.crawlerservice.enums.DedupState;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "news")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_id")
    private Long id;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "press", length = 100)
    private String press;

    @Column(name = "published_at")
    private String publishedAt;

    @Column(name = "reporter", length = 100)
    private String reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "dedup_state", length = 20)
    private DedupState dedupState;

    @Column(name = "trusted", nullable = false)
    private Boolean trusted;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "oid_aid", length = 100, unique = true)
    private String oidAid;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_name", nullable = false)
    private Category category;

    @Column(name = "link", length = 500)
    private String link;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime updatedAt;

    @Column(name = "view_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer viewCount = 0;

    // 날짜를 원하는 형식으로 변환하는 메서드
    public String getCreatedAtFormatted() {
        if (createdAt != null) {
            return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
        }
        return null;
    }

    public String getUpdatedAtFormatted() {
        if (updatedAt != null) {
            return updatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
        }
        return null;
    }
}
