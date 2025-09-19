# Enhanced 뉴스레터 API 가이드

## 개요

Enhanced 뉴스레터 API는 실시간 뉴스 필터링과 개인화된 콘텐츠 제공을 위한 새로운 엔드포인트입니다. 사용자가 뉴스레터 페이지를 방문할 때 각 카테고리별 실시간 주제와 헤드라인을 표시하고, 카테고리 선택 시 상세 정보를 제공합니다.

## 주요 기능

### 1. 실시간 뉴스 필터링
- 각 카테고리별 최신 헤드라인 자동 수집
- FeignClient를 통한 뉴스 서비스와의 실시간 연동
- 트렌딩 키워드 실시간 업데이트

### 2. 개인화된 콘텐츠
- 사용자 구독 정보 기반 맞춤 콘텐츠 제공
- 인증된 사용자와 비인증 사용자 모두 지원
- 구독 상태에 따른 차별화된 경험

### 3. 카테고리별 상세 정보
- 카테고리 선택 시 확장된 뉴스 정보 제공
- 카테고리별 트렌딩 키워드 표시
- 구독 상태 실시간 확인

## API 엔드포인트

### 1. Enhanced 뉴스레터 메인 API

#### 요청
```http
GET /api/newsletter/enhanced?headlinesPerCategory=5&trendingKeywordsLimit=8
Authorization: Bearer {jwt_token}  # 선택적
```

#### 파라미터
- `headlinesPerCategory` (선택적, 기본값: 5): 카테고리별 헤드라인 개수
- `trendingKeywordsLimit` (선택적, 기본값: 8): 트렌딩 키워드 개수

#### 응답 예시
```json
{
  "success": true,
  "data": {
    "categories": {
      "정치": [
        {
          "id": 1,
          "title": "정치 뉴스 제목",
          "summary": "뉴스 요약",
          "url": "https://example.com/news/1",
          "publishedAt": "2024-01-01T00:00:00",
          "category": "POLITICS"
        }
      ],
      "경제": [...],
      "사회": [...],
      "생활": [...],
      "세계": [...],
      "IT/과학": [...],
      "자동차/교통": [...],
      "여행/음식": [...],
      "예술": [...]
    },
    "trendingKeywords": [
      "AI", "블록체인", "환경", "부동산", "주식", "코로나", "기후변화", "디지털"
    ],
    "userSubscriptionInfo": {
      "userId": 1,
      "subscribedCategories": ["정치", "경제"],
      "totalSubscriptions": 2,
      "isPersonalized": true
    },
    "timestamp": "2024-01-01T12:00:00",
    "totalCategories": 9,
    "headlinesPerCategory": 5
  },
  "message": "Enhanced 뉴스레터 데이터가 조회되었습니다."
}
```

### 2. 카테고리별 상세 정보 API

#### 요청
```http
GET /api/newsletter/enhanced/category/{category}?headlinesLimit=10&keywordsLimit=8
Authorization: Bearer {jwt_token}  # 선택적
```

#### 파라미터
- `category` (필수): 카테고리명 (예: "정치", "경제", "사회" 등)
- `headlinesLimit` (선택적, 기본값: 10): 헤드라인 개수
- `keywordsLimit` (선택적, 기본값: 8): 키워드 개수

#### 응답 예시
```json
{
  "success": true,
  "data": {
    "category": "정치",
    "categoryEn": "POLITICS",
    "headlines": [
      {
        "id": 1,
        "title": "정치 뉴스 제목",
        "summary": "뉴스 요약",
        "url": "https://example.com/news/1",
        "publishedAt": "2024-01-01T00:00:00",
        "category": "POLITICS"
      }
    ],
    "trendingKeywords": [
      "대통령", "국회", "선거", "정당", "정책", "외교", "국방", "안보"
    ],
    "subscriptionStatus": {
      "isSubscribed": true,
      "subscriptionId": 1,
      "subscribedAt": "2024-01-01T00:00:00"
    },
    "timestamp": "2024-01-01T12:00:00",
    "totalHeadlines": 10,
    "totalKeywords": 8
  },
  "message": "카테고리 상세 정보가 조회되었습니다."
}
```

## 사용 시나리오

### 1. 뉴스레터 페이지 방문
```javascript
// 프론트엔드에서 Enhanced API 호출
const response = await fetch('/api/newsletter/enhanced?headlinesPerCategory=5&trendingKeywordsLimit=8', {
  headers: {
    'Authorization': 'Bearer ' + token  // 인증된 사용자의 경우
  }
});

const data = await response.json();
// 각 카테고리별 헤드라인과 트렌딩 키워드 표시
```

