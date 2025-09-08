package com.newsletterservice.service;

import com.newsletterservice.dto.NewsletterContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 카카오톡 메시지 템플릿 서비스
 * 사용자 인자를 활용한 개인화된 뉴스레터 메시지 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoTemplateService {

    private final KakaoMessageService kakaoMessageService;

    /**
     * 개인화된 뉴스레터 카카오톡 메시지 발송
     */
    public void sendPersonalizedNewsletterMessage(Long userId, NewsletterContent content, String accessToken) {
        try {
            log.info("사용자 {} 개인화 뉴스레터 카카오톡 메시지 발송 시작", userId);

            // 1. 사용자 인자 구성
            Map<String, Object> templateArgs = buildPersonalizedTemplateArgs(userId, content);

            // 2. 뉴스레터 템플릿 ID (실제 템플릿 ID로 변경 필요)
            Long templateId = getNewsletterTemplateId(content.getType());

            // 3. 카카오톡 메시지 발송
            kakaoMessageService.sendMessage(accessToken, templateId, templateArgs);

            log.info("사용자 {} 개인화 뉴스레터 카카오톡 메시지 발송 완료", userId);

        } catch (Exception e) {
            log.error("사용자 {} 개인화 뉴스레터 카카오톡 메시지 발송 실패", userId, e);
            throw new RuntimeException("카카오톡 메시지 발송 실패", e);
        }
    }

    /**
     * 개인화된 템플릿 인자 구성
     */
    private Map<String, Object> buildPersonalizedTemplateArgs(Long userId, NewsletterContent content) {
        Map<String, Object> templateArgs = new HashMap<>();

        // 1. 사용자 정보
        templateArgs.put("user_name", getUserDisplayName(userId));
        templateArgs.put("user_id", userId.toString());

        // 2. 뉴스레터 정보
        templateArgs.put("newsletter_title", content.getTitle());
        templateArgs.put("newsletter_content", truncateContent(content.getContent(), 100));
        templateArgs.put("newsletter_date", getCurrentDate());

        // 3. 개인화된 링크
        templateArgs.put("newsletter_link", buildPersonalizedLink(userId, content));
        templateArgs.put("unsubscribe_link", buildUnsubscribeLink(userId));

        // 4. 카테고리별 개인화
        if (content.getCategory() != null) {
            templateArgs.put("category_name", getCategoryDisplayName(content.getCategory()));
            templateArgs.put("category_emoji", getCategoryEmoji(content.getCategory()));
        }

        // 5. 읽기 시간 추정
        templateArgs.put("estimated_read_time", estimateReadTime(content.getContent()));

        // 6. 버튼 정렬 (사용자 선호도에 따라)
        templateArgs.put("BUT", getUserButtonPreference(userId));

        log.info("템플릿 인자 구성 완료: userId={}, args={}", userId, templateArgs.keySet());

        return templateArgs;
    }

    /**
     * 뉴스레터 타입별 템플릿 ID 반환
     */
    private Long getNewsletterTemplateId(String type) {
        // 실제 템플릿 ID로 매핑 (카카오톡 메시지 템플릿 도구에서 생성한 ID)
        return switch (type != null ? type.toUpperCase() : "DEFAULT") {
            case "DAILY" -> 123798L; // 일일 뉴스레터 템플릿
            case "WEEKLY" -> 123799L; // 주간 뉴스레터 템플릿
            case "MONTHLY" -> 123800L; // 월간 뉴스레터 템플릿
            case "BREAKING" -> 123801L; // 속보 뉴스레터 템플릿
            default -> 123798L; // 기본 템플릿
        };
    }

    /**
     * 사용자 표시 이름 조회
     */
    private String getUserDisplayName(Long userId) {
        // 실제 구현: UserServiceClient를 통해 사용자 이름 조회
        return "구독자님"; // 임시 구현
    }

    /**
     * 콘텐츠 요약 (100자 제한)
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        return content.length() > maxLength ? 
            content.substring(0, maxLength) + "..." : content;
    }

    /**
     * 현재 날짜 문자열 반환
     */
    private String getCurrentDate() {
        return java.time.LocalDate.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
        );
    }

    /**
     * 개인화된 뉴스레터 링크 생성
     */
    private String buildPersonalizedLink(Long userId, NewsletterContent content) {
        // 실제 구현: 사용자별 개인화된 뉴스레터 페이지 링크
        return String.format("https://newsletter.example.com/personalized/%d/%s", 
            userId, content.getId());
    }

    /**
     * 구독 해지 링크 생성
     */
    private String buildUnsubscribeLink(Long userId) {
        // 실제 구현: 구독 해지 페이지 링크
        return String.format("https://newsletter.example.com/unsubscribe/%d", userId);
    }

    /**
     * 카테고리 표시 이름 반환
     */
    private String getCategoryDisplayName(String category) {
        return switch (category != null ? category.toUpperCase() : "GENERAL") {
            case "POLITICS" -> "정치";
            case "ECONOMY" -> "경제";
            case "SOCIETY" -> "사회";
            case "LIFE" -> "생활";
            case "INTERNATIONAL" -> "세계";
            case "IT_SCIENCE" -> "IT/과학";
            default -> "일반";
        };
    }

    /**
     * 카테고리별 이모지 반환
     */
    private String getCategoryEmoji(String category) {
        return switch (category != null ? category.toUpperCase() : "GENERAL") {
            case "POLITICS" -> "🏛️";
            case "ECONOMY" -> "💰";
            case "SOCIETY" -> "🏘️";
            case "LIFE" -> "🏠";
            case "INTERNATIONAL" -> "🌍";
            case "IT_SCIENCE" -> "💻";
            default -> "📰";
        };
    }

    /**
     * 읽기 시간 추정 (분 단위)
     */
    private String estimateReadTime(String content) {
        if (content == null) return "1분";
        
        // 평균 읽기 속도: 분당 200자
        int estimatedMinutes = Math.max(1, content.length() / 200);
        return estimatedMinutes + "분";
    }

    /**
     * 사용자 버튼 선호도 조회
     */
    private String getUserButtonPreference(Long userId) {
        // 실제 구현: 사용자 설정에서 버튼 정렬 선호도 조회
        // "0": 가로 정렬, "1": 세로 정렬
        return "0"; // 기본값: 가로 정렬
    }

    /**
     * 카테고리별 뉴스레터 메시지 발송
     */
    public void sendCategoryNewsletterMessage(Long userId, String category, NewsletterContent content, String accessToken) {
        try {
            log.info("사용자 {} 카테고리 {} 뉴스레터 카카오톡 메시지 발송 시작", userId, category);

            // 카테고리별 특화 템플릿 인자 구성
            Map<String, Object> templateArgs = buildPersonalizedTemplateArgs(userId, content);
            templateArgs.put("category_focus", "true");
            templateArgs.put("category_priority", getCategoryPriority(category));

            // 카테고리별 템플릿 ID
            Long templateId = getCategoryTemplateId(category);

            // 카카오톡 메시지 발송
            kakaoMessageService.sendMessage(accessToken, templateId, templateArgs);

            log.info("사용자 {} 카테고리 {} 뉴스레터 카카오톡 메시지 발송 완료", userId, category);

        } catch (Exception e) {
            log.error("사용자 {} 카테고리 {} 뉴스레터 카카오톡 메시지 발송 실패", userId, category, e);
            throw new RuntimeException("카테고리 뉴스레터 카카오톡 메시지 발송 실패", e);
        }
    }

    /**
     * 카테고리별 템플릿 ID 반환
     */
    private Long getCategoryTemplateId(String category) {
        return switch (category != null ? category.toUpperCase() : "GENERAL") {
            case "POLITICS" -> 123802L; // 정치 뉴스레터 템플릿
            case "ECONOMY" -> 123803L; // 경제 뉴스레터 템플릿
            case "SOCIETY" -> 123804L; // 사회 뉴스레터 템플릿
            case "LIFE" -> 123805L; // 생활 뉴스레터 템플릿
            case "INTERNATIONAL" -> 123806L; // 세계 뉴스레터 템플릿
            case "IT_SCIENCE" -> 123807L; // IT/과학 뉴스레터 템플릿
            default -> 123798L; // 기본 템플릿
        };
    }

    /**
     * 카테고리 우선순위 반환
     */
    private String getCategoryPriority(String category) {
        return switch (category != null ? category.toUpperCase() : "GENERAL") {
            case "POLITICS", "ECONOMY" -> "high";
            case "SOCIETY", "INTERNATIONAL" -> "medium";
            case "LIFE", "IT_SCIENCE" -> "normal";
            default -> "normal";
        };
    }
}
