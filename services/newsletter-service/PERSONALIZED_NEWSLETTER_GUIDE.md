# 퍼스널라이즈드 뉴스레터 서비스 가이드

## 개요

이 문서는 **"구독 여부 + 선호 카테고리 + 뉴스레터 활성화 상태"**를 기준으로 퍼스널라이즈드 뉴스레터를 생성하는 서비스 오케스트레이션에 대한 가이드입니다.

## 아키텍처

```
Newsletter 서비스 ←(Feign)→ User/News 서비스
```

### 서비스 흐름

1. **사용자 인증 정보** → userId 확보
2. **내부 DB(SubscriptionRepository)** 에서 해당 userId의 구독 상태 확인
   - 없거나 INACTIVE면: 구독 유도/403 처리
3. **Feign(UserServiceClient)** 로 사용자 선호 카테고리 조회
   - 비어있으면 기본 카테고리/트렌딩으로 폴백
4. **Feign(NewsServiceClient)** 로 카테고리별 최신/인기 뉴스 조회
5. **개인화 규칙 적용** (선호 카테고리 우선, 남는 슬롯은 글로벌 인기/최신으로 채우기)
6. **NewsletterContent 조립** → EmailNewsletterRenderer 로 HTML 변환 or JSON 반환

## 구현된 컴포넌트

### 1. SubscriptionService
- 구독 상태 확인
- 활성 구독 정보 조회
- 개인화 뉴스레터 사용 여부 확인

### 2. NewsletterContentService (핵심 로직)
- 퍼스널라이즈드 뉴스레터 콘텐츠 생성
- 선호 카테고리 기반 뉴스 수집
- 폴백 로직 (인기/최신 뉴스)
- 중복 제거 및 개수 제한

### 3. NewsletterContentController
- JSON 응답: `/api/newsletter/{newsletterId}/content`
- HTML 응답: `/api/newsletter/{newsletterId}/html`
- 미리보기: `/api/newsletter/{newsletterId}/preview` (비구독자 처리 포함)

### 4. Feign 클라이언트 보강
- **NewsServiceClient**: 인기 뉴스, 카테고리별 최신 뉴스 API 추가
- **UserServiceClient**: 선호 카테고리 조회 API 활용
- **FeignConfig**: JWT 토큰 자동 전달, 에러 처리
- **FeignTimeoutConfig**: 타임아웃 및 재시도 설정

## API 엔드포인트

### 뉴스레터 콘텐츠 조회 (JSON)
```http
GET /api/newsletter/{newsletterId}/content
Authorization: Bearer {jwt_token}
```

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "newsletterId": 1,
    "userId": 123,
    "personalized": true,
    "title": "당신을 위한 맞춤 뉴스레터",
    "generatedAt": "2024-01-15T10:30:00",
    "sections": [
      {
        "heading": "당신을 위한 뉴스",
        "sectionType": "PERSONALIZED",
        "description": "관심 카테고리 기반으로 선별된 뉴스입니다.",
        "articles": [
          {
            "id": 1,
            "title": "AI 기술 발전 현황",
            "summary": "최신 AI 기술 동향...",
            "category": "IT_SCIENCE",
            "url": "https://example.com/news/1",
            "publishedAt": "2024-01-15T09:00:00",
            "imageUrl": "https://example.com/image1.jpg",
            "personalizedScore": 1.0
          }
        ]
      }
    ]
  }
}
```

### 뉴스레터 HTML 조회
```http
GET /api/newsletter/{newsletterId}/html
Authorization: Bearer {jwt_token}
```

### 뉴스레터 미리보기 (비구독자 처리)
```http
GET /api/newsletter/{newsletterId}/preview
Authorization: Bearer {jwt_token}
```

**비구독자 응답:**
```html
<h2>구독자 전용 컨텐츠입니다.</h2>
<p>뉴스레터 구독 후 이용해주세요.</p>
```

## 개인화 로직

### 1. 선호 카테고리 우선순위
- 사용자의 선호 카테고리 순서대로 뉴스 수집
- 카테고리당 최대 3개 뉴스
- 총 최대 8개 뉴스

### 2. 폴백 전략
- 선호 카테고리가 없거나 부족한 경우:
  1. 인기 뉴스로 보충
  2. 최신 뉴스로 보충

### 3. 중복 제거
- 동일한 뉴스 ID는 제외
- Set을 활용한 효율적인 중복 제거

## 에러 처리

### 1. 구독 상태 확인
- 비활성/미구독: `SUBSCRIPTION_REQUIRED` 에러
- 403 상태 코드 반환

### 2. 외부 서비스 장애
- User Service 장애: 빈 선호 카테고리로 폴백
- News Service 장애: 빈 뉴스 리스트로 폴백
- 로그 기록 및 모니터링

### 3. JWT 토큰
- FeignConfig에서 자동 전달
- 토큰 없으면 로그 기록

## 설정

### Feign 타임아웃 설정
```java
@Bean
public feign.Request.Options feignOptions() {
    return new feign.Request.Options(5000, 10000); // (connectTimeout, readTimeout)
}
```

### 재시도 설정
```java
@Bean
public Retryer feignRetryer() {
    return new Retryer.Default(100, 1000, 2); // (period, maxPeriod, maxAttempts)
}
```

## 필요한 News Service API

뉴스 서비스에서 다음 API들이 필요합니다:

```http
# 인기 뉴스 조회
GET /api/news/popular?size=8

# 카테고리별 최신 뉴스 조회
GET /api/news/by-category?category=IT_SCIENCE&size=3

# 최신 뉴스 조회 (기존)
GET /api/news/latest?size=8
```

## 모니터링 및 로깅

### 로그 레벨
- INFO: 주요 비즈니스 로직
- DEBUG: 상세한 처리 과정
- WARN: 외부 서비스 장애, 폴백 처리
- ERROR: 예상치 못한 오류

### 메트릭
- 뉴스레터 생성 성공/실패율
- 외부 서비스 응답 시간
- 개인화 vs 비개인화 비율

## 향후 개선 사항

1. **개인화 점수 계산**: 사용자 행동 데이터 기반 점수
2. **캐싱**: 자주 조회되는 뉴스 캐싱
3. **A/B 테스트**: 다양한 개인화 알고리즘 테스트
4. **실시간 업데이트**: 실시간 뉴스 피드 연동
5. **회로 차단기**: Resilience4j Circuit Breaker 적용

## 테스트

### 단위 테스트
```java
@Test
void testPersonalizedContentGeneration() {
    // given
    Long userId = 1L;
    Long newsletterId = 1L;
    
    // when
    NewsletterContent content = contentService.buildPersonalizedContent(userId, newsletterId);
    
    // then
    assertThat(content.isPersonalized()).isTrue();
    assertThat(content.getSections()).isNotEmpty();
}
```

### 통합 테스트
- Feign 클라이언트 모킹
- 외부 서비스 응답 시뮬레이션
- 에러 케이스 테스트
