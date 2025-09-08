package com.newnormallist.newsservice.summarizer.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newnormallist.newsservice.summarizer.dto.SummaryResponse;
import com.newnormallist.newsservice.summarizer.exception.SummarizerException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Flask 요약 API 클라이언트 - RestTemplate 사용
 * - 뉴스ID 요약: 문자열(줄바꿈 결합) 또는 SummaryResponse 형태로 반환
 * - 텍스트 요약: 문자열로 반환
 */
@Component
@RequiredArgsConstructor
public class SummarizerClient {

    private static final Logger log = LoggerFactory.getLogger(SummarizerClient.class);

    private final RestTemplateBuilder builder;

    private RestTemplate rt;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${summarizer.base-url:http://localhost:5000}")
    private String baseUrl;

    @Value("${summarizer.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${summarizer.read-timeout-ms:10000}")
    private int readTimeoutMs;

    @PostConstruct
    void init() {
        // 일부 환경에서 SimpleClientHttpRequestFactory가 필요할 수 있어 명시적 설정
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(connectTimeoutMs);
        rf.setReadTimeout(readTimeoutMs);

        this.rt = builder
                .requestFactory(() -> rf)
                .build();

        log.info("[SummarizerClient] baseUrl={} connectTimeoutMs={} readTimeoutMs={}",
                baseUrl, connectTimeoutMs, readTimeoutMs);
    }

    // -----------------------------
    // Public APIs
    // -----------------------------

    /** 뉴스ID 기반 요약(기존 방식): 응답을 N줄 문자열로 평탄화하여 반환 */
    public String summarizeByNewsId(Long newsId, String type, int lines, String promptOverride) {
        Map<String, Object> body = baseBody(type, lines, promptOverride);
        body.put("news_id", newsId);
        return callAndPlainText("/summary", body, lines);
    }

    /** 뉴스ID 기반 요약(신규): Flask가 최종 사용한 타입/라인/본문을 그대로 SummaryResponse로 반환 (재조회 불필요) */
    public SummaryResponse summarizeByNewsIdAsResponse(Long newsId, String type, int lines, String promptOverride) {
        Map<String, Object> body = baseBody(type, lines, promptOverride);
        body.put("news_id", newsId);

        String url = baseUrl + "/summary";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            log.debug("[SummarizerClient] POST {} body={}", url, body);
            ResponseEntity<String> resp = rt.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            String payload = resp.getBody();
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new SummarizerException("Flask API 오류: " + resp.getStatusCode() + " " + payload);
            }
            return parseSummaryResponse(payload, newsId, Math.max(1, lines));
        } catch (HttpStatusCodeException e) {
            log.error("[SummarizerClient] HTTP {} {} body={}",
                    e.getStatusCode(), e.getStatusText(), e.getResponseBodyAsString(), e);
            throw new SummarizerException("Flask API 오류: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("[SummarizerClient] request failed", e);
            throw new SummarizerException("Flask API 호출 실패", e);
        }
    }

    /** 자유 텍스트 요약: 응답을 N줄 문자열로 평탄화하여 반환 */
    public String summarizeByText(String text, String type, Integer lines, String promptOverride) {
        int n = (lines == null || lines <= 0) ? 3 : lines;
        Map<String, Object> body = baseBody(type, n, promptOverride);
        body.put("text", text);
        return callAndPlainText("/summary", body, n);
    }

    // -----------------------------
    // Internal helpers
    // -----------------------------

    private Map<String, Object> baseBody(String type, Integer lines, String promptOverride) {
        Map<String, Object> m = new HashMap<>();
        if (type != null && !type.isBlank() && !"DEFAULT".equals(type)) m.put("type", type);
        if (lines != null && lines > 0) m.put("lines", lines);
        if (promptOverride != null && !promptOverride.isBlank()) m.put("prompt", promptOverride);
        return m;
    }

