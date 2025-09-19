package com.newsletterservice.service;

import com.newsletterservice.dto.EmailTemplate;
import com.newsletterservice.dto.NewsletterContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Optional;

/**
 * 이메일 전송 서비스
 */
@Slf4j
@Service
@ConditionalOnBean(JavaMailSender.class)
public class EmailService {
    
    private final Optional<JavaMailSender> mailSender;
    
    public EmailService(Optional<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }
    
    /**
     * 이메일 전송
     * 
     * @param to 수신자 이메일 주소
     * @param template 이메일 템플릿
     */
    public void sendEmail(String to, EmailTemplate template) {
        if (!StringUtils.hasText(to) || template == null) {
            log.warn("이메일 전송 실패: 수신자 또는 템플릿이 없습니다. to={}", to);
            return;
        }
        
        mailSender.ifPresentOrElse(
            sender -> {
                try {
                    MimeMessage message = sender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                    
                    helper.setTo(to);
                    helper.setSubject(template.getSubject());
                    
                    if (StringUtils.hasText(template.getHtmlContent())) {
                        helper.setText(template.getHtmlContent(), true);
                    } else if (StringUtils.hasText(template.getTextContent())) {
                        helper.setText(template.getTextContent(), false);
                    } else {
                        log.warn("이메일 내용이 없습니다: to={}", to);
                        return;
                    }
                    
                    sender.send(message);
                    log.info("이메일 전송 완료: to={}, subject={}", to, template.getSubject());
                    
                } catch (MessagingException e) {
                    log.error("이메일 전송 실패: to={}, subject={}", to, template.getSubject(), e);
                    throw new RuntimeException("이메일 전송에 실패했습니다.", e);
                }
            },
            () -> log.warn("JavaMailSender가 설정되지 않았습니다. 이메일 전송을 건너뜁니다. to={}", to)
        );
    }

    /**
     * 뉴스레터 템플릿 생성
     * 
     * @param content 뉴스레터 콘텐츠
     * @return 이메일 템플릿
     */
    public EmailTemplate createNewsletterTemplate(NewsletterContent content) {
        if (content == null) {
            throw new IllegalArgumentException("뉴스레터 콘텐츠가 없습니다.");
        }

        String subject = content.getTitle() != null ? content.getTitle() : "뉴스레터";
        String htmlContent = generateNewsletterHtml(content);
        
        return EmailTemplate.builder()
                .subject(subject)
                .htmlContent(htmlContent)
                .textContent(generateNewsletterText(content))
                .build();
    }

    /**
     * 대량 이메일 전송
     * 
     * @param recipients 수신자 이메일 목록
     * @param template 이메일 템플릿
     */
    public void sendBulkEmail(List<String> recipients, EmailTemplate template) {
        if (recipients == null || recipients.isEmpty()) {
            log.warn("수신자 목록이 비어있습니다.");
            return;
        }

        if (template == null) {
            log.warn("이메일 템플릿이 없습니다.");
            return;
        }

        log.info("대량 이메일 전송 시작: recipientCount={}, subject={}", 
                recipients.size(), template.getSubject());

        int successCount = 0;
        int failCount = 0;

        for (String recipient : recipients) {
            try {
                sendEmail(recipient, template);
                successCount++;
            } catch (Exception e) {
                log.error("이메일 전송 실패: recipient={}", recipient, e);
                failCount++;
            }
        }

        log.info("대량 이메일 전송 완료: success={}, fail={}", successCount, failCount);
    }

    /**
     * 테스트 이메일 전송
     * 
     * @param to 수신자 이메일
     * @param subject 제목
     * @param content 내용
     */
    public void sendTestEmail(String to, String subject, String content) {
        if (!StringUtils.hasText(to) || !StringUtils.hasText(subject) || !StringUtils.hasText(content)) {
            log.warn("테스트 이메일 전송 실패: 필수 정보가 없습니다. to={}, subject={}", to, subject);
            return;
        }

        EmailTemplate template = EmailTemplate.builder()
                .subject(subject)
                .textContent(content)
                .htmlContent("<p>" + content + "</p>")
                .build();

        sendEmail(to, template);
    }

    /**
     * 뉴스레터 HTML 콘텐츠 생성
     * 
     * @param content 뉴스레터 콘텐츠
     * @return HTML 문자열
     */
    private String generateNewsletterHtml(NewsletterContent content) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'>");
        html.append("<title>").append(content.getTitle()).append("</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        html.append(".header { background-color: #f4f4f4; padding: 20px; text-align: center; }");
        html.append(".section { margin: 20px 0; }");
        html.append(".article { border-bottom: 1px solid #eee; padding: 15px 0; }");
        html.append(".article h3 { margin: 0 0 10px 0; }");
        html.append(".article p { margin: 0; color: #666; }");
        html.append("</style></head><body>");
        
        html.append("<div class='header'>");
        html.append("<h1>").append(content.getTitle()).append("</h1>");
        html.append("</div>");

        if (content.getSections() != null) {
            for (NewsletterContent.Section section : content.getSections()) {
                html.append("<div class='section'>");
                html.append("<h2>").append(section.getHeading()).append("</h2>");
                
                if (section.getArticles() != null) {
                    for (NewsletterContent.Article article : section.getArticles()) {
                        html.append("<div class='article'>");
                        html.append("<h3>").append(article.getTitle()).append("</h3>");
                        if (StringUtils.hasText(article.getSummary())) {
                            html.append("<p>").append(article.getSummary()).append("</p>");
                        }
                        html.append("</div>");
                    }
                }
                html.append("</div>");
            }
        }

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * 뉴스레터 텍스트 콘텐츠 생성
     * 
     * @param content 뉴스레터 콘텐츠
     * @return 텍스트 문자열
     */
    private String generateNewsletterText(NewsletterContent content) {
        StringBuilder text = new StringBuilder();
        text.append(content.getTitle()).append("\n\n");

        if (content.getSections() != null) {
            for (NewsletterContent.Section section : content.getSections()) {
                text.append(section.getHeading()).append("\n");
                text.append("=".repeat(section.getHeading().length())).append("\n\n");
                
                if (section.getArticles() != null) {
                    for (NewsletterContent.Article article : section.getArticles()) {
                        text.append(article.getTitle()).append("\n");
                        if (StringUtils.hasText(article.getSummary())) {
                            text.append(article.getSummary()).append("\n");
                        }
                        text.append("\n");
                    }
                }
            }
        }

        return text.toString();
    }
}