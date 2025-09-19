package com.newnormallist.tooltipservice.dto;

// 단어 정의 정보를 담을 DTO (간소화)
public record TermDefinitionResponseDto(
        String definition,
        Integer displayOrder
) {}