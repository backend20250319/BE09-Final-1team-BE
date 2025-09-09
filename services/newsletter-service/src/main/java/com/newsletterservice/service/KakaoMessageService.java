package com.newsletterservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.newsletterservice.client.UserServiceClient;
import com.newsletterservice.client.dto.UserResponse;
import com.newsletterservice.common.exception.NewsletterException;
import com.newsletterservice.dto.*;
import org.springframework.util.StringUtils;
import com.newsletterservice.exception.KakaoMessageException;
import com.newsletterservice.model.PushMessage;
import com.newsletterservice.model.PushSubscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class KakaoMessageService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final KakaoApiService kakaoApiService;
    private final UserServiceClient userServiceClient;
    @Lazy
    private final Optional<EmailService> emailService;
    private final WebPushService webPushService;
    private final PermissionEmailTemplateService permissionEmailTemplateService;
    private final UserService userService;
    private final FeedTemplateService feedTemplateService;
    
    public KakaoMessageService(RestTemplate restTemplate, ObjectMapper objectMapper, 
                              KakaoApiService kakaoApiService, UserServiceClient userServiceClient,
                              @Lazy Optional<EmailService> emailService, WebPushService webPushService,
                              PermissionEmailTemplateService permissionEmailTemplateService,
                              UserService userService, FeedTemplateService feedTemplateService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.kakaoApiService = kakaoApiService;
        this.userServiceClient = userServiceClient;
        this.emailService = emailService;
        this.webPushService = webPushService;
        this.permissionEmailTemplateService = permissionEmailTemplateService;
        this.userService = userService;
        this.feedTemplateService = feedTemplateService;
    }
    
    @Value("${kakao.api.talk.memo.url:https://kapi.kakao.com/v2/api/talk/memo/send}")
    private String kakaoApiUrl;
    
    @Value("${kakao.api.talk.friends.url:https://kapi.kakao.com/v1/api/talk/friends/message/send}")
    private String kakaoFriendsApiUrl;
    
    @Value("${kakao.message.enabled:false}")
    private boolean messageEnabled;
    
    @Value("${kakao.templates.newsletter:123798}")
    private Long newsletterTemplateId;
    
    @Value("${kakao.templates.feed-a:123799}")
    private Long feedATemplateId;
    
    @Value("${kakao.templates.feed-b:123800}")
    private Long feedBTemplateId;

    /**
     * 뉴스레터 콘텐츠로 카카오톡 메시지 전송 (기존 호환성 유지)
     */
    public void sendNewsletterMessage(NewsletterContent content) {
        try {
            // 이미지 URL 선택
            String featuredImage = selectFeaturedImage(content);

            // 템플릿 변수 설정
            Map<String, Object> templateArgs = Map.of(
                    "FEATURED_IMAGE", featuredImage,
                    "NEWSLETTER_TITLE", content.getTitle(),
                    "USER_NAME", "구독자님"
            );

            // 개발/테스트 환경에서는 시뮬레이션
            if (!messageEnabled) {
                log.info("카카오 메시지 기능이 비활성화되어 있습니다. 시뮬레이션 모드로 실행합니다.");
                simulateMessageSending(templateArgs);
                return;
            }

            // 실제 전송을 위해서는 accessToken이 필요하지만, 기존 호환성을 위해 시뮬레이션
            log.warn("sendNewsletterMessage(NewsletterContent)는 accessToken이 필요합니다. 시뮬레이션 모드로 실행합니다.");
            simulateMessageSending(templateArgs);

            log.info("카카오톡 뉴스레터 메시지 전송 완료: userId={}", content.getUserId());

        } catch (Exception e) {
            log.error("카카오톡 메시지 전송 실패: userId={}", content.getUserId(), e);
            throw new NewsletterException("카카오톡 메시지 전송에 실패했습니다.", "KAKAO_SEND_ERROR");
        }
    }
    
    /**
     * 시뮬레이션 메시지 전송 (개발/테스트용)
     */
    private void simulateMessageSending(Map<String, Object> templateArgs) {
        log.info("카카오톡 메시지 전송 시뮬레이션 시작");
        templateArgs.forEach((key, value) -> {
            log.info("템플릿 변수: {}={}", key, value);
        });
        log.info("카카오톡 메시지 전송 시뮬레이션 완료 (개발 환경)");
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

    
    public void sendMessage(String accessToken, Long templateId, Map<String, Object> templateArgs) {
        if (!messageEnabled) {
            log.info("카카오 메시지 기능이 비활성화되어 있습니다.");
            return;
        }
        
        validateInputs(accessToken, templateId, templateArgs);
        
        try {
            // JavaScript SDK Kakao.API.request의 data 파라미터 구성
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            
            // data: 사용자 정의 템플릿으로 나에게 메시지 발송
            // 1. template_id (필수) - Number 타입
            //    메시지 템플릿 도구에서 구성한 사용자 정의 템플릿의 ID
            params.add("template_id", templateId.toString());
            
            // 2. template_args (선택) - Object 타입, key:value 형식
            //    template_id로 지정한 템플릿에 사용자 인자(User argument)가 포함되어 있는 경우 대입할 값
            if (templateArgs != null && !templateArgs.isEmpty()) {
                String templateArgsJson = objectMapper.writeValueAsString(templateArgs);
                params.add("template_args", templateArgsJson);
            }
            
            HttpHeaders headers = buildHeaders(accessToken);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            // url: "/v2/api/talk/memo/send" (고정)
            ResponseEntity<String> response = restTemplate.postForEntity(kakaoApiUrl, request, String.class);
            
            // success: API 호출이 성공할 때 실행되는 콜백 함수 (서버사이드에서는 로그로 처리)
            handleResponse(response, templateId);
            log.info("나에게 보내기 메시지 전송 성공: templateId={}", templateId);
            
        } catch (RestClientException e) {
            // fail: API 호출이 실패할 때 실행되는 콜백 함수 (서버사이드에서는 예외로 처리)
            log.error("나에게 보내기 메시지 전송 실패: templateId={}, error={}", templateId, e.getMessage());
            
            // -402 에러 (insufficient scopes) 처리
            if (e.getMessage().contains("-402")) {
                log.warn("카카오톡 메시지 전송 권한 부족: templateId={}", templateId);
                throw new KakaoMessageException("카카오톡 메시지 전송 권한이 필요합니다. 추가 동의가 필요합니다.", "INSUFFICIENT_SCOPES", e);
            }
            
            throw new KakaoMessageException("나에게 보내기 메시지 전송 중 네트워크 오류", e);
        } catch (Exception e) {
            // fail: API 호출이 실패할 때 실행되는 콜백 함수 (서버사이드에서는 예외로 처리)
            log.error("나에게 보내기 메시지 전송 중 예상치 못한 오류: templateId={}", templateId, e);
            throw new KakaoMessageException("나에게 보내기 메시지 전송 실패", e);
        } finally {
            // always: API 호출 성공 여부에 관계없이 항상 호출되는 콜백 함수 (서버사이드에서는 finally로 처리)
            log.debug("나에게 보내기 메시지 전송 요청 완료: templateId={}", templateId);
        }
    }
    
    /**
     * 뉴스레터 메시지 전송 (간편 메서드)
     */
    public void sendNewsletterMessage(String accessToken, String title, String summary, String url) {
        Map<String, Object> templateArgs = Map.of(
            "title", title,
            "summary", summary,
            "url", url
        );
        
        sendMessage(accessToken, newsletterTemplateId, templateArgs);
    }
    
    // ========================================
    // Private Helper Methods
    // ========================================
    
    private void validateInputs(String accessToken, Long templateId, Map<String, Object> templateArgs) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Access token은 필수입니다");
        }
        
        if (templateId == null) {
            throw new IllegalArgumentException("Template ID는 필수입니다");
        }
        
        if (templateArgs == null) {
            throw new IllegalArgumentException("Template arguments는 필수입니다");
        }
    }
    
    private MultiValueMap<String, String> buildRequestParams(Long templateId, Map<String, Object> templateArgs) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("template_id", templateId.toString());
        
        try {
            String templateArgsJson = objectMapper.writeValueAsString(templateArgs);
            params.add("template_args", templateArgsJson);
        } catch (Exception e) {
            throw new KakaoMessageException("Template arguments JSON 변환 실패", e);
        }
        
        return params;
    }
    
    private HttpHeaders buildHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("User-Agent", "NewsletterService/1.0");
        return headers;
    }
    
    private void handleResponse(ResponseEntity<String> response, Long templateId) {
        if (response.getStatusCode().is2xxSuccessful()) {
            log.info("카카오톡 메시지 전송 성공: templateId={}", templateId);
        } else {
            String errorMsg = String.format("카카오톡 메시지 전송 실패: status=%s, body=%s", 
                    response.getStatusCode(), response.getBody());
            log.error(errorMsg);
            throw new KakaoMessageException(errorMsg);
        }
    }


    /**
     * 나에게 뉴스레터 전송
     */
    public void sendNewsletterToMe(NewsletterContent content, String accessToken) {
        try {
            log.info("나에게 뉴스레터 전송 시작: title={}", content.getTitle());
            
            // 이미지 URL 선택
            String featuredImage = selectFeaturedImage(content);

            // 템플릿 변수 설정
            Map<String, Object> templateArgs = Map.of(
                    "FEATURED_IMAGE", featuredImage,
                    "NEWSLETTER_TITLE", content.getTitle(),
                    "USER_NAME", "구독자님"
            );

            // 카카오톡 나에게 보내기 메시지 전송
            sendMessage(accessToken, newsletterTemplateId, templateArgs);

            log.info("나에게 뉴스레터 전송 완료: title={}", content.getTitle());

        } catch (Exception e) {
            log.error("나에게 뉴스레터 전송 실패: title={}", content.getTitle(), e);
            throw new NewsletterException("나에게 뉴스레터 전송에 실패했습니다.", "KAKAO_SEND_TO_ME_ERROR");
        }
    }

    /**
     * 친구들에게 뉴스레터 전송
     */
    public void sendNewsletterToFriends(NewsletterContent content, String accessToken) {
        try {
            log.info("친구들에게 뉴스레터 전송 시작: title={}", content.getTitle());
            
            // 이미지 URL 선택
            String featuredImage = selectFeaturedImage(content);

            // 템플릿 변수 설정
            Map<String, Object> templateArgs = Map.of(
                    "FEATURED_IMAGE", featuredImage,
                    "NEWSLETTER_TITLE", content.getTitle(),
                    "USER_NAME", "구독자님"
            );

            // 카카오톡 친구들에게 메시지 전송
            sendMessage(accessToken, newsletterTemplateId, templateArgs);

            log.info("친구들에게 뉴스레터 전송 완료: title={}", content.getTitle());

        } catch (Exception e) {
            log.error("친구들에게 뉴스레터 전송 실패: title={}", content.getTitle(), e);
            throw new NewsletterException("친구들에게 뉴스레터 전송에 실패했습니다.", "KAKAO_SEND_TO_FRIENDS_ERROR");
        }
    }
    
   
    public void sendMessageToFriends(String accessToken, Long templateId, 
                                   List<String> receiverUuids, Map<String, Object> templateArgs) {
        if (!messageEnabled) {
            log.info("카카오 메시지 기능이 비활성화되어 있습니다.");
            return;
        }
        
        validateInputs(accessToken, templateId, templateArgs);
        
        if (receiverUuids == null || receiverUuids.isEmpty()) {
            throw new IllegalArgumentException("받는 사람 UUID 목록은 필수입니다");
        }
        
        try {
            // JavaScript SDK Kakao.API.request의 data 파라미터 구성
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            
            // data: API에 전달할 파라미터
            // 1. template_id (필수) - Number 타입
            params.add("template_id", templateId.toString());
            
            // 2. receiver_uuids (필수) - String[] 타입, 최대 5개
            if (receiverUuids.size() > 5) {
                throw new IllegalArgumentException("receiver_uuids는 최대 5개까지 가능합니다. 현재: " + receiverUuids.size());
            }
            
            // receiver_uuids를 JSON 배열로 변환
            String receiverUuidsJson = objectMapper.writeValueAsString(receiverUuids);
            params.add("receiver_uuids", receiverUuidsJson);
            
            // 3. template_args (선택) - Object 타입, key:value 형식
            if (templateArgs != null && !templateArgs.isEmpty()) {
                String templateArgsJson = objectMapper.writeValueAsString(templateArgs);
                params.add("template_args", templateArgsJson);
            }
            
            HttpHeaders headers = buildHeaders(accessToken);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            // url: "/v1/api/talk/friends/message/send" (고정)
            ResponseEntity<String> response = restTemplate.postForEntity(kakaoFriendsApiUrl, request, String.class);
            
            // success: API 호출이 성공할 때 실행되는 콜백 함수 (서버사이드에서는 로그로 처리)
            handleResponse(response, templateId);
            log.info("친구들에게 메시지 전송 성공: templateId={}, receivers={}", templateId, receiverUuids.size());
            
        } catch (RestClientException e) {
            // fail: API 호출이 실패할 때 실행되는 콜백 함수 (서버사이드에서는 예외로 처리)
            log.error("친구들에게 메시지 전송 실패: templateId={}, error={}", templateId, e.getMessage());
            throw new KakaoMessageException("친구들에게 메시지 전송 중 네트워크 오류", e);
        } catch (Exception e) {
            // fail: API 호출이 실패할 때 실행되는 콜백 함수 (서버사이드에서는 예외로 처리)
            log.error("친구들에게 메시지 전송 중 예상치 못한 오류: templateId={}", templateId, e);
            throw new KakaoMessageException("친구들에게 메시지 전송 실패", e);
        } finally {
            // always: API 호출 성공 여부에 관계없이 항상 호출되는 콜백 함수 (서버사이드에서는 finally로 처리)
            log.debug("친구 메시지 전송 요청 완료: templateId={}", templateId);
        }
    }
    
    /**
     * 친구들에게 뉴스레터 전송 (JavaScript SDK 방식)
     */
    public void sendNewsletterToFriendsWithUuids(String accessToken, String title, String summary, 
                                               String url, List<String> receiverUuids) {
        Map<String, Object> templateArgs = Map.of(
            "title", title,
            "summary", summary,
            "url", url
        );
        
        sendMessageToFriends(accessToken, newsletterTemplateId, receiverUuids, templateArgs);
    }

    /**
     * 권한 확인 후 뉴스레터 전송 (권한 없으면 대체 전송)
     */
    public void sendNewsletterWithFallback(NewsletterContent content, String accessToken) {
        try {
            // 1. 카카오톡 메시지 권한 확인
            boolean hasPermission = checkTalkMessagePermission(accessToken);
            
            if (hasPermission) {
                // 권한이 있으면 카카오톡으로 전송 시도
                try {
                    sendNewsletterToMe(content, accessToken);
                    log.info("카카오톡 뉴스레터 전송 완료: title={}", content.getTitle());
                    return;
                } catch (KakaoMessageException e) {
                    if ("INSUFFICIENT_SCOPES".equals(e.getErrorCode())) {
                        // -402 에러: 권한 부족 - 대체 전송
                        log.warn("카카오톡 메시지 전송 권한 부족, 대체 방식으로 전송: title={}", content.getTitle());
                        sendFallbackNewsletter(content);
                        return;
                    }
                    throw e; // 다른 에러는 재throw
                }
            } else {
                // 권한이 없으면 대체 전송
                sendFallbackNewsletter(content);
                log.info("대체 방식으로 뉴스레터 전송 완료: title={}", content.getTitle());
            }
            
        } catch (Exception e) {
            log.error("뉴스레터 전송 실패, 대체 방식으로 재시도: title={}", content.getTitle(), e);
            // 카카오톡 전송 실패 시 대체 전송
            sendFallbackNewsletter(content);
        }
    }

    /**
     * 카카오톡 메시지 전송 권한 확인
     */
    private boolean checkTalkMessagePermission(String accessToken) {
        try {
            log.info("카카오톡 메시지 전송 권한 확인 중...");
            return kakaoApiService.hasTalkMessagePermission(accessToken);
        } catch (Exception e) {
            log.warn("카카오톡 메시지 권한 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 대체 방식으로 뉴스레터 전송 (푸시 알림, 앱 내 알림 등)
     */
    private void sendFallbackNewsletter(NewsletterContent content) {
        try {
            log.info("대체 방식으로 뉴스레터 전송 시작: title={}", content.getTitle());
            
            // 1. 웹 푸시 알림 전송 (선택사항)
            sendWebPushNotification(content);
            
            
            log.info("대체 방식 뉴스레터 전송 완료: title={}", content.getTitle());
            
        } catch (Exception e) {
            log.error("대체 방식 뉴스레터 전송 실패: title={}", content.getTitle(), e);
            throw new NewsletterException("대체 방식 뉴스레터 전송에 실패했습니다.", "FALLBACK_SEND_ERROR");
        }
    }


    /**
     * 웹 푸시 알림 전송
     */
    private void sendWebPushNotification(NewsletterContent content) {
        try {
            log.info("웹 푸시 알림 전송: title={}", content.getTitle());

            // 웹 푸시 구독자 토큰 조회
            List<PushSubscription> subscriptions = userService.getWebPushSubscriptions();
            
            if (subscriptions.isEmpty()) {
                log.info("웹 푸시 구독자가 없습니다.");
                return;
            }

            // 푸시 알림 메시지 생성
            PushMessage pushMessage = PushMessage.forNewsletter(
                    content.getTitle(),
                    truncateText(content.getSummary(), 100),
                    String.valueOf(content.getNewsletterId())
            );

            // 배치로 푸시 알림 전송
            List<List<PushSubscription>> batches = Lists.partition(subscriptions, 500);
            
            for (List<PushSubscription> batch : batches) {
                CompletableFuture.runAsync(() -> {
                    try {
                        webPushService.sendBulkNotification(batch, pushMessage);
                        log.info("웹 푸시 배치 전송 완료: {} 명", batch.size());
                    } catch (Exception e) {
                        log.error("웹 푸시 배치 전송 실패", e);
                    }
                });
            }

            log.info("웹 푸시 알림 전송 완료: title={}, 총 {} 명", content.getTitle(), subscriptions.size());

        } catch (Exception e) {
            log.error("웹 푸시 알림 전송 실패: title={}", content.getTitle(), e);
            // 푸시 알림 전송 실패는 전체 프로세스를 중단시키지 않음
        }
    }


    /**
     * 텍스트를 지정된 길이로 자르기
     */
    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }


    /**
     * 권한 부족 시 사용자에게 알림
     */
    public void notifyPermissionNeeded(Long userId) {
        try {
            log.info("사용자에게 카카오톡 권한 필요 알림: userId={}", userId);

            UserResponse user = userService.getUser(userId);
            
            // 이메일로 권한 설정 안내
            if (StringUtils.hasText(user.getEmail())) {
                emailService.ifPresent(service -> {
                    EmailTemplate emailTemplate = EmailTemplate.builder()
                            .subject("카카오톡 알림 권한 설정 안내")
                            .htmlContent(permissionEmailTemplateService.generatePermissionEmailHtml(user))
                            .build();
                    
                    service.sendEmail(user.getEmail(), emailTemplate);
                });
            }

            // 앱 내 알림 기능은 제거되었습니다.
            // TODO: user-service에서 알림 기능 구현 필요

            // 웹 푸시로 권한 설정 안내 (웹 푸시 권한이 있는 경우)
            if (user.hasWebPushPermission()) {
                PushMessage pushMessage = PushMessage.builder()
                        .title("카카오톡 알림 권한 필요")
                        .body("뉴스레터 알림을 받으려면 권한을 설정해주세요")
                        .url("/settings/permissions")
                        .build();
                
                webPushService.sendToUser(userId, pushMessage);
            }

            log.info("권한 필요 알림 전송 완료: userId={}", userId);

        } catch (Exception e) {
            log.error("권한 필요 알림 전송 실패: userId={}", userId, e);
        }
    }
    
    /**
     * 피드 A형 템플릿으로 뉴스레터 전송
     */
    public void sendFeedAMessage(Long userId, String accessToken) {
        try {
            log.info("피드 A형 뉴스레터 전송 시작: userId={}", userId);
            
            // 개인화된 피드 A형 템플릿 생성
            FeedTemplate feedTemplate = feedTemplateService.createPersonalizedFeedTemplate(
                    userId, FeedTemplate.FeedType.FEED_A);
            
            // 카카오톡 API용 템플릿 변수 생성
            Map<String, Object> templateArgs = feedTemplate.toKakaoTemplateArgs();
            
            // 개발/테스트 환경에서는 시뮬레이션
            if (!messageEnabled) {
                log.info("카카오 메시지 기능이 비활성화되어 있습니다. 시뮬레이션 모드로 실행합니다.");
                simulateMessageSending(templateArgs);
                return;
            }
            
            // 카카오톡 메시지 전송
            sendMessage(accessToken, feedATemplateId, templateArgs);
            
            log.info("피드 A형 뉴스레터 전송 완료: userId={}", userId);
            
        } catch (Exception e) {
            log.error("피드 A형 뉴스레터 전송 실패: userId={}", userId, e);
            throw new NewsletterException("피드 A형 뉴스레터 전송에 실패했습니다.", "FEED_A_SEND_ERROR");
        }
    }
    
    /**
     * 피드 B형 템플릿으로 뉴스레터 전송
     */
    public void sendFeedBMessage(Long userId, String accessToken) {
        try {
            log.info("피드 B형 뉴스레터 전송 시작: userId={}", userId);
            
            // 개인화된 피드 B형 템플릿 생성
            FeedTemplate feedTemplate = feedTemplateService.createPersonalizedFeedTemplate(
                    userId, FeedTemplate.FeedType.FEED_B);
            
            // 카카오톡 API용 템플릿 변수 생성
            Map<String, Object> templateArgs = feedTemplate.toKakaoTemplateArgs();
            
            // 개발/테스트 환경에서는 시뮬레이션
            if (!messageEnabled) {
                log.info("카카오 메시지 기능이 비활성화되어 있습니다. 시뮬레이션 모드로 실행합니다.");
                simulateMessageSending(templateArgs);
                return;
            }
            
            // 카카오톡 메시지 전송
            sendMessage(accessToken, feedBTemplateId, templateArgs);
            
            log.info("피드 B형 뉴스레터 전송 완료: userId={}", userId);
            
        } catch (Exception e) {
            log.error("피드 B형 뉴스레터 전송 실패: userId={}", userId, e);
            throw new NewsletterException("피드 B형 뉴스레터 전송에 실패했습니다.", "FEED_B_SEND_ERROR");
        }
    }
    
    /**
     * 카테고리별 피드 A형 템플릿으로 뉴스레터 전송
     */
    public void sendCategoryFeedAMessage(String category, String accessToken) {
        try {
            log.info("카테고리별 피드 A형 뉴스레터 전송 시작: category={}", category);
            
            // 카테고리별 피드 A형 템플릿 생성
            FeedTemplate feedTemplate = feedTemplateService.createCategoryFeedTemplate(
                    category, FeedTemplate.FeedType.FEED_A);
            
            // 카카오톡 API용 템플릿 변수 생성
            Map<String, Object> templateArgs = feedTemplate.toKakaoTemplateArgs();
            
            // 개발/테스트 환경에서는 시뮬레이션
            if (!messageEnabled) {
                log.info("카카오 메시지 기능이 비활성화되어 있습니다. 시뮬레이션 모드로 실행합니다.");
                simulateMessageSending(templateArgs);
                return;
            }
            
            // 카카오톡 메시지 전송
            sendMessage(accessToken, feedATemplateId, templateArgs);
            
            log.info("카테고리별 피드 A형 뉴스레터 전송 완료: category={}", category);
            
        } catch (Exception e) {
            log.error("카테고리별 피드 A형 뉴스레터 전송 실패: category={}", category, e);
            throw new NewsletterException("카테고리별 피드 A형 뉴스레터 전송에 실패했습니다.", "CATEGORY_FEED_A_SEND_ERROR");
        }
    }
    
    /**
     * 카테고리별 피드 B형 템플릿으로 뉴스레터 전송
     */
    public void sendCategoryFeedBMessage(String category, String accessToken) {
        try {
            log.info("카테고리별 피드 B형 뉴스레터 전송 시작: category={}", category);
            
            // 카테고리별 피드 B형 템플릿 생성
            FeedTemplate feedTemplate = feedTemplateService.createCategoryFeedTemplate(
                    category, FeedTemplate.FeedType.FEED_B);
            
            // 카카오톡 API용 템플릿 변수 생성
            Map<String, Object> templateArgs = feedTemplate.toKakaoTemplateArgs();
            
            // 개발/테스트 환경에서는 시뮬레이션
            if (!messageEnabled) {
                log.info("카카오 메시지 기능이 비활성화되어 있습니다. 시뮬레이션 모드로 실행합니다.");
                simulateMessageSending(templateArgs);
                return;
            }
            
            // 카카오톡 메시지 전송
            sendMessage(accessToken, feedBTemplateId, templateArgs);
            
            log.info("카테고리별 피드 B형 뉴스레터 전송 완료: category={}", category);
            
        } catch (Exception e) {
            log.error("카테고리별 피드 B형 뉴스레터 전송 실패: category={}", category, e);
            throw new NewsletterException("카테고리별 피드 B형 뉴스레터 전송에 실패했습니다.", "CATEGORY_FEED_B_SEND_ERROR");
        }
    }
    
    /**
     * 트렌딩 뉴스 피드 A형 템플릿으로 뉴스레터 전송
     */
    public void sendTrendingFeedAMessage(String accessToken) {
        try {
            log.info("트렌딩 뉴스 피드 A형 뉴스레터 전송 시작");
            
            // 트렌딩 뉴스 피드 A형 템플릿 생성
            FeedTemplate feedTemplate = feedTemplateService.createTrendingFeedTemplate(
                    FeedTemplate.FeedType.FEED_A);
            
            // 카카오톡 API용 템플릿 변수 생성
            Map<String, Object> templateArgs = feedTemplate.toKakaoTemplateArgs();
            
            // 개발/테스트 환경에서는 시뮬레이션
            if (!messageEnabled) {
                log.info("카카오 메시지 기능이 비활성화되어 있습니다. 시뮬레이션 모드로 실행합니다.");
                simulateMessageSending(templateArgs);
                return;
            }
            
            // 카카오톡 메시지 전송
            sendMessage(accessToken, feedATemplateId, templateArgs);
            
            log.info("트렌딩 뉴스 피드 A형 뉴스레터 전송 완료");
            
        } catch (Exception e) {
            log.error("트렌딩 뉴스 피드 A형 뉴스레터 전송 실패", e);
            throw new NewsletterException("트렌딩 뉴스 피드 A형 뉴스레터 전송에 실패했습니다.", "TRENDING_FEED_A_SEND_ERROR");
        }
    }
    
    /**
     * 트렌딩 뉴스 피드 B형 템플릿으로 뉴스레터 전송
     */
    public void sendTrendingFeedBMessage(String accessToken) {
        try {
            log.info("트렌딩 뉴스 피드 B형 뉴스레터 전송 시작");
            
            // 트렌딩 뉴스 피드 B형 템플릿 생성
            FeedTemplate feedTemplate = feedTemplateService.createTrendingFeedTemplate(
                    FeedTemplate.FeedType.FEED_B);
            
            // 카카오톡 API용 템플릿 변수 생성
            Map<String, Object> templateArgs = feedTemplate.toKakaoTemplateArgs();
            
            // 개발/테스트 환경에서는 시뮬레이션
            if (!messageEnabled) {
                log.info("카카오 메시지 기능이 비활성화되어 있습니다. 시뮬레이션 모드로 실행합니다.");
                simulateMessageSending(templateArgs);
                return;
            }
            
            // 카카오톡 메시지 전송
            sendMessage(accessToken, feedBTemplateId, templateArgs);
            
            log.info("트렌딩 뉴스 피드 B형 뉴스레터 전송 완료");
            
        } catch (Exception e) {
            log.error("트렌딩 뉴스 피드 B형 뉴스레터 전송 실패", e);
            throw new NewsletterException("트렌딩 뉴스 피드 B형 뉴스레터 전송에 실패했습니다.", "TRENDING_FEED_B_SEND_ERROR");
        }
    }
}