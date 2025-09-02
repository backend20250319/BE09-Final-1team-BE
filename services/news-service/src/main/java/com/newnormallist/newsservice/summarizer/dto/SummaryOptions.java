package com.newnormallist.newsservice.summarizer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;


/** 컨트롤러→서비스로 전달할 정규화 옵션 */
@Builder
public record SummaryOptions(
        String type,     // 정규화된 타입
        int lines,
        String prompt,
        boolean force
) {
    public static SummaryOptions of(String rawType, Integer lines, String prompt, Boolean force) {
        String t = normalizeType(rawType);
        int n = (lines == null || lines < 1) ? 3 : lines;
        boolean f = Boolean.TRUE.equals(force);
        return SummaryOptions.builder().type(t).lines(n).prompt(prompt).force(f).build();
    }

    /** 안전 정규화 */
    public static String normalizeType(String raw) {
        String u = raw == null ? "" : raw.trim().toUpperCase().replace('-', '_');
        u = u.replaceAll("^[^A-Z0-9]+", "");  // 선행 특수문자 제거
        u = u.replaceAll("[^A-Z0-9_]", "");   // 허용문자만
        if (u.isEmpty()) return "DEFAULT";
        if ("AIBOT".equals(u)) return "DEFAULT"; // 레거시 호환
        return u;
    }
}