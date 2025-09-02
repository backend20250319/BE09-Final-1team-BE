package com.newnormallist.tooltipservice.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.newnormallist.tooltipservice.dto.TermDefinitionResponseDto;
import com.newnormallist.tooltipservice.dto.TermDetailResponseDto;
import com.newnormallist.tooltipservice.entity.VocabularyTerm;
import com.newnormallist.tooltipservice.repository.VocabularyTermRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisCacheService {

    private final VocabularyTermRepository vocabularyTermRepository;
    private final CacheManager cacheManager;

    @Cacheable(value = "difficultWords", key = "'all'")
    public Set<String> getDifficultWordsFromCache() {
        log.info("🔴 REDIS 캐시 미스 발생! DB에서 어려운 단어 목록을 로드합니다...");
        log.info("🔴 CacheManager 타입: {}", cacheManager.getClass().getSimpleName());
        log.info("🔴 사용 가능한 캐시: {}", cacheManager.getCacheNames());

        List<VocabularyTerm> allTerms = vocabularyTermRepository.findAll();

        Set<String> difficultWords = allTerms.stream()
                .map(VocabularyTerm::getTerm)
                .peek(term -> log.debug("DB에서 로드된 어려운 단어: '{}'", term))
                .collect(java.util.stream.Collectors.toSet());

        log.info("🔴 총 {}개의 어려운 단어를 Spring이 자동으로 Redis에 저장할 예정입니다.", difficultWords.size());
        log.info("🔴 저장 위치: Redis key = 'difficultWords::all'");

        if (difficultWords.isEmpty()) {
            log.warn("⚠️ DB에 vocabulary_term 데이터가 없습니다!");
        }

        return difficultWords;
    }



    @Cacheable(value = "termDetails", key = "#term.toLowerCase()")
    public TermDetailResponseDto getTermDefinitions(String term) {
        log.info("DB에서 '{}' 단어의 정의를 조회합니다.", term);

        VocabularyTerm vocabularyTerm = vocabularyTermRepository.findByTerm(term)
                .or(() -> vocabularyTermRepository.findByTermStartingWith(term))
                .orElseThrow(() -> new NoSuchElementException("단어를 찾을 수 없습니다: " + term));

        List<TermDefinitionResponseDto> definitionDtos = vocabularyTerm.getDefinitions().stream()
                .sorted((def1, def2) -> {
                    Integer order1 = def1.getDisplayOrder() != null ? def1.getDisplayOrder() : Integer.MAX_VALUE;
                    Integer order2 = def2.getDisplayOrder() != null ? def2.getDisplayOrder() : Integer.MAX_VALUE;
                    return order1.compareTo(order2);
                })
                .map(def -> new TermDefinitionResponseDto(
                        def.getDefinition(),
                        def.getDisplayOrder()
                ))
                .collect(Collectors.toList());

        return new TermDetailResponseDto(vocabularyTerm.getTerm(), definitionDtos);
    }

    @CacheEvict(value = "difficultWords", key = "'all'")
    public void refreshDifficultWordsCache() {
        log.info("어려운 단어 캐시를 강제로 갱신합니다.");
    }
}


