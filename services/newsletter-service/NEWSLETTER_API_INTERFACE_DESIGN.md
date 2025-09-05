# 뉴스레터 서비스 API 인터페이스 설계서

## 1. 개요

### 1.1 서비스 개요
- **서비스명**: 뉴스레터 서비스 (Newsletter Service)
- **기본 URL**: `/api/newsletter`
- **설명**: 개인화된 뉴스레터 구독, 발송, 통계 관리 서비스

### 1.2 주요 기능
- 구독 관리 (구독, 해지, 상태 변경)
- 뉴스레터 발송 (즉시/예약)
- 콘텐츠 조회 (JSON/HTML)
- 통계 및 분석
- 카테고리별 헤드라인 조회

## 2. 공통 응답 형식

### 2.1 ApiResponse<T> 구조
```json
{
  "success": boolean,
  "data": T,
  "errorCode": string,
  "message": string
}
```

### 2.2 인증
- **방식**: JWT Bearer Token
- **헤더**: `Authorization: Bearer {token}`
- **토큰 추출**: `extractUserIdFromToken()` 메서드 사용

## 3. API 엔드포인트 상세

### 3.1 구독 관리 기능

#### 3.1.1 뉴스레터 구독
- **URL**: `POST /api/newsletter/subscribe`
- **인증**: 필수
- **요청**:
```json
{
  "email": "user@example.com",
  "frequency": "DAILY|WEEKLY|MONTHLY|IMMEDIATE",
  "preferredCategories": ["POLITICS", "ECONOMY"],
  "keywords": ["AI", "블록체인"],
  "sendTime": 9,
  "isPersonalized": true
}
```
- **응답**:
```json
{
  "success": true,
  "data": {
    "subscriptionId": 1,
    "userId": 1,
    "email": "user@example.com",
    "frequency": "DAILY",
    "status": "ACTIVE",
    "preferredCategories": ["POLITICS", "ECONOMY"],
    "keywords": ["AI", "블록체인"],
    "sendTime": 9,
    "isPersonalized": true,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  },
  "message": "구독이 완료되었습니다."
}
```

#### 3.1.2 구독 정보 조회
- **URL**: `GET /api/newsletter/subscription/{id}`
- **인증**: 필수
- **응답**: SubscriptionResponse 객체

#### 3.1.3 내 구독 목록 조회
- **URL**: `GET /api/newsletter/subscription/my`
- **인증**: 필수
- **응답**: `List<SubscriptionResponse>`

#### 3.1.4 구독 해지
- **URL**: `DELETE /api/newsletter/subscription/{id}`
- **인증**: 필수
- **응답**:
```json
{
  "success": true,
  "data": "구독이 해지되었습니다."
}
```

#### 3.1.5 활성 구독 목록 조회
- **URL**: `GET /api/newsletter/subscription/active`
- **인증**: 필수
- **응답**: `List<SubscriptionResponse>`

#### 3.1.6 구독 상태 변경
- **URL**: `PUT /api/newsletter/subscription/{subscriptionId}/status`
- **인증**: 필수
- **요청**:
```json
{
  "status": "ACTIVE|PAUSED|CANCELLED"
}
```
- **응답**: SubscriptionResponse 객체

#### 3.1.7 구독 재활성화
- **URL**: `PUT /api/newsletter/subscription/{id}/reactivate`
- **인증**: 필수
- **응답**: SubscriptionResponse 객체

### 3.2 콘텐츠 조회 기능

