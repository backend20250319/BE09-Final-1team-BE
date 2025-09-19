# News Service API

뉴스 서비스의 API 문서입니다.

## 🚀 API 엔드포인트

### 1. 뉴스 크롤링 데이터 저장

크롤러에서 수집한 뉴스 데이터를 저장하는 API입니다.

**POST** `/api/news/crawl`

**Request Body:**

```json
{
  "link": "https://example.com/news/article/12345",
  "title": "뉴스 제목",
  "press": "언론사명",
  "content": "뉴스 내용",
  "reporter": "기자명",
  "date": "2025-08-02T14:00:00",
  "categoryId": 1
}
```

**Response:**

```
HTTP 200 OK
```

### 2. 뉴스 승격 (관리자용)

크롤링된 뉴스를 승격하여 프론트엔드에 노출할 뉴스로 전환합니다.

**POST** `/api/news/promote/{newsCrawlId}`

**Response:**

```json
{
  "message": "뉴스가 성공적으로 승격되었습니다."
}
```

### 3. 커스텀 요약으로 뉴스 승격 (관리자용)

요약과 신뢰도를 직접 지정하여 뉴스를 승격합니다.

**POST** `/api/news/promote/{newsCrawlId}/custom?summary=요약내용&trusted=85`

**Parameters:**

- `summary`: 뉴스 요약 (필수)
- `trusted`: 신뢰도 점수 0-100 (필수)

### 4. 승격 대기 뉴스 목록 조회 (관리자용)

아직 승격되지 않은 크롤링 뉴스 목록을 조회합니다.

**GET** `/api/news/pending`

**Response:**

```json
[
  {
    "rawId": 1,
    "link": "https://example.com/news1",
    "title": "뉴스 제목",
    "press": "언론사",
    "content": "뉴스 내용",
    "reporterName": "기자명",
    "categoryId2": 1,
    "createdAt": "2025-08-02T14:00:00"
  }
]
```

### 5. 뉴스 조회 (프론트엔드용)

승격된 뉴스만 조회합니다.

**GET** `/api/news`

**Query Parameters:**

- `category`: 뉴스 카테고리 (선택사항)
- `keyword`: 검색 키워드 (선택사항)
- `page`: 페이지 번호 (기본값: 0)
- `size`: 페이지 크기 (기본값: 20)

### 6. 특정 뉴스 조회

**GET** `/api/news/{newsId}`

### 7. 개인화 뉴스 조회

**GET** `/api/news/personalized`

**Headers:**

- `X-User-Id`: 사용자 ID

### 8. 인기 뉴스 조회

**GET** `/api/news/trending`

### 9. 조회수 증가

**POST** `/api/news/{newsId}/view`

## 📊 전체 데이터 흐름

### 단계별 처리 과정

1. **크롤링 단계**

   ```
   크롤러 → POST /api/news/crawl → news_crawl 테이블 저장
   ```

2. **승격 단계** (관리자 또는 자동화)

   ```
   GET /api/news/pending → 승격 대기 목록 확인
   POST /api/news/promote/{id} → news 테이블로 승격
   ```

3. **노출 단계** (프론트엔드)
   ```
   GET /api/news → 승격된 뉴스만 조회하여 사용자에게 제공
   ```

### 데이터베이스 구조

- **`news_crawl`**: 크롤링된 원본 데이터 (승격 전)
- **`news`**: 승격된 정제된 데이터 (사용자에게 노출)

## 🔧 크롤러 연동 방법

### 1. 크롤러용 DTO (NewsDetail.java)

```java
@Getter @Setter @Builder
public class NewsDetail {
    private String link;
    private String title;
    private String press;
    private String content;
    private String reporter;
    private String date; // "2025-08-02T14:00:00"
    private Integer categoryId;
}
```

### 2. 크롤러 전송 코드 (NewsSender.java)

```java
public class NewsSender {
    public static void main(String[] args) {
        NewsDetail dto = NewsDetail.builder()
            .link("https://example.com/news1")
            .title("AI 기술의 발전")
            .press("테스트 언론사")
            .content("본문 내용")
            .reporter("홍길동")
            .date("2025-08-02T14:00:00")
            .categoryId(3)
            .build();

        RestTemplate restTemplate = new RestTemplate();
        String apiUrl = "http://localhost:8082/api/news/crawl";
        restTemplate.postForObject(apiUrl, dto, Void.class);
    }
}
```

### 3. 백엔드 DTO 구조

```java
public class NewsCrawlDto {
    private String link;           // 뉴스 링크
    private String title;          // 뉴스 제목
    private String press;          // 언론사
    private String content;        // 뉴스 내용
    private String reporter;       // 기자명
    private String date;           // 날짜 (ISO 형식)
    private Integer categoryId;    // 카테고리 ID
}
```

## 📊 데이터베이스 스키마

### news_crawl 테이블

- `raw_id`: 기본키 (자동 생성)
- `link_id`: 링크 ID
- `link`: 뉴스 링크 (TEXT)
- `title`: 뉴스 제목 (TEXT)
- `press`: 언론사 (TEXT)
- `content`: 뉴스 내용 (TEXT, NOT NULL)
- `created_at`: 생성 시간 (자동 생성)
- `reporter_name`: 기자명 (TEXT)
- `category_id2`: 카테고리 ID

## 🛠️ 개발 환경

- **Java**: 17
- **Spring Boot**: 3.x
- **Database**: MySQL/PostgreSQL
- **Build Tool**: Gradle

## 🚀 실행 방법

```bash
cd news-service
./gradlew bootRun
```

서비스는 기본적으로 `http://localhost:8082`에서 실행됩니다.

## 📝 테스트

테스트 데이터 생성:

```bash
curl -X POST http://localhost:8082/api/news/test-data
```

크롤링 데이터 전송 테스트:

```bash
curl -X POST http://localhost:8082/api/news/crawl \
  -H "Content-Type: application/json" \
  -d '{
    "link": "https://example.com/news/article/12345",
    "title": "테스트 뉴스",
    "press": "테스트 언론사",
    "content": "테스트 뉴스 내용입니다.",
    "reporter": "테스트 기자",
    "date": "2025-08-02T14:00:00",
    "categoryId": 1
  }'
```

## 📧 뉴스레터 API

### 1. 뉴스레터 구독

**POST** `/api/newsletter/subscribe`

**Request Body:**

```json
{
  "email": "user@example.com"
}
```

**Response:**

```json
{
  "message": "구독 확인 메일을 발송했습니다."
}
```

### 2. 구독 확인

**GET** `/api/newsletter/confirm?token={token}`

**Response:**

```json
{
  "message": "구독이 완료되었습니다."
}
```

### 3. 구독자 수 조회

**GET** `/api/newsletter/count`

**Response:**

```json
{
  "count": 1234
}
```

### 뉴스레터 테스트

```bash
# 구독 요청
curl -X POST http://localhost:8082/api/newsletter/subscribe \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'

# 구독자 수 조회
curl -X GET http://localhost:8082/api/newsletter/count
```

## 📊 카테고리 시스템

### 지원하는 카테고리

- `POLITICS`: 정치 🏛️
- `ECONOMY`: 경제 💰
- `SOCIETY`: 사회 👥
- `LIFE`: 생활/문화 🎭
- `INTERNATIONAL`: 세계 🌍
- `IT_SCIENCE`: IT/과학 💻

### 카테고리별 뉴스 조회

**GET** `/api/news?category={category}&page={page}&size={size}`

**예시:**

```bash
# 사회 카테고리 뉴스 조회
curl -X GET "http://localhost:8082/api/news?category=SOCIETY&page=0&size=10"
```
