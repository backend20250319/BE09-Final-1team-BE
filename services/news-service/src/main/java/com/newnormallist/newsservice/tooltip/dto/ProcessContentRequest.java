package com.newnormallist.newsservice.tooltip.dto;

public record ProcessContentRequest(
    Long newsId,
    String originalContent
) {
}
