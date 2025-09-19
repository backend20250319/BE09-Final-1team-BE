package com.newnormallist.tooltipservice.dto;

// 요청 본문을 위한 DTO (record 타입)
public record ProcessContentRequest(Long newsId, String originalContent) {
}