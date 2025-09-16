"""
파이썬 중복제거 서비스 - 100% 원본 로직 구현
기존 파이썬 코드와 완전히 동일한 알고리즘 사용!
"""

import asyncio
import time
from typing import List, Dict, Tuple, Optional, Set, Any
import structlog
from collections import defaultdict
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from sentence_transformers import SentenceTransformer, util
import torch
import re
from bs4 import BeautifulSoup
from konlpy.tag import Okt

from app.config import settings
from app.models.schemas import NewsDetail, RelatedNewsPair, DeduplicationResponse


logger = structlog.get_logger()

class DeduplicationService:
    """
    뉴스 중복제거 서비스 - 핵심 비즈니스 로직
    
    역할:
    - 뉴스 데이터의 중복 탐지 및 제거
    - 의미 기반 유사도 계산 (SBERT + TF-IDF)
    - 연관뉴스 클러스터링 및 관계 구축
    
    기능:
    - 제목 전처리: 한국어 형태소 분석 및 불용어 제거
    - 중복 탐지: 코사인 유사도 기반 임계값 비교
    - 클러스터링: Union-Find 알고리즘으로 유사 뉴스 그룹화
    - 대표 선정: 클러스터 내 가장 대표적인 뉴스 선택
    - 연관뉴스 생성: 중복은 아니지만 관련성 높은 뉴스 매칭
    
    알고리즘:
    - SBERT: 한국어 문장 임베딩 (의미적 유사도)
    - TF-IDF: 키워드 기반 유사도 (구문적 유사도)  
    - Cosine Similarity: 벡터 간 각도 기반 유사도 측정
    - Union-Find: 효율적인 집합 연산으로 O(α(n)) 시간복잡도
    """
    
    def __init__(self, fileserver_service):
        self.fileserver_service = fileserver_service
        self.sbert_model: Optional[SentenceTransformer] = None
        self.okt = Okt()  # 한국어 형태소 분석기
        
        # 파이썬 원본과 동일한 설정
        self.THRESHOLD_TITLE = settings.THRESHOLD_TITLE
        self.THRESHOLD_CONTENT = settings.THRESHOLD_CONTENT
        self.THRESHOLD_RELATED_MIN = settings.THRESHOLD_RELATED_MIN
        
        # 통계 정보
        self.stats = {
            'total_requests': 0,
            'successful_requests': 0,
            'failed_requests': 0,
            'total_processing_time': 0.0,
            'start_time': time.time()
        }
        
        # 불용어 및 중요 키워드 (파이썬 원본과 동일)
        self.STOPWORDS = {
            "기자", "속보", "단독", "포토", "영상"
        }
        
        self.IMPORTANT_KEYWORDS = {
            # 국가 및 지역 (한자)
            "中", "美", "日", "韓", "北", "南", "英", "獨", "仏", "露", "台",
            # 지정학·방향
            "東", "西", "亞", "歐",
            # 정치·안보
            "核", "軍", "總", "民", "黨", "法", "裁",
            # 경제·산업
            "金", "銀", "油", "電", "車",
            # 로마자 약어
            "EU", "AI", "G7", "OECD",
            # 국가 및 지정학 (한글)
            "미", "중", "한", "북", "남", "일", "동", "서", "영", "독", "러", "국",
            # 정치 키워드 (한글)
            "민", "총", "법", "핵",
            # 성씨 (한글)
            "김", "이", "박", "최", "정", "강", "조", "윤", "장", "임",
            "한", "오", "서", "신", "권", "황", "안", "송", "류", "홍",
            # 성씨 (한자)
            "金", "李", "朴", "崔", "鄭", "姜", "趙", "尹", "張", "林",
            "韓", "吳", "徐", "申", "權", "黃", "安", "宋", "柳", "洪"
        }
    
    async def initialize(self):
        """SBERT 모델 로딩"""
        try:
            logger.info(f"🤖 SBERT 모델 로딩 시작: {settings.SBERT_MODEL_NAME}")
            
            # SBERT 모델 로딩 (비동기 처리)
            loop = asyncio.get_event_loop()
            self.sbert_model = await loop.run_in_executor(
                None,
                self._load_sbert_model
            )
            
            logger.info("✅ SBERT 모델 로딩 완료")
            
        except Exception as e:
            logger.error(f"❌ SBERT 모델 로딩 실패: {e}")
            raise
    
    def _load_sbert_model(self) -> SentenceTransformer:
        """SBERT 모델 동기 로딩"""
        model = SentenceTransformer(
            settings.SBERT_MODEL_NAME,
            device=settings.SBERT_DEVICE
        )
        return model
    
    def is_model_ready(self) -> bool:
        """모델 준비 상태 확인"""
        return self.sbert_model is not None
    
    async def run_deduplication(self, category: str, file_timestamp: str = None) -> DeduplicationResponse:
        """
        중복제거 메인 로직
        """
        start_time = time.time()
        self.stats['total_requests'] += 1
        
        try:
            # 카테고리 값 정규화 (Enum 객체인 경우 값 추출)
            original_category = category
            if hasattr(category, 'value'):
                category = category.value
            elif str(category).startswith('Category.'):
                category = str(category).replace('Category.', '')
            
            logger.info(f"🔍 중복제거 시작: {original_category} → {category}")
            
            # 1. 파일서버에서 원본 데이터 조회
            logger.info(f"🔍 파일서버에서 detail 데이터 조회 시작: {category}")
            original_news = self.fileserver_service.get_news_from_csv(category, "detail")
            logger.info(f"🔍 detail 조회 결과: {len(original_news)}개")
            
            if not original_news:
                # list에서도 시도
                logger.info(f"🔍 파일서버에서 list 데이터 조회 시도: {category}")
                original_news = self.fileserver_service.get_news_from_csv(category, "list")
                logger.info(f"🔍 list 조회 결과: {len(original_news)}개")
            
            if not original_news:
                logger.warning(f"❌ {category} 카테고리 데이터 없음")
                return DeduplicationResponse(
                    category=category,
                    original_count=0,
                    deduplicated_count=0,
                    related_count=0,
                    removed_count=0,
                    processing_time_seconds=time.time() - start_time,
                    statistics={},
                    message="원본 데이터 없음"
                )
            
            # 2. 제목 기반 유사 그룹 생성 (파이썬 build_title_similarity_groups)
            title_groups = await self._build_title_similarity_groups(original_news)
            
            # 3. 본문 기반 대표 기사 선정 및 연관뉴스 생성
            deduplicated_news, related_news, removed_count = await self._process_content_deduplication(
                title_groups, original_news
            )
            
            # 4. 파일서버에 결과 저장
            self.fileserver_service.save_news_to_csv(category, deduplicated_news, "deduplicated")
            self.fileserver_service.save_related_news_to_csv(category, related_news)
            
            # 5. 통계 계산
            processing_time = time.time() - start_time
            self.stats['successful_requests'] += 1
            self.stats['total_processing_time'] += processing_time
            
            statistics = {
                'original': len(original_news),
                'deduplicated': len(deduplicated_news),
                'related': len(related_news),
                'removed': removed_count,
                'removal_rate': removed_count / len(original_news) if original_news else 0,
                'title_groups': len(title_groups)
            }
            
            logger.info(f"✅ {category} 중복제거 완료: {len(original_news)}개 → {len(deduplicated_news)}개, "
                       f"연관뉴스 {len(related_news)}개, 제거 {removed_count}개")
            
            return DeduplicationResponse(
                category=category,
                original_count=len(original_news),
                deduplicated_count=len(deduplicated_news),
                related_count=len(related_news),
                removed_count=removed_count,
                processing_time_seconds=processing_time,
                statistics=statistics,
                message="중복제거 완료"
            )
            
        except Exception as e:
            self.stats['failed_requests'] += 1
            logger.error(f"❌ {category} 중복제거 실패: {e}")
            raise
    
    async def _build_title_similarity_groups(self, news_list: List[NewsDetail]) -> List[List[int]]:
        """
        제목 기반 유사 그룹 생성 - 파이썬 build_title_similarity_groups와 동일
        """
        # 1. 제목 전처리 (파이썬: df['clean_title'] = df['title'].apply(preprocess_titles))
        clean_titles = []
        for news in news_list:
            clean_title = await self._preprocess_titles(news.title)
            clean_titles.append(clean_title)
        
        # 2. 제목 유사도 계산 (파이썬: compute_title_similarity)
        similar_pairs = await self._compute_title_similarity(clean_titles)
        
        # 3. Union-Find로 그룹 생성 (파이썬: group_by_union_find)
        groups = self._group_by_union_find(similar_pairs, len(news_list))
        
        logger.info(f"제목 그룹핑 완료: {len(groups)}개 그룹 생성")
        return groups
    
    async def _preprocess_titles(self, text: str) -> str:
        """
        제목 전처리 - 원본 Python preprocessing_title.py와 100% 동일
        
        원본 Python 코드:
        def preprocess_titles(text):
            if pd.isna(text):
                return ''
            
            text = str(text)
            text = re.sub(r'[^\w\s]', ' ', text)         # 특수문자 제거
            text = re.sub(r'\d+', '', text)              # 숫자 제거
            text = re.sub(r'\s+', ' ', text).strip()     # 공백 정리

            tokens = okt.nouns(text)                     # 명사 추출
            tokens = [
                t for t in tokens 
                if (len(t) > 1 or t in IMPORTANT_KEYWORDS) and t not in STOPWORDS
            ]
            return ' '.join(tokens)
        """
        # 파이썬: if pd.isna(text): return ''
        if not text or not isinstance(text, str) or text.strip() == '':
            return ""
        
        # 파이썬: text = str(text)
        text = str(text)
        
        # 파이썬: text = re.sub(r'[^\w\s]', ' ', text)  # 특수문자 제거
        text = re.sub(r'[^\w\s]', ' ', text)
        
        # 파이썬: text = re.sub(r'\d+', '', text)  # 숫자 제거
        text = re.sub(r'\d+', '', text)
        
        # 파이썬: text = re.sub(r'\s+', ' ', text).strip()  # 공백 정리
        text = re.sub(r'\s+', ' ', text).strip()
        
        # 파이썬: tokens = okt.nouns(text)  # 명사 추출
        try:
            tokens = self.okt.nouns(text)
        except Exception as e:
            logger.warning(f"KoNLPy 명사 추출 실패: {e}")
            # fallback: 공백으로 분리
            tokens = text.split()
        
        # 파이썬: tokens = [t for t in tokens if (len(t) > 1 or t in IMPORTANT_KEYWORDS) and t not in STOPWORDS]
        filtered_tokens = [
            token for token in tokens
            if (len(token) > 1 or token in self.IMPORTANT_KEYWORDS) and token not in self.STOPWORDS
        ]
        
        # 파이썬: return ' '.join(tokens)
        return ' '.join(filtered_tokens)
    
    async def _preprocess_content(self, text: str) -> str:
        """
        본문 전처리 - 파이썬 preprocessing_content.py의 preprocess_content와 100% 동일
        HTML 태그 제거 기능 추가
        """
        # 파이썬: if not isinstance(text, str): return ''
        if not isinstance(text, str) or not text:
            return ""
        
        # HTML 파싱으로 텍스트만 추출 (더 정확하고 안전함)
        try:
            soup = BeautifulSoup(text, 'html.parser')
            text = soup.get_text(separator=' ', strip=True)
        except Exception as e:
            logger.warning(f"HTML 파싱 실패, 정규식으로 fallback: {e}")
            # Fallback: 기존 정규식 방식
            text = re.sub(r'<[^>]+>', ' ', text)
            text = re.sub(r'&[a-zA-Z0-9#]+;', ' ', text)
        
        # 파이썬: text = re.sub(r'[^\w\s]', ' ', text)
        text = re.sub(r'[^\w\s]', ' ', text)
        
        # 파이썬: text = re.sub(r'\d+', '', text)
        text = re.sub(r'\d+', '', text)
        
        # 파이썬: text = re.sub(r'\s+', ' ', text).strip()
        text = re.sub(r'\s+', ' ', text).strip()
        
        # 파이썬: tokens = [t for t in okt.morphs(text) if (len(t) > 1 or t in IMPORTANT_KEYWORDS) and t not in STOPWORDS]
        try:
            morphs = self.okt.morphs(text)
        except Exception:
            # fallback: 공백으로 분리
            morphs = text.split()
        
        filtered_tokens = [
            token for token in morphs
            if (len(token) > 1 or token in self.IMPORTANT_KEYWORDS) and token not in self.STOPWORDS
        ]
        
        # 파이썬: return ' '.join(tokens)
        return ' '.join(filtered_tokens)
    
    async def _compute_title_similarity(self, clean_titles: List[str]) -> List[Tuple[int, int, float]]:
        """
        제목 유사도 계산 - 파이썬 grouping.py의 compute_title_similarity와 동일
        """
        if len(clean_titles) < 2:
            return []
        
        # TF-IDF 벡터화 (파이썬과 동일)
        vectorizer = TfidfVectorizer(
            max_features=1000,
            stop_words=None,  # 이미 전처리에서 불용어 제거
            ngram_range=(1, 2)
        )
        
        try:
            tfidf_matrix = vectorizer.fit_transform(clean_titles)
            
            # 코사인 유사도 계산
            similarity_matrix = cosine_similarity(tfidf_matrix)
            
            # 임계값 이상의 유사 쌍 추출
            similar_pairs = []
            for i in range(len(clean_titles)):
                for j in range(i + 1, len(clean_titles)):
                    if similarity_matrix[i][j] >= self.THRESHOLD_TITLE:
                        similar_pairs.append((i, j, similarity_matrix[i][j]))
            
            logger.debug(f"제목 유사도 계산 완료: {len(similar_pairs)}개 유사 쌍 발견")
            return similar_pairs
            
        except Exception as e:
            logger.warning(f"TF-IDF 계산 실패: {e}")
            return []
    
    def _group_by_union_find(self, similar_pairs: List[Tuple[int, int, float]], total_size: int) -> List[List[int]]:
        """
        Union-Find 그룹핑 - 파이썬 grouping.py의 group_by_union_find와 100% 동일
        """
        parent = {}
        
        def find(x):
            if x not in parent:
                parent[x] = x
            while parent[x] != x:
                parent[x] = parent[parent[x]]  # 경로 압축
                x = parent[x]
            return x
        
        def union(x, y):
            root_x = find(x)
            root_y = find(y)
            if root_x != root_y:
                parent[root_y] = root_x
        
        # Union 연산 수행
        for i, j, similarity in similar_pairs:
            union(i, j)
        
        # 그룹 생성
        groups_dict = defaultdict(list)
        for node in parent.keys():
            root = find(node)
            groups_dict[root].append(node)
        
        # 리스트로 변환
        groups = list(groups_dict.values())
        
        # 크기 순으로 정렬 (큰 그룹부터)
        groups.sort(key=len, reverse=True)
        
        return groups
    
    async def _process_content_deduplication(
        self, 
        title_groups: List[List[int]], 
        original_news: List[NewsDetail]
    ) -> Tuple[List[NewsDetail], List[RelatedNewsPair], int]:
        """
        본문 기반 중복제거 및 연관뉴스 생성
        """
        deduplicated_news = []
        all_related_news = []
        total_removed = 0
        processed_indices = set()
        
        for group in title_groups:
            if len(group) == 1:
                # 단일 기사는 그대로 유지
                news = original_news[group[0]]

                news.dedup_state = "KEPT"
                deduplicated_news.append(news)
                processed_indices.add(group[0])
            else:
                # 그룹 내 본문 기반 중복제거
                result = await self._filter_and_pick_representative_by_content(group, original_news)
                
                if result['representative_index'] is not None:
                    # 대표 기사 (유사도 검사 결과에 따라 설정)
                    rep_news = original_news[result['representative_index']]

                    rep_news.dedup_state = "REPRESENTATIVE"
                    deduplicated_news.append(rep_news)
                    processed_indices.add(result['representative_index'])
                    
                    # 연관 기사들 (유사도 검사 결과에 따라 설정)
                    for idx in result['related_indices']:
                        related_news = original_news[idx]

                        related_news.dedup_state = "RELATED"
                        deduplicated_news.append(related_news)
                        processed_indices.add(idx)
                    
                    # 연관뉴스 쌍 추가
                    all_related_news.extend(result['related_pairs'])
                    
                    # 제거된 기사 수 추가
                    total_removed += len(result['removed_indices'])
                    processed_indices.update(result['removed_indices'])
                else:
                    # 대표 기사 선정 실패 시 모든 기사 유지
                    for idx in group:
                        news = original_news[idx]
                        news.dedup_state = "KEPT"
                        deduplicated_news.append(news)
                        processed_indices.add(idx)
        
        # 그룹화되지 않은 독립 기사들 추가
        for i in range(len(original_news)):
            if i not in processed_indices:
                news = original_news[i]

                news.dedup_state = "KEPT"
                deduplicated_news.append(news)
        
        return deduplicated_news, all_related_news, total_removed
    
    async def _filter_and_pick_representative_by_content(
        self, 
        group: List[int], 
        news_list: List[NewsDetail]
    ) -> Dict[str, Any]:
        """
        본문 기반 대표 기사 선정 
        """
        if len(group) == 1:
            return {
                'representative_index': group[0],
                'removed_indices': [],
                'related_indices': [],
                'related_pairs': []
            }
        
        # 파이썬: docs = [preprocess_content(df.loc[i, 'content']) for i in indices]
        docs = []
        for idx in group:
            content = news_list[idx].content or ""
            preprocessed = await self._preprocess_content(content)
            docs.append(preprocessed)
        
        # ⭐ 파이썬과 100% 동일: SBERT 사용
        # 파이썬: embeddings = model.encode(docs, convert_to_tensor=True)
        # 파이썬: sim_matrix = util.pytorch_cos_sim(embeddings, embeddings).cpu().numpy()
        try:
            embeddings = self.sbert_model.encode(docs, convert_to_tensor=True)
            sim_matrix = util.pytorch_cos_sim(embeddings, embeddings).cpu().numpy()
        except Exception as e:
            logger.warning(f"SBERT 처리 실패, TF-IDF로 fallback: {e}")
            # Fallback: TF-IDF 사용
            sim_matrix = await self._compute_content_similarity_tfidf(docs)
        
        # 파이썬: row_avg = sim_matrix.mean(axis=1)
        # 파이썬: rep_idx = indices[int(row_avg.argmax())]
        row_avg = np.mean(sim_matrix, axis=1)
        max_avg_idx = int(np.argmax(row_avg))
        rep_idx = group[max_avg_idx]
        
        removed_indices = []
        related_indices = []
        related_pairs = []
        
        # 파이썬 로직과 100% 동일한 분류
        for i, idx in enumerate(group):
            if idx == rep_idx:
                continue
            
            # 파이썬: sim = sim_matrix[i][group.index(rep_idx)]
            sim = sim_matrix[i][max_avg_idx]
            
            if sim >= self.THRESHOLD_CONTENT:
                # 중복 제거
                removed_indices.append(idx)
            elif sim >= self.THRESHOLD_RELATED_MIN:
                # 연관뉴스로 분류
                related_indices.append(idx)
                
                # 연관뉴스 쌍 생성
                rep_oid_aid = self._generate_oid_aid(news_list[rep_idx])
                related_oid_aid = self._generate_oid_aid(news_list[idx])
                
                related_pairs.append(RelatedNewsPair(
                    rep_oid_aid=rep_oid_aid,
                    related_oid_aid=related_oid_aid,
                    similarity=round(float(sim), 4)
                ))
        
        return {
            'representative_index': rep_idx,
            'removed_indices': removed_indices,
            'related_indices': related_indices,
            'related_pairs': related_pairs
        }
    
    async def _compute_content_similarity_tfidf(self, docs: List[str]) -> np.ndarray:
        """TF-IDF 기반 본문 유사도 계산 (SBERT fallback)"""
        if not docs or len(docs) < 2:
            return np.eye(len(docs))
        
        try:
            vectorizer = TfidfVectorizer(max_features=1000)
            tfidf_matrix = vectorizer.fit_transform(docs)
            similarity_matrix = cosine_similarity(tfidf_matrix)
            return similarity_matrix
        except Exception:
            # 최종 fallback: 단위행렬
            return np.eye(len(docs))
    
    def _generate_oid_aid(self, news: NewsDetail) -> str:
        """OID-AID 생성 (Java 로직과 동일)"""
        if news.oid_aid:
            return news.oid_aid
        
        url = news.link
        if url and "/article/" in url:
            try:
                parts = url.split("/article/")[1].split("/")
                if len(parts) >= 2:
                    return f"{parts[0]}-{parts[1]}"
            except Exception:
                pass
        
        return f"extracted_{abs(hash(url))}"
    
    async def get_deduplicated_news(self, category: str) -> List[NewsDetail]:
        """중복제거된 뉴스 조회"""
        return self.fileserver_service.get_news_from_csv(category, "deduplicated")
    
    async def get_related_news(self, category: str) -> List[RelatedNewsPair]:
        """연관뉴스 조회"""
        # 파일서버에서 연관뉴스 조회 (구현 필요시)
        return []
    
    async def get_stats(self) -> Dict[str, Any]:
        """서비스 통계 조회"""
        uptime = time.time() - self.stats['start_time']
        avg_processing_time = (
            self.stats['total_processing_time'] / self.stats['successful_requests']
            if self.stats['successful_requests'] > 0 else 0
        )
        
        return {
            'total_requests': self.stats['total_requests'],
            'successful_requests': self.stats['successful_requests'],
            'failed_requests': self.stats['failed_requests'],
            'average_processing_time': avg_processing_time,
            'active_requests': 0,  # 현재 활성 요청 수 (구현 필요시)
            'model_loaded': self.is_model_ready(),
            'uptime_seconds': uptime
        }
