"""
파이썬 중복제거 마이크로서비스 - FastAPI 애플리케이션

역할:
- 뉴스 중복제거 전용 마이크로서비스
- Java MSA 크롤러 서비스와 HTTP 통신
- 파일서버 기반 데이터 처리

기능:
- POST /api/v1/deduplicate: 카테고리별 중복제거 실행
- POST /api/v1/deduplicate/batch: 전체 카테고리 일괄 처리
- GET /health: 서비스 헬스체크
- GET /stats: 중복제거 통계 조회
- GET /metrics: Prometheus 메트릭 수집

기술 스택:
- FastAPI: 고성능 비동기 웹 프레임워크
- SBERT: 한국어 의미 기반 문장 임베딩
- KoNLPy: 한국어 자연어 처리 (형태소 분석)
- TF-IDF + Cosine Similarity: 텍스트 유사도 계산
- Union-Find: 효율적인 클러스터링 알고리즘
"""

from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import structlog
import uvicorn
from prometheus_client import make_asgi_app, Counter, Histogram, Gauge
import asyncio

from app.config import settings
from app.services.dedup_service import DeduplicationService
from app.services.fileserver_service import FileServerService
from app.models.schemas import (
    DeduplicationRequest,
    DeduplicationResponse,
    HealthResponse,
    StatsResponse
)

# 메트릭스 설정
REQUEST_COUNT = Counter('dedup_requests_total', 'Total deduplication requests', ['category', 'status'])
REQUEST_DURATION = Histogram('dedup_request_duration_seconds', 'Deduplication request duration')
ACTIVE_REQUESTS = Gauge('dedup_active_requests', 'Active deduplication requests')

# 로거 설정
logger = structlog.get_logger()

# 서비스 인스턴스들
dedup_service = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    """애플리케이션 생명주기 관리"""
    global dedup_service
    
    logger.info("🚀 중복제거 서비스 시작 중...")
    
    try:
        # 파일서버 서비스 초기화
        fileserver_service = FileServerService()
        logger.info("✅ 파일서버 서비스 초기화 완료")
        
        # 중복제거 서비스 초기화 (SBERT 모델 로딩)
        dedup_service = DeduplicationService(fileserver_service)
        await dedup_service.initialize()
        logger.info("✅ SBERT 모델 로딩 완료")
        
        logger.info("🎉 중복제거 서비스 초기화 완료!")
        
    except Exception as e:
        logger.error(f"❌ 서비스 초기화 실패: {e}")
        raise
    
    yield
    
    # 정리 작업
    logger.info("🔄 중복제거 서비스 종료 중...")
    # 파일서버는 별도 연결 종료가 필요없음
    logger.info("✅ 중복제거 서비스 종료 완료")

# FastAPI 앱 생성
app = FastAPI(
    title="News Deduplication Service",
    description="파이썬 기반 뉴스 중복제거 마이크로서비스 - 100% 원본 로직 구현",
    version="1.0.0",
    lifespan=lifespan
)

# CORS 설정 (다른 MSA 서비스들과 통신)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 프로덕션에서는 특정 도메인만 허용
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Prometheus 메트릭스 엔드포인트
metrics_app = make_asgi_app()
app.mount("/metrics", metrics_app)

@app.get("/health", response_model=HealthResponse)
async def health_check():
    """헬스체크 엔드포인트"""
    try:
        return HealthResponse(
            status="healthy",
            message="중복제거 서비스 정상 동작 중",
            redis_connected=False,  # 파일서버 기반
            sbert_loaded=dedup_service.is_model_ready() if dedup_service else False
        )
    except Exception as e:
        logger.error(f"헬스체크 실패: {e}")
        raise HTTPException(status_code=503, detail="서비스 불가능")

@app.get("/stats", response_model=StatsResponse)
async def get_stats():
    """서비스 통계 조회"""
    try:
        stats = await dedup_service.get_stats()
        return StatsResponse(**stats)
    except Exception as e:
        logger.error(f"통계 조회 실패: {e}")
        raise HTTPException(status_code=500, detail="통계 조회 실패")



