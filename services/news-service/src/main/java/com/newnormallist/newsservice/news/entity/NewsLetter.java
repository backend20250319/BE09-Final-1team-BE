package com.newnormallist.newsservice.news.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "news_letter")
public class NewsLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "newsletter_id")
    private Long id;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "user_id")
    private Long userId;

    @OneToMany(mappedBy = "newsletter")
    private List<NewsletterNews> newsletterNewsList;
}