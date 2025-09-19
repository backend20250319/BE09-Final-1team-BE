# 뉴스 서비스 API 테스트케이스

## 1. 시스템 관리 API 테스트

### TC-SYS-001: 헬스 체크
```http
GET /api/system/health
```
**예상 결과**: 200 OK, "News Service is running"

### TC-SYS-002: 데이터베이스 연결 테스트
```http
GET /api/system/test-db
```
**예상 결과**: 200 OK, "데이터베이스 연결 성공. 뉴스 개수: 1234"

## 2. 뉴스 관리 API 테스트

### TC-NEWS-001: 뉴스 총 개수 조회
```http
GET /api/news/count
```
**예상 결과**: 200 OK, 1234

### TC-NEWS-002: 뉴스 목록 조회
```http
GET /api/news?page=0&size=10
```
**예상 결과**: 200 OK, 페이징된 뉴스 목록

### TC-NEWS-003: 뉴스 상세 조회
```http
GET /api/news/1
```
**예상 결과**: 200 OK, 뉴스 상세 정보

### TC-NEWS-004: 조회수 증가
```http
POST /api/news/1/view
```
**예상 결과**: 200 OK

### TC-NEWS-005: 뉴스 신고 (인증 필요)
```http
POST /api/news/1/report
Authorization: Bearer {token}
```
**예상 결과**: 200 OK

### TC-NEWS-006: 뉴스 스크랩 (인증 필요)
```http
POST /api/news/1/scrap
Authorization: Bearer {token}
```
**예상 결과**: 200 OK

## 3. 카테고리 관리 API 테스트

### TC-CAT-001: 카테고리 목록 조회
```http
GET /api/news/categories
```
**예상 결과**: 200 OK, 카테고리 목록

### TC-CAT-002: 카테고리별 뉴스 조회
```http
GET /api/news/categories/POLITICS/news?page=0&size=10
```
**예상 결과**: 200 OK, 정치 카테고리 뉴스

### TC-CAT-003: 잘못된 카테고리 요청
```http
GET /api/news/categories/INVALID/news
```
**예상 결과**: 400 Bad Request, 에러 메시지

## 4. 트렌딩 관리 API 테스트

### TC-TREND-001: 트렌딩 뉴스
```http
GET /api/trending?page=0&size=10
```
**예상 결과**: 200 OK, 트렌딩 뉴스 목록

### TC-TREND-002: 실시간 인기 키워드
```http
GET /api/trending/trending-keywords?limit=10
```
**예상 결과**: 200 OK, 인기 키워드 목록

### TC-TREND-003: 카테고리별 트렌딩 키워드
```http
GET /api/trending/trending-keywords/category/POLITICS
```
**예상 결과**: 200 OK, 정치 카테고리 키워드

## 5. 검색 관리 API 테스트

### TC-SEARCH-001: 뉴스 검색
```http
GET /api/search?query=AI&page=0&size=10
```
**예상 결과**: 200 OK, 검색 결과

### TC-SEARCH-002: 언론사별 뉴스
```http
GET /api/search/press/조선일보?page=0&size=10
```
**예상 결과**: 200 OK, 조선일보 뉴스

### TC-SEARCH-003: 기간별 뉴스
```http
GET /api/search/date-range?startDate=2024-01-01&endDate=2024-01-31
```
**예상 결과**: 200 OK, 기간 내 뉴스

## 6. 개인화 관리 API 테스트

### TC-PERS-001: 개인화 뉴스
```http
GET /api/personalization/news
X-User-Id: 123
```
**예상 결과**: 200 OK, 개인화 뉴스

### TC-PERS-002: 추천 뉴스
```http
GET /api/personalization/recommendations?userId=123
```
**예상 결과**: 200 OK, 추천 뉴스

## 7. 마이페이지 관리 API 테스트

### TC-MYPAGE-001: 내 스크랩 목록 (인증 필요)
```http
GET /api/news/mypage/scraps
Authorization: Bearer {token}
```
**예상 결과**: 200 OK, 스크랩 목록

### TC-MYPAGE-002: 스크랩 해제 (인증 필요)
```http
DELETE /api/news/mypage/scraps/1
Authorization: Bearer {token}
```
**예상 결과**: 204 No Content

