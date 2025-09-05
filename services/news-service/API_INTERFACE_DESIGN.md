# 뉴스 서비스 API 인터페이스 설계서

## 1. 개요

뉴스 서비스는 뉴스 조회, 검색, 개인화, 트렌딩, 관리자 기능 등을 제공하는 RESTful API 서비스입니다.

### 기본 정보
- **Base URL**: `/api`
- **Content-Type**: `application/json`
- **인증 방식**: Bearer Token (JWT)
- **CORS**: 모든 도메인 허용 (`*`)

## 2. API 엔드포인트 목록

### 2.1 시스템 관리 (SystemController)
- `GET /api/system/health` - 헬스 체크
- `GET /api/system/test-db` - 데이터베이스 연결 테스트

### 2.2 뉴스 관리 (NewsController)
- `GET /api/news/count` - 뉴스 총 개수 조회
- `POST /api/news/{newsId}/view` - 조회수 증가
- `GET /api/news` - 뉴스 목록 조회 (페이징)
- `GET /api/news/{newsId}` - 뉴스 상세 조회
- `POST /api/news/{newsId}/report` - 뉴스 신고
- `POST /api/news/{newsId}/scrap` - 뉴스 스크랩
- `GET /api/news/collections` - 스크랩 보관함 목록
- `POST /api/news/collections` - 보관함 생성
- `POST /api/news/collections/{collectionId}/news` - 보관함에 뉴스 추가

### 2.3 카테고리 관리 (CategoryController)
- `GET /api/news/categories` - 카테고리 목록 조회
- `GET /api/news/categories/{categoryName}/news` - 카테고리별 뉴스 조회
- `GET /api/news/categories/{categoryName}/count` - 카테고리별 뉴스 개수

### 2.4 트렌딩 관리 (TrendingController)
- `GET /api/trending` - 트렌딩 뉴스 (페이징)
- `GET /api/trending/list` - 트렌딩 뉴스 리스트
- `GET /api/trending/trending-keywords` - 실시간 인기 키워드
- `GET /api/trending/trending-keywords/category/{categoryName}` - 카테고리별 트렌딩 키워드
- `GET /api/trending/popular` - 인기 뉴스
- `GET /api/trending/latest` - 최신 뉴스
- `GET /api/trending/keywords` - 트렌딩 키워드
- `GET /api/trending/keywords/popular` - 인기 키워드

### 2.5 검색 관리 (SearchController)
- `GET /api/search` - 뉴스 검색
- `GET /api/search/press/{press}` - 언론사별 뉴스
- `GET /api/search/date-range` - 기간별 뉴스

### 2.6 개인화 관리 (PersonalizationController)
- `GET /api/personalization/news` - 개인화 뉴스
- `GET /api/personalization/recommendations` - 추천 뉴스

### 2.7 마이페이지 관리 (MyPageController)
- `GET /api/news/mypage/scraps` - 내 스크랩 목록
- `DELETE /api/news/mypage/scraps/{newsId}` - 스크랩 해제

### 2.8 키워드 구독 관리 (KeywordSubscriptionController)
- `POST /api/keywords/subscribe` - 키워드 구독
- `DELETE /api/keywords/unsubscribe` - 키워드 구독 해제
- `GET /api/keywords/user/{userId}` - 사용자 키워드 구독 목록

### 2.9 연관 뉴스 관리 (RelatedNewsController)
- `GET /api/related/{newsId}` - 연관 뉴스 조회

### 2.10 크롤링 관리 (NewsCrawlController)
- `POST /api/news/crawl` - 크롤링 뉴스 저장
- `POST /api/news/crawl/preview` - 크롤링 뉴스 미리보기

### 2.11 관리자 관리 (AdminController)
- `GET /api/admin/crawled-news` - 크롤링된 뉴스 목록 조회
- `POST /api/admin/promote/{newsCrawlId}` - 크롤링 뉴스 승격

## 3. 상세 API 명세

### 3.1 시스템 관리 API

#### 3.1.1 헬스 체크
```http
GET /api/system/health
```

**응답**
```json
{
  "status": "News Service is running"
}
```

#### 3.1.2 데이터베이스 연결 테스트
```http
GET /api/system/test-db
```

**응답**
```json
{
  "status": "데이터베이스 연결 성공. 뉴스 개수: 1234"
}
```

### 3.2 뉴스 관리 API

#### 3.2.1 뉴스 총 개수 조회
```http
GET /api/news/count
```

