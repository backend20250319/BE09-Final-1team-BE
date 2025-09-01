package com.newnormallist.newsservice.summarizer.service;

import com.newnormallist.newsservice.summarizer.client.SummarizerClient;
import com.newnormallist.newsservice.summarizer.dto.SummaryOptions;
import com.newnormallist.newsservice.summarizer.dto.SummaryResponse;
import com.newnormallist.newsservice.summarizer.repository.NewsSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NewsSummaryService {

    private final NewsSummaryRepository repo;
    private final SummarizerClient client;

    /** Flask와 동일 규칙으로 타입 정규화 */
    private static String canonType(String t) {
        if (t == null || t.isBlank()) return "DEFAULT";
        return t.trim()
                .toUpperCase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_")
                .replace("/", "_");
    }

    /**
     * 뉴스 ID 기반 요약
     * - 캐시 히트면 DB에서 즉시 반환
     * - 미스면 Flask에 생성 요청 후, Flask가 응답한 최종 타입/라인/본문을 그대로 반환 (재조회 없음)
     */
    @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
    public SummaryResponse summarize(Long newsId, SummaryOptions opt) {
        final String type = canonType(opt.type());
        final int lines = (opt.lines() > 0) ? opt.lines() : 3;

        // 1) 캐시 우선
        if (!opt.force()) {
            var hit = repo.findTopByNewsIdAndSummaryTypeAndLinesOrderByIdDesc(newsId, type, lines);
            if (hit.isPresent()) {
                return SummaryResponse.fromEntity(hit.get(), true);
            }
        }

        // 2) 캐시 미스 → Flask 생성 요청 → 응답 그대로 반환(재조회 X)
        return client.summarizeByNewsIdAsResponse(newsId, type, lines, opt.prompt());
    }

    /** 자유 텍스트 즉시 요약(저장 없음) */
    @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
    public String summarizeText(String text, SummaryOptions opt) {
        final String type = canonType(opt.type());
        final int lines = (opt.lines() > 0) ? opt.lines() : 3;
        return client.summarizeByText(text, type, lines, opt.prompt());
    }
}
