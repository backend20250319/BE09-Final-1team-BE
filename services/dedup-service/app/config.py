"""
설정 관리 - 기존 MSA와 동일한 설정 사용
"""

from pydantic_settings import BaseSettings
from typing import Optional
import os

class Settings(BaseSettings):
    """애플리케이션 설정"""
    
    # 서비스 기본 설정
    SERVICE_NAME: str = "dedup-service"
    VERSION: str = "1.0.0"
    DEBUG: bool = False
    PORT: int = 8084
    
    # 파일서버 설정
    FILESERVER_PATH: str = "http://dev.macacolabs.site:8008/1"
    
    # 파일서버 기반으로 변경됨
    
    # SBERT 모델 설정
    SBERT_MODEL_NAME: str = "snunlp/KR-SBERT-V40K-klueNLI-augSTS"
    SBERT_DEVICE: str = "cpu"  # "cuda" for GPU
    SBERT_BATCH_SIZE: int = 32
    
    # 중복제거 임계값 (파이썬 원본과 동일)
    THRESHOLD_TITLE: float = 0.3
    THRESHOLD_CONTENT: float = 0.8
    THRESHOLD_RELATED_MIN: float = 0.4
    
    # 파일서버 키 설정
    FILE_TTL_HOURS: int = 24
    
    # 로깅 설정
    LOG_LEVEL: str = "INFO"
    LOG_FORMAT: str = "json"
    
    # 성능 설정
    MAX_CONCURRENT_REQUESTS: int = 10
    REQUEST_TIMEOUT_SECONDS: int = 300  # 5분
    
    # Eureka 설정 (서비스 디스커버리)
    EUREKA_SERVER: str = "http://localhost:8761/eureka"
    EUREKA_ENABLED: bool = True
    
    class Config:
        env_file = ".env"
        case_sensitive = True

# 전역 설정 인스턴스
settings = Settings()

# 환경별 설정 오버라이드
if os.getenv("ENVIRONMENT") == "docker":
    # Docker 환경에서의 기본 설정

    settings.EUREKA_SERVER = "http://eureka:8761/eureka"
elif os.getenv("ENVIRONMENT") == "kubernetes":
    # Kubernetes 환경에서의 기본 설정

    settings.EUREKA_SERVER = "http://eureka-service:8761/eureka"
