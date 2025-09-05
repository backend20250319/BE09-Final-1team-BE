package com.newnormallist.newsservice.news.controller;


import com.newnormallist.newsservice.news.service.NewsImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class NewsImageController {

    private final NewsImageService newsImageService;

    @GetMapping("/personalized-section")
    public String getPersonalizedSectionImage() {
        return newsImageService.getPersonalizedSectionImage();
    }

    @GetMapping("/trending-section")
    public String getTrendingSectionImage() {
        return newsImageService.getTrendingSectionImage();
    }

    @GetMapping("/latest-news")
    public String getLatestNewsImage() {
        return newsImageService.getLatestNewsImage();
    }
}