    private String callAndPlainText(String path, Map<String, Object> body, int lines) {
        String url = baseUrl + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            log.debug("[SummarizerClient] POST {} body={}", url, body);
            ResponseEntity<String> resp = rt.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            String raw = resp.getBody();
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new SummarizerException("Flask API 오류: " + resp.getStatusCode() + " " + raw);
            }
            return extractPlainText(raw, lines);
        } catch (HttpStatusCodeException e) {
            log.error("[SummarizerClient] HTTP {} {} body={}",
                    e.getStatusCode(), e.getStatusText(), e.getResponseBodyAsString(), e);
            throw new SummarizerException("Flask API 오류: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("[SummarizerClient] request failed", e);
            throw new SummarizerException("Flask API 호출 실패", e);
        }
    }

    /** JSON/텍스트 무엇이든 → “줄바꿈 기준 N줄 텍스트”로 평탄화 */
    private String extractPlainText(String raw, int limit) {
        if (raw == null || raw.isBlank()) return "";
        try {
            JsonNode root = om.readTree(raw);

            // 1) summary_text / summary 우선
            if (root.has("summary_text")) return root.get("summary_text").asText();
            if (root.has("summary")) return root.get("summary").asText();

            // 2) lines 배열이면 줄바꿈으로 합치되 최대 limit 줄까지만
            if (root.has("lines") && root.get("lines").isArray()) {
                StringBuilder sb = new StringBuilder();
                int n = 0;
                for (JsonNode nline : root.get("lines")) {
                    if (n >= Math.max(1, limit)) break;
                    String s = nline.asText();
                    if (s != null && !s.isBlank()) {
                        if (sb.length() > 0) sb.append('\n');
                        sb.append(s.trim());
                        n++;
                    }
                }
                return sb.toString();
            }

            // 3) count/기타만 있으면 원문 반환
            return raw.trim();
        } catch (Exception ignore) {
            // JSON이 아니면 그냥 원문 반환
            return raw.trim();
        }
    }

    /** Flask 응답(JSON) → SummaryResponse 로 변환 */
    private SummaryResponse parseSummaryResponse(String raw, Long newsId, int fallbackLines) throws Exception {
        JsonNode root = om.readTree(raw);

        // newsId
        Long nid = newsId;
        if (root.has("news_id")) nid = root.get("news_id").asLong();
        else if (root.has("newsId")) nid = root.get("newsId").asLong();

        // 최종 타입
        String resolvedType = "DEFAULT";
        if (root.has("resolved_type")) resolvedType = root.get("resolved_type").asText();
        else if (root.has("type")) resolvedType = root.get("type").asText();

        // 라인 수
        Integer usedLines = null;
        if (root.has("lines") && root.get("lines").isInt()) {
            usedLines = root.get("lines").asInt();
        } else if (root.has("count")) {
            usedLines = root.get("count").asInt();
        } else if (root.has("lines") && root.get("lines").isArray()) {
            usedLines = root.get("lines").size();
        }
        int lines = usedLines != null && usedLines > 0 ? usedLines : fallbackLines;

        // 요약 텍스트
        String summaryText = "";
        if (root.has("summary_text")) {
            summaryText = root.get("summary_text").asText();
        } else if (root.has("summary")) {
            summaryText = root.get("summary").asText();
        } else if (root.has("lines") && root.get("lines").isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode n : root.get("lines")) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(n.asText());
            }
            summaryText = sb.toString();
        }

        // createdAt 파싱 (Flask가 "2025-08-31T10:12:34Z" 같은 ISO8601로 줌)
        Instant createdAt = null;
        if (root.has("createdAt")) {
            String s = root.get("createdAt").asText();
            if (s != null && !s.isBlank()) {
                createdAt = Instant.parse(s);
            }
        } else if (root.has("created_at")) { // 혹시 스네이크 케이스로 올 경우 대비
            String s = root.get("created_at").asText();
            if (s != null && !s.isBlank()) {
                createdAt = Instant.parse(s);
            }
        }

        return SummaryResponse.builder()
                .id(null)                 // (Flask가 id를 안 주므로 null 유지)
                .newsId(nid)
                .type(resolvedType)
                .lines(lines)
                .summary(summaryText)
                .cached(false)
                .createdAt(createdAt)
                .build();
    }
}
