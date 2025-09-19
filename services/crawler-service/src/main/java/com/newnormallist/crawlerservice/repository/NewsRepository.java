package com.newnormallist.crawlerservice.repository;

import com.newnormallist.crawlerservice.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    
    Optional<News> findByOidAid(String oidAid);
    
    boolean existsByOidAid(String oidAid);
}
