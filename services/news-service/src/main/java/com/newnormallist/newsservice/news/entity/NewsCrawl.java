package com.newnormallist.newsservice.news.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "news_crawl")
public class NewsCrawl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "raw_id")
    private Long rawId;

    @Column(name = "link_id")
    private Long linkId;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String press;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reporter_name")
    private String reporterName;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category category;
    
    // 기본 생성자
    public NewsCrawl() {}
    
    // 전체 생성자
    public NewsCrawl(Long rawId, Long linkId, String title, String press, String content,
                    LocalDateTime createdAt, String reporterName, LocalDateTime publishedAt, Category category) {
        this.rawId = rawId;
        this.linkId = linkId;
        this.title = title;
        this.press = press;
        this.content = content;
        this.createdAt = createdAt;
        this.reporterName = reporterName;
        this.publishedAt = publishedAt;
        this.category = category;
    }
    
    // Builder 패턴
    public static NewsCrawlBuilder builder() {
        return new NewsCrawlBuilder();
    }
    
    // Getter/Setter 메서드들
    public Long getRawId() { return rawId; }
    public void setRawId(Long rawId) { this.rawId = rawId; }
    
    public Long getLinkId() { return linkId; }
    public void setLinkId(Long linkId) { this.linkId = linkId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getPress() { return press; }
    public void setPress(String press) { this.press = press; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    // Builder 클래스
    public static class NewsCrawlBuilder {
        private Long rawId;
        private Long linkId;
        private String title;
        private String press;
        private String content;
        private LocalDateTime createdAt;
        private String reporterName;
        private LocalDateTime publishedAt;
        private Category category;
        
        public NewsCrawlBuilder rawId(Long rawId) {
            this.rawId = rawId;
            return this;
        }
        
        public NewsCrawlBuilder linkId(Long linkId) {
            this.linkId = linkId;
            return this;
        }
        
        public NewsCrawlBuilder title(String title) {
            this.title = title;
            return this;
        }
        
        public NewsCrawlBuilder press(String press) {
            this.press = press;
            return this;
        }
        
        public NewsCrawlBuilder content(String content) {
            this.content = content;
            return this;
        }
        
        public NewsCrawlBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public NewsCrawlBuilder reporterName(String reporterName) {
            this.reporterName = reporterName;
            return this;
        }
        
        public NewsCrawlBuilder publishedAt(LocalDateTime publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }
        
        public NewsCrawlBuilder category(Category category) {
            this.category = category;
            return this;
        }
        
        public NewsCrawl build() {
            return new NewsCrawl(rawId, linkId, title, press, content, createdAt, reporterName, publishedAt, category);
        }
    }
}