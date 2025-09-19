package com.newnormallist.newsservice.news.repository;

import com.newnormallist.newsservice.news.entity.KeywordSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KeywordSubscriptionRepository extends JpaRepository<KeywordSubscription, Long> {
    
    List<KeywordSubscription> findByUserIdAndIsActiveTrue(Long userId);
    
    Optional<KeywordSubscription> findByUserIdAndKeywordAndIsActiveTrue(Long userId, String keyword);
    
    boolean existsByUserIdAndKeywordAndIsActiveTrue(Long userId, String keyword);
    
    @Query("SELECT k.keyword, COUNT(k) as count FROM KeywordSubscription k " +
           "WHERE k.isActive = true " +
           "GROUP BY k.keyword " +
           "ORDER BY count DESC")
    List<Object[]> findPopularKeywords();
}
