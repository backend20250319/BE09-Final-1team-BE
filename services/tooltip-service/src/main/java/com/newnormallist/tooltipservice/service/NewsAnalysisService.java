package com.newnormallist.tooltipservice.service;

import com.newnormallist.tooltipservice.dto.ProcessContentRequest;
import com.newnormallist.tooltipservice.dto.ProcessContentResponse;
import com.newnormallist.tooltipservice.dto.TermDetailResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsAnalysisService {

    private final AnalysisCacheService analysisCacheService;
    private final NlpService nlpService;

    // Redis 캐시로 변경됨 - 메모리 캐시 제거

    /**
     * 뉴스 본문을 분석하여 어려운 단어에 마크업을 추가합니다.
     */
    @Cacheable(value = "processedContent", key = "#request.newsId()")
    public ProcessContentResponse processContent(ProcessContentRequest request) {
        log.info("뉴스 ID {}의 본문 분석을 시작합니다.", request.newsId());

        // Redis 캐시에서 어려운 단어 목록을 가져와서 마크업 처리
        log.info("🟡 어려운 단어 목록 조회를 시작합니다...");
        Set<String> difficultWords = analysisCacheService.getDifficultWordsFromCache();
        log.info("🟢 어려운 단어 목록 조회 완료! 총 {}개 (Redis에서 가져왔다면 위 🔴 로그가 없을 것입니다)", difficultWords.size());
        
        // NlpService를 직접 호출하여 마크업 처리
        String analyzedContent = nlpService.markupDifficultWords(request.originalContent(), difficultWords);

        return new ProcessContentResponse(analyzedContent);
    }

    // --- Delegations for controller compatibility ---
    public TermDetailResponseDto getTermDefinitions(String term) {
        return analysisCacheService.getTermDefinitions(term);
    }

    public void refreshDifficultWordsCache() {
        analysisCacheService.refreshDifficultWordsCache();
    }

    /**
     * Redis 캐시에서 어려운 단어 목록을 조회합니다.
     * 캐시 미스 시 DB에서 로드하여 캐시에 저장합니다.
     */
    // termDetails 및 캐시 갱신은 AnalysisCacheService로 이동
}