@app.post("/api/v1/deduplicate", response_model=DeduplicationResponse)
async def deduplicate_news(
    request: DeduplicationRequest,
    background_tasks: BackgroundTasks
):
    """
    뉴스 중복제거 메인 API
    - 파이썬 원본 로직 100% 구현
    - 파일서버에서 데이터 조회
    - 중복제거 수행
    - 결과를 파일서버에 저장
    """
    ACTIVE_REQUESTS.inc()
    
    try:
        with REQUEST_DURATION.time():
            logger.info(f"🔍 중복제거 시작: 카테고리={request.category}")
            
            # 중복제거 실행 (타임스탬프 전달)
            result = await dedup_service.run_deduplication(request.category)
            
            REQUEST_COUNT.labels(category=request.category, status="success").inc()
            logger.info(f"✅ 중복제거 완료: 카테고리={request.category}, "
                       f"원본={result.original_count}개 → 결과={result.deduplicated_count}개, "
                       f"연관뉴스={result.related_count}개")
            
            return result
            
    except Exception as e:
        REQUEST_COUNT.labels(category=request.category, status="error").inc()
        logger.error(f"❌ 중복제거 실패: 카테고리={request.category}, 오류={e}")
        raise HTTPException(status_code=500, detail=f"중복제거 실패: {str(e)}")
    
    finally:
        ACTIVE_REQUESTS.dec()

@app.post("/api/v1/deduplicate/batch")
async def deduplicate_batch(background_tasks: BackgroundTasks):
    """
    전체 카테고리 일괄 중복제거
    - 모든 카테고리에 대해 중복제거 수행
    - 백그라운드에서 비동기 실행
    """
    try:
        logger.info("🔄 전체 카테고리 일괄 중복제거 시작")
        
        # 백그라운드에서 실행
        background_tasks.add_task(run_batch_deduplication)
        
        return {
            "message": "일괄 중복제거가 백그라운드에서 시작되었습니다",
            "status": "started"
        }
        
    except Exception as e:
        logger.error(f"❌ 일괄 중복제거 시작 실패: {e}")
        raise HTTPException(status_code=500, detail=f"일괄 중복제거 시작 실패: {str(e)}")

async def run_batch_deduplication():
    """백그라운드에서 실행되는 일괄 중복제거"""
    categories = ["POLITICS", "ECONOMY", "SOCIETY", "LIFE", "INTERNATIONAL", 
                 "IT_SCIENCE", "VEHICLE", "TRAVEL_FOOD", "ART"]
    
    for category in categories:
        try:
            await dedup_service.run_deduplication(category)
            logger.info(f"✅ {category} 카테고리 중복제거 완료")
        except Exception as e:
            logger.error(f"❌ {category} 카테고리 중복제거 실패: {e}")
        
        # 카테고리 간 잠시 대기 (시스템 부하 방지)
        await asyncio.sleep(1)

@app.get("/api/v1/categories/{category}/deduplicated")
async def get_deduplicated_news(category: str):
    """중복제거된 뉴스 조회"""
    try:
        news_list = await dedup_service.get_deduplicated_news(category)
        return {
            "category": category,
            "count": len(news_list),
            "news": news_list
        }
    except Exception as e:
        logger.error(f"중복제거된 뉴스 조회 실패: {e}")
        raise HTTPException(status_code=500, detail=f"조회 실패: {str(e)}")

@app.get("/api/v1/categories/{category}/related")
async def get_related_news(category: str):
    """연관뉴스 조회"""
    try:
        related_list = await dedup_service.get_related_news(category)
        return {
            "category": category,
            "count": len(related_list),
            "related_news": related_list
        }
    except Exception as e:
        logger.error(f"연관뉴스 조회 실패: {e}")
        raise HTTPException(status_code=500, detail=f"조회 실패: {str(e)}")

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=settings.PORT,
        reload=settings.DEBUG,
        log_level="info"
    )
