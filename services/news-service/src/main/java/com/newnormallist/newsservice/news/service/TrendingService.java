package com.newnormallist.newsservice.news.service;

import com.newnormallist.newsservice.news.dto.TrendingKeywordDto;
import com.newnormallist.newsservice.news.entity.News;
import com.newnormallist.newsservice.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrendingService {

    private final NewsRepository newsRepository;

    /**
     * 최근 hours 시간 동안의 기사 제목/요약에서 키워드 토큰을 추출해 상위 limit개 집계
     */
    public List<TrendingKeywordDto> getTrendingKeywords(int hours, int limit) {
        int safeHours = Math.max(1, hours);
        int safeLimit = Math.max(1, limit);

        LocalDateTime since = LocalDateTime.now().minusHours(safeHours);
        List<News> recent = newsRepository.findByPublishedAtAfter(since);

        // 제목 + 요약(가능하면)에서 키워드 추출   
        Map<String, Long> counts = recent.stream()
                .flatMap(n -> tokenizeKo(joinTitleSummary(n)).stream())
                .filter(tok -> !STOPWORDS.contains(tok))
                .filter(tok -> tok.length() >= 2) // 1글자 제거
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(safeLimit)
                .map(e -> TrendingKeywordDto.builder()
                        .keyword(e.getKey())
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());

    }

    private String joinTitleSummary(News n) {
        String t = Optional.ofNullable(n.getTitle()).orElse("");

        return (t + " ").trim();
    }

    /**
     * 아주 단순한 한국어/영문 토크나이저 (MVP).
     * 향후 형태소 분석기/키워드 컬럼/검색 로그로 교체 권장.
     */
    private List<String> tokenizeKo(String text) {
        if (text == null || text.isBlank()) return List.of();
        String cleaned = text
                .replaceAll("[^가-힣0-9A-Za-z\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.isEmpty()) return List.of();
        return Arrays.asList(cleaned.split(" "));
    }

    // 너무 일반적인 단어는 제외 (필요에 따라 계속 확장)
    private static final Set<String> STOPWORDS = Set.of(
            "속보","영상","단독","인터뷰","기자","사진","종합","오늘","내일",
            "정부","대통령","국회","한국","대한민국","뉴스","기사","외신",
            "관련","이번","지난","현재","최대","최소","전망","분석","현장"
    );
}
