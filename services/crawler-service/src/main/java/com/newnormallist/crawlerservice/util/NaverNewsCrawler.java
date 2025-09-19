package com.newnormallist.crawlerservice.util;

import com.newnormallist.crawlerservice.dto.NewsDetail;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * 네이버 뉴스 크롤러
 * 
 * 역할:
 * - 네이버 뉴스 사이트에서 실제 뉴스 데이터 수집
 * - Selenium WebDriver 기반 동적 웹 크롤링
 * - 카테고리별 뉴스 목록 및 상세 정보 추출
 * 
 * 기능:
 * - 뉴스 목록 크롤링: 카테고리별 뉴스 링크 및 기본 정보 수집
 * - 뉴스 상세 크롤링: 개별 뉴스의 본문, 이미지, 메타데이터 추출
 * - 더보기 버튼 자동 클릭: 목표 개수(100개)까지 뉴스 수집
 * - 언론사 필터링: 신뢰할 수 있는 언론사만 선별
 * - 오류 처리: 네트워크 오류, 페이지 로딩 실패 등 예외 상황 대응
 */
@Slf4j
@Component
public class NaverNewsCrawler {

    private static final Map<Integer, String> CATEGORIES = Map.of(
            100, "POLITICS",
            101, "ECONOMY",
            102, "SOCIETY", 
            104, "INTERNATIONAL",
            105, "IT_SCIENCE"
    );

    private static final Set<String> ALLOWED_PRESSES = Set.of(
            "경향신문", "국민일보", "동아일보", "문화일보", "서울신문", "조선일보", "중앙일보", "한겨레", "한국일보",
            "뉴스1", "뉴시스", "연합뉴스", "연합뉴스TV", "채널A", "한국경제TV", "JTBC", "KBS", "MBC", "MBN",
            "SBS", "SBS Biz", "TV조선", "YTN", "매일경제", "머니투데이", "비즈워치", "서울경제", "아시아경제",
            "이데일리", "조선비즈", "파이낸셜뉴스", "한국경제", "헤럴드경제", "디지털데일리", "디지털타임스",
            "블로터", "전자신문", "지디넷코리아"
    );

