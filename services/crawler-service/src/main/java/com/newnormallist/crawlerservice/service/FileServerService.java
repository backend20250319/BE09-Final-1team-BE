package com.newnormallist.crawlerservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newnormallist.crawlerservice.dto.NewsDetail;
import com.newnormallist.crawlerservice.dto.RelatedNewsDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 파일서버 관리 서비스
 * 
 * 역할:
 * - 뉴스 데이터를 파일서버에 CSV 형태로 저장/조회
 * - 중간 저장소 역할 (파일 기반)
 * - 시간 기반 디렉터리 구조 관리
 * 
 * 기능:
 * - CSV 파일 저장: 크롤링된 뉴스를 단계별로 저장
 * - CSV 파일 조회: 저장된 뉴스 데이터를 읽어서 객체로 변환
 * - 최신 파일 찾기: 타임스탬프 기반으로 가장 최신 파일 자동 탐색
 * - 디렉터리 구조: /am|pm/yyyy-MM-dd_am|pm/stage/category_stage_yyyy-MM-dd-HH-mm.csv
 */
@Slf4j
@Service
public class FileServerService {

    private final ObjectMapper objectMapper;
    // HTTP 클라이언트 (UTF-8 인코딩 설정)
    private final RestTemplate restTemplate;
    
    public FileServerService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
        // UTF-8 인코딩을 위한 메시지 컨버터 설정
        this.restTemplate.getMessageConverters().forEach(converter -> {
            if (converter instanceof org.springframework.http.converter.StringHttpMessageConverter) {
                ((org.springframework.http.converter.StringHttpMessageConverter) converter).setDefaultCharset(java.nio.charset.StandardCharsets.UTF_8);
            }
        });
    }
    
    @Value("${fileserver.base-path:/tmp/news-data}")
    private String basePath;
    
    // 시간 포맷터
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("HH");
    
    /**
     * 현재 시간 기반 디렉터리 경로 생성
     * 예: /fileserver/am/2025-08-19_am/ 또는 /fileserver/pm/2025-08-19_pm/
     */
    private String getCurrentTimePath() {
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(TIME_FORMAT);
        String period = Integer.parseInt(now.format(HOUR_FORMAT)) < 12 ? "am" : "pm";
        return String.format("%s/%s/%s_%s", basePath, period, date, period);
    }
    
    /**
     * CSV 파일 저장
     */
    public void saveNewsListToCsv(String category, List<NewsDetail> newsList, String stage) {
        String dirPath = getCurrentTimePath() + "/" + stage;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"));
        String fileName = category.toLowerCase() + "_" + stage + "_" + timestamp + ".csv";
        String fullPath = dirPath + "/" + fileName;
        
        try {
            // CSV 데이터를 메모리에서 생성
            StringBuilder csvContent = new StringBuilder();
            
            // CSV 헤더
            csvContent.append("title,press,reporter,date,link,imageUrl,oidAid,trusted,content,dedupState,categoryName,createdAt\n");
            
            // 데이터 쓰기
            for (NewsDetail news : newsList) {
                            csvContent.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\",\"%s\",\"%s\"%n",
                escapeCSV(news.getTitle()),
                escapeCSV(news.getPress()),
                escapeCSV(news.getReporter()),
                escapeCSV(news.getDate()),
                escapeCSV(news.getLink()),
                escapeCSV(news.getImageUrl()),
                escapeCSV(news.getOidAid()),
                news.getTrusted() != null ? news.getTrusted() : 1,  // 기본값 1 (true)
                escapeCSV(news.getContent()),
                escapeCSV(news.getDedupState()),
                escapeCSV(news.getCategoryName()),  // categoryName 추가
                escapeCSV(news.getCreatedAt() != null ? news.getCreatedAt().toString() : "")  // createdAt 추가
            ));
            }
            
            // HTTP 파일서버에 업로드
            uploadFileToServer(dirPath, fileName, csvContent.toString());
            
            log.info("📁 파일서버 업로드 완료: {} - 카테고리: {}, 개수: {}", dirPath + "/" + fileName, category, newsList.size());
            
        } catch (Exception e) {
            log.error("📁 파일서버 업로드 실패: {}, 오류: {}", dirPath + "/" + fileName, e.getMessage());
            throw new RuntimeException("파일서버 업로드 실패", e);
        }
    }
    
    /**
     * FTP 업로드 API 호출
     */
    private void uploadFileToServer(String dirPath, String fileName, String content) {
        try {
            // 상대 경로 추출 (basePath 제거)
            String relativePath = dirPath.replace(basePath + "/", "");
            
            // FTP 업로드 요청 데이터 생성 (UTF-8 인코딩 명시)
            Map<String, String> uploadRequest = Map.of(
                "path", relativePath,
                "filename", fileName,
                "content", content  // 이미 UTF-8 String
            );
            
            // HTTP 헤더 설정 (UTF-8 인코딩 명시)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept-Charset", "UTF-8");
            
            // 요청 엔티티 생성
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(uploadRequest, headers);
            
            // 내부 FTP 업로드 API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:8083/api/ftp/upload", // 크롤러 서비스 FTP API 호출
                org.springframework.http.HttpMethod.POST, 
                requestEntity, 
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("FTP 업로드 성공: {}/{}", relativePath, fileName);
            } else {
                throw new RuntimeException("FTP 업로드 실패: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("FTP 업로드 오류: {}/{}", dirPath, fileName, e);
            throw new RuntimeException("FTP 업로드 실패", e);
        }
    }
    
    /**
     * HTTP 파일서버에서 파일 다운로드
     */
    public String downloadFileFromServer(String fileUrl) {
        try {
            // UTF-8 인코딩을 위한 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept-Charset", "UTF-8");
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                fileUrl, 
                HttpMethod.GET, 
                entity, 
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("파일 다운로드 성공: {}", fileUrl);
                return response.getBody();
            } else {
                log.warn("파일 다운로드 실패: {} - 상태코드: {}", fileUrl, response.getStatusCode());
                return null;
            }
            
        } catch (Exception e) {
            log.warn("HTTP 파일 다운로드 오류: {}, 오류: {}", fileUrl, e.getMessage());
            return null;
        }
    }
    
    /**
     * CSV 파일 저장 (타임스탬프 지정)
     */
    public void saveNewsListToCsvWithTimestamp(String category, List<NewsDetail> newsList, String stage, String timestamp) {
        String dirPath = getCurrentTimePath() + "/" + stage;
        String fileName = category.toLowerCase() + "_" + stage + "_" + timestamp + ".csv";
        
        try {
            // CSV 데이터를 메모리에서 생성
            StringBuilder csvContent = new StringBuilder();
            
            // CSV 헤더
            csvContent.append("title,press,reporter,date,link,imageUrl,oidAid,trusted,content,dedupState,categoryName,createdAt\n");
            
            // 데이터 쓰기
            for (NewsDetail news : newsList) {
                            csvContent.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,\"%s\",\"%s\",\"%s\",\"%s\"%n",
                escapeCSV(news.getTitle()),
                escapeCSV(news.getPress()),
                escapeCSV(news.getReporter()),
                escapeCSV(news.getDate()),
                escapeCSV(news.getLink()),
                escapeCSV(news.getImageUrl()),
                escapeCSV(news.getOidAid()),
                news.getTrusted() != null ? news.getTrusted() : 1,  // 기본값 1 (true)
                escapeCSV(news.getContent()),
                escapeCSV(news.getDedupState()),
                escapeCSV(news.getCategoryName()),  // categoryName 추가
                escapeCSV(news.getCreatedAt() != null ? news.getCreatedAt().toString() : "")  // createdAt 추가
            ));
            }
            
            // HTTP 파일서버에 업로드
            uploadFileToServer(dirPath, fileName, csvContent.toString());
            
            log.info("📁 파일서버 업로드 완료 (지정 타임스탬프): {}/{} - 카테고리: {}, 개수: {}", dirPath, fileName, category, newsList.size());
            
        } catch (Exception e) {
            log.error("📁 파일서버 업로드 실패 (지정 타임스탬프): {}/{}, 오류: {}", dirPath, fileName, e.getMessage());
            throw new RuntimeException("파일서버 업로드 실패", e);
        }
    }
    
    /**
     * CSV에서 뉴스 목록 조회 (타임스탬프 지정)
     */
    public List<NewsDetail> getNewsListFromCsvWithTimestamp(String category, String stage, String timestamp) {
        String timePath = getCurrentTimePath();
        String dirPath = timePath + "/" + stage;
        List<NewsDetail> newsList = new ArrayList<>();
        
        // 지정된 타임스탬프로 파일명 생성
        String fileName = category.toLowerCase() + "_" + stage + "_" + timestamp + ".csv";
        String fullPath = dirPath + "/" + fileName;
        
        try {
            // HTTP 파일서버에서 파일 다운로드
            String csvContent = downloadFileFromServer(fullPath);
            if (csvContent == null) {
                log.info("📁 지정 타임스탬프 파일이 존재하지 않음: {}", fullPath);
                return newsList;
            }
            
            // CSV 내용 파싱
            String[] lines = csvContent.split("\n");
            boolean isFirstLine = true;
            
            for (String line : lines) {
                if (isFirstLine) {
                    isFirstLine = false; // 헤더 스킵
                    continue;
                }
                
                if (line.trim().isEmpty()) continue;
                
                NewsDetail news = parseCSVLine(line);
                if (news != null) {
                    newsList.add(news);
                }
            }
            
            log.info("📁 파일서버 조회 완료 (지정 타임스탬프): {} - 카테고리: {}, 개수: {}", fullPath, category, newsList.size());
            
        } catch (Exception e) {
            log.error("📁 파일서버 조회 실패 (지정 타임스탬프): {}, 오류: {}", fullPath, e.getMessage());
        }
        
        return newsList;
    }
    
    /**
     * CSV에서 뉴스 목록 조회
     */
    public List<NewsDetail> getNewsListFromCsv(String category, String stage, String timePath) {
        if (timePath == null) {
            timePath = getCurrentTimePath();
        }
        
        String dirPath = timePath + "/" + stage;
        List<NewsDetail> newsList = new ArrayList<>();
        
        // HTTP 파일서버에서는 최신 파일 찾기 대신 현재 시간 기반으로 파일명 생성
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"));
        String fileName = category.toLowerCase() + "_" + stage + "_" + timestamp + ".csv";
        String fullPath = dirPath + "/" + fileName;
        
        // 만약 해당 시간의 파일이 없다면, 몇 분 전의 파일들을 시도
        String csvContent = null;
        for (int i = 0; i < 180; i++) { // 최대 3시간 전까지 시도 (180분)
            String tryTimestamp = LocalDateTime.now().minusMinutes(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"));
            String tryFileName = category.toLowerCase() + "_" + stage + "_" + tryTimestamp + ".csv";
            String tryFullPath = dirPath + "/" + tryFileName;
            
            csvContent = downloadFileFromServer(tryFullPath);
            if (csvContent != null) {
                fullPath = tryFullPath;
                log.info("📁 파일 발견 ({}분 전): {}", i, fullPath);
                break;
            }
        }
        
        // PM 경로에서 찾지 못했다면 AM 경로도 시도
        if (csvContent == null) {
            log.info("📁 PM 경로에서 파일을 찾지 못함, AM 경로 시도: {}", dirPath);
            String amTimePath = timePath.replace("/pm/", "/am/").replace("_pm", "_am");
            String amDirPath = amTimePath + "/" + stage;
            
            for (int i = 0; i < 180; i++) { // 최대 3시간 전까지 시도 (180분)
                String tryTimestamp = LocalDateTime.now().minusMinutes(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"));
                String tryFileName = category.toLowerCase() + "_" + stage + "_" + tryTimestamp + ".csv";
                String tryFullPath = amDirPath + "/" + tryFileName;
                
                csvContent = downloadFileFromServer(tryFullPath);
                if (csvContent != null) {
                    fullPath = tryFullPath;
                    dirPath = amDirPath;
                    log.info("📁 AM 경로에서 파일 발견 ({}분 전): {}", i, fullPath);
                    break;
                }
            }
        }
        
        if (csvContent == null) {
            log.info("📁 해당 카테고리의 파일이 존재하지 않음: {}/{}", dirPath, category);
            return newsList;
        }
        
        try {
            
            // CSV 내용 파싱
            String[] lines = csvContent.split("\n");
            boolean isFirstLine = true;
            
            for (String line : lines) {
                if (isFirstLine) {
                    isFirstLine = false; // 헤더 스킵
                    continue;
                }
                
                if (line.trim().isEmpty()) continue;
                
                NewsDetail news = parseCSVLine(line);
                if (news != null) {
                    newsList.add(news);
                }
            }
            
            log.info("📁 파일서버 조회 완료: {} - 카테고리: {}, 개수: {}", fullPath, category, newsList.size());
            
        } catch (Exception e) {
            log.error("📁 파일서버 조회 실패: {}, 오류: {}", fullPath, e.getMessage());
            throw new RuntimeException("파일서버 조회 실패", e);
        }
        
        return newsList;
    }
    
    /**
     * 최신 시간대 디렉터리 찾기
     */
    public String getLatestTimePath() {
        try {
            Path baseDir = Paths.get(basePath);
            if (!Files.exists(baseDir)) {
                return getCurrentTimePath();
            }
            
            // am, pm 디렉터리 순회
            String latestPath = null;
            LocalDateTime latestTime = null;
            
            for (String period : List.of("am", "pm")) {
                Path periodDir = baseDir.resolve(period);
                if (!Files.exists(periodDir)) continue;
                
                Files.list(periodDir)
                    .filter(Files::isDirectory)
                    .forEach(path -> {
                        try {
                            String dirName = path.getFileName().toString();
                            // 2025-08-19_am 형태에서 시간 추출
                            String dateStr = dirName.replace("_am", "").replace("_pm", "");
                            LocalDateTime dirTime = LocalDateTime.parse(dateStr + "T" + (period.equals("am") ? "06:00:00" : "18:00:00"));
                            
                            if (latestTime == null || dirTime.isAfter(latestTime)) {
                                // 최신 시간 업데이트
                            }
                        } catch (Exception e) {
                            log.debug("디렉터리 시간 파싱 실패: {}", path);
                        }
                    });
            }
            
            return latestPath != null ? latestPath : getCurrentTimePath();
            
        } catch (Exception e) {
            log.error("최신 시간대 조회 실패: {}", e.getMessage());
            return getCurrentTimePath();
        }
    }
    
    /**
     * CSV 필드 이스케이프
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"").replace("\n", "\\n").replace("\r", "\\r");
    }
    
    /**
     * CSV 라인 파싱 (견고한 파싱 로직)
     */
    private NewsDetail parseCSVLine(String line) {
        try {
            if (line == null || line.trim().isEmpty()) {
                return null;
            }
            
            // CSV 필드 파싱 (따옴표 처리 개선)
            List<String> fields = parseCSVFields(line);
            
                    if (fields.size() < 12) { // 중복제거 파일은 12개 필드 (title,press,reporter,date,link,imageUrl,oidAid,trusted,content,dedupState,categoryName,createdAt)
            log.debug("CSV 필드 개수 부족: {} (필요: 12개, 실제: {}개)", line.substring(0, Math.min(50, line.length())), fields.size());
            return null;
        }
            
            // trusted 필드 안전하게 파싱
            Integer trusted = 0;
            try {
                String trustedStr = fields.get(7).trim();
                if (!trustedStr.isEmpty()) {
                    trusted = Integer.parseInt(trustedStr);
                }
            } catch (NumberFormatException e) {
                log.debug("trusted 값 파싱 실패, 기본값 사용: {}", fields.get(7));
            }
            
            // createdAt 필드 안전하게 파싱
            LocalDateTime createdAt = null;
            try {
                String createdAtStr = unescapeCSV(fields.get(11));
                if (createdAtStr != null && !createdAtStr.trim().isEmpty()) {
                    createdAt = LocalDateTime.parse(createdAtStr);
                }
            } catch (Exception e) {
                log.debug("createdAt 파싱 실패, 현재 시간 사용: {}", fields.get(11));
                createdAt = LocalDateTime.now();
            }

            return NewsDetail.builder()
                .title(unescapeCSV(fields.get(0)))
                .press(unescapeCSV(fields.get(1)))
                .reporter(unescapeCSV(fields.get(2)))
                .date(unescapeCSV(fields.get(3)))
                .link(unescapeCSV(fields.get(4)))
                .imageUrl(unescapeCSV(fields.get(5)))
                .oidAid(unescapeCSV(fields.get(6)))
                .trusted(trusted)
                .content(unescapeCSV(fields.get(8)))
                .dedupState(unescapeCSV(fields.get(9)))
                .categoryName(unescapeCSV(fields.get(10))) // categoryName 파싱
                .createdAt(createdAt) // 안전하게 파싱된 createdAt
                .build();
                
        } catch (Exception e) {
            log.warn("CSV 라인 파싱 실패: {} - 오류: {}", line.substring(0, Math.min(100, line.length())), e.getMessage());
            return null;
        }
    }
    
    /**
     * CSV 필드를 정확하게 파싱 (따옴표와 쉼표 처리)
     */
    private List<String> parseCSVFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // 연속된 따옴표는 이스케이프된 따옴표
                    currentField.append('"');
                    i++; // 다음 따옴표 스킵
                } else {
                    // 따옴표 상태 토글
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // 따옴표 밖의 쉼표는 필드 구분자
                fields.add(currentField.toString());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
        }
        
        // 마지막 필드 추가
        fields.add(currentField.toString());
        
        return fields;
    }
    
    /**
     * 디렉터리에서 가장 최신 파일 찾기
     */
    private String findLatestFile(String dirPath, String prefix) {
        try {
            Path dir = Paths.get(dirPath);
            if (!Files.exists(dir)) {
                return null;
            }
            
            return Files.list(dir)
                .filter(path -> path.getFileName().toString().startsWith(prefix))
                .filter(path -> path.getFileName().toString().endsWith(".csv"))
                .max((p1, p2) -> {
                    try {
                        // 파일명에서 타임스탬프 추출하여 비교
                        String name1 = p1.getFileName().toString();
                        String name2 = p2.getFileName().toString();
                        
                        // politics_detail_2025-08-19-14-30.csv 형태에서 타임스탬프 부분 추출
                        String timestamp1 = extractTimestamp(name1);
                        String timestamp2 = extractTimestamp(name2);
                        
                        return timestamp1.compareTo(timestamp2);
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .map(path -> path.getFileName().toString())
                .orElse(null);
                
        } catch (Exception e) {
            log.error("📁 최신 파일 찾기 실패: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 파일명에서 타임스탬프 추출
     */
    private String extractTimestamp(String fileName) {
        // politics_detail_2025-08-19-14-30.csv -> 2025-08-19-14-30
        int lastUnderscore = fileName.lastIndexOf('_');
        int lastDot = fileName.lastIndexOf('.');
        
        if (lastUnderscore != -1 && lastDot != -1 && lastUnderscore < lastDot) {
            return fileName.substring(lastUnderscore + 1, lastDot);
        }
        
        return fileName; // 파싱 실패시 원본 반환
    }
    
    /**
     * CSV 필드 언이스케이프
     */
    private String unescapeCSV(String value) {
        if (value == null) return null;
        return value.replace("\"\"", "\"").replace("\\n", "\n").replace("\\r", "\r");
    }
    
    /**
     * 연관뉴스 CSV 파일에서 데이터 조회 (타임스탬프 지정)
     */
    public List<RelatedNewsDetail> getRelatedNewsFromCsvWithTimestamp(String category, String timestamp) {
        String timePath = getCurrentTimePath();
        String dirPath = timePath + "/related";
        List<RelatedNewsDetail> relatedNewsList = new ArrayList<>();
        
        try {
            String fileName = category.toLowerCase() + "_related_" + timestamp + ".csv";
            String fullPath = basePath + "/" + dirPath + "/" + fileName;
            
            log.debug("📁 연관뉴스 파일서버 조회 시도: {}", fullPath);
            
            String csvContent = downloadFileFromServer(fullPath);
            if (csvContent != null) {
                relatedNewsList = parseRelatedNewsCsv(csvContent);
                log.info("📁 파일서버 조회 완료 (지정 타임스탬프): {} - 카테고리: {}, 개수: {}", fullPath, category, relatedNewsList.size());
            } else {
                log.info("📁 지정 타임스탬프 파일이 존재하지 않음: {}", fullPath);
            }
            
        } catch (Exception e) {
            log.error("📁 연관뉴스 파일서버 조회 실패 (지정 타임스탬프): {}/{} - 오류: {}", category, timestamp, e.getMessage());
        }
        
        return relatedNewsList;
    }
    
    /**
     * 연관뉴스 CSV 파일에서 데이터 조회 (최신 파일 자동 검색)
     */
    public List<RelatedNewsDetail> getRelatedNewsFromCsv(String category, String timePath) {
        if (timePath == null) {
            timePath = getCurrentTimePath();
        }
        
        String dirPath = timePath + "/related";
        List<RelatedNewsDetail> relatedNewsList = new ArrayList<>();
        
        try {
            // 최근 3시간간의 파일을 시도
            for (int i = 0; i < 180; i++) {
                String tryTimestamp = LocalDateTime.now().minusMinutes(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"));
                String fileName = category.toLowerCase() + "_related_" + tryTimestamp + ".csv";
                String tryFullPath = dirPath + "/" + fileName; // basePath 중복 제거
                
                log.debug("📁 연관뉴스 파일서버 조회 시도: {}", tryFullPath);
                
                String csvContent = downloadFileFromServer(tryFullPath);
                if (csvContent != null) {
                    relatedNewsList = parseRelatedNewsCsv(csvContent);
                    log.info("📁 연관뉴스 파일서버 조회 완료 ({}분 전): {} - 카테고리: {}, 개수: {}", i, tryFullPath, category, relatedNewsList.size());
                    break;
                }
            }
            
            // PM 경로에서 찾지 못했다면 AM 경로도 시도
            if (relatedNewsList.isEmpty()) {
                log.info("📁 PM 경로에서 연관뉴스 파일을 찾지 못함, AM 경로 시도: {}", dirPath);
                String amTimePath = timePath.replace("/pm/", "/am/").replace("_pm", "_am");
                String amDirPath = amTimePath + "/related";
                
                for (int i = 0; i < 180; i++) {
                    String tryTimestamp = LocalDateTime.now().minusMinutes(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"));
                    String fileName = category.toLowerCase() + "_related_" + tryTimestamp + ".csv";
                    String tryFullPath = amDirPath + "/" + fileName;
                    
                    log.debug("📁 연관뉴스 AM 경로 조회 시도: {}", tryFullPath);
                    
                    String csvContent = downloadFileFromServer(tryFullPath);
                    if (csvContent != null) {
                        relatedNewsList = parseRelatedNewsCsv(csvContent);
                        log.info("📁 연관뉴스 AM 경로에서 파일 발견 ({}분 전): {} - 카테고리: {}, 개수: {}", i, tryFullPath, category, relatedNewsList.size());
                        break;
                    }
                }
            }
            
            if (relatedNewsList.isEmpty()) {
                log.info("📁 연관뉴스 최신 파일을 찾을 수 없음: {}/{}_related_*.csv", dirPath, category.toLowerCase());
            }
            
        } catch (Exception e) {
            log.error("📁 연관뉴스 파일서버 조회 실패: {}/{} - 오류: {}", category, timePath, e.getMessage());
        }
        
        return relatedNewsList;
    }
    
    /**
     * 연관뉴스 CSV 내용 파싱
     */
    private List<RelatedNewsDetail> parseRelatedNewsCsv(String csvContent) {
        List<RelatedNewsDetail> relatedNewsList = new ArrayList<>();
        
        try {
            String[] lines = csvContent.split("\n");
            boolean isFirstLine = true;
            
            for (String line : lines) {
                if (isFirstLine) {
                    isFirstLine = false; // 헤더 스킵
                    continue;
                }
                
                if (line.trim().isEmpty()) continue;
                
                RelatedNewsDetail relatedNews = parseRelatedNewsCsvLine(line);
                if (relatedNews != null) {
                    relatedNewsList.add(relatedNews);
                }
            }
            
        } catch (Exception e) {
            log.error("📁 연관뉴스 CSV 파싱 실패: {}", e.getMessage());
        }
        
        return relatedNewsList;
    }
    
    /**
     * 연관뉴스 CSV 라인 파싱
     */
    private RelatedNewsDetail parseRelatedNewsCsvLine(String line) {
        try {
            if (line == null || line.trim().isEmpty()) {
                return null;
            }
            
            List<String> fields = parseCSVFields(line);
            if (fields.size() < 4) { // repOidAid, relatedOidAid, similarity, category 최소 4개
                log.debug("연관뉴스 CSV 필드 개수 부족: {} (필요: 4개, 실제: {}개)", line.substring(0, Math.min(50, line.length())), fields.size());
                return null;
            }
            
            Float similarity = 0.0f;
            try {
                String similarityStr = fields.get(2).trim();
                if (!similarityStr.isEmpty()) {
                    similarity = Float.parseFloat(similarityStr);
                }
            } catch (NumberFormatException e) {
                log.debug("similarity 값 파싱 실패, 기본값 사용: {}", fields.get(2));
            }
            
            return RelatedNewsDetail.builder()
                .repOidAid(unescapeCSV(fields.get(0)))
                .relatedOidAid(unescapeCSV(fields.get(1)))
                .similarity(similarity)
                .category(unescapeCSV(fields.get(3)))
                .createdAt(LocalDateTime.now()) // CSV에 createdAt이 없으면 현재 시간
                .build();
                
        } catch (Exception e) {
            log.warn("연관뉴스 CSV 라인 파싱 실패: {} - 오류: {}", line.substring(0, Math.min(100, line.length())), e.getMessage());
            return null;
        }
    }
}
