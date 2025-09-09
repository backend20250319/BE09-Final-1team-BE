package com.newsletterservice.service;

import com.newsletterservice.dto.NewsletterContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNewsletterRenderer {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    /**
     * 뉴스레터 콘텐츠를 이메일용 HTML로 렌더링
     */
    public String renderToHtml(NewsletterContent content) {
        log.info("Rendering newsletter content to HTML for user: {}", content.getUserId());
        
        StringBuilder html = new StringBuilder();
        
        // HTML 헤더
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='ko'>\n");
        html.append("<head>\n");
        html.append("    <meta charset='UTF-8'>\n");
        html.append("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("    <title>").append(content.getTitle()).append("</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }\n");
        html.append("        .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n");
        html.append("        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }\n");
        html.append("        .header h1 { margin: 0; font-size: 24px; font-weight: 300; }\n");
        html.append("        .content { padding: 30px; }\n");
        html.append("        .section { margin-bottom: 30px; }\n");
        html.append("        .section-header { border-bottom: 2px solid #667eea; padding-bottom: 10px; margin-bottom: 20px; }\n");
        html.append("        .section-title { font-size: 20px; color: #333; margin: 0; }\n");
        html.append("        .section-description { color: #666; font-size: 14px; margin-top: 5px; }\n");
        html.append("        .article { border: 1px solid #e0e0e0; border-radius: 6px; padding: 15px; margin-bottom: 15px; background-color: #fafafa; }\n");
        html.append("        .article:hover { border-color: #667eea; box-shadow: 0 2px 8px rgba(102, 126, 234, 0.2); }\n");
        html.append("        .article-title { font-size: 16px; font-weight: 600; color: #333; margin: 0 0 8px 0; }\n");
        html.append("        .article-title a { color: #333; text-decoration: none; }\n");
        html.append("        .article-title a:hover { color: #667eea; }\n");
        html.append("        .article-summary { color: #666; font-size: 14px; line-height: 1.5; margin-bottom: 10px; }\n");
        html.append("        .article-meta { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: #999; }\n");
        html.append("        .article-category { background-color: #667eea; color: white; padding: 2px 8px; border-radius: 12px; font-size: 11px; }\n");
        html.append("        .personalized-badge { background-color: #ff6b6b; color: white; padding: 2px 6px; border-radius: 10px; font-size: 10px; margin-left: 5px; }\n");
        html.append("        .footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }\n");
        html.append("        .personalization-info { background: linear-gradient(135deg, #e3f2fd 0%, #f3e5f5 100%); padding: 20px; border-radius: 8px; margin-bottom: 20px; border-left: 4px solid #667eea; }\n");
        html.append("        .personalization-info h3 { margin: 0 0 10px 0; color: #333; font-size: 16px; }\n");
        html.append("        .personalization-info p { margin: 5px 0; color: #555; font-size: 14px; }\n");
        html.append("        .score-badge { display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 11px; font-weight: bold; margin-left: 8px; }\n");
        html.append("        .score-badge.personalized { background-color: #e8f5e8; color: #2e7d32; }\n");
        html.append("        .score-badge.trending { background-color: #fff3e0; color: #f57c00; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        // 컨테이너 시작
        html.append("<div class='container'>\n");
        
        // 헤더
        html.append("    <div class='header'>\n");
        html.append("        <h1>📰 ").append(content.getTitle()).append("</h1>\n");
        if (content.isPersonalized()) {
            html.append("        <p>🎯 당신만을 위한 맞춤 뉴스레터</p>\n");
        }
        html.append("        <p>").append(content.getGeneratedAt().format(DATE_FORMATTER)).append(" 발행</p>\n");
        html.append("    </div>\n");
        
        // 콘텐츠 시작
        html.append("    <div class='content'>\n");
        
        // 개인화 정보 (개인화된 경우)
        if (content.isPersonalized() && content.getPersonalizationInfo() != null) {
            html.append(renderPersonalizationInfo(content.getPersonalizationInfo()));
        }
        
        // 섹션들 렌더링
        for (NewsletterContent.Section section : content.getSections()) {
            html.append(renderSection(section));
        }
        
        html.append("    </div>\n");
        
        // 푸터
        html.append("    <div class='footer'>\n");
        html.append("        <p>이 뉴스레터는 자동으로 생성되었습니다.</p>\n");
        html.append("        <p>구독 해지나 설정 변경은 웹사이트에서 가능합니다.</p>\n");
        html.append("    </div>\n");
        
        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>");
        
        return html.toString();
    }

    /**
     * 뉴스레터 콘텐츠를 미리보기용 HTML로 렌더링
     */
    public String renderToPreviewHtml(NewsletterContent content) {
        log.info("Rendering newsletter content to preview HTML for user: {}", content.getUserId());
        
        StringBuilder html = new StringBuilder();
        
        // HTML 헤더
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='ko'>\n");
        html.append("<head>\n");
        html.append("    <meta charset='UTF-8'>\n");
        html.append("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("    <title>미리보기 - ").append(content.getTitle()).append("</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f0f2f5; }\n");
        html.append("        .preview-container { max-width: 800px; margin: 0 auto; background-color: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.15); }\n");
        html.append("        .preview-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 40px; text-align: center; position: relative; }\n");
        html.append("        .preview-badge { position: absolute; top: 20px; right: 20px; background-color: rgba(255,255,255,0.2); padding: 8px 16px; border-radius: 20px; font-size: 12px; font-weight: bold; }\n");
        html.append("        .preview-header h1 { margin: 0; font-size: 28px; font-weight: 300; }\n");
        html.append("        .preview-content { padding: 40px; }\n");
        html.append("        .preview-section { margin-bottom: 40px; }\n");
        html.append("        .preview-section-header { border-bottom: 3px solid #667eea; padding-bottom: 15px; margin-bottom: 25px; }\n");
        html.append("        .preview-section-title { font-size: 24px; color: #333; margin: 0; }\n");
        html.append("        .preview-section-description { color: #666; font-size: 16px; margin-top: 8px; }\n");
        html.append("        .preview-article { border: 2px solid #e0e0e0; border-radius: 8px; padding: 20px; margin-bottom: 20px; background-color: #fafafa; transition: all 0.3s ease; }\n");
        html.append("        .preview-article:hover { border-color: #667eea; box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3); transform: translateY(-2px); }\n");
        html.append("        .preview-article-title { font-size: 18px; font-weight: 600; color: #333; margin: 0 0 12px 0; }\n");
        html.append("        .preview-article-title a { color: #333; text-decoration: none; }\n");
        html.append("        .preview-article-title a:hover { color: #667eea; }\n");
        html.append("        .preview-article-summary { color: #666; font-size: 16px; line-height: 1.6; margin-bottom: 15px; }\n");
        html.append("        .preview-article-meta { display: flex; justify-content: space-between; align-items: center; font-size: 14px; color: #999; }\n");
        html.append("        .preview-article-category { background-color: #667eea; color: white; padding: 4px 12px; border-radius: 16px; font-size: 12px; font-weight: bold; }\n");
        html.append("        .preview-personalized-badge { background-color: #ff6b6b; color: white; padding: 4px 10px; border-radius: 12px; font-size: 11px; margin-left: 8px; }\n");
        html.append("        .preview-footer { background-color: #f8f9fa; padding: 30px; text-align: center; color: #666; font-size: 14px; }\n");
        html.append("        .preview-personalization-info { background-color: #e3f2fd; border-left: 5px solid #2196f3; padding: 20px; margin-bottom: 25px; border-radius: 6px; }\n");
        html.append("        .preview-stats { background-color: #f0f8ff; border: 1px solid #b3d9ff; border-radius: 8px; padding: 20px; margin-bottom: 25px; }\n");
        html.append("        .preview-stats h3 { margin: 0 0 15px 0; color: #0066cc; }\n");
        html.append("        .preview-stats ul { margin: 0; padding-left: 20px; }\n");
        html.append("        .preview-stats li { margin-bottom: 8px; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        // 미리보기 컨테이너 시작
        html.append("<div class='preview-container'>\n");
        
        // 미리보기 헤더
        html.append("    <div class='preview-header'>\n");
        html.append("        <div class='preview-badge'>미리보기</div>\n");
        html.append("        <h1>📰 ").append(content.getTitle()).append("</h1>\n");
        if (content.isPersonalized()) {
            html.append("        <p>🎯 당신만을 위한 맞춤 뉴스레터</p>\n");
        }
        html.append("        <p>").append(content.getGeneratedAt().format(DATE_FORMATTER)).append(" 발행 예정</p>\n");
        html.append("    </div>\n");
        
        // 미리보기 콘텐츠 시작
        html.append("    <div class='preview-content'>\n");
        
        // 개인화 정보 (개인화된 경우)
        if (content.isPersonalized()) {
            html.append("        <div class='preview-personalization-info'>\n");
            html.append("            <strong>🎯 개인화 정보</strong><br>\n");
            html.append("            이 뉴스레터는 당신의 관심사와 행동 패턴을 분석하여 맞춤 구성되었습니다.\n");
            html.append("        </div>\n");
        }
        
        // 미리보기 통계 정보
        html.append("        <div class='preview-stats'>\n");
        html.append("            <h3>📊 뉴스레터 구성 정보</h3>\n");
        html.append("            <ul>\n");
        html.append("                <li>총 섹션 수: ").append(content.getSections().size()).append("개</li>\n");
        html.append("                <li>총 기사 수: ").append(content.getSections().stream().mapToInt(s -> s.getArticles().size()).sum()).append("개</li>\n");
        html.append("                <li>개인화 여부: ").append(content.isPersonalized() ? "예" : "아니오").append("</li>\n");
        html.append("                <li>생성 시간: ").append(content.getGeneratedAt().format(DATE_FORMATTER)).append("</li>\n");
        html.append("            </ul>\n");
        html.append("        </div>\n");
        
        // 섹션들 렌더링
        for (NewsletterContent.Section section : content.getSections()) {
            html.append(renderPreviewSection(section));
        }
        
        html.append("    </div>\n");
        
        // 미리보기 푸터
        html.append("    <div class='preview-footer'>\n");
        html.append("        <p><strong>📧 이메일 발송 시 표시될 내용입니다.</strong></p>\n");
        html.append("        <p>실제 발송 전 미리보기로 확인하세요.</p>\n");
        html.append("        <p>구독 해지나 설정 변경은 웹사이트에서 가능합니다.</p>\n");
        html.append("    </div>\n");
        
        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>");
        
        return html.toString();
    }
    
    /**
     * 섹션 렌더링
     */
    private String renderSection(NewsletterContent.Section section) {
        StringBuilder html = new StringBuilder();
        
        html.append("        <div class='section'>\n");
        html.append("            <div class='section-header'>\n");
        html.append("                <h2 class='section-title'>").append(section.getHeading()).append("</h2>\n");
        if (section.getDescription() != null && !section.getDescription().isEmpty()) {
            html.append("                <p class='section-description'>").append(section.getDescription()).append("</p>\n");
        }
        html.append("            </div>\n");
        
        // 아티클들 렌더링
        for (NewsletterContent.Article article : section.getArticles()) {
            html.append(renderArticle(article, section.getSectionType()));
        }
        
        html.append("        </div>\n");
        
        return html.toString();
    }
    
    /**
     * 아티클 렌더링
     */
    private String renderArticle(NewsletterContent.Article article, String sectionType) {
        StringBuilder html = new StringBuilder();
        
        html.append("            <div class='article'>\n");
        
        // 제목
        html.append("                <h3 class='article-title'>\n");
        html.append("                    <a href='").append(article.getUrl()).append("' target='_blank'>\n");
        html.append("                        ").append(article.getTitle()).append("\n");
        if ("PERSONALIZED".equals(sectionType) && article.getPersonalizedScore() != null && article.getPersonalizedScore() > 0.7) {
            html.append("                        <span class='personalized-badge'>추천</span>\n");
        }
        html.append("                    </a>\n");
        html.append("                </h3>\n");
        
        // 요약
        if (article.getSummary() != null && !article.getSummary().isEmpty()) {
            html.append("                <p class='article-summary'>").append(article.getSummary()).append("</p>\n");
        }
        
        // 메타 정보
        html.append("                <div class='article-meta'>\n");
        html.append("                    <span class='article-category'>").append(convertCategoryToKorean(article.getCategory())).append("</span>\n");
        if (article.getPublishedAt() != null) {
            html.append("                    <span>").append(article.getPublishedAt().format(DATE_FORMATTER)).append("</span>\n");
        }
        
        // 개인화 점수 표시
        if (article.getPersonalizedScore() != null && article.getPersonalizedScore() > 0.7) {
            html.append("                    <span class='score-badge personalized'>개인화 추천</span>\n");
        }
        
        // 트렌드 점수 표시
        if (article.getTrendScore() != null && article.getTrendScore() > 0.8) {
            html.append("                    <span class='score-badge trending'>인기</span>\n");
        }
        
        html.append("                </div>\n");
        
        html.append("            </div>\n");
        
        return html.toString();
    }

    /**
     * 개인화 정보 렌더링
     */
    private String renderPersonalizationInfo(Map<String, Object> personalizationInfo) {
        StringBuilder html = new StringBuilder();
        
        html.append("        <div class='personalization-info'>\n");
        html.append("            <h3>🎯 개인화 정보</h3>\n");
        
        // 개인화 점수
        Object score = personalizationInfo.get("personalizationScore");
        if (score != null) {
            double scoreValue = (Double) score;
            String scoreText = String.format("%.1f", scoreValue * 100);
            html.append("            <p><strong>개인화 점수:</strong> ").append(scoreText).append("%</p>\n");
        }
        
        // 선호 카테고리
        @SuppressWarnings("unchecked")
        List<String> preferredCategories = (List<String>) personalizationInfo.get("preferredCategories");
        if (preferredCategories != null && !preferredCategories.isEmpty()) {
            String categories = preferredCategories.stream()
                    .map(this::convertCategoryToKorean)
                    .collect(Collectors.joining(", "));
            html.append("            <p><strong>관심 카테고리:</strong> ").append(categories).append("</p>\n");
        }
        
        // 읽기 기록
        Object totalReadCount = personalizationInfo.get("totalReadCount");
        if (totalReadCount != null) {
            html.append("            <p><strong>최근 읽은 뉴스:</strong> ").append(totalReadCount).append("개</p>\n");
        }
        
        html.append("            <p>이 뉴스레터는 당신의 관심사와 행동 패턴을 분석하여 맞춤 구성되었습니다.</p>\n");
        html.append("        </div>\n");
        
        return html.toString();
    }

    /**
     * 카테고리명을 한국어로 변환
     */
    private String convertCategoryToKorean(String englishCategory) {
        if (englishCategory == null) return "뉴스";
        
        return switch (englishCategory.toUpperCase()) {
            case "POLITICS" -> "정치";
            case "ECONOMY" -> "경제";
            case "SOCIETY" -> "사회";
            case "LIFE" -> "생활";
            case "INTERNATIONAL" -> "세계";
            case "IT_SCIENCE" -> "IT/과학";
            case "VEHICLE" -> "자동차/교통";
            case "TRAVEL_FOOD" -> "여행/음식";
            case "ART" -> "예술";
            default -> "뉴스";
        };
    }

    /**
     * 미리보기 섹션 렌더링
     */
    private String renderPreviewSection(NewsletterContent.Section section) {
        StringBuilder html = new StringBuilder();
        
        html.append("        <div class='preview-section'>\n");
        html.append("            <div class='preview-section-header'>\n");
        html.append("                <h2 class='preview-section-title'>").append(section.getHeading()).append("</h2>\n");
        if (section.getDescription() != null && !section.getDescription().isEmpty()) {
            html.append("                <p class='preview-section-description'>").append(section.getDescription()).append("</p>\n");
        }
        html.append("            </div>\n");
        
        if (section.getArticles() != null && !section.getArticles().isEmpty()) {
            for (NewsletterContent.Article article : section.getArticles()) {
                html.append(renderPreviewArticle(article, section.getSectionType()));
            }
        } else {
            html.append("            <p style='color: #999; font-style: italic;'>현재 뉴스를 불러올 수 없습니다.</p>\n");
        }
        
        html.append("        </div>\n");
        
        return html.toString();
    }

    /**
     * 미리보기 아티클 렌더링
     */
    private String renderPreviewArticle(NewsletterContent.Article article, String sectionType) {
        StringBuilder html = new StringBuilder();
        
        html.append("            <div class='preview-article'>\n");
        
        // 제목
        html.append("                <h3 class='preview-article-title'>\n");
        html.append("                    <a href='").append(article.getUrl()).append("' target='_blank'>\n");
        html.append("                        ").append(article.getTitle()).append("\n");
        if ("PERSONALIZED".equals(sectionType) && article.getPersonalizedScore() != null && article.getPersonalizedScore() > 0.7) {
            html.append("                        <span class='preview-personalized-badge'>추천</span>\n");
        }
        html.append("                    </a>\n");
        html.append("                </h3>\n");
        
        // 요약
        if (article.getSummary() != null && !article.getSummary().isEmpty()) {
            html.append("                <p class='preview-article-summary'>").append(article.getSummary()).append("</p>\n");
        }
        
        // 메타 정보
        html.append("                <div class='preview-article-meta'>\n");
        if (article.getCategory() != null) {
            html.append("                    <span class='preview-article-category'>").append(article.getCategory()).append("</span>\n");
        }
        if (article.getPublishedAt() != null) {
            html.append("                    <span>").append(article.getPublishedAt().format(DATE_FORMATTER)).append("</span>\n");
        }
        html.append("                </div>\n");
        
        html.append("            </div>\n");
        
        return html.toString();
    }
}
