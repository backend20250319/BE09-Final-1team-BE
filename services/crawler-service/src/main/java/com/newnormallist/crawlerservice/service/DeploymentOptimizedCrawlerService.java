package com.newnormallist.crawlerservice.service;

import com.newnormallist.crawlerservice.dto.NewsDetail;
import com.newnormallist.crawlerservice.util.NaverNewsCrawler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 배포 환경 최적화 크롤링 서비스
 * 
 * 역할:
 * - 전체 뉴스 크롤링 프로세스 오케스트레이션
 * - 파일서버 기반 데이터 플로우 관리
 * - Python 중복제거 서비스와 연동
 * 
 * 기능:
 * - 9개 카테고리별 뉴스 크롤링 (각 100개)
 * - 파일서버에 단계별 데이터 저장 (list → detail → deduplicated → related)
 * - Python 중복제거 서비스 호출 및 결과 처리
 * - 최종 데이터를 MySQL DB에 저장
 * - 배포 환경에 최적화된 성능 및 안정성 보장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentOptimizedCrawlerService {

    private final NaverNewsCrawler naverNewsCrawler;
    private final FileServerService fileServerService;
    private final PythonDeduplicationIntegrationService pythonDeduplicationIntegrationService;
    private final FileServerDatabaseService fileServerDatabaseService;

    // 기본 카테고리용 고정값 (코드에서 각 카테고리별 개수는 하드코딩됨)
    private final int targetCount = 100;

    // 요청 간격 (밀리초) - 서버 부하 방지
    private final int requestDelay = 1500;
    
    // 크롤링 세션 타임스탬프 (1단계와 2단계에서 동일한 타임스탬프 사용)
    private String sessionTimestamp;


    /**
     * 배포 환경 최적화된 전체 크롤링 프로세스
     */
    public void runDeploymentOptimizedCrawling() {
        // 🚨 디버깅: 호출자 추적
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        log.info("🚨🚨🚨 크롤링 시작! 호출자: {}", stackTrace[2].toString());
        for (int i = 2; i < Math.min(stackTrace.length, 7); i++) {
            log.info("  -> {}", stackTrace[i].toString());
        }
        
        log.info("배포 환경 최적화 크롤링 시작");
        
        try {
            // 크롤링 세션 시작 시 타임스탬프 설정
            sessionTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"));
            log.info("크롤링 세션 타임스탬프: {}", sessionTimestamp);
            
            // 1단계: 뉴스 목록 크롤링 → 파일서버 저장
            log.info("1단계: 뉴스 목록 크롤링 및 파일서버 저장");
            crawlAndSaveNewsList();
            
            // 2단계: 파일서버에서 뉴스 목록 조회 → 상세 크롤링 → 파일서버 저장
            log.info("2단계: 뉴스 상세 크롤링 및 파일서버 저장");
            crawlAndSaveNewsDetails();
            
            // 3단계: 파일서버 기반 중복 제거
            log.info("3단계: 파일서버 기반 중복 제거");
            runDeduplication();
            
            // 4단계: JPA 기반 DB 저장
            log.info("4단계: JPA 기반 DB 저장");
            saveToDatabase();
            
            log.info("배포 환경 최적화 크롤링 완료!");
            
        } catch (Exception e) {
            log.error("배포 환경 크롤링 실패: {}", e.getMessage(), e);
            throw new RuntimeException("배포 환경 크롤링 실패", e);
        }
    }

    /**
     * 1단계: 뉴스 목록 크롤링 및 파일서버 저장
     */
    private void crawlAndSaveNewsList() {
        log.info("뉴스 목록 크롤링 시작 - 목표: {}개씩", targetCount);
        
        Map<String, List<NewsDetail>> newsList = naverNewsCrawler.crawlAllCategories(targetCount);
        
        int totalCount = newsList.values().stream()
            .mapToInt(List::size)
            .sum();
        
        log.info("뉴스 목록 크롤링 완료 - 총 {}개", totalCount);
        
        // 파일서버에 뉴스 목록 저장
        for (Map.Entry<String, List<NewsDetail>> entry : newsList.entrySet()) {
            String category = entry.getKey();
            List<NewsDetail> categoryNews = entry.getValue();
            
            try {
                fileServerService.saveNewsListToCsvWithTimestamp(category, categoryNews, "list", sessionTimestamp);
                log.info("{} 카테고리 뉴스 목록 파일서버 저장 완료: {}개", category, categoryNews.size());
            } catch (Exception e) {
                log.error("{} 카테고리 뉴스 목록 파일서버 저장 실패: {}", category, e.getMessage());
            }
        }
    }

    /**
     * 2단계: 파일서버에서 뉴스 목록 조회 → 상세 크롤링 → 파일서버 저장
     */
    private void crawlAndSaveNewsDetails() {
        log.info("뉴스 상세 크롤링 시작");
        
        String[] categories = {"POLITICS", "ECONOMY", "SOCIETY", "LIFE", "INTERNATIONAL", "IT_SCIENCE", "VEHICLE", "TRAVEL_FOOD", "ART"};
        
        for (String category : categories) {
            try {
                // 파일서버에서 뉴스 목록 조회 (세션 타임스탬프 사용)
                List<NewsDetail> newsList = fileServerService.getNewsListFromCsvWithTimestamp(category, "list", sessionTimestamp);
                if (newsList.isEmpty()) {
                    log.info("{} 카테고리 뉴스 목록이 비어있음", category);
                    continue;
                }
                
                // 상세 크롤링
                List<NewsDetail> detailedNews = crawlCategoryDetails(category, newsList);
                
                // 파일서버에 상세 정보 저장 (동일한 세션 타임스탬프 사용)
                fileServerService.saveNewsListToCsvWithTimestamp(category, detailedNews, "detail", sessionTimestamp);
                log.info("{} 카테고리 상세 크롤링 완료: {}개", category, detailedNews.size());
                
            } catch (Exception e) {
                log.error("{} 카테고리 상세 크롤링 실패: {}", category, e.getMessage());
            }
        }
        
        log.info("뉴스 상세 크롤링 완료");
    }

    /**
     * 카테고리별 상세 크롤링
     */
    private List<NewsDetail> crawlCategoryDetails(String category, List<NewsDetail> newsList) {
        log.info("{} 카테고리 상세 크롤링 시작: {}개", category, newsList.size());
        
        List<NewsDetail> detailedNews = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        List<Future<NewsDetail>> futures = new ArrayList<>();

        for (NewsDetail news : newsList) {
            futures.add(executor.submit(() -> crawlSingleNewsDetail(news)));
        }

        executor.shutdown();
        try {
            for (Future<NewsDetail> future : futures) {
                NewsDetail detailedNewsItem = future.get();
                if (detailedNewsItem != null) {
                    detailedNews.add(detailedNewsItem);
                }
            }
        } catch (Exception e) {
            log.error("{} 카테고리 상세 크롤링 실패: {}", category, e.getMessage());
        }

        log.info("{} 카테고리 상세 크롤링 완료: {}개", category, detailedNews.size());
        return detailedNews;
    }

    /**
     * 개별 뉴스 상세 크롤링
     */
    private NewsDetail crawlSingleNewsDetail(NewsDetail news) {
        try {
            // 요청 간격 조절
            Thread.sleep(requestDelay);
            
            Document doc = Jsoup.connect(news.getLink())
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();

            // 제목은 이미 크롤링할 때 받아온 것을 사용 (원래 로직)
            
            // 내용 추출
            String content = extractContent(doc);
            
            // 기자명 추출
            String reporter = extractReporter(doc);
            
            // 날짜 추출
            String date = extractDate(doc);
            
            // 이미지 URL 추출
            String imageUrl = extractImageUrl(doc);

            return NewsDetail.builder()
                .link(news.getLink())
                .title(news.getTitle())  // 원래 로직: 이미 크롤링할 때 받아온 제목 사용
                .press(news.getPress())
                .content(content)
                .reporter(reporter)
                .date(date != null ? date : news.getDate())
                .categoryName(news.getCategoryName())
                .imageUrl(imageUrl)
                .trusted(1)  // trusted 필드 추가
                .oidAid(extractOidAidFromUrl(news.getLink()))  // oid_aid 추가
                .createdAt(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.warn("뉴스 상세 크롤링 실패: {} - {}", news.getLink(), e.getMessage());
            // 실패 시 기본 정보만 반환
            return NewsDetail.builder()
                .link(news.getLink())
                .title(news.getTitle())
                .press(news.getPress())
                .content("상세 내용 크롤링 실패")
                .reporter("")
                .date(news.getDate())
                .categoryName(news.getCategoryName())
                .imageUrl("")
                .trusted(1)  // trusted 필드 추가
                .oidAid(extractOidAidFromUrl(news.getLink()))  // oid_aid 추가
                .createdAt(LocalDateTime.now())
                .build();
        }
    }



    /**
     * 내용 추출
     */
    private String extractContent(Document doc) {
        try {
            // VOD 기사 체크 (스킵 대상)
            if (doc.select("#contents > div._VOD_PLAYER_WRAP").size() > 0) {
                log.debug("VOD 기사로 스킵");
                return null;
            }

            // 네이버 뉴스 본문 추출 (정확한 선택자)
            Element contentElement = doc.selectFirst("#dic_area");
            if (contentElement != null) {
                // 기사만 가져올 경우
//                String content = contentElement.text().trim();
                String content = contentElement.outerHtml();
                
                // 본문이 너무 짧으면 제외
                if (content.length() < 120) {   
                    log.debug("본문이 너무 짧아서 제외: {}자", content.length());
                    return null;
                }
                
                return content;
            }
            
            return "내용을 추출할 수 없습니다.";
            
        } catch (Exception e) {
            return "내용 추출 중 오류가 발생했습니다.";
        }
    }

    /**
     * 기자명 추출
     */
    private String extractReporter(Document doc) {
        try {
            // 우선순위 1: 일반적인 기자 정보 필드
            Elements reporterElements = doc.select("#ct > div.media_end_head.go_trans > div.media_end_head_info.nv_notrans > div.media_end_head_journalist > a > em");
            if (!reporterElements.isEmpty()) {
                List<String> reporterNames = new ArrayList<>();
                for (Element element : reporterElements) {
                    String reporterName = element.text().trim();
                    if (!reporterName.isEmpty()) {
                        reporterNames.add(cleanReporterName(reporterName));
                    }
                }
                if (!reporterNames.isEmpty()) {
                    return String.join(", ", reporterNames);
                }
            }
            
            // 우선순위 2: 여러 기자인 경우의 선택자
            Elements multiReporterElements = doc.select("#_JOURNALIST_BUTTON > em");
            if (!multiReporterElements.isEmpty()) {
                List<String> reporterNames = new ArrayList<>();
                for (Element element : multiReporterElements) {
                    String reporterName = element.text().trim();
                    if (!reporterName.isEmpty()) {
                        reporterNames.add(cleanReporterName(reporterName));
                    }
                }
                if (!reporterNames.isEmpty()) {
                    return String.join(", ", reporterNames);
                }
            }
            
            // 우선순위 3: 대체 선택자에서 기자 정보 추출
            Elements bylineSpans = doc.select("#contents > div.byline > p > span");
            if (!bylineSpans.isEmpty()) {
                List<String> reporterParts = new ArrayList<>();
                for (Element span : bylineSpans) {
                    String spanText = span.text().trim();
                    if (!spanText.isEmpty()) {
                        // 첫 번째 띄어쓰기 또는 괄호까지의 글자만 추출
                        int spaceIndex = spanText.indexOf(' ');
                        int parenthesisIndex = spanText.indexOf('(');
                        
                        int endIndex = -1;
                        if (spaceIndex > 0 && parenthesisIndex > 0) {
                            endIndex = Math.min(spaceIndex, parenthesisIndex);
                        } else if (spaceIndex > 0) {
                            endIndex = spaceIndex;
                        } else if (parenthesisIndex > 0) {
                            endIndex = parenthesisIndex;
                        }
                        
                        if (endIndex > 0) {
                            reporterParts.add(cleanReporterName(spanText.substring(0, endIndex)));
                        } else {
                            reporterParts.add(cleanReporterName(spanText));
                        }
                    }
                }
                if (!reporterParts.isEmpty()) {
                    return String.join(", ", reporterParts);
                }
            }
            
            return "";
            
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 날짜 추출
     */
    private String extractDate(Document doc) {
        try {
            // 네이버 뉴스 날짜 정보 추출 (정확한 선택자)
            Element dateElement = doc.selectFirst("span.media_end_head_info_datestamp_time._ARTICLE_DATE_TIME");
            if (dateElement != null) {
                String date = dateElement.attr("data-date-time");
                if (!date.isEmpty()) {
                    return date;
                }
            }
            
            return LocalDateTime.now().toString();
            
        } catch (Exception e) {
            return LocalDateTime.now().toString();
        }
    }

    /**
     * 이미지 URL 추출
     */
    private String extractImageUrl(Document doc) {
        try {
            // 우선순위 1: 메인 이미지
            Element imageElement = doc.selectFirst("#img1");
            if (imageElement != null) {
                String imageUrl = imageElement.attr("src");
                if (!imageUrl.isEmpty()) {
                    return imageUrl;
                }
            }
            
            // 우선순위 2: 대체 이미지 선택자들
            Element altImageElement = doc.selectFirst("div.end_body_wrp img");
            if (altImageElement != null) {
                String imageUrl = altImageElement.attr("src");
                if (!imageUrl.isEmpty()) {
                    return imageUrl;
                }
            }
            
            // 우선순위 3: og:image 메타 태그
            Element ogImageElement = doc.selectFirst("meta[property=og:image]");
            if (ogImageElement != null) {
                String imageUrl = ogImageElement.attr("content");
                if (!imageUrl.isEmpty()) {
                    return imageUrl;
                }
            }
            
            // 우선순위 4: twitter:image 메타 태그
            Element twitterImageElement = doc.selectFirst("meta[name=twitter:image]");
            if (twitterImageElement != null) {
                String imageUrl = twitterImageElement.attr("content");
                if (!imageUrl.isEmpty()) {
                    return imageUrl;
                }
            }
            
            return "";
            
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 기자 이름에서 "기자" 텍스트를 안전하게 제거
     */
    private String cleanReporterName(String reporterName) {
        if (reporterName == null || reporterName.trim().isEmpty()) {
            return "";
        }
        
        String cleaned = reporterName.trim();
        
        // "기자"로 끝나는 경우만 제거 (이름에 "기자"가 포함된 경우는 보존)
        if (cleaned.endsWith(" 기자")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        } else if (cleaned.endsWith("기자")) {
            cleaned = cleaned.substring(0, cleaned.length() - 2).trim();
        }
        
        // 다른 직책들도 제거
        String[] titles = {" 특파원", "특파원", " 객원기자", "객원기자", " 통신원", "통신원"};
        for (String title : titles) {
            if (cleaned.endsWith(title)) {
                cleaned = cleaned.substring(0, cleaned.length() - title.length()).trim();
                break;
            }
        }
        
        return cleaned;
    }

    /**
     * URL에서 oid_aid 추출
     */
    private String extractOidAidFromUrl(String url) {
        try {
            // 새로운 URL 패턴: https://n.news.naver.com/mnews/article/{oid}/{aid}
            if (url.contains("/mnews/article/")) {
                String[] parts = url.split("/mnews/article/");
                if (parts.length > 1) {
                    String[] oidAid = parts[1].split("/");
                    if (oidAid.length >= 2) {
                        String oid = oidAid[0];
                        String aid = oidAid[1].split("\\?")[0]; // 쿼리 파라미터 제거
                        return oid + "-" + aid;  // 파이썬 코드와 동일하게 "-" 사용
                    }
                }
            }
            // 기존 URL 패턴: https://news.naver.com/main/read.naver?oid=xxx&aid=xxx
            else if (url.contains("oid=") && url.contains("aid=")) {
                String oid = url.split("oid=")[1].split("&")[0];
                String aid = url.split("aid=")[1].split("&")[0];
                return oid + "-" + aid;  // 파이썬 코드와 동일하게 "-" 사용
            }
        } catch (Exception e) {
            log.warn("URL에서 oid_aid 추출 실패: {}", url);
        }
        return "extracted_" + Math.abs(url.hashCode());
    }



    /**
     * 중복 제거 실행
     */
    private void runDeduplication() {
        log.info("파일서버 기반 중복 제거 시작");
        
        try {
            // Python 기반 중복제거 실행 (파일서버 경로 전달)
            pythonDeduplicationIntegrationService.runFileServerDeduplication();
            
            // 중복 제거 완료 로그
            log.info("파이썬 파일서버 중복 제거 완료");
            
            log.info("파일서버 기반 중복 제거 완료");
            
        } catch (Exception e) {
            log.error("파일서버 기반 중복 제거 실패: {}", e.getMessage());
            throw new RuntimeException("파일서버 기반 중복 제거 실패", e);
                }
    }
    
    /**
     * 파일서버 기반 DB 저장
     */
    private void saveToDatabase() {
        log.info("파일서버 기반 DB 저장 시작");
        
        try {
            fileServerDatabaseService.saveLatestDataToDatabase();
            fileServerDatabaseService.summarizeLatestData();
            log.info("파일서버 기반 DB 저장 완료");
            
        } catch (Exception e) {
            log.error("파일서버 기반 DB 저장 실패: {}", e.getMessage());
            throw new RuntimeException("파일서버 기반 DB 저장 실패", e);
        }
    }
    

}
