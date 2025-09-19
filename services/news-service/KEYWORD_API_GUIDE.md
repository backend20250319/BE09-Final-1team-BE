# 키워드 구독 및 트렌딩 키워드 API 가이드

## 1. 키워드 구독 API

### 1.1 키워드 구독

**POST** `/api/keywords/subscribe`

사용자가 특정 키워드를 구독합니다.

**요청 파라미터:**

- `userId` (Long, 필수): 사용자 ID
- `keyword` (String, 필수): 구독할 키워드

**응답:**

```json
{
  "subscriptionId": 1,
  "userId": 1,
  "keyword": "AI",
  "isActive": true,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

### 1.2 키워드 구독 해제

**DELETE** `/api/keywords/unsubscribe`

사용자의 키워드 구독을 해제합니다.

**요청 파라미터:**

- `userId` (Long, 필수): 사용자 ID
- `keyword` (String, 필수): 구독 해제할 키워드

**응답:** 200 OK

### 1.3 사용자 키워드 구독 목록 조회

**GET** `/api/keywords/user/{userId}`

특정 사용자의 활성화된 키워드 구독 목록을 조회합니다.

**경로 파라미터:**

- `userId` (Long, 필수): 사용자 ID

**응답:**

```json
[
  {
    "subscriptionId": 1,
    "userId": 1,
    "keyword": "AI",
    "isActive": true,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  },
  {
    "subscriptionId": 2,
    "userId": 1,
    "keyword": "블록체인",
    "isActive": true,
    "createdAt": "2024-01-01T11:00:00",
    "updatedAt": "2024-01-01T11:00:00"
  }
]
```

## 2. 트렌딩 키워드 API

### 2.1 트렌딩 키워드 조회

**GET** `/api/trending/keywords`

현재 트렌딩 중인 키워드 목록을 조회합니다.

**요청 파라미터:**

- `limit` (int, 선택): 조회할 키워드 개수 (기본값: 10)

**응답:**

```json
[
  {
    "keyword": "AI",
    "count": 150,
    "trendScore": 150.0
  },
  {
    "keyword": "블록체인",
    "count": 120,
    "trendScore": 120.0
  }
]
```

### 2.2 인기 키워드 조회

**GET** `/api/trending/keywords/popular`

가장 많이 구독된 키워드 목록을 조회합니다.

**요청 파라미터:**

- `limit` (int, 선택): 조회할 키워드 개수 (기본값: 10)

**응답:**

```json
[
  {
    "keyword": "AI",
    "count": 200,
    "trendScore": 200.0
  },
  {
    "keyword": "블록체인",
    "count": 180,
    "trendScore": 180.0
  }
]
```

## 3. 검색 API 필터링 기능

### 3.1 고급 검색

**GET** `/api/search`

키워드 검색에 정렬 및 필터링 기능을 추가한 API입니다.

**요청 파라미터:**

- `query` (String, 필수): 검색 키워드
- `sortBy` (String, 선택): 정렬 기준 ("date", "title", "press")
- `sortOrder` (String, 선택): 정렬 순서 ("asc", "desc")
- `category` (String, 선택): 카테고리 필터
- `press` (String, 선택): 언론사 필터
- `startDate` (String, 선택): 시작 날짜 (YYYY-MM-DD HH:mm:ss)
- `endDate` (String, 선택): 종료 날짜 (YYYY-MM-DD HH:mm:ss)
- 페이징 파라미터: `page`, `size`, `sort`

**예시 요청:**

```
GET /api/search?query=AI&sortBy=date&sortOrder=desc&category=IT_SCIENCE&press=조선일보&page=0&size=10
```

**응답:**

```json
{
  "content": [
    {
      "newsId": 1,
      "title": "AI 기술 발전 현황",
      "summary": "최신 AI 기술 동향...",
      "press": "조선일보",
      "publishedAt": "2024-01-01T10:00:00",
      "categoryName": "IT_SCIENCE",
      "categoryDescription": "IT/과학"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 50,
  "totalPages": 5
}
```

## 4. 에러 처리

### 4.1 일반적인 에러 응답

```json
{
  "error": "에러 메시지",
  "timestamp": "2024-01-01T10:00:00",
  "status": 400
}
```

### 4.2 주요 에러 코드

- `400`: 잘못된 요청 (이미 구독 중인 키워드, 구독하지 않은 키워드 해제 등)
- `404`: 리소스를 찾을 수 없음
- `500`: 서버 내부 오류

## 5. 사용 예시

### 5.1 키워드 구독 플로우

```bash
# 1. 키워드 구독
curl -X POST "http://localhost:8080/api/keywords/subscribe?userId=1&keyword=AI"

# 2. 구독 목록 확인
curl -X GET "http://localhost:8080/api/keywords/user/1"

# 3. 구독 해제
curl -X DELETE "http://localhost:8080/api/keywords/unsubscribe?userId=1&keyword=AI"
```

### 5.2 트렌딩 키워드 조회

```bash
# 트렌딩 키워드 조회
curl -X GET "http://localhost:8080/api/trending/keywords?limit=5"

# 인기 키워드 조회
curl -X GET "http://localhost:8080/api/trending/keywords/popular?limit=5"
```

### 5.3 고급 검색

```bash
# 필터링이 적용된 검색
curl -X GET "http://localhost:8080/api/search?query=AI&sortBy=date&sortOrder=desc&category=IT_SCIENCE&page=0&size=10"
```
