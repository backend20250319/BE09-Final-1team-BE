package com.newnormallist.newsservice.news.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DedupState {
        REPRESENTATIVE("대표"),
        RELATED("관련"),
        KEPT("유지");

        private final String description;
}
