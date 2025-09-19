"""
API 스키마 정의 - Java MSA와 호환되는 데이터 모델
"""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime
from enum import Enum

class Category(str, Enum):
    """뉴스 카테고리 (Java enum과 동일)"""
    POLITICS = "POLITICS"
    ECONOMY = "ECONOMY"
    SOCIETY = "SOCIETY"
    LIFE = "LIFE"
    INTERNATIONAL = "INTERNATIONAL"
    IT_SCIENCE = "IT_SCIENCE"
    VEHICLE = "VEHICLE"
    TRAVEL_FOOD = "TRAVEL_FOOD"
    ART = "ART"

class DedupState(str, Enum):
    """중복제거 상태 (Java enum과 동일)"""
    REPRESENTATIVE = "REPRESENTATIVE"
    KEPT = "KEPT"
    RELATED = "RELATED"

class NewsDetail(BaseModel):
    """뉴스 상세 정보 (Java DTO와 100% 호환)"""
    category_name: str = Field(..., alias="categoryName")  # 카테고리명 (필수)
    press: str = ""
    title: str = ""
    reporter: Optional[str] = None
    date: str = ""  # Java에서는 String으로 처리
    link: str = ""
    created_at: Optional[str] = Field(None, alias="createdAt")
    image_url: Optional[str] = Field(None, alias="imageUrl")
    trusted: int = 1  # 기본값 1 (true)
    oid_aid: str = ""
    content: str = ""
    dedup_state: Optional[str] = Field(None, alias="dedupState")
    
    class Config:
        populate_by_name = True
        allow_population_by_field_name = True

class RelatedNewsPair(BaseModel):
    """연관뉴스 쌍 정보"""
    rep_oid_aid: str = Field(..., alias="repOidAid")
    related_oid_aid: str = Field(..., alias="relatedOidAid")
    similarity: float
    category: Optional[str] = None
    created_at: Optional[str] = Field(None, alias="createdAt")
    
    class Config:
        populate_by_name = True

class DeduplicationRequest(BaseModel):
    """중복제거 요청"""
    category: str  # 문자열로 변경 (Java에서 문자열로 전달)
    force_refresh: bool = Field(default=False, description="강제 새로고침 여부")


class DeduplicationResponse(BaseModel):
    """중복제거 응답"""
    category: str
    original_count: int
    deduplicated_count: int
    related_count: int
    removed_count: int
    processing_time_seconds: float
    statistics: Dict[str, Any]
    message: str

class DeduplicationStats(BaseModel):
    """중복제거 통계"""
    total_processed: int
    total_removed: int
    total_related: int
    removal_rate: float
    processing_time: float

class HealthResponse(BaseModel):
    """헬스체크 응답"""
    status: str
    message: str
    fileserver_connected: bool
    sbert_loaded: bool
    timestamp: datetime = Field(default_factory=datetime.now)

class StatsResponse(BaseModel):
    """서비스 통계 응답"""
    total_requests: int
    successful_requests: int
    failed_requests: int
    average_processing_time: float
    active_requests: int
    model_loaded: bool
    uptime_seconds: float

class ErrorResponse(BaseModel):
    """에러 응답"""
    error: str
    message: str
    category: Optional[str] = None
    timestamp: datetime = Field(default_factory=datetime.now)

# 파일서버에서 조회한 뉴스 목록 응답
class NewsListResponse(BaseModel):
    """뉴스 목록 응답"""
    category: str
    count: int
    news: List[NewsDetail]

# 연관뉴스 목록 응답
class RelatedNewsResponse(BaseModel):
    """연관뉴스 목록 응답"""
    category: str
    count: int
    related_news: List[RelatedNewsPair]
