# News Service API 마이그레이션 가이드

## 🔄 컨트롤러 분리 완료

기존의 단일 `NewsController`가 7개의 분리된 컨트롤러로 나뉘어졌습니다.

### 📋 새로운 컨트롤러 구조

| 컨트롤러                    | 역할             | 새로운 URL 패턴          |
| --------------------------- | ---------------- | ------------------------ |
| `SystemController`          | 시스템 관리      | `/api/system/*`          |
| `NewsController`            | 핵심 뉴스 CRUD   | `/api/news/*`            |
| `CategoryController`        | 카테고리 관리    | `/api/categories/*`      |
| `TrendingController`        | 트렌딩/인기 뉴스 | `/api/trending/*`        |
| `PersonalizationController` | 개인화/추천      | `/api/personalization/*` |
| `SearchController`          | 검색/필터링      | `/api/search/*`          |
| `AdminController`           | 관리자 기능      | `/api/admin/*`           |

## 🔗 URL 변경사항

### 기존 → 새로운 URL 매핑

| 기존 URL                        | 새로운 URL                                 | 설명               |
| ------------------------------- | ------------------------------------------ | ------------------ |
| `GET /api/news/health`          | `GET /api/system/health`                   | 헬스체크           |
| `GET /api/news/test-db`         | `GET /api/system/test-db`                  | DB 연결 테스트     |
| `GET /api/news/categories`      | `GET /api/categories`                      | 카테고리 목록      |
| `GET /api/news/category/{name}` | `GET /api/categories/{name}/news`          | 카테고리별 뉴스    |
| `GET /api/news/trending`        | `GET /api/trending`                        | 트렌딩 뉴스        |
| `GET /api/news/trending/list`   | `GET /api/trending/list`                   | 트렌딩 뉴스 리스트 |
| `GET /api/news/popular`         | `GET /api/trending/popular`                | 인기 뉴스          |
| `GET /api/news/latest`          | `GET /api/trending/latest`                 | 최신 뉴스          |
| `GET /api/news/personalized`    | `GET /api/personalization/news`            | 개인화 뉴스        |
| `GET /api/news/recommendations` | `GET /api/personalization/recommendations` | 추천 뉴스          |
| `GET /api/news/search`          | `GET /api/search`                          | 키워드 검색        |
| `GET /api/news/press/{press}`   | `GET /api/search/press/{press}`            | 언론사별 뉴스      |
| `POST /api/news/promote/{id}`   | `POST /api/admin/promote/{id}`             | 뉴스 승격          |

## 🚀 마이그레이션 전략

### 1단계: 점진적 적용 (현재)

- ✅ 새로운 컨트롤러들 생성 완료
- ✅ `LegacyNewsController`로 기존 URL 호환성 유지
- ✅ 기존 API는 `@Deprecated` 경고와 함께 계속 동작

### 2단계: 클라이언트 업데이트

- 프론트엔드에서 새로운 URL로 점진적 전환
- 기존 URL 사용량 모니터링

### 3단계: 기존 API 제거

- 사용량이 0이 되면 `LegacyNewsController` 제거
- 기존 URL 완전 폐기

## 📊 각 컨트롤러별 API 목록

### SystemController (`/api/system`)

```http
GET /api/system/health          # 헬스체크
GET /api/system/test-db         # DB 연결 테스트
```

### NewsController (`/api/news`)

```http
GET /api/news                   # 뉴스 목록 (페이징)
GET /api/news/{newsId}          # 뉴스 상세 조회
GET /api/news/count             # 뉴스 개수
POST /api/news/{newsId}/view    # 조회수 증가
```

### CategoryController (`/api/categories`)

```http
GET /api/categories                    # 카테고리 목록
GET /api/categories/{name}/news        # 카테고리별 뉴스
GET /api/categories/{name}/count       # 카테고리별 뉴스 개수
```

### TrendingController (`/api/trending`)

```http
GET /api/trending              # 트렌딩 뉴스 (페이징)
GET /api/trending/list         # 트렌딩 뉴스 (리스트)
GET /api/trending/popular      # 인기 뉴스
GET /api/trending/latest       # 최신 뉴스
```

### PersonalizationController (`/api/personalization`)

```http
GET /api/personalization/news           # 개인화 뉴스
GET /api/personalization/recommendations # 추천 뉴스
```

### SearchController (`/api/search`)

```http
GET /api/search                # 키워드 검색
GET /api/search/press/{press}  # 언론사별 뉴스
GET /api/search/date-range     # 기간별 뉴스
```

### AdminController (`/api/admin`)

```http
POST /api/admin/promote/{newsCrawlId}  # 뉴스 승격
```

## ⚠️ 주의사항

1. **호환성 유지**: 기존 URL들은 `LegacyNewsController`를 통해 계속 동작합니다.
2. **Deprecated 경고**: 기존 API들은 `@Deprecated` 어노테이션이 추가되어 경고를 표시합니다.
3. **점진적 전환**: 클라이언트들이 새로운 URL로 전환할 시간을 충분히 제공합니다.

## 🎯 다음 단계

1. 새로운 API 구조 테스트
2. 프론트엔드 팀과 협의하여 마이그레이션 계획 수립
3. 모니터링 도구 설정으로 기존 API 사용량 추적
4. 사용량이 0이 되면 기존 API 제거

---

**마이그레이션 완료일**: 2024년 현재  
**예상 완전 제거일**: 기존 API 사용량 0% 달성 시