**응답**
```json
{
  "count": 1234
}
```

#### 3.2.2 조회수 증가
```http
POST /api/news/{newsId}/view
```

**경로 변수**
- `newsId` (Long): 뉴스 ID

**응답**
```http
200 OK
```

#### 3.2.3 뉴스 목록 조회
```http
GET /api/news?category=POLITICS&keyword=AI&page=0&size=10&sort=publishedAt,desc
```

**쿼리 파라미터**
- `category` (String, optional): 카테고리
- `keyword` (String, optional): 키워드
- `page` (Integer, optional): 페이지 번호 (기본값: 0)
- `size` (Integer, optional): 페이지 크기 (기본값: 10)
- `sort` (String, optional): 정렬 기준

**응답**
```json
{
  "content": [
    {
      "id": 1,
      "title": "뉴스 제목",
      "content": "뉴스 내용",
      "category": "POLITICS",
      "press": "조선일보",
      "publishedAt": "2024-01-01T10:00:00",
      "views": 100,
      "imageUrl": "https://example.com/image.jpg"
    }
  ],
  "totalElements": 100,
  "totalPages": 10,
  "size": 10,
  "number": 0
}
```

#### 3.2.4 뉴스 상세 조회
```http
GET /api/news/{newsId}
```

**경로 변수**
- `newsId` (Long): 뉴스 ID

**응답**
```json
{
  "id": 1,
  "title": "뉴스 제목",
  "content": "뉴스 내용",
  "category": "POLITICS",
  "press": "조선일보",
  "publishedAt": "2024-01-01T10:00:00",
  "views": 100,
  "imageUrl": "https://example.com/image.jpg",
  "url": "https://example.com/news/1"
}
```

#### 3.2.5 뉴스 신고
```http
POST /api/news/{newsId}/report
Authorization: Bearer {token}
```

**경로 변수**
- `newsId` (Long): 뉴스 ID

**응답**
```http
200 OK
```

#### 3.2.6 뉴스 스크랩
```http
POST /api/news/{newsId}/scrap
Authorization: Bearer {token}
```

**경로 변수**
- `newsId` (Long): 뉴스 ID

**응답**
```http
200 OK
```

#### 3.2.7 스크랩 보관함 목록
```http
GET /api/news/collections
Authorization: Bearer {token}
```

**응답**
```json
[
  {
    "id": 1,
    "storageName": "내 보관함",
    "newsCount": 5,
    "createdAt": "2024-01-01T10:00:00"
  }
]
```

#### 3.2.8 보관함 생성
```http
POST /api/news/collections
Authorization: Bearer {token}
Content-Type: application/json

{
  "storageName": "새 보관함"
}
```

**요청 본문**
```json
{
  "storageName": "새 보관함"
}
```

**응답**
```json
{
  "id": 2,
  "storageName": "새 보관함",
  "newsCount": 0,
  "createdAt": "2024-01-01T10:00:00"
}
```

#### 3.2.9 보관함에 뉴스 추가
```http
POST /api/news/collections/{collectionId}/news
Authorization: Bearer {token}
Content-Type: application/json

{
  "newsId": 123
}
```

**경로 변수**
- `collectionId` (Integer): 보관함 ID

**요청 본문**
```json
{
  "newsId": 123
}
```

**응답**
```http
200 OK
```

### 3.3 카테고리 관리 API

#### 3.3.1 카테고리 목록 조회
```http
GET /api/news/categories
```

**응답**
```json
[
  {
    "id": 1,
    "name": "POLITICS",
    "displayName": "정치",
    "description": "정치 관련 뉴스"
  }
]
```

#### 3.3.2 카테고리별 뉴스 조회
```http
GET /api/news/categories/{categoryName}/news?page=0&size=10
```

**경로 변수**
- `categoryName` (String): 카테고리명

**쿼리 파라미터**
- `page` (Integer, optional): 페이지 번호
- `size` (Integer, optional): 페이지 크기

**응답**
```json
{
  "content": [
    {
      "id": 1,
      "title": "뉴스 제목",
      "category": "POLITICS"
    }
  ],
  "totalElements": 100,
  "totalPages": 10
}
```

#### 3.3.3 카테고리별 뉴스 개수
```http
GET /api/news/categories/{categoryName}/count
```

**경로 변수**
- `categoryName` (String): 카테고리명

**응답**
```json
{
  "count": 100
}
```

