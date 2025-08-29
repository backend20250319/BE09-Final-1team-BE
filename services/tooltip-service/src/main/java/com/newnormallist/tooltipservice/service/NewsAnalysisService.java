package com.newnormallist.tooltipservice.service;

import com.newnormallist.tooltipservice.dto.ProcessContentRequest;
import com.newnormallist.tooltipservice.dto.ProcessContentResponse;
import com.newnormallist.tooltipservice.dto.TermDetailResponseDto;
import com.newnormallist.tooltipservice.dto.TermDefinitionResponseDto;
import com.newnormallist.tooltipservice.entity.VocabularyTerm;
import com.newnormallist.tooltipservice.repository.VocabularyTermRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsAnalysisService {

    private final VocabularyTermRepository vocabularyTermRepository;
    private final NlpService nlpService;

    // 어려운 단어 캐시 (애플리케이션 시작 시 로드)
    private final Set<String> difficultWordCache = new HashSet<>();

    /**
     * 뉴스 본문을 분석하여 어려운 단어에 마크업을 추가합니다.
     */
    public ProcessContentResponse processContent(ProcessContentRequest request) {
        log.info("뉴스 ID {}의 본문 분석을 시작합니다.", request.newsId());

        // 캐시된 어려운 단어 목록을 사용하여 마크업 처리
        if (difficultWordCache.isEmpty()) {
            loadDifficultWordsCache();
        }

        String analyzedContent = getAnalyzedContent(request.newsId(), request.originalContent());

        return new ProcessContentResponse(analyzedContent);
    }

    /**
     * 어려운 단어 캐시를 로드합니다.
     */
    private void loadDifficultWordsCache() {
        log.info("어려운 단어 캐시를 로드합니다...");
        List<VocabularyTerm> allTerms = vocabularyTermRepository.findAll();
        difficultWordCache.clear();
        allTerms.forEach(term -> difficultWordCache.add(term.getTerm()));
        log.info("총 {}개의 어려운 단어를 캐시에 로드했습니다.", difficultWordCache.size());
    }

    @Cacheable(value = "processedContent", key = "#newsId")
    public String getAnalyzedContent(Long newsId, String originalContent) {
        log.info("캐시 미스 발생! 뉴스 ID {}에 대한 NLP 분석을 시작합니다.", newsId);
        return nlpService.markupDifficultWords(originalContent, this.difficultWordCache);
    }

    // --- 반환 타입 수정됨 ---
    @Transactional(readOnly = true)
    @Cacheable(value = "termDetails", key = "#term.toLowerCase()")
    public TermDetailResponseDto getTermDefinitions(String term) {
        log.info("DB에서 '{}' 단어의 정의를 조회합니다.", term);
        
        // 먼저 정확 일치로 찾아보고, 없으면 부분 일치로 찾기
        VocabularyTerm vocabularyTerm = vocabularyTermRepository.findByTerm(term)
                .or(() -> vocabularyTermRepository.findByTermStartingWith(term))
                .orElseThrow(() -> new NoSuchElementException("단어를 찾을 수 없습니다: " + term));

        // 정의 목록을 displayOrder 순서대로 정렬하여 DTO로 변환
        List<TermDefinitionResponseDto> definitionDtos = vocabularyTerm.getDefinitions().stream()
                .sorted((def1, def2) -> {
                    // displayOrder가 null인 경우를 대비해 안전하게 정렬
                    Integer order1 = def1.getDisplayOrder() != null ? def1.getDisplayOrder() : Integer.MAX_VALUE;
                    Integer order2 = def2.getDisplayOrder() != null ? def2.getDisplayOrder() : Integer.MAX_VALUE;
                    return order1.compareTo(order2);
                })
                .map(def -> new TermDefinitionResponseDto(
                        def.getDefinition(),
                        def.getDisplayOrder()
                ))
                .collect(Collectors.toList());

        // DB에 저장된 단어 그대로 반환 (한자 포함)
        return new TermDetailResponseDto(vocabularyTerm.getTerm(), definitionDtos);
    }
}