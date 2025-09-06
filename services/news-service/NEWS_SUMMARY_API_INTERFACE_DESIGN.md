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
| `lines` | Integer | 요약 라인 수 |
| `summary` | String | 요약된 텍스트 |
| `cached` | Boolean | 캐시 히트 여부 |
| `createAt` | String | 생성 시간 |

## 4. 요청 시나리오

### 4.1 뉴스 ID 기반 요약

```json
{
  "newsId": 12345,
  "type": "DEFAULT",
  "lines": 5,
  "force": false
}
```

**동작:**
1. DB에서 캐시된 요약 확인
2. 캐시가 있으면 반환 (force=false인 경우)
3. 캐시가 없으면 Flask API 호출하여 요약 생성
4. 생성된 요약을 DB에 저장 후 반환

### 4.2 텍스트 기반 임시 요약

```json
{
  "text": "요약할 뉴스 내용...",
  "type": "DEFAULT",
  "lines": 3
}
```

**동작:**
1. Flask API 호출하여 텍스트 요약
2. DB 저장 없이 결과 반환

### 4.3 강제 재생성

```json
{
  "newsId": 12345,
  "type": "DEFAULT",
  "lines": 3,
  "force": true
}
```

**동작:**
1. 캐시 무시하고 Flask API 호출
2. 새로운 요약 생성 후 DB 업데이트
3. 결과 반환

## 5. 에러 응답

### 5.1 유효성 검증 에러

```json
{
  "timestamp": "2024-01-01T00:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "newsId 또는 text 중 하나는 필수입니다."
}
```

### 5.2 뉴스 없음 에러

```json
{
  "timestamp": "2024-01-01T00:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "뉴스를 찾을 수 없습니다: id=12345"
}
```

## 6. 제약사항

- `lines` 필드는 1-10 범위 내에서만 유효
- `type` 필드의 "AIBOT"은 "DEFAULT"로 자동 변환
- `newsId`와 `text` 중 하나는 반드시 제공되어야 함
- 텍스트 요약은 DB에 저장되지 않음
- 캐시는 `(newsId, summaryType)` 조합으로 관리

## 7. 성능 고려사항

- DB 캐싱을 통한 응답 시간 최적화
- Flask API 호출 시 타임아웃 설정
- 대용량 텍스트 처리 시 메모리 사용량 고려
- 동시 요청 처리 시 DB 락 최소화
