package com.newnormallist.crawlerservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Python 중복제거 서비스 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeduplicationRequest {
    
    /**
     * 중복제거할 카테고리
     */
    private String category;
    
    /**
     * 강제 새로고침 여부
     */
    @Builder.Default
    private boolean forceRefresh = false;
    


}
