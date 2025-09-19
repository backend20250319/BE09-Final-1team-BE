# 연관뉴스 API 가이드

## 개요

뉴스 상세 페이지에서 하단에 표시될 연관뉴스를 조회하는 API입니다.

## API 엔드포인트

### 연관뉴스 조회

```
GET /api/news/{newsId}/related
```

**요청 파라미터:**

- `newsId` (path): 조회할 뉴스의 ID

**응답:**

```json
[
  {
    "newsId": 1,
    "title": "뉴스 제목",
    "press": "언론사명",
    "publishedAt": "2024-01-01 12:00:00",
    "reporter": "기자명",
    "createdAt": "2024-01-01T12:00:00",
    "imageUrl": "이미지 URL",
    "summary": "뉴스 요약",
    "categoryName": "POLITICS"
  }
]
```

## 연관뉴스 조회 로직

### 1. REPRESENTATIVE 상태의 뉴스

- 해당 뉴스의 `oid_aid`가 `related_news` 테이블의 `rep_oid_aid`인 행들의 `related_oid_aid`를 조회
- 해당 `related_oid_aid`를 가진 뉴스들을 연관뉴스로 선정
- 4개 이상이면 랜덤으로 4개 선택
- 4개 미만이면 같은 `created_at`, 같은 `category_name`, 같은 시간대(오전/오후)인 뉴스로 채움

### 2. KEPT 상태의 뉴스

- 같은 `created_at`, 같은 `category_name`인 뉴스를 랜덤으로 4개 조회 (해당 뉴스 제외)

### 3. RELATED 상태의 뉴스

- 해당 뉴스의 `oid_aid`가 `related_news` 테이블의 `related_oid_aid`인 행의 `rep_oid_aid` 조회
- 해당 `rep_oid_aid`를 `oid_aid`로 가지는 뉴스와 연관관계인 뉴스들을 조회
- 4개 미만이면 같은 `created_at`, 같은 `category_name`, 같은 시간대인 뉴스로 채움

## 데이터베이스 스키마

### related_news 테이블

```sql
CREATE TABLE related_news (
    rep_oid_aid VARCHAR(255) NOT NULL COMMENT '대표 뉴스의 oid_aid',
    related_oid_aid VARCHAR(255) NOT NULL COMMENT '연관 뉴스의 oid_aid',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (rep_oid_aid, related_oid_aid)
);
```

## 테스트

### API 테스트

```bash
# 테스트 스크립트 실행
chmod +x test_related_news_api.sh
./test_related_news_api.sh
```

### 수동 테스트

```bash
curl -X GET "http://localhost:8083/api/news/1/related"
```

## 파일 구조

```
services/news-service/src/main/java/com/newsservice/news/
├── entity/
│   └── RelatedNews.java              # RelatedNews 엔티티
├── repository/
│   └── RelatedNewsRepository.java    # RelatedNews 리포지토리
├── dto/
│   └── RelatedNewsResponseDto.java   # 연관뉴스 응답 DTO
├── service/
│   ├── RelatedNewsService.java       # 연관뉴스 서비스 인터페이스
│   └── RelatedNewsServiceImpl.java   # 연관뉴스 서비스 구현체
└── controller/
    └── RelatedNewsController.java    # 연관뉴스 컨트롤러
```

## 주의사항

1. **데이터베이스 설정**: `related_news` 테이블이 생성되어 있어야 합니다.
2. **인덱스**: 성능을 위해 `rep_oid_aid`, `related_oid_aid` 컬럼에 인덱스가 설정되어 있습니다.
3. **최대 개수**: 연관뉴스는 최대 4개까지만 반환됩니다.
4. **시간대 필터링**: 오전/오후 시간대가 동일한 뉴스만 연관뉴스로 선정됩니다.
