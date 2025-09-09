# 카테고리별 구독 관리 API 가이드

## 개요
사용자가 뉴스레터 카테고리를 구독하거나 해지할 수 있는 API들을 제공합니다.

## 지원 카테고리
- 정치 (POLITICS)
- 경제 (ECONOMY)
- 사회 (SOCIETY)
- 생활 (LIFE)
- 세계 (INTERNATIONAL)
- IT/과학 (IT_SCIENCE)
- 자동차/교통 (VEHICLE)
- 여행/음식 (TRAVEL_FOOD)
- 예술 (ART)

## API 엔드포인트

### 1. 구독 목록 조회
**GET** `/api/newsletter/subscription/my`

사용자의 활성 구독 목록을 조회합니다.

**Headers:**
```
Authorization: Bearer {access_token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "count": 2,
    "subscriptions": [
      {
        "id": 1330027361,
        "subscriptionId": 1330027361,
        "categoryId": -1234567890,
        "category": "정치",
        "categoryName": "POLITICS",
        "categoryNameKo": "정치",
        "isActive": true,
        "subscribedAt": "2025-09-08T00:42:16.898094",
        "updatedAt": "2025-09-08T00:42:16.898156",
        "subscriberCount": 1
      }
    ],
    "preferredCategories": ["POLITICS", "ECONOMY"],
    "userId": 1,
    "timestamp": "2025-09-08T10:00:00.000"
  },
  "message": "구독 목록 조회가 완료되었습니다."
}
```

### 2. 카테고리 구독/해지
**POST** `/api/newsletter/subscription/category/{category}`

특정 카테고리를 구독하거나 해지합니다.

**Parameters:**
- `category`: 카테고리명 (한글) - 예: "정치", "경제"
- `subscribe`: 구독 여부 (기본값: true)

**Headers:**
```
Authorization: Bearer {access_token}
```

**구독 요청 예시:**
```bash
POST /api/newsletter/subscription/category/경제?subscribe=true
```

**해지 요청 예시:**
```bash
POST /api/newsletter/subscription/category/경제?subscribe=false
```

**Response:**
```json
{
  "success": true,
  "data": {
    "action": "subscribed",
    "message": "구독이 완료되었습니다.",
    "subscriptionId": 1330027362,
    "category": "ECONOMY",
    "categoryKo": "경제",
    "userId": 1,
    "timestamp": "2025-09-08T10:00:00.000",
    "totalActiveSubscriptions": 2
  },
  "message": "구독 상태가 변경되었습니다."
}
```

**가능한 action 값:**
- `subscribed`: 새로운 구독 생성
- `reactivated`: 기존 구독 재활성화
- `already_subscribed`: 이미 구독 중
- `unsubscribed`: 구독 해지
- `already_unsubscribed`: 이미 구독 해지됨
- `not_subscribed`: 구독하지 않은 카테고리

### 3. 카테고리 구독 상태 조회
**GET** `/api/newsletter/subscription/category/{category}/status`

특정 카테고리의 구독 상태를 조회합니다.

**Parameters:**
- `category`: 카테고리명 (한글) - 예: "정치", "경제"

**Headers:**
```
Authorization: Bearer {access_token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "category": "ECONOMY",
    "categoryKo": "경제",
    "userId": 1,
    "isSubscribed": true,
    "subscriptionId": 1330027362,
    "subscribedAt": "2025-09-08T10:00:00.000",
    "updatedAt": "2025-09-08T10:00:00.000"
  },
  "message": "구독 상태 조회 완료"
}
```

### 4. 디버깅용 API

#### 구독 상태 상세 조회
**GET** `/api/newsletter/debug/subscriptions/{userId}`

사용자의 모든 구독 상태를 상세히 조회합니다.

#### 경제 카테고리 구독 추가 (디버깅용)
**POST** `/api/newsletter/debug/subscribe-economy/{userId}`

경제 카테고리 구독을 추가하거나 활성화합니다.

## 사용 예시

### JavaScript/TypeScript 예시

```javascript
// 구독 목록 조회
async function getMySubscriptions() {
  const response = await fetch('/api/newsletter/subscription/my', {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });
  return await response.json();
}

// 카테고리 구독
async function subscribeToCategory(category) {
  const response = await fetch(`/api/newsletter/subscription/category/${category}?subscribe=true`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });
  return await response.json();
}

// 카테고리 구독 해지
async function unsubscribeFromCategory(category) {
  const response = await fetch(`/api/newsletter/subscription/category/${category}?subscribe=false`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });
  return await response.json();
}

// 카테고리 구독 상태 확인
async function getCategorySubscriptionStatus(category) {
  const response = await fetch(`/api/newsletter/subscription/category/${category}/status`, {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });
  return await response.json();
}
```

### cURL 예시

```bash
# 구독 목록 조회
curl -X GET "http://localhost:8085/api/newsletter/subscription/my" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# 경제 카테고리 구독
curl -X POST "http://localhost:8085/api/newsletter/subscription/category/경제?subscribe=true" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# 정치 카테고리 구독 해지
curl -X POST "http://localhost:8085/api/newsletter/subscription/category/정치?subscribe=false" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# 경제 카테고리 구독 상태 확인
curl -X GET "http://localhost:8085/api/newsletter/subscription/category/경제/status" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 에러 처리

### 일반적인 에러 응답
```json
{
  "success": false,
  "errorCode": "ERROR_CODE",
  "message": "에러 메시지"
}
```

### 주요 에러 코드
- `INVALID_CATEGORY`: 유효하지 않은 카테고리
- `SUBSCRIPTION_TOGGLE_ERROR`: 구독 상태 변경 중 오류
- `SUBSCRIPTION_STATUS_ERROR`: 구독 상태 조회 중 오류
- `SUBSCRIPTION_LIST_ERROR`: 구독 목록 조회 중 오류

## 주의사항

1. 모든 API는 인증이 필요합니다 (Bearer Token)
2. 카테고리명은 한글로 입력해야 합니다
3. 구독 해지는 데이터를 삭제하지 않고 `isActive`를 `false`로 설정합니다
4. 동일한 카테고리를 중복 구독할 수 없습니다
5. 구독 상태 변경 시 `updatedAt` 필드가 자동으로 업데이트됩니다