#### 3.2.1 카테고리별 헤드라인 조회
- **URL**: `GET /api/newsletter/category/{category}/headlines?limit=5`
- **인증**: 불필요
- **응답**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "뉴스 제목",
      "summary": "뉴스 요약",
      "url": "https://example.com/news/1",
      "publishedAt": "2024-01-01T00:00:00",
      "category": "POLITICS"
    }
  ]
}
```

#### 3.2.2 카테고리별 기사 조회
- **URL**: `GET /api/newsletter/category/{category}/articles?limit=5`
- **인증**: 불필요
- **응답**:
```json
{
  "success": true,
  "data": {
    "articles": [...],
    "trendingKeywords": ["키워드1", "키워드2"]
  }
}
```

#### 3.2.3 트렌드 키워드 조회
- **URL**: `GET /api/newsletter/trending-keywords?limit=10`
- **인증**: 불필요
- **응답**: `List<String>`

#### 3.2.4 카테고리별 트렌드 키워드 조회
- **URL**: `GET /api/newsletter/category/{category}/trending-keywords?limit=8`
- **인증**: 불필요
- **응답**: `List<String>`

#### 3.2.5 개인화된 뉴스레터 콘텐츠 조회 (JSON)
- **URL**: `GET /api/newsletter/{newsletterId}/content`
- **인증**: 필수
- **응답**: NewsletterContent 객체

#### 3.2.6 개인화된 뉴스레터 HTML 조회
- **URL**: `GET /api/newsletter/{newsletterId}/html`
- **인증**: 필수
- **응답**: HTML 문자열

#### 3.2.7 개인화된 뉴스레터 미리보기
- **URL**: `GET /api/newsletter/{newsletterId}/preview`
- **인증**: 필수
- **응답**: HTML 문자열

### 3.3 통계 및 분석 기능

#### 3.3.1 카테고리별 구독자 수 조회
- **URL**: `GET /api/newsletter/category/{category}/subscribers`
- **인증**: 불필요
- **응답**:
```json
{
  "success": true,
  "data": {
    "category": "POLITICS",
    "subscriberCount": 1500,
    "growthRate": 5.2,
    "trend": "INCREASING"
  }
}
```

#### 3.3.2 전체 카테고리별 구독자 수 조회
- **URL**: `GET /api/newsletter/categories/subscribers`
- **인증**: 불필요
- **응답**:
```json
{
  "success": true,
  "data": {
    "totalSubscribers": 10000,
    "categories": {
      "POLITICS": 1500,
      "ECONOMY": 2000,
      "SOCIETY": 1800
    }
  }
}
```

#### 3.3.3 구독자 통계 조회
- **URL**: `GET /api/newsletter/stats/subscribers?category=POLITICS`
- **인증**: 불필요
- **응답**: 카테고리별 또는 전체 통계

### 3.4 발송 관리 기능

#### 3.4.1 뉴스레터 즉시 발송
- **URL**: `POST /api/newsletter/delivery/send-now`
- **인증**: 필수
- **요청**:
```json
{
  "newsletterId": 1,
  "targetUserIds": [1, 2, 3],
  "deliveryMethod": "EMAIL",
  "isPersonalized": true,
  "isScheduled": false
}
```
- **응답**:
```json
{
  "success": true,
  "data": {
    "deliveryId": 1,
    "totalRecipients": 100,
    "deliveredCount": 95,
    "failedCount": 5,
    "openedCount": 45,
    "clickedCount": 12,
    "openRate": 47.4,
    "clickRate": 12.6,
    "deliveryTime": "2024-01-01T09:00:00",
    "status": "COMPLETED",
    "successRate": 95.0
  },
  "message": "뉴스레터 발송이 시작되었습니다."
}
```

#### 3.4.2 뉴스레터 예약 발송
- **URL**: `POST /api/newsletter/delivery/schedule`
- **인증**: 필수
- **요청**:
```json
{
  "newsletterId": 1,
  "targetUserIds": [1, 2, 3],
  "deliveryMethod": "EMAIL",
  "isPersonalized": true,
  "isScheduled": true,
  "scheduledAt": "2024-01-01T09:00:00"
}
```
- **응답**: DeliveryStats 객체

#### 3.4.3 발송 취소
- **URL**: `PUT /api/newsletter/delivery/{deliveryId}/cancel`
- **인증**: 필수
- **응답**:
```json
{
  "success": true,
  "data": "발송이 취소되었습니다."
}
```

#### 3.4.4 발송 재시도
- **URL**: `PUT /api/newsletter/delivery/{deliveryId}/retry`
- **인증**: 필수
- **응답**:
```json
{
  "success": true,
  "data": "발송 재시도가 시작되었습니다."
}
```

### 3.5 공유 통계 기능

#### 3.5.1 공유 통계 기록
- **URL**: `POST /api/newsletter/share`
- **인증**: 필수
- **요청**:
```json
{
  "type": "kakao|facebook|twitter",
  "newsId": 1,
  "category": "POLITICS"
}
```
- **응답**:
```json
{
  "success": true,
  "data": {
    "type": "kakao",
    "shareCount": 25,
    "message": "공유가 기록되었습니다.",
    "success": true
  },
  "message": "공유 통계가 기록되었습니다."
}
```

### 3.6 관리자 기능

#### 3.6.1 카테고리별 구독자 수 동기화
- **URL**: `POST /api/newsletter/admin/sync-category-subscribers`
- **인증**: 필수
- **응답**:
```json
{
  "success": true,
  "data": "카테고리별 구독자 수 동기화가 완료되었습니다."
}
```

## 4. 데이터 모델

### 4.1 Enum 타입

#### 4.1.1 SubscriptionFrequency
```java
DAILY("매일")
WEEKLY("주간")
MONTHLY("월간")
IMMEDIATE("즉시")
```

#### 4.1.2 NewsCategory
```java
POLITICS("정치", "🏛️")
ECONOMY("경제", "💰")
SOCIETY("사회", "👥")
LIFE("생활", "🎭")
INTERNATIONAL("세계", "🌍")
IT_SCIENCE("IT/과학", "💻")
VEHICLE("자동차/교통", "🚗")
TRAVEL_FOOD("여행/음식", "🧳")
ART("예술", "🎨")
```

#### 4.1.3 DeliveryMethod
```java
EMAIL("이메일", 1)
SMS("SMS", 2)
PUSH("푸시 알림", 3)
```

### 4.2 주요 DTO 클래스

#### 4.2.1 SubscriptionRequest
- userId: Long
- email: String (필수, 이메일 형식)
- frequency: SubscriptionFrequency (필수)
- preferredCategories: List<NewsCategory>
- keywords: List<String>
- sendTime: Integer (0-23)
- isPersonalized: boolean

#### 4.2.2 NewsletterDeliveryRequest
- newsletterId: Long (필수)
- targetUserIds: List<Long>
- deliveryMethod: DeliveryMethod
- isPersonalized: boolean
- isScheduled: boolean
- scheduledAt: LocalDateTime

#### 4.2.3 NewsletterContent
- newsletterId: Long
- userId: Long
- personalized: Boolean
- title: String
- generatedAt: LocalDateTime
- sections: List<Section>

#### 4.2.4 DeliveryStats
- deliveryId: Long
- totalRecipients: int
- deliveredCount: int
- failedCount: int
- openedCount: int
- clickedCount: int
- openRate: double
- clickRate: double
- deliveryTime: LocalDateTime
- status: String
- errorMessage: String
- totalSent: int
- totalFailed: int
- totalScheduled: int
- successRate: double

## 5. 에러 처리

### 5.1 공통 에러 코드
- `SUBSCRIPTION_ERROR`: 구독 처리 오류
- `SUBSCRIPTION_FETCH_ERROR`: 구독 정보 조회 오류
- `SUBSCRIPTION_LIST_ERROR`: 구독 목록 조회 오류
- `UNSUBSCRIBE_ERROR`: 구독 해지 오류
- `STATUS_CHANGE_ERROR`: 상태 변경 오류
- `CONTENT_FETCH_ERROR`: 콘텐츠 조회 오류
- `DELIVERY_ERROR`: 발송 오류
- `SCHEDULE_ERROR`: 예약 오류
- `CANCEL_ERROR`: 취소 오류
- `RETRY_ERROR`: 재시도 오류
- `SHARE_STATS_ERROR`: 공유 통계 오류
- `AUTHENTICATION_REQUIRED`: 인증 필요

### 5.2 HTTP 상태 코드
- `200 OK`: 성공
- `400 Bad Request`: 잘못된 요청
- `401 Unauthorized`: 인증 실패
- `403 Forbidden`: 권한 없음
- `404 Not Found`: 리소스 없음
- `500 Internal Server Error`: 서버 오류

## 6. 보안 고려사항

### 6.1 인증
- JWT 토큰 기반 인증
- Bearer 토큰 방식
- 토큰 만료 시간 관리

### 6.2 권한 관리
- 사용자별 구독 정보 접근 제한
- 관리자 기능 접근 제한

### 6.3 데이터 검증
- 입력값 검증 (Bean Validation)
- SQL Injection 방지
- XSS 방지

## 7. 성능 고려사항

### 7.1 캐싱
- 카테고리별 헤드라인 캐싱
- 트렌드 키워드 캐싱
- 구독자 통계 캐싱

### 7.2 페이지네이션
- 대용량 데이터 조회 시 페이지네이션 적용
- limit 파라미터로 조회 개수 제한

### 7.3 비동기 처리
- 뉴스레터 발송 비동기 처리
- 통계 계산 비동기 처리
