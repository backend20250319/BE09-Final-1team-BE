# 카테고리별 다중 구독 지원 가이드

## 개요

뉴스레터 서비스에서 카테고리별로 여러 구독이 가능하도록 개선되었습니다. 사용자는 같은 카테고리에 대해 서로 다른 설정(빈도, 발송시간, 개인화 옵션 등)으로 여러 구독을 생성할 수 있습니다.

## 주요 변경사항

### 1. 데이터베이스 스키마 변경
- `user_newsletter_subscriptions` 테이블에서 `UNIQUE KEY uk_user_category (user_id, category)` 제약조건 제거
- 사용자당 카테고리별 여러 구독 레코드 허용

### 2. API 개선사항

#### 구독 생성 API (`POST /api/newsletter/subscription-management/subscribe`)
- **변경 전**: 같은 카테고리 구독 시 "이미 구독 중인 카테고리입니다" 오류 반환
- **변경 후**: 카테고리별 다중 구독 허용, 추가 옵션 지원

**요청 예시:**
```json
{
  "category": "POLITICS",
  "frequency": "DAILY",
  "sendTime": "09:00",
  "isPersonalized": true,
  "keywords": "정치,선거,국회"
}
```

**응답 예시:**
```json
{
  "success": true,
  "message": "구독이 완료되었습니다.",
  "data": {
    "subscriptionId": 123,
    "category": "POLITICS",
    "isActive": true,
    "frequency": "DAILY",
    "sendTime": "09:00",
    "isPersonalized": true,
    "keywords": "정치,선거,국회",
    "subscribedAt": "2024-01-15T09:00:00",
    "updatedAt": "2024-01-15T09:00:00"
  }
}
```

#### 구독 취소 API (`POST /api/newsletter/subscription-management/unsubscribe`)
- **변경 전**: 카테고리별 첫 번째 구독만 취소
- **변경 후**: 카테고리별 모든 구독을 비활성화

#### 구독 여부 확인 API (`GET /api/newsletter/subscription-management/check/{category}`)
- **변경 전**: 단일 구독 정보만 반환
- **변경 후**: 카테고리별 모든 구독 정보 반환

**응답 예시:**
```json
{
  "success": true,
  "message": "구독 여부를 확인했습니다.",
  "data": {
    "category": "POLITICS",
    "isSubscribed": true,
    "totalSubscriptions": 2,
    "activeSubscriptions": 1,
    "inactiveSubscriptions": 1,
    "subscriptions": [
      {
        "subscriptionId": 123,
        "isActive": true,
        "subscribedAt": "2024-01-15T09:00:00",
        "updatedAt": "2024-01-15T09:00:00"
      },
      {
        "subscriptionId": 124,
        "isActive": false,
        "subscribedAt": "2024-01-16T10:00:00",
        "updatedAt": "2024-01-16T11:00:00"
      }
    ]
  }
}
```

### 3. 새로운 API 엔드포인트

#### 카테고리별 구독 목록 조회 (`GET /api/newsletter/subscription-management/category/{category}`)
특정 카테고리의 모든 구독 정보를 상세히 조회할 수 있습니다.

**응답 예시:**
```json
{
  "success": true,
  "message": "카테고리별 구독 목록을 조회했습니다.",
  "data": [
    {
      "subscriptionId": 123,
      "category": "POLITICS",
      "isActive": true,
      "subscribedAt": "2024-01-15T09:00:00",
      "updatedAt": "2024-01-15T09:00:00",
      "frequency": "DAILY",
      "sendTime": "09:00",
      "isPersonalized": true,
      "keywords": "정치,선거,국회"
    }
  ]
}
```

## 사용 시나리오

### 시나리오 1: 다양한 빈도로 구독
사용자가 정치 뉴스를 매일 받으면서, 동시에 주간 요약도 받고 싶은 경우:

```bash
# 매일 오전 9시 정치 뉴스 구독
POST /api/newsletter/subscription-management/subscribe
{
  "category": "POLITICS",
  "frequency": "DAILY",
  "sendTime": "09:00"
}

# 주간 정치 뉴스 요약 구독
POST /api/newsletter/subscription-management/subscribe
{
  "category": "POLITICS",
  "frequency": "WEEKLY",
  "sendTime": "18:00"
}
```

### 시나리오 2: 개인화 옵션별 구독
일반 정치 뉴스와 특정 키워드 기반 개인화 뉴스를 동시에 구독:

```bash
# 일반 정치 뉴스 구독
POST /api/newsletter/subscription-management/subscribe
{
  "category": "POLITICS",
  "isPersonalized": false
}

# 개인화 정치 뉴스 구독
POST /api/newsletter/subscription-management/subscribe
{
  "category": "POLITICS",
  "isPersonalized": true,
  "keywords": "대통령,국회,정치개혁"
}
```

## 마이그레이션 가이드

### 1. 데이터베이스 마이그레이션
```sql
-- UNIQUE 제약조건 제거
ALTER TABLE user_newsletter_subscriptions DROP INDEX IF EXISTS uk_user_category;
```

### 2. 기존 데이터 영향
- 기존 구독 데이터는 그대로 유지됩니다
- 기존 API 호출은 정상적으로 작동합니다
- 새로운 다중 구독 기능을 점진적으로 활용할 수 있습니다

## 주의사항

1. **구독 취소 시**: 카테고리별 모든 구독이 비활성화됩니다
2. **개별 구독 관리**: 특정 구독만 취소하려면 구독 ID를 사용한 API를 활용하세요
3. **성능**: 인덱스는 유지되므로 성능에 큰 영향은 없습니다
4. **데이터 정합성**: 각 구독은 고유한 ID를 가지므로 개별 관리가 가능합니다

## 테스트 방법

1. 같은 카테고리로 여러 구독 생성
2. 각 구독의 설정이 올바르게 저장되는지 확인
3. 구독 목록 조회 시 모든 구독이 표시되는지 확인
4. 개별 구독 취소/활성화가 정상 작동하는지 확인