### 2. 카테고리 선택 및 확장
```javascript
// 사용자가 "정치" 카테고리 선택 시
const categoryResponse = await fetch('/api/newsletter/enhanced/category/정치?headlinesLimit=10&keywordsLimit=8', {
  headers: {
    'Authorization': 'Bearer ' + token
  }
});

const categoryData = await categoryResponse.json();
// 해당 카테고리의 상세 주제들과 최근 뉴스 헤드라인 표시
```

### 3. 구독 및 개인화
```javascript
// 사용자 구독 정보 확인
if (data.userSubscriptionInfo && data.userSubscriptionInfo.subscribedCategories.length > 0) {
  // 구독된 카테고리 우선 표시
  displaySubscribedCategories(data.userSubscriptionInfo.subscribedCategories);
}

// 개인화된 콘텐츠 제공
if (data.userSubscriptionInfo.isPersonalized) {
  displayPersonalizedContent(data);
}
```

## 기술적 특징

### 1. FeignClient 연동
- NewsServiceClient를 통한 실시간 뉴스 데이터 수집
- 트렌딩 키워드 실시간 조회
- 카테고리별 키워드 동적 수집

### 2. 폴백 메커니즘
- 외부 서비스 장애 시 기본 키워드 제공
- 안정적인 서비스 운영 보장
- 사용자 경험 지속성 유지

### 3. 인증 처리
- JWT 토큰 기반 사용자 인증
- 인증된 사용자와 비인증 사용자 모두 지원
- 선택적 인증으로 유연한 접근 허용

### 4. 성능 최적화
- 비동기 처리로 응답 시간 단축
- 캐싱을 통한 데이터 재사용
- 효율적인 데이터 구조 설계

## 에러 처리

### 일반적인 에러 응답
```json
{
  "success": false,
  "errorCode": "ENHANCED_NEWSLETTER_ERROR",
  "message": "Enhanced 뉴스레터 조회 중 오류가 발생했습니다: {상세 오류 메시지}"
}
```

### 주요 에러 코드
- `ENHANCED_NEWSLETTER_ERROR`: Enhanced 뉴스레터 조회 실패
- `CATEGORY_DETAILS_ERROR`: 카테고리 상세 정보 조회 실패
- `INVALID_CATEGORY`: 유효하지 않은 카테고리

## 향후 확장 계획

### 1. AI 기반 개인화
- 사용자 행동 패턴 분석
- 더 정확한 뉴스 추천 알고리즘
- 머신러닝 기반 콘텐츠 최적화

### 2. 실시간 알림
- WebSocket을 통한 실시간 뉴스 알림
- 푸시 알림 통합
- 사용자별 알림 설정

### 3. 소셜 기능
- 뉴스 공유 기능
- 댓글 및 평점 시스템
- 사용자 간 상호작용

### 4. 분석 대시보드
- 사용자별 읽기 패턴 분석
- 참여도 및 선호도 추적
- 콘텐츠 성과 분석

## 테스트 방법

### 1. 기본 테스트
```bash
# Enhanced 뉴스레터 API 테스트
curl -X GET "http://localhost:8085/api/newsletter/enhanced?headlinesPerCategory=5&trendingKeywordsLimit=8"

# 카테고리별 상세 정보 테스트
curl -X GET "http://localhost:8085/api/newsletter/enhanced/category/정치?headlinesLimit=10&keywordsLimit=8"
```

### 2. 인증 테스트
```bash
# JWT 토큰과 함께 테스트
curl -X GET "http://localhost:8085/api/newsletter/enhanced" \
  -H "Authorization: Bearer {jwt_token}"
```

### 3. 파라미터 테스트
```bash
# 다양한 파라미터로 테스트
curl -X GET "http://localhost:8085/api/newsletter/enhanced?headlinesPerCategory=10&trendingKeywordsLimit=15"
```

## 주의사항

1. **외부 서비스 의존성**: NewsServiceClient에 의존하므로 뉴스 서비스가 정상 동작해야 함
2. **성능 고려**: 대량의 데이터 조회 시 응답 시간이 길어질 수 있음
3. **캐싱 전략**: 자주 조회되는 데이터에 대한 캐싱 전략 필요
4. **에러 처리**: 외부 서비스 장애 시 폴백 메커니즘 동작 확인

## 관련 파일

- `NewsletterController.java`: Enhanced API 구현
- `NewsServiceClient.java`: 뉴스 서비스 연동
- `NewsletterService.java`: 뉴스레터 서비스 로직
- `UserNewsletterSubscriptionRepository.java`: 구독 정보 관리
