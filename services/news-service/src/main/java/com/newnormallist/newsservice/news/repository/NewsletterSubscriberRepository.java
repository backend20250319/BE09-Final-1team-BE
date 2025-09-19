package com.newnormallist.newsservice.news.repository;

import com.newnormallist.newsservice.news.entity.NewsletterSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, Long> {

    boolean existsByEmail(String email);
    
    Optional<NewsletterSubscriber> findByToken(String token);
    
    @Query("SELECT COUNT(n) FROM NewsletterSubscriber n WHERE n.confirmed = true")
    long countConfirmedSubscribers();
}
