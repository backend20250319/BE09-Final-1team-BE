package com.newnormallist.newsservice.summarizer.repository;

import com.newnormallist.newsservice.summarizer.entity.NewsSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsSummaryRepository extends JpaRepository<NewsSummaryEntity, Long> {

    // 캐시 조회: 키는 newsId + summaryType (스키마 유니크와 동일)
    Optional<NewsSummaryEntity> findTopByNewsIdAndSummaryTypeAndLinesOrderByIdDesc(Long newsId, String summaryType, Integer lines);

    // 존재 여부만 빠르게 확인하고 싶을 때(선택)
    boolean existsByNewsIdAndSummaryType(long newsId, String summaryType);
}