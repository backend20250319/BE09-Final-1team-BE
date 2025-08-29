package com.newnormallist.tooltipservice.dto;

import java.util.List;

// 단어의 상세 정보(대표 용어 + 정의 목록)를 담을 새로운 DTO
public record TermDetailResponseDto(
        String term,
        List<TermDefinitionResponseDto> definitions
) {}