package com.newnormallist.tooltipservice.service;

import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class NlpService {

    private final Komoran komoran;

    public NlpService() {
        // Komoran 객체를 생성합니다. 모델은 경량화된 LIGHT 모델을 사용합니다.
        // 더 높은 정확도가 필요하면 DEFAULT_MODEL.FULL을 사용할 수 있습니다.
        this.komoran = new Komoran(DEFAULT_MODEL.LIGHT);
        log.info("Komoran 형태소 분석기 초기화 완료.");
    }

    /**
     * 원본 텍스트에서 어려운 단어를 찾아 span 태그로 감싸는 메소드
     * @param originalContent 원본 뉴스 기사 본문
     * @param difficultWords DB에서 가져온 어려운 단어 목록
     * @return span 태그가 삽입된 HTML 텍스트
     */
    public String markupDifficultWords(String originalContent, Set<String> difficultWords) {
        if (originalContent == null || originalContent.isBlank() || difficultWords == null || difficultWords.isEmpty()) {
            return originalContent;
        }

        long startTime = System.currentTimeMillis();

        // Komoran을 사용하여 텍스트를 형태소 단위로 분석합니다.
        List<Token> tokens = komoran.analyze(originalContent).getTokenList();
        StringBuilder markedUpContent = new StringBuilder();
        int lastIndex = 0;

        for (Token token : tokens) {
            // 이전 토큰과 현재 토큰 사이의 공백이나 특수문자를 그대로 유지합니다.
            if (token.getBeginIndex() > lastIndex) {
                markedUpContent.append(originalContent, lastIndex, token.getBeginIndex());
            }

            String term = token.getMorph();
            String pos = token.getPos(); // 품사 (예: NNP-고유명사, NNG-일반명사)

            // 단어가 명사(NNG, NNP)이고, 어려운 단어 목록에 포함되어 있다면
            if ((pos.equals("NNG") || pos.equals("NNP")) && difficultWords.contains(term)) {
                // 툴팁 기능을 위한 span 태그를 추가합니다.
                markedUpContent.append("<span class=\"tooltip-word\" data-term=\"").append(term).append("\">").append(term).append("</span>");
            } else {
                // 그렇지 않으면 원본 단어를 그대로 추가합니다.
                markedUpContent.append(term);
            }
            lastIndex = token.getEndIndex();
        }

        // 마지막 토큰 이후의 나머지 텍스트를 추가합니다.
        if (lastIndex < originalContent.length()) {
            markedUpContent.append(originalContent.substring(lastIndex));
        }

        long endTime = System.currentTimeMillis();
        log.debug("NLP 마크업 처리 시간: {}ms", (endTime - startTime));

        return markedUpContent.toString();
    }
}