### 3.4 트렌딩 관리 API

#### 3.4.1 트렌딩 뉴스
```http
GET /api/trending?page=0&size=10
```

**쿼리 파라미터**
- `page` (Integer, optional): 페이지 번호
- `size` (Integer, optional): 페이지 크기

**응답**
```json
{
  "content": [
    {
      "id": 1,
      "title": "트렌딩 뉴스",
      "views": 1000
    }
  ],
  "totalElements": 100
}
```

#### 3.4.2 트렌딩 뉴스 리스트
```http
GET /api/trending/list
```

**응답**
```json
[
  {
    "id": 1,
    "title": "트렌딩 뉴스",
    "views": 1000
  }
]
```

#### 3.4.3 실시간 인기 키워드
```http
GET /api/trending/trending-keywords?limit=10&period=24h
```

**쿼리 파라미터**
- `limit` (Integer, optional): 반환 개수 (기본값: 10)
- `period` (String, optional): 집계 기간 (기본값: 24h)
- `hours` (Integer, optional): 집계 시간(시간)

**응답**
```json
{
  "success": true,
  "data": [
    {
      "keyword": "AI",
      "count": 100,
      "trend": "up"
    }
  ]
}
```

#### 3.4.4 카테고리별 트렌딩 키워드
```http
GET /api/trending/trending-keywords/category/{categoryName}?limit=8&hours=24
```

**경로 변수**
- `categoryName` (String): 카테고리명

**쿼리 파라미터**
- `limit` (Integer, optional): 반환 개수 (기본값: 8)
- `hours` (Integer, optional): 집계 시간 (기본값: 24)

**응답**
```json
{
  "success": true,
  "data": [
    {
      "keyword": "AI",
      "count": 50,
      "trend": "up"
    }
  ]
}
```

#### 3.4.5 인기 뉴스
```http
GET /api/trending/popular?page=0&size=10
```

**쿼리 파라미터**
- `page` (Integer, optional): 페이지 번호
- `size` (Integer, optional): 페이지 크기

**응답**
```json
{
  "content": [
    {
      "id": 1,
      "title": "인기 뉴스",
      "views": 2000
    }
  ],
  "totalElements": 100
}
```

#### 3.4.6 최신 뉴스
```http
GET /api/trending/latest?page=0&size=10
```

**쿼리 파라미터**
- `page` (Integer, optional): 페이지 번호
- `size` (Integer, optional): 페이지 크기

**응답**
```json
{
  "content": [
    {
      "id": 1,
      "title": "최신 뉴스",
      "publishedAt": "2024-01-01T10:00:00"
    }
  ],
  "totalElements": 100
}
```

### 3.5 검색 관리 API

#### 3.5.1 뉴스 검색
```http
GET /api/search?query=AI&sortBy=publishedAt&sortOrder=desc&category=POLITICS&press=조선일보&startDate=2024-01-01&endDate=2024-01-31&page=0&size=10
```

**쿼리 파라미터**
- `query` (String, required): 검색어
- `sortBy` (String, optional): 정렬 기준 (publishedAt, views, relevance)
- `sortOrder` (String, optional): 정렬 순서 (asc, desc)
- `category` (String, optional): 카테고리
- `press` (String, optional): 언론사
- `startDate` (String, optional): 시작일 (YYYY-MM-DD)
- `endDate` (String, optional): 종료일 (YYYY-MM-DD)
- `page` (Integer, optional): 페이지 번호
- `size` (Integer, optional): 페이지 크기

**응답**
```json
{
  "content": [
    {
      "id": 1,
      "title": "AI 관련 뉴스",
      "content": "AI 기술 발전...",
      "category": "POLITICS",
      "press": "조선일보"
    }
  ],
  "totalElements": 50,
  "totalPages": 5
}
```

#### 3.5.2 언론사별 뉴스
```http
GET /api/search/press/{press}?page=0&size=10
```

**경로 변수**
- `press` (String): 언론사명

**쿼리 파라미터**
- `page` (Integer, optional): 페이지 번호
- `size` (Integer, optional): 페이지 크기

**응답**
```json
{
  "content": [
    {
      "id": 1,
      "title": "조선일보 뉴스",
      "press": "조선일보"
    }
  ],
  "totalElements": 100
}
```

#### 3.5.3 기간별 뉴스
```http
GET /api/search/date-range?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59
```

