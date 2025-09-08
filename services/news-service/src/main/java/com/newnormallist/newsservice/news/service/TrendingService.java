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
                .filter(this::isValidKeyword)
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

    // 확장된 불용어 목록 - 의미없는 단어들을 체계적으로 필터링
    private static final Set<String> STOPWORDS = Set.of(
            // 뉴스 관련 일반 용어
            "속보", "영상", "단독", "인터뷰", "기자", "사진", "종합", "뉴스", "기사", "외신",
            "현장", "보도", "취재", "논평", "사설", "칼럼", "특집", "기획", "리포트",
            
            // 시간 관련
            "오늘", "내일", "어제", "이번", "지난", "현재", "최근", "곧", "이제",
            "년", "월", "일", "시", "분", "초", "주", "달", "년도",
            
            // 일반적인 조사/어미
            "것", "수", "등", "및", "또는", "그리고", "하지만", "그러나", "따라서",
            "있다", "없다", "하다", "되다", "이다", "아니다", "같다", "다르다",
            "위해", "통해", "대해", "관해", "대한", "관련", "위한", "통한",
            
            // 정부/기관 관련
            "정부", "대통령", "국회", "한국", "대한민국", "국가", "정부기관", "공공기관",
            "시청", "구청", "군청", "도청", "청", "부", "처", "원",
            
            // 일반적인 형용사/부사
            "최대", "최소", "매우", "정말", "진짜", "완전", "엄청", "너무", "아주",
            "많이", "조금", "약간", "좀", "더", "가장", "제일", "특히", "특별히",
            
            // 기타 의미없는 단어들
            "내용", "정보", "자료", "데이터", "결과", "상황", "문제", "이슈", "사건",
            "분석", "전망", "동향", "소식", "업데이트", "변화", "발전", "진전",
            "영향", "효과", "원인", "이유", "목적", "방법", "과정"
    );
    
    /**
     * 키워드 유효성 검사 - 체계적인 필터링
     */
    private boolean isValidKeyword(String word) {
        if (word == null || word.length() < 2) {
            return false;
        }
        
        // 1. 불용어 목록에 포함된 단어 제외
        if (STOPWORDS.contains(word)) {
            return false;
        }
        
        // 2. 숫자만으로 구성된 단어 제외 (연도, 날짜 등)
        if (word.matches("^\\d+$")) {
            return false;
        }
        
        // 3. 특수 패턴 제외
        if (word.matches(".*[#@$%^&*()].*")) {
            return false;
        }
        
        // 4. 너무 짧은 영문 단어 제외 (2글자 이하)
        if (word.matches("^[A-Za-z]{1,2}$")) {
            return false;
        }
        
        // 5. 반복 문자 패턴 제외 (예: "ㅋㅋㅋ", "ㅎㅎㅎ")
        if (word.matches("(.)\\1{2,}")) {
            return false;
        }
        
        // 6. 의미없는 조합어 제외
        if (isMeaninglessCombination(word)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 의미없는 조합어 판별
     */
    private boolean isMeaninglessCombination(String word) {
        // 의미없는 조합어 패턴들
        String[] meaninglessPatterns = {
            "영화의", "기사의", "뉴스의", "사진의", "영상의", "내용의", "정보의",
            "추출할", "분석할", "조사할", "확인할", "검토할", "검증할",
            "관련된", "대한", "위한", "통한", "통해", "대해", "관해",
            "있는", "없는", "같은", "다른", "이런", "그런", "저런",
            "하는", "되는", "이되는"
        };
        
        for (String pattern : meaninglessPatterns) {
            if (word.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
}
