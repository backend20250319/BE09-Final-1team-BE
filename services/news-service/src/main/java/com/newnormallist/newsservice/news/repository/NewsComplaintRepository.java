package com.newnormallist.newsservice.news.repository;

import com.newnormallist.newsservice.news.entity.NewsComplaint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsComplaintRepository extends JpaRepository<NewsComplaint, Long> {
    long countByNewsNewsId(Long newsId);
}