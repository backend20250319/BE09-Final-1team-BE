package com.newnormallist.newsservice.news.service;

import com.newnormallist.newsservice.news.dto.RelatedNewsResponseDto;
import com.newnormallist.newsservice.news.entity.News;
import com.newnormallist.newsservice.news.exception.NewsNotFoundException;
import com.newnormallist.newsservice.news.repository.NewsRepository;
import com.newnormallist.newsservice.news.repository.RelatedNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RelatedNewsServiceImpl implements RelatedNewsService {

    private final NewsRepository newsRepository;
    private final RelatedNewsRepository relatedNewsRepository;
    private static final int MAX_RELATED_NEWS = 4;
    // DB의 날짜 형식(yyyy-MM-dd HH:mm:ss)과 일치하는 포매터
    private static final DateTimeFormatter DB_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<RelatedNewsResponseDto> getRelatedNews(Long newsId) {
        log.info("=== 연관 뉴스 조회 시작: newsId = {} ===", newsId);

        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsNotFoundException("뉴스를 찾을 수 없습니다: " + newsId));

        log.info("뉴스 정보: newsId={}, oidAid={}, dedupState={}, title={}",
                news.getNewsId(), news.getOidAid(), news.getDedupState(), news.getTitle());

        List<News> relatedNewsList = switch (news.getDedupState()) {
            case REPRESENTATIVE -> {
                log.info("REPRESENTATIVE 뉴스 처리 시작");
                yield getRelatedNewsForRepresentative(news);
            }
            case RELATED -> {
                log.info("RELATED 뉴스 처리 시작");
                yield getRelatedNewsForRelated(news);
            }
            default -> {
                log.info("KEPT 또는 알 수 없는 상태의 뉴스 처리 시작");
                yield getRelatedNewsForKept(news);
            }
        };

        log.info("=== 연관 뉴스 조회 완료: newsId={}, 결과 개수={} ===", newsId, relatedNewsList.size());

        return relatedNewsList.stream()
                .map(RelatedNewsResponseDto::from)
                .toList();
    }

    /**
     * REPRESENTATIVE 상태의 뉴스에 대한 연관뉴스 조회
     */
    private List<News> getRelatedNewsForRepresentative(News news) {
        log.info("REPRESENTATIVE 처리: oidAid = {}", news.getOidAid());

        // 대표 기사와 동일한 oidAid를 가진 모든 기사(클러스터)를 조회
        List<News> clusterNews = new ArrayList<>(newsRepository.findByOidAid(news.getOidAid()));

        // related_news 테이블에서 연관된 oidAid 목록을 조회
        List<String> relatedOidAids = relatedNewsRepository.findRelatedOidAidsByRepOidAid(news.getOidAid());
        log.info("related_news 테이블에서 조회한 related_oid_aids: {}", relatedOidAids);

        if (!relatedOidAids.isEmpty()) {
            clusterNews.addAll(newsRepository.findByOidAidIn(relatedOidAids));
        }

        // 조회된 뉴스 목록에서 중복을 제거하고, 자기 자신을 제외
        List<News> relatedNewsList = clusterNews.stream()
                .distinct()
                .filter(n -> !n.getNewsId().equals(news.getNewsId()))
                .collect(Collectors.toCollection(ArrayList::new));

        log.info("클러스터에서 찾은 연관 뉴스 개수: {}", relatedNewsList.size());

        // 부족한 뉴스는 Fallback 로직으로 채웁니다.
        if (relatedNewsList.size() < MAX_RELATED_NEWS) {
            findFallbackNews(relatedNewsList, news);
        }

        // 결과를 반환하기 전에 목록을 무작위로 섞습니다.
        Collections.shuffle(relatedNewsList);

        // 최종적으로 중복을 한번 더 제거하고 개수를 제한하여 반환
        return relatedNewsList.stream().distinct().limit(MAX_RELATED_NEWS).toList();
    }

    /**
     * RELATED 상태의 뉴스에 대한 연관뉴스 조회
     */
    private List<News> getRelatedNewsForRelated(News news) {
        log.info("RELATED 처리: oidAid = {}", news.getOidAid());
        List<News> relatedNewsList = new ArrayList<>();

        // 현재 뉴스의 대표 oidAid를 찾음
        String repOidAid = relatedNewsRepository.findRepOidAidByRelatedOidAid(news.getOidAid());
        log.info("RELATED: 찾은 rep_oid_aid = {}", repOidAid);

        if (repOidAid != null) {
            // 대표 oidAid로 클러스터의 모든 뉴스를 조회
            List<News> clusterNews = new ArrayList<>(newsRepository.findByOidAid(repOidAid));

            List<String> allRelatedOidAids = relatedNewsRepository.findRelatedOidAidsByRepOidAid(repOidAid);
            log.info("RELATED: rep_oid_aid로 찾은 related_oid_aids: {}", allRelatedOidAids);

            if (!allRelatedOidAids.isEmpty()) {
                clusterNews.addAll(newsRepository.findByOidAidIn(allRelatedOidAids));
            }

            // 조회된 뉴스 목록에서 중복을 제거하고, 자기 자신을 제외
            relatedNewsList = clusterNews.stream()
                    .distinct()
                    .filter(n -> !n.getNewsId().equals(news.getNewsId()))
                    .collect(Collectors.toCollection(ArrayList::new));

            log.info("클러스터에서 찾은 전체 연관 뉴스 개수: {}", relatedNewsList.size());

        } else {
            log.info("RELATED: 해당 oid_aid가 related_oid_aid로 등록되지 않음");
        }

        // 부족한 뉴스는 Fallback 로직으로 채웁니다.
        if (relatedNewsList.size() < MAX_RELATED_NEWS) {
            findFallbackNews(relatedNewsList, news);
        }

        // 결과를 반환하기 전에 목록을 무작위로 섞습니다.
        Collections.shuffle(relatedNewsList);

        // 최종적으로 중복을 한번 더 제거하고 개수를 제한하여 반환
        return relatedNewsList.stream().distinct().limit(MAX_RELATED_NEWS).toList();
    }

    /**
     * KEPT 상태의 뉴스에 대한 연관뉴스 조회 (Fallback 로직만 사용)
     */
    private List<News> getRelatedNewsForKept(News news) {
        log.info("KEPT 처리: oidAid = {}", news.getOidAid());
        List<News> relatedNewsList = new ArrayList<>();

        // KEPT 상태는 명시적 연관 관계가 없으므로, 바로 Fallback 로직으로 유사 뉴스를 찾습니다.
        findFallbackNews(relatedNewsList, news);

        // 결과를 반환하기 전에 목록을 무작위로 섞습니다.
        Collections.shuffle(relatedNewsList);

        return relatedNewsList.stream().distinct().limit(MAX_RELATED_NEWS).toList();
    }

    /**
     * 관련 뉴스를 찾지 못했을 경우, 다른 방법으로 뉴스를 채우는 Fallback 로직
     *
     * @param newsList      현재까지 찾은 뉴스 리스트 (이 리스트에 뉴스가 추가됨)
     * @param referenceNews 기준이 되는 원본 뉴스
     */
    private void findFallbackNews(List<News> newsList, News referenceNews) {
        if (newsList.size() < MAX_RELATED_NEWS) {
            log.info("Fallback: 부족한 뉴스를 다른 방법으로 채움. 현재: {}, 필요: {}",
                    newsList.size(), MAX_RELATED_NEWS - newsList.size());
        }

        // 1. 같은 시간대, 같은 카테고리 뉴스로 채우기
        if (newsList.size() < MAX_RELATED_NEWS) {
            int needed = MAX_RELATED_NEWS - newsList.size();
            newsList.addAll(getNewsBySameTimeAndCategory(referenceNews, newsList, needed));
        }

        // 2. 최근 3일간 같은 카테고리 뉴스로 채우기
        if (newsList.size() < MAX_RELATED_NEWS) {
            int needed = MAX_RELATED_NEWS - newsList.size();
            newsList.addAll(getRecentNewsByCategory(referenceNews, newsList, needed));
        }

        // 3. 날짜 상관없이 같은 카테고리 뉴스로 채우기
        if (newsList.size() < MAX_RELATED_NEWS) {
            int needed = MAX_RELATED_NEWS - newsList.size();
            newsList.addAll(getAnyNewsByCategory(referenceNews, newsList, needed));
        }
    }

    /**
     * 같은 시간대와 카테고리의 뉴스를 조회하여 추가
     */
    private List<News> getNewsBySameTimeAndCategory(News news, List<News> excludeNews, int count) {
        List<Long> excludeNewsIds = new ArrayList<>(excludeNews.stream().map(News::getNewsId).toList());
        excludeNewsIds.add(news.getNewsId());

        try {
            LocalDateTime newsDateTime = parsePublishedAt(news.getPublishedAt());
            LocalDateTime startOfDay = newsDateTime.toLocalDate().atStartOfDay(); // yyyy-MM-dd 00:00:00
            LocalDateTime endOfDay = newsDateTime.toLocalDate().atTime(LocalTime.MAX); // yyyy-MM-dd 23:59:59.999...

            List<News> sameDayCategoryNews = newsRepository.findByCategoryNameAndPublishedAtBetweenAndNewsIdNotIn(
                    news.getCategoryName(),
                    startOfDay.format(DB_DATETIME_FORMATTER),
                    endOfDay.format(DB_DATETIME_FORMATTER),
                    excludeNewsIds);

            // 오전/오후 시간대 필터링
            boolean isMorning = newsDateTime.toLocalTime().isBefore(LocalTime.NOON);

            List<News> filteredNews = new ArrayList<>(sameDayCategoryNews.stream()
                    .filter(n -> {
                        try {
                            LocalTime time = parsePublishedAt(n.getPublishedAt()).toLocalTime();
                            // 기준 뉴스가 오전이면 오전 뉴스만, 오후면 오후 뉴스만 필터링
                            return isMorning == time.isBefore(LocalTime.NOON);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .toList());

            Collections.shuffle(filteredNews);
            return filteredNews.stream().limit(count).toList();

        } catch (Exception e) {
            log.warn("날짜 파싱 실패로 인해 '같은 시간대 뉴스' 조회를 건너뜁니다: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 최근 3일간 같은 카테고리의 뉴스를 조회하여 추가
     */
    private List<News> getRecentNewsByCategory(News news, List<News> excludeNews, int count) {
        List<Long> excludeNewsIds = new ArrayList<>(excludeNews.stream().map(News::getNewsId).toList());
        excludeNewsIds.add(news.getNewsId());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysAgo = now.minusDays(3);

        List<News> recentCategoryNews = newsRepository.findByCategoryNameAndPublishedAtBetweenAndNewsIdNotIn(
                news.getCategoryName(),
                threeDaysAgo.format(DB_DATETIME_FORMATTER),
                now.format(DB_DATETIME_FORMATTER),
                excludeNewsIds);

        Collections.shuffle(recentCategoryNews);
        return recentCategoryNews.stream().limit(count).toList();
    }

    /**
     * 같은 카테고리의 뉴스를 조회하여 추가 (날짜 무관, 최종 보루)
     */
    private List<News> getAnyNewsByCategory(News news, List<News> excludeNews, int count) {
        log.debug("최종 보루 로직 실행: 같은 카테고리 뉴스 조회 (날짜 무관)");
        List<Long> excludeNewsIds = new ArrayList<>(excludeNews.stream().map(News::getNewsId).toList());
        excludeNewsIds.add(news.getNewsId());

        // Pageable을 사용하여 조회할 개수를 제한. 제외할 ID를 고려하여 넉넉하게 가져옴.
        Pageable pageable = PageRequest.of(0, count + 20); // 필터링 후 개수를 보장하기 위해 조금 더 조회
        Page<News> newsPage = newsRepository.findByCategoryNameAndNewsIdNotIn(news.getCategoryName(), excludeNewsIds, pageable);

        List<News> availableNews = new ArrayList<>(newsPage.getContent());
        Collections.shuffle(availableNews);

        // 최종적으로 필요한 개수만큼만 잘라서 반환
        return availableNews.stream().limit(count).toList();
    }

    /**
     * 안전한 날짜 파싱 메서드
     */
    private LocalDateTime parsePublishedAt(String publishedAt) {
        if (publishedAt == null || publishedAt.trim().isEmpty()) {
            throw new IllegalArgumentException("날짜 문자열(publishedAt)이 null이거나 비어있습니다.");
        }
        // MySQL DATETIME 형식에서 밀리초(.) 이후 부분을 제거
        String parsableDateTime = publishedAt;
        int dotIndex = publishedAt.indexOf('.');
        if (dotIndex != -1) {
            parsableDateTime = publishedAt.substring(0, dotIndex);
        }
        return LocalDateTime.parse(parsableDateTime, DB_DATETIME_FORMATTER);
    }
}