**쿼리 파라미터**
- `startDate` (LocalDateTime, optional): 시작일시
- `endDate` (LocalDateTime, optional): 종료일시

**응답**
```json
[
  {
    "id": 1,
    "title": "기간 내 뉴스",
    "publishedAt": "2024-01-15T10:00:00"
  }
]
```

### 3.6 개인화 관리 API

#### 3.6.1 개인화 뉴스
```http
GET /api/personalization/news
X-User-Id: 123
```

**헤더**
- `X-User-Id` (String, required): 사용자 ID

**응답**
```json
[
  {
    "id": 1,
    "title": "개인화 뉴스",
    "category": "POLITICS"
  }
]
```

#### 3.6.2 추천 뉴스
```http
GET /api/personalization/recommendations?userId=123&page=0&size=10
```

**쿼리 파라미터**
- `userId` (Long, optional): 사용자 ID
- `page` (Integer, optional): 페이지 번호
- `size` (Integer, optional): 페이지 크기

**응답**
```json
{
  "content": [
    {
      "id": 1,
      "title": "추천 뉴스",
      "category": "POLITICS"
    }
  ],
  "totalElements": 100
}
```

### 3.7 마이페이지 관리 API

#### 3.7.1 내 스크랩 목록
```http
GET /api/news/mypage/scraps?category=POLITICS&page=0&size=10
Authorization: Bearer {token}
```

**쿼리 파라미터**
- `category` (String, optional): 카테고리
- `page` (Integer, optional): 페이지 번호
- `size` (Integer, optional): 페이지 크기

**응답**
```json
{
  "content": [
    {
      "id": 1,
      "title": "스크랩한 뉴스",
      "category": "POLITICS"
    }
  ],
  "totalElements": 50
}
```

#### 3.7.2 스크랩 해제
```http
DELETE /api/news/mypage/scraps/{newsId}
Authorization: Bearer {token}
```

**경로 변수**
- `newsId` (Long): 뉴스 ID

**응답**
```http
204 No Content
```

### 3.8 키워드 구독 관리 API

#### 3.8.1 키워드 구독
```http
POST /api/keywords/subscribe?userId=123&keyword=AI
```

**쿼리 파라미터**
- `userId` (Long, required): 사용자 ID
- `keyword` (String, required): 구독할 키워드

**응답**
```json
{
  "id": 1,
  "userId": 123,
  "keyword": "AI",
  "createdAt": "2024-01-01T10:00:00"
}
```

#### 3.8.2 키워드 구독 해제
```http
DELETE /api/keywords/unsubscribe?userId=123&keyword=AI
```

**쿼리 파라미터**
- `userId` (Long, required): 사용자 ID
- `keyword` (String, required): 해제할 키워드

**응답**
```http
200 OK
```

#### 3.8.3 사용자 키워드 구독 목록
```http
GET /api/keywords/user/{userId}
```

**경로 변수**
- `userId` (Long): 사용자 ID

**응답**
```json
[
  {
    "id": 1,
    "userId": 123,
    "keyword": "AI",
    "createdAt": "2024-01-01T10:00:00"
  }
]
```

### 3.9 연관 뉴스 관리 API

#### 3.9.1 연관 뉴스 조회
```http
GET /api/related/{newsId}
```

**경로 변수**
- `newsId` (Long): 기준 뉴스 ID

**응답**
```json
[
  {
    "id": 2,
    "title": "연관 뉴스",
    "similarity": 0.85
  }
]
```

### 3.10 크롤링 관리 API

#### 3.10.1 크롤링 뉴스 저장
```http
POST /api/news/crawl
Content-Type: application/json

{
  "title": "크롤링된 뉴스",
  "content": "뉴스 내용",
  "category": "POLITICS",
  "press": "조선일보",
  "url": "https://example.com/news"
}
```

**요청 본문**
```json
{
  "title": "크롤링된 뉴스",
  "content": "뉴스 내용",
  "category": "POLITICS",
  "press": "조선일보",
  "url": "https://example.com/news"
}
```

**응답**
```json
{
  "message": "뉴스가 성공적으로 저장되었습니다."
}
```

#### 3.10.2 크롤링 뉴스 미리보기
```http
POST /api/news/crawl/preview
Content-Type: application/json

{
  "title": "크롤링된 뉴스",
  "content": "뉴스 내용",
  "category": "POLITICS",
  "press": "조선일보",
  "url": "https://example.com/news"
}
```

