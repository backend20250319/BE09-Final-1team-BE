package com.newnormallist.crawlerservice.repository;

import com.newnormallist.crawlerservice.entity.RelatedNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RelatedNewsRepository extends JpaRepository<RelatedNews, RelatedNews.RelatedNewsId> {
    
    boolean existsByRepOidAidAndRelatedOidAid(String repOidAid, String relatedOidAid);
}
