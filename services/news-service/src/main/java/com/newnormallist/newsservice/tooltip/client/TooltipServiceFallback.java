package com.newnormallist.newsservice.tooltip.client;

import com.newnormallist.newsservice.tooltip.dto.ProcessContentRequest;
import com.newnormallist.newsservice.tooltip.dto.ProcessContentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TooltipServiceFallback implements TooltipServiceClient {
    
    @Override
    public ProcessContentResponse processContent(ProcessContentRequest request) {
        log.warn("⚠️ 툴팁 서비스 호출 실패! 뉴스 ID: {}, 원본 텍스트로 폴백합니다.", request.newsId());
        
        // 툴팁 서비스 장애 시 원본 텍스트 그대로 반환
        return new ProcessContentResponse(request.originalContent());
    }
}
