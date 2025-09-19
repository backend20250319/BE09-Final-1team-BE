package com.newnormallist.crawlerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsDetail {
    // 카테고리 정보
    private String categoryName;         // 카테고리명 (POLITICS, ECONOMY, etc.)
    private String press;                // press
    private String title;                // title
    private String reporter;             // reporter
    private String date;                 // published_at
    private String link;                 // link
    private LocalDateTime createdAt;     // created_at
    private String imageUrl;             // image_url
    private Integer trusted;             // trusted
    private String oidAid;               // oid_aid (원래 Python과 동일)
    private String content;              // content
    
    // 중복 제거 결과용 추가 필드들
    private String dedupState;           // dedup_state (REPRESENTATIVE/RELATED/KEPT/REMOVED)
}