## 8. 키워드 구독 관리 API 테스트

### TC-KEYWORD-001: 키워드 구독
```http
POST /api/keywords/subscribe?userId=123&keyword=AI
```
**예상 결과**: 200 OK, 구독 정보

### TC-KEYWORD-002: 키워드 구독 해제
```http
DELETE /api/keywords/unsubscribe?userId=123&keyword=AI
```
**예상 결과**: 200 OK

### TC-KEYWORD-003: 사용자 키워드 구독 목록
```http
GET /api/keywords/user/123
```
**예상 결과**: 200 OK, 구독 목록

## 9. 연관 뉴스 관리 API 테스트

### TC-RELATED-001: 연관 뉴스 조회
```http
GET /api/related/1
```
**예상 결과**: 200 OK, 연관 뉴스 목록

## 10. 크롤링 관리 API 테스트

### TC-CRAWL-001: 크롤링 뉴스 저장
```http
POST /api/news/crawl
Content-Type: application/json
{
  "title": "크롤링된 뉴스",
  "content": "뉴스 내용",
  "category": "POLITICS",
  "press": "조선일보"
}
```
**예상 결과**: 200 OK, 저장 성공 메시지

### TC-CRAWL-002: 크롤링 뉴스 미리보기
```http
POST /api/news/crawl/preview
Content-Type: application/json
{
  "title": "크롤링된 뉴스",
  "content": "뉴스 내용"
}
```
**예상 결과**: 200 OK, 미리보기 데이터

## 11. 관리자 관리 API 테스트

### TC-ADMIN-001: 크롤링된 뉴스 목록 조회 (인증 필요)
```http
GET /api/admin/crawled-news
Authorization: Bearer {token}
```
**예상 결과**: 200 OK, 크롤링 뉴스 목록

### TC-ADMIN-002: 크롤링 뉴스 승격 (인증 필요)
```http
POST /api/admin/promote/1
Authorization: Bearer {token}
```
**예상 결과**: 200 OK, 승격 성공 메시지

## 12. 에러 케이스 테스트

### TC-ERROR-001: 인증 없는 요청
```http
GET /api/news/mypage/scraps
```
**예상 결과**: 401 Unauthorized

### TC-ERROR-002: 존재하지 않는 리소스
```http
GET /api/news/99999
```
**예상 결과**: 404 Not Found

### TC-ERROR-003: 잘못된 파라미터
```http
GET /api/news/categories/INVALID/news
```
**예상 결과**: 400 Bad Request

### TC-ERROR-004: 서버 오류 시뮬레이션
```http
GET /api/system/test-db
```
**예상 결과**: 500 Internal Server Error (DB 연결 실패 시)

## 13. 성능 테스트

### TC-PERF-001: 대용량 데이터 조회
```http
GET /api/news?page=0&size=1000
```
**예상 결과**: 응답 시간 < 2초

### TC-PERF-002: 복잡한 검색
```http
GET /api/search?query=AI&category=POLITICS&sortBy=publishedAt&sortOrder=desc
```
**예상 결과**: 응답 시간 < 1초

## 14. 보안 테스트

### TC-SEC-001: SQL 인젝션 방지
```http
GET /api/search?query='; DROP TABLE news; --
```
**예상 결과**: 400 Bad Request

### TC-SEC-002: XSS 방지
```http
POST /api/news/crawl
Content-Type: application/json
{
  "title": "<script>alert('XSS')</script>"
}
```
**예상 결과**: 400 Bad Request

## 15. 테스트 실행 가이드

### 15.1 환경 설정
```bash
# 서버 시작
cd services/news-service
./gradlew bootRun

# JWT 토큰 생성 (테스트용)
export TEST_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 15.2 테스트 실행
```bash
# cURL 예시
curl -X GET "http://localhost:8080/api/system/health"
curl -X GET "http://localhost:8080/api/news/count"
curl -X GET "http://localhost:8080/api/news/mypage/scraps" \
  -H "Authorization: Bearer $TEST_TOKEN"
```

### 15.3 테스트 결과 검증
- 응답 코드 확인 (200, 201, 204, 400, 401, 404, 500)
- JSON 응답 형식 검증
- 필수 필드 존재 확인
- 페이징 정보 검증
- 에러 메시지 검증
