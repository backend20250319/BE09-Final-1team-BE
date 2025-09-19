package com.newnormallist.newsservice.news.repository;

import com.newnormallist.newsservice.news.entity.RelatedNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RelatedNewsRepository extends JpaRepository<RelatedNews, RelatedNews.RelatedNewsId> {

    // rep_oid_aid로 관련 뉴스들의 related_oid_aid 조회
    @Query("SELECT rn.id.relatedOidAid FROM RelatedNews rn WHERE rn.id.repOidAid = :repOidAid")
    List<String> findRelatedOidAidsByRepOidAid(@Param("repOidAid") String repOidAid);

    // related_oid_aid로 대표 뉴스의 rep_oid_aid 조회
    @Query("SELECT rn.id.repOidAid FROM RelatedNews rn WHERE rn.id.relatedOidAid = :relatedOidAid")
    String findRepOidAidByRelatedOidAid(@Param("relatedOidAid") String relatedOidAid);

    // rep_oid_aid로 관련 뉴스들 조회
    @Query("SELECT rn FROM RelatedNews rn WHERE rn.id.repOidAid = :repOidAid")
    List<RelatedNews> findByIdRepOidAid(@Param("repOidAid") String repOidAid);

    // related_oid_aid로 관련 뉴스들 조회
    @Query("SELECT rn FROM RelatedNews rn WHERE rn.id.relatedOidAid = :relatedOidAid")
    List<RelatedNews> findByIdRelatedOidAid(@Param("relatedOidAid") String relatedOidAid);
}
