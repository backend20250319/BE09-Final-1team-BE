package com.newnormallist.newsservice.tooltip.client;

import com.newnormallist.newsservice.tooltip.dto.ProcessContentRequest;
import com.newnormallist.newsservice.tooltip.dto.ProcessContentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "tooltip-service",
    fallback = TooltipServiceFallback.class
)
public interface TooltipServiceClient {
    
    @PostMapping("/api/news/analysis/process")
    ProcessContentResponse processContent(@RequestBody ProcessContentRequest request);
}
