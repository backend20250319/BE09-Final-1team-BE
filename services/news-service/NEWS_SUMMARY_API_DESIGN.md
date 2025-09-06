# 뉴스 요약 서비스 API 인터페이스 설계서

## 1. 개요

뉴스 요약 서비스는 뉴스 ID 기반 요약과 텍스트 기반 임시 요약을 제공하는 API입니다. Flask 기반의 요약 엔진과 연동하여 뉴스 내용을 요약하고, DB 캐싱을 통해 성능을 최적화합니다.

## 2. API 엔드포인트

### 2.1 뉴스 요약 API

**URL:** `POST /api/news/summary`  
**Content-Type:** `application/json`  
**Description:** 뉴스 요약 생성/조회 API

#### 요청 로직
- `newsId`가 있으면: ID 기반 요약 (DB 캐시 우선, force 지원)
- `text`가 있으면: 텍스트 임시 요약 (DB 저장 안 함)
- 둘 다 있으면: `newsId` 우선

## 3. 요청/응답 스키마

### 3.1 요청 스키마 (SummaryRequest)

```json
{
  "newsId": "Long (optional)",
  "text": "String (optional)",
  "type": "String (optional)",
  "lines": "Integer (1-10, optional, default: 3)",
  "promptOverride": "String (optional)",
  "force": "Boolean (optional, default: false)"
}
```

#### 필드 설명

| 필드명 | 타입 | 필수 | 기본값 | 설명 |
|--------|------|------|--------|------|
| `newsId` | Long | 조건부 | - | 뉴스 ID (newsId 또는 text 중 하나는 필수) |
| `text` | String | 조건부 | - | 요약할 텍스트 (newsId 또는 text 중 하나는 필수) |
| `type` | String | 선택 | "DEFAULT" | 요약 타입 (AIBOT → DEFAULT로 변환) |
| `lines` | Integer | 선택 | 3 | 요약 라인 수 (1-10 범위) |
| `promptOverride` | String | 선택 | - | 커스텀 프롬프트 |
| `force` | Boolean | 선택 | false | 캐시 무시하고 재생성 여부 |

### 3.2 응답 스키마 (SummaryResponse)

```json
{
  "newsId": "Long",
  "resolvedType": "String",
  "lines": "Integer",
  "summary": "String",
  "cached": "Boolean",
  "createAt": "String"
}
```

#### 필드 설명

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `newsId` | Long | 뉴스 ID (텍스트 요약시 0) |
| `resolvedType` | String | 실제 적용된 요약 타입 |
| `lines` | Integer | 적용된 요약 라인 수 |
| `summary` | String | 생성된 요약 텍스트 |
| `cached` | Boolean | 캐시 히트 여부 |
| `createAt` | String | 생성 시간 |

## 4. 비즈니스 로직

### 4.1 뉴스 ID 기반 요약 (getOrCreateSummary)

1. **캐시 확인**: `force=false`인 경우 DB에서 기존 요약 조회
2. **캐시 히트**: 기존 요약이 있으면 즉시 반환
3. **뉴스 조회**: 뉴스 ID로 뉴스 본문 조회
4. **요약 생성**: Flask API 호출하여 요약 생성
5. **DB 저장**: 요약 결과를 DB에 저장/업데이트

### 4.2 텍스트 기반 임시 요약 (summarizeText)

1. **텍스트 검증**: 입력 텍스트 유효성 확인
2. **요약 생성**: Flask API 호출하여 요약 생성
3. **응답 반환**: DB 저장 없이 결과만 반환

### 4.3 타입 정규화

- `null` 또는 빈 문자열 → "DEFAULT"
- "AIBOT" → "DEFAULT" (레거시 호환)
- 대문자 변환 및 하이픈을 언더스코어로 변환

### 4.4 라인 수 정규화

- `null` 또는 범위 외 값 → 3 (기본값)
- 1-10 범위 내 값 → 그대로 사용

## 5. 에러 처리

### 5.1 검증 에러

```json
{
  "error": "Validation Error",
  "message": "newsId 또는 text 중 하나는 필수입니다."
}
```

### 5.2 비즈니스 에러

```json
{
  "error": "Business Error",
  "message": "뉴스를 찾을 수 없습니다: id=123"
}
```

## 6. 성능 최적화

### 6.1 캐싱 전략

- **DB 캐싱**: (newsId, summaryType) 복합 키로 캐시
- **캐시 히트**: 기존 요약이 있으면 Flask API 호출 생략
- **Force 옵션**: `force=true`로 캐시 무시 가능

### 6.2 트랜잭션 관리

- **읽기 전용**: 캐시 조회, 텍스트 요약
- **쓰기 트랜잭션**: ID 기반 요약 생성/저장

## 7. 보안 고려사항

- 입력 텍스트 길이 제한 (Flask API에서 처리)
- 요약 타입 화이트리스트 검증
- SQL Injection 방지 (JPA 사용)

## 8. 모니터링

- 요약 생성 성공/실패 로그
- 캐시 히트율 모니터링
- Flask API 응답 시간 모니터링
- 요약 타입별 사용 통계
