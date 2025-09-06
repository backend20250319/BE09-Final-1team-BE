package com.newsletterservice.service;

import com.newsletterservice.common.exception.NewsletterException;
import com.newsletterservice.dto.NewsletterContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public final class KakaoMessageService {

    public void sendNewsletterMessage(NewsletterContent content) {
        try {
            // 이미지 URL 선택
            String featuredImage = selectFeaturedImage(content);

            // 템플릿 변수 설정
            Map<String, String> templateArgs = Map.of(
                    "FEATURED_IMAGE", featuredImage,
                    "NEWSLETTER_TITLE", content.getTitle(),
                    "USER_NAME", "구독자님"
            );

            // 카카오톡 메시지 전송
            sendMessage(123798L, templateArgs); // 실제 템플릿 ID로 변경

            log.info("카카오톡 뉴스레터 메시지 전송 완료: userId={}", content.getUserId());

        } catch (Exception e) {
            log.error("카카오톡 메시지 전송 실패: userId={}", content.getUserId(), e);
            throw new NewsletterException("카카오톡 메시지 전송에 실패했습니다.", "KAKAO_SEND_ERROR");
        }
    }

    private String selectFeaturedImage(NewsletterContent content) {
        // 섹션에서 첫 번째 이미지 찾기
        return content.getSections().stream()
                .flatMap(section -> section.getArticles().stream())
                .filter(article -> article.getImageUrl() != null && !article.getImageUrl().isEmpty())
                .findFirst()
                .map(NewsletterContent.Article::getImageUrl)
                .orElse("http://be09-final-1team-fe-env.eba-92qhhhzz.ap-northeast-2.elasticbeanstalk.com/images/newsletter-default.jpg");
    }

    private void sendMessage(Long templateId, Map<String, String> templateArgs) {
        try {
            log.info("카카오톡 메시지 전송 시작: templateId={}, args={}", templateId, templateArgs);

            // 실제 카카오톡 API 호출 로직
            // 1. 카카오 개발자 도구에서 설정한 템플릿 ID 사용
            // 2. templateArgs의 값들이 카카오 템플릿의 사용자 인자로 전달됨

            // 예시: RestTemplate이나 WebClient를 사용한 API 호출
        /*
        String kakaoApiUrl = "https://kapi.kakao.com/v2/api/talk/memo/send";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("template_id", templateId.toString());
        params.add("template_args", convertToJson(templateArgs));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        restTemplate.postForEntity(kakaoApiUrl, request, String.class);
        */

            // 현재는 로깅만 수행
            templateArgs.forEach((key, value) -> {
                log.info("템플릿 변수: {}={}", key, value);
            });

            log.info("카카오톡 메시지 전송 완료");

        } catch (Exception e) {
            log.error("카카오톡 메시지 전송 실패: templateId={}", templateId, e);
            throw new RuntimeException("카카오톡 메시지 전송 실패", e);
        }
    }

    private String convertToJson(Map<String, String> map) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            log.error("JSON 변환 실패", e);
            return "{}";
        }
    }
}