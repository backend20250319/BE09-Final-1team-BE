package com.newnormallist.crawlerservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "related_news")
@EntityListeners(AuditingEntityListener.class)
@IdClass(RelatedNews.RelatedNewsId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatedNews {

    @Id
    @Column(name = "rep_oid_aid", nullable = false, length = 255)
    private String repOidAid;

    @Id
    @Column(name = "related_oid_aid", nullable = false, length = 255)
    private String relatedOidAid;

    @Column(name = "similarity")
    private Float similarity;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 복합키 구조에서는 단순 참조만 유지 (DDL의 FK 제약조건은 데이터베이스 레벨에서 처리)

    // 복합키 클래스
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedNewsId implements Serializable {
        private String repOidAid;
        private String relatedOidAid;
    }
}