    /**
     * 모든 카테고리 크롤링 실행
     */
    public Map<String, List<NewsDetail>> crawlAllCategories(int targetCount) {
        log.info("모든 카테고리 크롤링 시작 - 목표: {}개씩", targetCount);

        Map<String, List<NewsDetail>> results = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        List<Future<?>> futures = new ArrayList<>();

        // 기본 카테고리 크롤링
        for (Map.Entry<Integer, String> category : CATEGORIES.entrySet()) {
            final int categoryCode = category.getKey();
            final String categoryName = category.getValue();

            futures.add(executor.submit(() -> {
                List<NewsDetail> newsList = crawlCategory(categoryCode, categoryName, targetCount);
                results.put(categoryName, newsList);
                log.info("{} 카테고리 크롤링 완료: {}개", categoryName, newsList.size());
            }));
        }

        // 추가 카테고리 크롤링
        String[] additionalCategories = {"VEHICLE", "LIFE", "TRAVEL_FOOD", "ART"};
        for (String categoryName : additionalCategories) {
            futures.add(executor.submit(() -> {
                List<NewsDetail> newsList = crawlAdditionalCategory(categoryName, targetCount);
                results.put(categoryName, newsList);
                log.info("{} 카테고리 크롤링 완료: {}개", categoryName, newsList.size());
            }));
        }

        executor.shutdown();
        try {
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (Exception e) {
            log.error("크롤링 중 오류 발생: {}", e.getMessage(), e);
        }

        log.info("모든 카테고리 크롤링 완료");
        return results;
    }

    /**
     * 기본 카테고리 크롤링
     */
    private List<NewsDetail> crawlCategory(int categoryCode, String categoryName, int targetCount) {
        log.info("{} 카테고리 크롤링 시작 - 목표: {}개", categoryName, targetCount);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        Set<String> collectedLinks = new HashSet<>();
        List<NewsDetail> newsList = new ArrayList<>();

        try {
            String url = "https://news.naver.com/section/" + categoryCode;
            driver.get(url);

            // 더보기 버튼을 여러 번 클릭하여 충분한 기사 로드
            int clickCount = 0;
            int maxClicks = 10; // 최대 10번 클릭

            while (clickCount < maxClicks) {
                if (!clickMoreButton(wait)) {
                    log.info("더보기 버튼 클릭 실패 또는 버튼 없음 ({}번째 시도)", clickCount + 1);
                    break;
                }
                clickCount++;
                log.info("더보기 버튼 {}번째 클릭 완료", clickCount);

                // 클릭 후 잠시 대기
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            Document doc = Jsoup.parse(driver.getPageSource());
            Elements articles = doc.select("#newsct div.section_latest_article ul li");

            // 다른 선택자도 시도
            if (articles.isEmpty()) {
                articles = doc.select(".sa_item");
            }
            if (articles.isEmpty()) {
                articles = doc.select(".section_latest li");
            }

            log.info("🔍 {} 카테고리 URL: {}", categoryName, url);
            log.info("🔍 전체 페이지 크기: {} bytes", driver.getPageSource().length());
            log.info("🔍 찾은 기사 요소 개수: {}", articles.size());

            // 첫 번째 기사 요소의 HTML 구조 확인 (디버깅용)
            if (!articles.isEmpty()) {
                log.info("🔍 첫 번째 기사 요소 HTML: {}", articles.first().outerHtml().substring(0, Math.min(500, articles.first().outerHtml().length())));
            }

            for (Element article : articles) {
                if (collectedLinks.size() >= targetCount) break;

                try {
                    Element linkElement = article.selectFirst("a");
                    if (linkElement == null) {
                        log.debug("❌ 링크 요소 없음");
                        continue;
                    }
                    String link = linkElement.attr("href");
                    if (link.isEmpty() || collectedLinks.contains(link)) {
                        log.debug("❌ 빈 링크 또는 중복: {}", link);
                        continue;
                    }
                    Element titleElement = article.selectFirst("strong");
                    if (titleElement == null) {
                        log.debug("❌ 제목 요소 없음");
                        continue;
                    }
                    String title = titleElement.text().trim();
                    if (title.isEmpty()) {
                        log.debug("❌ 빈 제목");
                        continue;
                    }
                    
                    // 대괄호 안에 "시사", "칼럼", "컬럼" 등이 포함된 기사 필터링
                    if (containsFilteredKeywords(title)) {
                        log.info("🚫 필터링된 기사: {}", title);
                        continue;
                    }
                    // 다양한 언론사 선택자 시도
                    Element pressElement = article.selectFirst("span.press");
                    if (pressElement == null) {
                        pressElement = article.selectFirst(".press");
                    }
                    if (pressElement == null) {
                        pressElement = article.selectFirst("em");
                    }
                    if (pressElement == null) {
                        pressElement = article.selectFirst(".sa_text_press");
                    }
                    String press = pressElement != null ? pressElement.text().trim() : "알 수 없음";

                    log.info("🔍 발견된 언론사: '{}', 제목: '{}'", press, title.substring(0, Math.min(title.length(), 30)));

                    // 허용된 언론사만 수집
                    if (!ALLOWED_PRESSES.contains(press)) {
                        log.info("❌ 허용되지 않은 언론사: '{}'", press);
                        continue;
                    }

                    collectedLinks.add(link);

                    NewsDetail news = NewsDetail.builder()
                            .link(link)
                            .title(title)
                            .press(press)
                            .categoryName(categoryName)      // 카테고리명 설정
                            .createdAt(LocalDateTime.now())
                            .build();

                    newsList.add(news);

                } catch (Exception e) {
                    log.warn("기사 파싱 실패: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("❌ {} 카테고리 크롤링 실패: {}", categoryName, e.getMessage(), e);
        } finally {
            driver.quit();
        }

        log.info("📊 {} 카테고리 크롤링 결과: {}개 수집", categoryName, newsList.size());
        return newsList;
    }

    /**
     * 추가 카테고리 크롤링 (자동차, 생활, 여행, 예술)
     */
    private List<NewsDetail> crawlAdditionalCategory(String categoryName, int targetCount) {
        log.info("{} 카테고리 크롤링 시작", categoryName);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        Set<String> collectedLinks = new HashSet<>();
        List<NewsDetail> newsList = new ArrayList<>();

        try {
            // 카테고리별 URL과 개수 매핑
            CategoryCrawlConfig config = getCategoryCrawlConfig(categoryName);
            if (config == null) {
                log.error("{} 카테고리 설정을 찾을 수 없습니다", categoryName);
                return newsList;
            }

            for (UrlConfig urlConfig : config.getUrls()) {
                if (collectedLinks.size() >= config.getTotalTarget()) break;

                log.info("{} 카테고리 {} 크롤링 중: {}개 목표", categoryName, urlConfig.getUrl(), urlConfig.getTargetCount());

                driver.get(urlConfig.getUrl());
                Thread.sleep(2000); // 페이지 로딩 대기

                // 더보기 버튼을 여러 번 클릭하여 충분한 기사 로드
                int clickCount = 0;
                int maxClicks = 5; // 추가 카테고리는 최대 5번 클릭

                while (clickCount < maxClicks) {
                    if (!clickMoreButton(wait)) {
                        log.info("더보기 버튼 클릭 실패 또는 버튼 없음 ({}번째 시도)", clickCount + 1);
                        break;
                    }
                    clickCount++;
                    log.info("더보기 버튼 {}번째 클릭 완료", clickCount);

                    // 클릭 후 잠시 대기
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                Document doc = Jsoup.parse(driver.getPageSource());
                Elements articles = doc.select("#newsct div.section_latest_article ul li");

                for (Element article : articles) {
                    if (collectedLinks.size() >= urlConfig.getTargetCount()) break;

                    try {
                        Element linkElement = article.selectFirst("a");
                        if (linkElement == null) continue;

                        String link = linkElement.attr("href");
                        if (link.isEmpty() || collectedLinks.contains(link)) continue;

                        Element titleElement = article.selectFirst("strong");
                        if (titleElement == null) continue;

                        String title = titleElement.text().trim();
                        if (title.isEmpty()) continue;
                        
                        // 대괄호 안에 "시사", "칼럼", "컬럼" 등이 포함된 기사 필터링
                        if (containsFilteredKeywords(title)) {
                            log.info("🚫 필터링된 기사: {}", title);
                            continue;
                        }

                        // 다양한 언론사 선택자 시도
                        Element pressElement = article.selectFirst("span.press");
                        if (pressElement == null) {
                            pressElement = article.selectFirst(".press");
                        }
                        if (pressElement == null) {
                            pressElement = article.selectFirst("em");
                        }
                        if (pressElement == null) {
                            pressElement = article.selectFirst(".sa_text_press");
                        }
                        String press = pressElement != null ? pressElement.text().trim() : "알 수 없음";

                        // 허용된 언론사만 수집
                        if (!ALLOWED_PRESSES.contains(press)) continue;

                        collectedLinks.add(link);

                        NewsDetail news = NewsDetail.builder()
                                .link(link)
                                .title(title)
                                .press(press)
                                .categoryName(categoryName)      // 카테고리명 설정
                                .createdAt(LocalDateTime.now())
                                .build();

                        newsList.add(news);

                    } catch (Exception e) {
                        log.warn("기사 파싱 실패: {}", e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            log.error("{} 카테고리 크롤링 실패: {}", categoryName, e.getMessage(), e);
        } finally {
            driver.quit();
        }

        log.info("{} 카테고리 크롤링 결과: {}개 수집", categoryName, newsList.size());
        return newsList;
    }

    /**
     * 카테고리별 크롤링 설정 반환
     */
    private CategoryCrawlConfig getCategoryCrawlConfig(String categoryName) {
        return switch (categoryName) {
            case "VEHICLE" -> new CategoryCrawlConfig(239, 40, List.of(
                    new UrlConfig("https://news.naver.com/breakingnews/section/103/239", 35),
                    new UrlConfig("https://news.naver.com/breakingnews/section/103/240", 5)
            ));
            case "LIFE" -> new CategoryCrawlConfig(241, 120, List.of(
                    new UrlConfig("https://news.naver.com/breakingnews/section/103/241", 30),
                    new UrlConfig("https://news.naver.com/breakingnews/section/103/248", 40),
                    new UrlConfig("https://news.naver.com/breakingnews/section/103/245", 50)
            ));
            case "TRAVEL_FOOD" -> new CategoryCrawlConfig(237, 50, List.of(
                    new UrlConfig("https://news.naver.com/breakingnews/section/103/237", 40),
                    new UrlConfig("https://news.naver.com/breakingnews/section/103/238", 10)
            ));
            case "ART" -> new CategoryCrawlConfig(242, 80, List.of(
                    new UrlConfig("https://news.naver.com/breakingnews/section/103/242", 45),
                    new UrlConfig("https://news.naver.com/breakingnews/section/103/243", 20),
                    new UrlConfig("https://news.naver.com/breakingnews/section/103/376", 15)
            ));
            default -> null;
        };
    }

    // 내부 클래스들
    private static class CategoryCrawlConfig {
        private final int categoryId;
        private final int totalTarget;
        private final List<UrlConfig> urls;

        public CategoryCrawlConfig(int categoryId, int totalTarget, List<UrlConfig> urls) {
            this.categoryId = categoryId;
            this.totalTarget = totalTarget;
            this.urls = urls;
        }

        public int getTotalTarget() { return totalTarget; }
        public List<UrlConfig> getUrls() { return urls; }
    }

    private static class UrlConfig {
        private final String url;
        private final int targetCount;

        public UrlConfig(String url, int targetCount) {
            this.url = url;
            this.targetCount = targetCount;
        }

        public String getUrl() { return url; }
        public int getTargetCount() { return targetCount; }
    }

    /**
     * 제목에서 대괄호 안에 필터링할 키워드가 포함되어 있는지 확인
     * [시사], [*시사], [*시사*], [시사*] 등 모든 패턴을 감지
     */
    private static boolean containsFilteredKeywords(String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }
        
        // 대괄호 안의 내용을 찾는 정규식 패턴
        java.util.regex.Pattern bracketPattern = java.util.regex.Pattern.compile("\\[([^\\]]+)\\]");
        java.util.regex.Matcher matcher = bracketPattern.matcher(title);
        
        while (matcher.find()) {
            String bracketContent = matcher.group(1).toLowerCase().trim();
            
            // 필터링할 키워드들 (와일드카드 패턴도 고려)
            String[] filteredKeywords = {
                "운세", "시사", "칼럼", "컬럼", "deep read", "이우석의 푸드로지", 
                "가정예배", "기고", "리포트", "프로젝트", "오늘의 운세", "포토",
                "사설", "논설", "오피니언", "독자투고", "기자수첩", "취재후기",
                "인터뷰", "좌담", "대담", "특별기고", "특별대담", "특집", "이슈", "칼럼", "월간", "주간", "이슈전파사", "속보" 
            };
            
            for (String keyword : filteredKeywords) {
                // 키워드가 대괄호 내용에 포함되어 있는지 확인
                // 예: [시사], [*시사], [시사*], [*시사*], [시사칼럼] 등 모두 감지
                if (bracketContent.contains(keyword.toLowerCase())) {
                    log.info("🚫 [필터링] 제외된 기사: {}", title);
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * 더보기 버튼 클릭
     */
    private boolean clickMoreButton(WebDriverWait wait) {
        try {
            // 여러 가지 더보기 버튼 선택자 시도
            String[] moreButtonSelectors = {
                    "#newsct > div.section_latest > div > div.section_more > a",
                    "button.sa_more_btn",
                    ".sa_more_btn",
                    "button[class*='more']",
                    "a[class*='more']",
                    ".more_wrap button",
                    ".sa_more"
            };

            for (String selector : moreButtonSelectors) {
                try {
                    WebElement moreButton = wait.until(ExpectedConditions.elementToBeClickable(
                            By.cssSelector(selector)
                    ));
                    moreButton.click();
                    log.debug("더보기 버튼 클릭 성공: {}", selector);
                    Thread.sleep(1000);
                    return true;
                } catch (Exception e) {
                    log.debug("더보기 버튼 선택자 실패: {}", selector);
                    continue;
                }
            }

            log.debug("모든 더보기 버튼 선택자 실패");
            return false;
        } catch (Exception e) {
            log.debug("더보기 버튼 클릭 중 오류: {}", e.getMessage());
            return false;
        }
    }
}
