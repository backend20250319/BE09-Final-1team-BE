# 🐍 Python 중복제거 마이크로서비스

파이썬 기반 뉴스 중복제거 서비스로, 기존 Java MSA 아키텍처와 완벽하게 통합됩니다.

## 🎯 **주요 기능**

- **100% 파이썬 원본 로직**: KoNLPy, SBERT, 유사도 계산 완전 구현
- **MSA 통합**: 기존 Java 서비스들과 완벽한 연동
- **파일서버 기반**: CSV 파일을 통한 데이터 교환
- **고성능**: FastAPI 기반 비동기 처리
- **모니터링**: Prometheus 메트릭 및 헬스체크

## 🏗️ **아키텍처**

```
Java Crawler Service ──→ Python Dedup Service ──→ Java DB Service
        │                        │                       │
        ├── 크롤링               ├── 중복제거             ├── DB 저장
        ├── 파일서버 저장        ├── SBERT 처리          ├── 연관뉴스 저장
        └── API 호출             └── 결과 반환           └── 통계 생성
```

## 🚀 **빠른 시작**

### 1. 로컬 환경

```bash
# 1. 의존성 설치
pip install -r requirements.txt

# 2. 파일서버 디렉터리 생성
mkdir -p /data/news-fileserver

# 3. 서비스 시작
./start.sh
# 또는
uvicorn app.main:app --host 0.0.0.0 --port 8002 --reload
```

### 2. Docker 환경

```bash
# 1. 이미지 빌드
docker build -t dedup-service .

# 2. 컨테이너 실행
docker-compose up -d

# 3. 로그 확인
docker-compose logs -f dedup-service
```

### 3. MSA 통합 환경

```bash
# 기존 MSA 네트워크에 추가
docker network create msa-network  # 네트워크가 없는 경우만
docker-compose up -d
```

## 📡 **API 엔드포인트**

### 헬스체크

```http
GET /health
```

### 중복제거 실행

```http
POST /api/v1/deduplicate
Content-Type: application/json

{
  "category": "POLITICS",
  "force_refresh": false
}
```

### 일괄 중복제거

```http
POST /api/v1/deduplicate/batch
```

### 결과 조회

```http
GET /api/v1/categories/{category}/deduplicated
GET /api/v1/categories/{category}/related
```

### 통계 조회

```http
GET /stats
```

### API 문서

- Swagger UI: http://localhost:8084/docs
- ReDoc: http://localhost:8084/redoc

## ⚙️ **설정**

### 환경 변수

| 변수명                  | 기본값                              | 설명            |
| ----------------------- | ----------------------------------- | --------------- |
| `REDIS_HOST`            | localhost                           | Redis 호스트    |
| `REDIS_PORT`            | 6379                                | Redis 포트      |
| `SBERT_MODEL_NAME`      | snunlp/KR-SBERT-V40K-klueNLI-augSTS | SBERT 모델      |
| `THRESHOLD_CONTENT`     | 0.8                                 | 중복제거 임계값 |
| `THRESHOLD_RELATED_MIN` | 0.4                                 | 연관뉴스 임계값 |

### Redis 키 패턴

```
# 입력 데이터 (Java에서 저장)
crawled:news:{category}:{timestamp}
news:list:{category}:{timestamp}

# 출력 데이터 (Python에서 저장)
deduplicated:news:{category}:{timestamp}
related:news:{category}:{timestamp}
```

## 🔄 **Java 연동**

### Java 클라이언트 사용

```java
@Autowired
private PythonDeduplicationIntegrationService pythonService;

// 중복제거 실행
Map<String, DeduplicationResponse> results = pythonService.runDeduplication();

// 결과 조회
List<Object> deduplicatedNews = pythonService.getDeduplicatedNews("POLITICS");
List<Object> relatedNews = pythonService.getRelatedNews("POLITICS");
```

### 설정 (application.yml)

```yaml
services:
  dedup:
    url: http://localhost:8084
    timeout: 300
```

## 📊 **모니터링**

### Prometheus 메트릭

- `dedup_requests_total`: 총 요청 수
- `dedup_request_duration_seconds`: 요청 처리 시간
- `dedup_active_requests`: 활성 요청 수

### 로그 구조

```json
{
  "timestamp": "2025-01-18T12:00:00Z",
  "level": "INFO",
  "message": "중복제거 완료",
  "category": "POLITICS",
  "original_count": 100,
  "deduplicated_count": 85,
  "processing_time": 45.2
}
```

## 🧪 **테스트**

### 단위 테스트

```bash
pytest tests/
```

### 통합 테스트

```bash
# Python 서비스 시작 후
curl -X POST http://localhost:8084/api/v1/deduplicate \
  -H "Content-Type: application/json" \
  -d '{"category": "POLITICS"}'
```

## 🐛 **트러블슈팅**

### 1. SBERT 모델 로딩 실패

```bash
# GPU 메모리 부족 시
export SBERT_DEVICE=cpu

# 모델 다운로드 실패 시
pip install --upgrade sentence-transformers
```

### 2. Redis 연결 실패

```bash
# Redis 서버 확인
redis-cli ping

# 연결 설정 확인
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

### 3. Java 연동 문제

```bash
# 네트워크 확인
curl http://localhost:8084/health

# 로그 확인
docker-compose logs dedup-service
```

## 📈 **성능 최적화**

### 메모리 사용량

- 기본: ~2GB (SBERT 모델 포함)
- 최적화: `SBERT_DEVICE=cpu`, `SBERT_BATCH_SIZE=16`

### 처리 속도

- 100개 기사: ~30초
- 1000개 기사: ~5분
- 병렬 처리로 카테고리별 동시 실행

## 🔧 **개발**

### 코드 구조

```
app/
├── main.py              # FastAPI 앱
├── config.py            # 설정 관리
├── models/
│   └── schemas.py       # 데이터 모델
└── services/
    ├── redis_service.py # Redis 연동
    └── dedup_service.py # 중복제거 로직
```

### 코드 스타일

```bash
# 포매팅
black app/

# 린팅
flake8 app/
```

## 📝 **라이선스**

이 프로젝트는 기존 MSA 프로젝트의 일부입니다.
