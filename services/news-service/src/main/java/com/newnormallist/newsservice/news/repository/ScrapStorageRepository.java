package com.newnormallist.newsservice.news.repository;

import com.newnormallist.newsservice.news.entity.ScrapStorage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScrapStorageRepository extends JpaRepository<ScrapStorage, Integer> {
    List<ScrapStorage> findByUserId(Long userId);
}
