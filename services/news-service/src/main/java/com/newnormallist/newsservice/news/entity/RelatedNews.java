package com.newnormallist.newsservice.news.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "related_news")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatedNews {

    @EmbeddedId
    private RelatedNewsId id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 복합키 클래스
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class RelatedNewsId implements Serializable {

        @Column(name = "rep_oid_aid", nullable = false)
        private String repOidAid;

        @Column(name = "related_oid_aid", nullable = false)
        private String relatedOidAid;
    }
}
