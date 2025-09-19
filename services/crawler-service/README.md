# Crawler Service

뉴스 크롤링을 담당하는 마이크로 서비스입니다.

## 🚀 주요 기능

- **네이버 뉴스 크롤링**: 정치, 경제, 사회, 세계, IT과학 등 9개 카테고리
- **Redis 캐싱**: 크롤링 결과 임시 저장
- **중복 제거**: Python 스크립트와 연동하여 중복 뉴스 제거
- **News Service 연동**: Feign Client를 통한 서비스 간 통신
- **스케줄링**: 매일 오전 9시, 오후 7시 자동 크롤링

## 📋 시스템 구성

### 1. 크롤링 프로세스

1. **뉴스 목록 크롤링**: 네이버 뉴스 섹션에서 뉴스 목록 수집
2. **상세 내용 크롤링**: 각 뉴스 링크에서 상세 내용 수집
3. **중복 제거**: Python 스크립트를 통한 중복 뉴스 제거
4. **News Service 전송**: 처리된 뉴스를 News Service로 전송

### 2. MSA 구성요소

- **Eureka Client**: 서비스 디스커버리 등록
- **Feign Client**: News Service와의 통신
- **Redis**: 크롤링 결과 캐싱
- **REST API**: 외부 요청 처리

## 🛠️ 설치 및 실행

### 1. 의존성 설치

```bash
cd services/crawler-service
./gradlew build
```

### 2. 서비스 실행

```bash
./gradlew bootRun
```

### 3. 서비스 확인

```bash
# 헬스 체크
curl http://localhost:8083/api/crawler/health

# 크롤링 시작
curl -X POST http://localhost:8083/api/crawler/start

# 상태 확인
curl http://localhost:8083/api/crawler/status
```

## 📡 API 엔드포인트

### 크롤링 관리

- `POST /api/crawler/start` - 전체 크롤링 프로세스 시작
- `GET /api/crawler/status` - 크롤링 상태 확인
- `POST /api/crawler/category/{category}` - 특정 카테고리 크롤링
- `GET /api/crawler/config` - 크롤링 설정 조회

### 헬스 체크

- `GET /api/crawler/health` - 서비스 헬스 체크

## ⚙️ 설정

### application.yml

```yaml
spring:
  application:
    name: crawler-service

  data:
    redis:
      host: localhost
      port: 6379

server:
  port: 8083

crawler:
  target-count: 100
  batch-size: 5
  max-concurrent-requests: 3
```

### 크롤링 설정

- **목표 개수**: 각 카테고리별 100개 (기본값)
- **배치 크기**: News Service 전송 시 5개씩
- **동시 요청**: 최대 3개
- **재시도**: 3회
- **대기 시간**: 요청 간 1.5초

## 🔄 서비스 간 통신

### News Service 연동

```java
@FeignClient(name = "news-service")
public interface NewsServiceClient {
    @PostMapping("/api/news/crawl/batch")
    ResponseEntity<String> saveNewsBatch(@RequestBody List<NewsDetail> newsList);
}
```

### Redis 캐싱

```java
// 크롤링 결과 저장
crawlerCacheService.saveCrawledNews(category, newsList);

// 크롤링 결과 조회
List<NewsDetail> newsList = crawlerCacheService.getCrawledNews(category);
```

## 🐍 Python 연동

### 중복 제거 스크립트

- **스크립트 경로**: `duplicate_detector/run_all_categories.py`
- **실행 방식**: ProcessBuilder를 통한 외부 프로세스 실행
- **결과 처리**: 로그를 통한 실행 상태 모니터링

## 📊 모니터링

### 로그 레벨

- **INFO**: 일반적인 크롤링 진행 상황
- **WARN**: 경고 상황 (파싱 실패 등)
- **ERROR**: 오류 상황 (크롤링 실패, 서비스 연결 실패 등)

### 주요 로그 메시지

```
🚀 전체 크롤링 프로세스 시작
📰 1단계: 뉴스 목록 크롤링 시작
📄 2단계: 뉴스 상세 크롤링 시작
🔍 3단계: 중복 제거 처리 시작
📤 4단계: News Service 전송 시작
✅ 전체 크롤링 프로세스 완료!
```

## 🚨 주의사항

1. **Chrome Driver**: Selenium 크롤링을 위해 Chrome Driver가 필요합니다.
2. **Redis 연결**: 크롤링 결과 캐싱을 위해 Redis 서버가 실행되어야 합니다.
3. **Python 환경**: 중복 제거를 위해 Python 3.7+와 필요한 패키지가 설치되어야 합니다.
4. **네트워크 안정성**: 크롤링 중 네트워크 오류가 발생할 수 있으므로 재시도 로직이 포함되어 있습니다.

## 🔧 문제 해결

### 크롤링 실패

1. Chrome Driver 버전 확인
2. 네트워크 연결 상태 확인
3. 네이버 뉴스 페이지 구조 변경 여부 확인

### Redis 연결 실패

1. Redis 서버 실행 상태 확인
2. Redis 포트(6379) 접근 가능 여부 확인
3. Redis 메모리 사용량 확인

### Python 스크립트 실행 실패

1. Python 설치 및 버전 확인
2. 필요한 패키지 설치 확인
3. 스크립트 경로 및 권한 확인