**요청 본문**
```json
{
  "title": "크롤링된 뉴스",
  "content": "뉴스 내용",
  "category": "POLITICS",
  "press": "조선일보",
  "url": "https://example.com/news"
}
```

**응답**
```json
{
  "title": "크롤링된 뉴스",
  "content": "뉴스 내용",
  "category": "POLITICS",
  "press": "조선일보",
  "url": "https://example.com/news"
}
```

### 3.11 관리자 관리 API

#### 3.11.1 크롤링된 뉴스 목록 조회
```http
GET /api/admin/crawled-news?page=0&size=10
Authorization: Bearer {token}
```

**쿼리 파라미터**
- `page` (Integer, optional): 페이지 번호
- `size` (Integer, optional): 페이지 크기

**응답**
```json
{
  "content": [
    {
      "id": 1,
      "title": "크롤링된 뉴스",
      "category": "POLITICS",
      "press": "조선일보",
      "crawledAt": "2024-01-01T10:00:00"
    }
  ],
  "totalElements": 100
}
```

#### 3.11.2 크롤링 뉴스 승격
```http
POST /api/admin/promote/{newsCrawlId}
Authorization: Bearer {token}
```

**경로 변수**
- `newsCrawlId` (Long): 크롤링 뉴스 ID

**응답**
```json
{
  "message": "뉴스가 성공적으로 승격되었습니다."
}
```

## 4. 공통 응답 형식

### 4.1 성공 응답
```json
{
  "success": true,
  "data": {},
  "message": "성공"
}
```

### 4.2 실패 응답
```json
{
  "success": false,
  "error": "에러 메시지",
  "code": "ERROR_CODE"
}
```

### 4.3 페이징 응답
```json
{
  "content": [],
  "totalElements": 100,
  "totalPages": 10,
  "size": 10,
  "number": 0,
  "first": true,
  "last": false
}
```

## 5. 에러 코드

| 코드 | 설명 |
|------|------|
| 400 | 잘못된 요청 |
| 401 | 인증 필요 |
| 403 | 권한 없음 |
| 404 | 리소스를 찾을 수 없음 |
| 500 | 서버 내부 오류 |

## 6. 카테고리 목록

| 코드 | 표시명 | 설명 |
|------|--------|------|
| POLITICS | 정치 | 정치 관련 뉴스 |
| ECONOMY | 경제 | 경제 관련 뉴스 |
| SOCIETY | 사회 | 사회 관련 뉴스 |
| LIFE | 생활 | 생활 관련 뉴스 |
| INTERNATIONAL | 국제 | 국제 관련 뉴스 |
| IT_SCIENCE | IT/과학 | IT/과학 관련 뉴스 |
| VEHICLE | 자동차 | 자동차 관련 뉴스 |
| TRAVEL_FOOD | 여행/음식 | 여행/음식 관련 뉴스 |
| ART | 예술 | 예술 관련 뉴스 |

## 7. 정렬 옵션

| 필드 | 설명 |
|------|------|
| publishedAt | 발행일 기준 |
| views | 조회수 기준 |
| relevance | 관련도 기준 |

## 8. 보안

### 8.1 인증이 필요한 API
- 뉴스 신고 (`POST /api/news/{newsId}/report`)
- 뉴스 스크랩 (`POST /api/news/{newsId}/scrap`)
- 스크랩 보관함 관련 API
- 마이페이지 API
- 관리자 API

### 8.2 인증 방식
```http
Authorization: Bearer {JWT_TOKEN}
```

## 9. 레거시 API

레거시 API는 하위 호환성을 위해 유지되지만, 새로운 개발에서는 새로운 API를 사용하는 것을 권장합니다.

- `GET /api/news/health` → `GET /api/system/health`
- `GET /api/news/categories` → `GET /api/news/categories`
- `GET /api/news/category/{categoryName}` → `GET /api/news/categories/{categoryName}/news`
- `GET /api/news/trending` → `GET /api/trending`
- `GET /api/news/popular` → `GET /api/trending/popular`
- `GET /api/news/latest` → `GET /api/trending/latest`
- `GET /api/news/personalized` → `GET /api/personalization/news`
- `GET /api/news/recommendations` → `GET /api/personalization/recommendations`
- `GET /api/news/search` → `GET /api/search`
- `GET /api/news/press/{press}` → `GET /api/search/press/{press}`
- `POST /api/news/promote/{newsCrawlId}` → `POST /api/admin/promote/{newsCrawlId}`
