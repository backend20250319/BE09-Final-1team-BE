package com.newnormallist.crawlerservice.scheduler;

import com.newnormallist.crawlerservice.service.DeploymentOptimizedCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "crawler.scheduling.enabled", havingValue = "true", matchIfMissing = false)
public class CrawlingScheduler {

    private final DeploymentOptimizedCrawlerService deploymentOptimizedCrawlerService;

    /**
     * 매일 오전 9시, 오후 7시 자동 크롤링
     * 
     * 0: 초 (0초)
     * 0: 분 (0분)  
     * 9,19: 시 (9시, 19시)
     * *: 일 (매일)
     * *: 월 (매월)
     * *: 요일 (매 요일)
     */
    @Scheduled(cron = "0 0 9,19 * * *", zone = "Asia/Seoul")
    public void scheduledCrawling() {
        log.info("스케줄된 크롤링 시작 - {}", java.time.LocalDateTime.now());
        
        try {
            deploymentOptimizedCrawlerService.runDeploymentOptimizedCrawling();
            log.info("스케줄된 크롤링 완료");
            
        } catch (Exception e) {
            log.error("스케줄된 크롤링 실패: {}", e.getMessage(), e);
        }
    }
}
