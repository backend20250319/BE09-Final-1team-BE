package com.newnormallist.newsservice.news.service;

import com.newnormallist.newsservice.news.dto.NewsListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 마이페이지 관련 비즈니스 로직을 정의하는 인터페이스
 */
public interface MyPageService {

    /**
     * 특정 사용자가 스크랩한 뉴스 목록을 조회합니다.
     *
     * @param userId   사용자의 ID
     * @param category
     * @param query    검색어
     * @param uncollectedOnly
     * @param pageable 페이징 정보
     * @return 페이징 처리된 스크랩 뉴스 목록
     */
    Page<NewsListResponse> getScrappedNews(Long userId, String category, String query, boolean uncollectedOnly, Pageable pageable);

    /**
     * 특정 뉴스의 스크랩을 삭제합니다.
     * @param userId 사용자의 ID
     * @param newsId 삭제할 뉴스의 ID
     */
    void deleteScrap(Long userId, Long newsId);
}
