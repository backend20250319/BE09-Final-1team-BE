# 🚀 Crawler Service 배포 환경 가이드

## 📋 개요

이 가이드는 Crawler Service를 배포 환경에서 실행하기 위한 설정과 최적화 방법을 설명합니다.

## 🔧 배포 환경 최적화 사항

### ✅ 해결된 문제점들

1. **CSV 파일 의존성 완전 제거**

   - ❌ 기존: CSV 파일 생성 → Python 스크립트 실행
   - ✅ 개선: 메모리 기반 크롤링 → 파일서버 저장 → Python 중복 제거

2. **파일 시스템 접근 제한 해결**

   - 컨테이너 환경에서 안정적 동작
   - 임시 파일 시스템 의존성 없음

3. **확장성 및 안정성 향상**
   - 파일서버 기반 데이터 관리
   - 배치 처리로 메모리 효율성 증대
   - 재시도 및 타임아웃 설정

## 🏗️ 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Naver News    │───▶│  Crawler Service │───▶│  File Server    │
│   (크롤링 대상)  │    │  (메모리 기반)   │    │   (중간 저장)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │  중복 제거      │    │  News Service   │
                       │ (파일서버 기반) │───▶│  (최종 저장)    │
                       └─────────────────┘    └─────────────────┘
```

## 🚀 실행 방법

### 1. 환경 변수 설정

```bash
# 파일서버 설정
export FILESERVER_BASE_PATH=/data/news-fileserver

# 데이터베이스 설정
export DATABASE_URL=jdbc:mysql://your-mysql-host:3306/newsdb
export DATABASE_USERNAME=your-username
export DATABASE_PASSWORD=your-password

# 크롤링 설정
export CRAWLER_TARGET_COUNT=100
export CRAWLER_BATCH_SIZE=10
export CRAWLER_MAX_CONCURRENT=5

# 서비스 설정
export SERVER_PORT=8083
export EUREKA_SERVER_URL=http://discovery-service:8761/eureka/
```

### 2. 애플리케이션 실행

```bash
# 개발 환경
./gradlew bootRun

# 배포 환경
java -jar -Dspring.profiles.active=prod crawler-service.jar
```

### 3. Docker 실행

```bash
# Docker 이미지 빌드
docker build -t crawler-service .

# Docker 컨테이너 실행
docker run -d \
  --name crawler-service \
  -p 8083:8083 \
  -e REDIS_HOST=redis \
  -e DATABASE_URL=jdbc:mysql://mysql:3306/newsdb \
  crawler-service
```

## 📊 API 엔드포인트

### 크롤링 시작

```bash
# 배포 환경 최적화 크롤링
curl -X POST http://localhost:8083/api/crawler/deployment-optimized/start

# 응답
{
  "status": "success",
  "message": "배포 환경 최적화 크롤링이 시작되었습니다.",
  "timestamp": "2024-01-15T10:30:00"
}
```

### 중복 제거 통계 조회

```bash
curl http://localhost:8083/api/crawler/deduplication/stats

# 응답
{
  "status": "success",
  "stats": {
    "정치": {
      "original": 100,
      "deduplicated": 85,
      "removed": 15,
      "removalRate": 15.0
    },
    "경제": {
      "original": 100,
      "deduplicated": 90,
      "removed": 10,
      "removalRate": 10.0
    }
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

## ⚙️ 설정 옵션

### application-prod.yml 주요 설정

```yaml
# Redis 설정 (배포 환경)
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      maxmemory: 1gb
      maxmemory-policy: allkeys-lru

# 크롤링 설정
crawler:
  target-count: ${CRAWLER_TARGET_COUNT:100}
  batch-size: ${CRAWLER_BATCH_SIZE:10}
  max-concurrent-requests: ${CRAWLER_MAX_CONCURRENT:5}
  request-delay: ${CRAWLER_REQUEST_DELAY:1000}

# 모니터링 설정
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

## 🔍 모니터링

### 헬스 체크

```bash
curl http://localhost:8083/actuator/health
```

### 메트릭 조회

```bash
curl http://localhost:8083/actuator/metrics
```

### Prometheus 메트릭

```bash
curl http://localhost:8083/actuator/prometheus
```

## 🐛 문제 해결

### 1. Redis 연결 실패

```bash
# Redis 연결 확인
redis-cli -h your-redis-host -p 6379 ping

# 로그 확인
docker logs crawler-service | grep Redis
```

### 2. 크롤링 실패

```bash
# 크롤링 로그 확인
docker logs crawler-service | grep "크롤링"

# 메모리 사용량 확인
docker stats crawler-service
```

### 3. 중복 제거 실패

```bash
# 중복 제거 통계 확인
curl http://localhost:8083/api/crawler/deduplication/stats

# Redis 데이터 확인
redis-cli -h your-redis-host keys "crawled:news:*"
```

## 📈 성능 최적화

### 1. 메모리 설정

```yaml
# JVM 힙 메모리 설정
JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC"
```

### 2. Redis 최적화

```yaml
# Redis 메모리 정책
maxmemory-policy: allkeys-lru
maxmemory: 1gb
```

### 3. 크롤링 최적화

```yaml
# 동시 요청 수 조정
max-concurrent-requests: 5
request-delay: 1000
batch-size: 10
```

## 🔄 스케줄링

### 자동 크롤링 설정

```yaml
# 매일 오전 9시, 오후 7시 크롤링
@Scheduled(cron = "0 0 9,19 * * *")
```

### 수동 크롤링

```bash
# API 호출로 수동 크롤링
curl -X POST http://localhost:8083/api/crawler/deployment-optimized/start
```

## 📝 로그 관리

### 로그 레벨 설정

```yaml
logging:
  level:
    com.newnormallist.crawlerservice: INFO
    org.springframework.web: WARN
    org.hibernate: OFF
  file:
    name: logs/crawler-service.log
```

### 로그 모니터링

```bash
# 실시간 로그 확인
docker logs -f crawler-service

# 특정 패턴 로그 확인
docker logs crawler-service | grep "크롤링 완료"
```

## 🎯 성공 지표

### 정상 동작 확인 사항

1. ✅ Redis 연결 성공
2. ✅ 크롤링 시작 및 완료
3. ✅ 중복 제거 실행
4. ✅ News Service 전송 성공
5. ✅ 메모리 사용량 안정적
6. ✅ 로그에 오류 없음

### 성능 지표

- **크롤링 속도**: 카테고리당 100개 기사 < 5분
- **중복 제거율**: 10-20%
- **메모리 사용량**: < 2GB
- **Redis 메모리**: < 1GB
- **응답 시간**: API 호출 < 1초

## 🚨 주의사항

1. **네트워크 안정성**: 크롤링 중 네트워크 오류 대비
2. **메모리 관리**: 대용량 데이터 처리 시 메모리 모니터링
3. **Redis 백업**: 중요 데이터 백업 정책 수립
4. **모니터링**: 지속적인 성능 및 오류 모니터링
5. **스케일링**: 트래픽 증가 시 수평 확장 고려
