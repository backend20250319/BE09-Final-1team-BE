package com.newnormallist.newsservice.news.client;

import com.newnormallist.newsservice.news.dto.ScrappedNewsResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "news-service")
public interface NewsServiceClient {

    @GetMapping("/api/news/scrap")
    Page<ScrappedNewsResponse> getScrappedNews(@RequestParam("userId") Long userId,
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("sort") List<String> sort);
}