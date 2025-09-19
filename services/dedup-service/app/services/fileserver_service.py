"""
파일서버 기반 데이터 저장/조회 서비스
Redis를 대체하여 CSV 파일 형태로 데이터를 관리
"""

import os
import csv
import json
import glob
import requests
from datetime import datetime
from typing import List, Optional, Dict, Any
from pathlib import Path
from io import StringIO
import structlog

from app.models.schemas import NewsDetail, RelatedNewsPair
from app.config import settings

logger = structlog.get_logger()

class FileServerService:
    """
    파일서버 관리 서비스 - 파일 기반 중간 저장소
    
    역할:
    - 뉴스 데이터를 파일시스템에 CSV 형태로 저장/조회
    - Java-Python 서비스 간 데이터 교환 매개체
    - 시간 기반 디렉터리 구조 관리
    
    기능:
    - CSV 저장: 뉴스 데이터를 구조화된 CSV 파일로 저장
    - CSV 조회: 저장된 CSV 파일을 파싱하여 객체로 변환
    - 최신 파일 탐색: 타임스탬프 기반 최신 데이터 자동 감지
    - 디렉터리 관리: am/pm 기반 시간대별 폴더 구조 생성
    
    파일 구조:
    - 경로: {base_path}/{am|pm}/{yyyy-MM-dd}_{am|pm}/{stage}/
    - 파일명: {category}_{stage}_{yyyy-MM-dd-HH-mm}.csv
    - 단계: list → detail → deduplicated → related
    """
    
    def __init__(self):
        self.base_path = getattr(settings, 'FILESERVER_PATH', '/data/news-fileserver')
        self.time_format = "%Y-%m-%d"
        self.hour_format = "%H"
        
    def _get_current_time_path(self) -> str:
        """
        현재 시간 기반 디렉터리 경로 생성
        예: /data/news-fileserver/am/2025-08-19_am/ 또는 /data/news-fileserver/pm/2025-08-19_pm/
        """
        now = datetime.now()
        date_str = now.strftime(self.time_format)
        period = "am" if now.hour < 12 else "pm"
        return f"{self.base_path}/{period}/{date_str}_{period}"
    
    def _find_latest_file(self, dir_path: str, file_pattern: str) -> Optional[str]:
        """
        디렉터리에서 패턴에 맞는 최신 파일 찾기
        
        Args:
            dir_path: 검색할 디렉터리 경로
            file_pattern: 파일 패턴 (예: politics_detail_*.csv)
            
        Returns:
            최신 파일의 전체 경로 또는 None
        """
        try:
            # HTTP 파일서버에서 최신 파일 찾기
            return self._find_latest_file_from_server(dir_path, file_pattern)
            
        except Exception as e:
            logger.error(f"📁 최신 파일 검색 실패: {dir_path}/{file_pattern}, 오류: {e}")
            return None
    
    def _find_latest_file_from_server(self, dir_path: str, file_pattern: str) -> Optional[str]:
        """
        HTTP 파일서버에서 최신 파일 찾기
        """
        try:
            # 현재 시간부터 10분 전까지 시도
            from datetime import datetime, timedelta
            
            base_pattern = file_pattern.replace("*", "")  # politics_detail_.csv
            category_stage = base_pattern.replace(".csv", "")  # politics_detail_
            
            for i in range(30):  # 최대 10분 전까지
                try_time = datetime.now() - timedelta(minutes=i)
                timestamp = try_time.strftime("%Y-%m-%d-%H-%M")
                filename = f"{category_stage}{timestamp}.csv"
                full_url = f"{dir_path}/{filename}"
                
                # HTTP GET으로 파일 존재 확인
                try:
                    response = requests.get(full_url, timeout=5)
                    if response.status_code == 200:
                        logger.info(f"📁 최신 파일 발견: {full_url}")
                        return full_url
                except requests.RequestException:
                    continue
                    
            return None
            
        except Exception as e:
            logger.error(f"📁 HTTP 파일서버 검색 실패: {e}")
            return None
    
    def _download_file_from_server(self, file_url: str) -> Optional[str]:
        """
        HTTP 파일서버에서 파일 내용 다운로드
        
        Args:
            file_url: 파일의 HTTP URL
            
        Returns:
            파일 내용 문자열 또는 None
        """
        try:
            response = requests.get(file_url, timeout=10)
            if response.status_code == 200:
                logger.info(f"📁 파일 다운로드 성공: {file_url}")
                return response.text
            else:
                logger.warning(f"📁 파일 다운로드 실패: {file_url} - 상태코드: {response.status_code}")
                return None
                
        except requests.RequestException as e:
            logger.error(f"📁 파일 다운로드 오류: {file_url}, 오류: {e}")
            return None
    
    def _ensure_directory(self, dir_path: str):
        """디렉터리가 없으면 생성"""
        Path(dir_path).mkdir(parents=True, exist_ok=True)
    
    def _upload_file_to_server(self, upload_url: str, content: str):
        """Java 크롤러 서비스의 FTP API를 통해 파일 업로드"""
        try:
            # upload_url에서 경로와 파일명 추출
            # 예: "http://dev.macacolabs.site:8008/1/pm/2025-08-19_pm/deduplicated/politics_deduplicated_2025-08-19-17-45.csv"
            # → path: "pm/2025-08-19_pm/deduplicated/", filename: "politics_deduplicated_2025-08-19-17-45.csv"
            
            url_parts = upload_url.replace("http://dev.macacolabs.site:8008/1/", "")
            path_parts = url_parts.split("/")
            filename = path_parts[-1]
            relative_path = "/".join(path_parts[:-1]) + "/"
            
            # Java 크롤러 서비스의 FTP API 호출
            ftp_api_url = "http://localhost:8083/api/ftp/upload"
            
            payload = {
                "path": relative_path,
                "filename": filename,
                "content": content
            }
            
            headers = {
                'Content-Type': 'application/json; charset=utf-8',
                'Accept-Charset': 'UTF-8'
            }
            
            response = requests.post(ftp_api_url, json=payload, headers=headers)
            response.raise_for_status()
            
            logger.debug(f"FTP API 업로드 성공: {relative_path}{filename}")
            
        except Exception as e:
            logger.error(f"FTP API 업로드 오류: {upload_url}, 오류: {e}")
            raise RuntimeError(f"FTP API 업로드 실패: {e}")
    
    def save_news_to_csv(self, category: str, news_list: List[NewsDetail], stage: str) -> str:
        """
        뉴스 데이터를 CSV 파일로 저장
        
        Args:
            category: 카테고리 (POLITICS, ECONOMY 등)
            news_list: 뉴스 데이터 리스트
            stage: 단계 (list, detail, deduplicated)
            
        Returns:
            저장된 파일 경로
        """
        try:
            time_path = self._get_current_time_path()
            dir_path = f"{time_path}/{stage}"
            
            timestamp = datetime.now().strftime("%Y-%m-%d-%H-%M")
            file_name = f"{category.lower()}_{stage}_{timestamp}.csv"
            
            # CSV 데이터를 메모리에서 생성
            csv_content = StringIO()
            fieldnames = [
                'title', 'press', 'reporter', 'date', 'link',
                'imageUrl', 'oidAid', 'trusted', 'content', 'dedupState',
                'categoryName', 'createdAt'
            ]
            writer = csv.DictWriter(csv_content, fieldnames=fieldnames)
            
            # 헤더 쓰기
            writer.writeheader()
            
            # 데이터 쓰기
            for news in news_list:
                row = {
                    'title': news.title or '',
                    'press': news.press or '',
                    'reporter': news.reporter or '',
                    'date': news.date or '',
                    'link': news.link or '',
                    'imageUrl': news.image_url or '',
                    'oidAid': news.oid_aid or '',
                    'trusted': news.trusted or 0,
                    'content': news.content or '',
                    'dedupState': news.dedup_state or '',
                    'categoryName': news.category_name or category,  # 카테고리명 보존
                    'createdAt': news.created_at or datetime.now().isoformat()  # 생성시간 보존
                }
                writer.writerow(row)
            
            # HTTP 파일서버에 업로드
            upload_url = f"{dir_path}/{file_name}"
            self._upload_file_to_server(upload_url, csv_content.getvalue())
            
            logger.info(f"📁 파일서버 업로드 완료: {upload_url} - 카테고리: {category}, 개수: {len(news_list)}")
            return upload_url
            
        except Exception as e:
            logger.error(f"📁 파일서버 저장 실패: {category}/{stage}, 오류: {e}")
            raise
    
    def get_news_from_csv(self, category: str, stage: str, time_path: Optional[str] = None) -> List[NewsDetail]:
        """
        CSV 파일에서 뉴스 데이터 조회
        
        Args:
            category: 카테고리
            stage: 단계
            time_path: 특정 시간 경로 (None이면 최신 사용)
            
        Returns:
            뉴스 데이터 리스트
        """
        try:
            if time_path is None:
                time_path = self._get_latest_time_path()
            
            dir_path = f"{time_path}/{stage}"
            
            # 최신 파일 찾기 (타임스탬프 기반)
            file_pattern = f"{category.lower()}_{stage}_*.csv"
            file_path = self._find_latest_file(dir_path, file_pattern)
            
            if not file_path:
                logger.info(f"📁 파일이 존재하지 않음: {dir_path}/{file_pattern}")
                return []
            
            news_list = []
            
            # HTTP URL에서 파일 내용 다운로드
            csv_content = self._download_file_from_server(file_path)
            if not csv_content:
                logger.error(f"📁 파일서버 조회 실패: {category}/{stage}, 파일 다운로드 실패")
                return []
            
            # CSV 내용을 StringIO로 파싱
            csv_reader = csv.DictReader(StringIO(csv_content))
            for row in csv_reader:
                    # trusted 필드 안전한 변환
                    trusted_value = row.get('trusted', '0')
                    if trusted_value in ['null', 'None', '', None]:
                        trusted_value = 0
                    else:
                        try:
                            trusted_value = int(trusted_value)
                        except (ValueError, TypeError):
                            trusted_value = 0
                    
                    news = NewsDetail(
                        title=row.get('title', ''),
                        press=row.get('press', ''),
                        reporter=row.get('reporter', ''),
                        date=row.get('date', ''),
                        link=row.get('link', ''),
                        image_url=row.get('imageUrl', ''),
                        oid_aid=row.get('oidAid', ''),
                        trusted=trusted_value,
                        content=row.get('content', ''),
                        dedup_state=row.get('dedupState', ''),
                        # CSV에서 카테고리명 읽기
                        category_name=row.get('categoryName', category),
                        created_at=row.get('createdAt', '')
                    )
                    news_list.append(news)
            
            logger.info(f"📁 파일서버 조회 완료: {file_path} - 카테고리: {category}, 개수: {len(news_list)}")
            return news_list
            
        except Exception as e:
            logger.error(f"📁 파일서버 조회 실패: {category}/{stage}, 오류: {e}")
            return []
    
    def save_related_news_to_csv(self, category: str, related_pairs: List[RelatedNewsPair]) -> str:
        """
        연관뉴스를 CSV 파일로 저장
        """
        try:
            time_path = self._get_current_time_path()
            dir_path = f"{time_path}/related"
            
            timestamp = datetime.now().strftime("%Y-%m-%d-%H-%M")
            file_name = f"{category.lower()}_related_{timestamp}.csv"
            
            # CSV 데이터를 메모리에서 생성
            csv_content = StringIO()
            fieldnames = ['repOidAid', 'relatedOidAid', 'similarity', 'category', 'createdAt']
            writer = csv.DictWriter(csv_content, fieldnames=fieldnames)
            
            writer.writeheader()
            for pair in related_pairs:
                row = {
                    'repOidAid': pair.rep_oid_aid,
                    'relatedOidAid': pair.related_oid_aid,
                    'similarity': pair.similarity,
                    'category': category,
                    'createdAt': datetime.now().isoformat()
                }
                writer.writerow(row)
            
            # HTTP 파일서버에 업로드
            upload_url = f"{dir_path}/{file_name}"
            self._upload_file_to_server(upload_url, csv_content.getvalue())
            
            logger.info(f"📁 연관뉴스 파일서버 업로드 완료: {upload_url} - 카테고리: {category}, 개수: {len(related_pairs)}")
            return upload_url
            
        except Exception as e:
            logger.error(f"📁 연관뉴스 파일서버 저장 실패: {category}, 오류: {e}")
            raise
    
    def get_related_news_from_csv(self, category: str, time_path: Optional[str] = None) -> List[RelatedNewsPair]:
        """
        CSV 파일에서 연관뉴스 조회
        """
        try:
            if time_path is None:
                time_path = self._get_latest_time_path()
            
            dir_path = f"{time_path}/related"
            file_name = f"{category.lower()}_related_날짜 및 시간.csv"
            file_path = f"{dir_path}/{file_name}"
            
            if not os.path.exists(file_path):
                logger.info(f"📁 연관뉴스 파일이 존재하지 않음: {file_path}")
                return []
            
            related_list = []
            with open(file_path, 'r', encoding='utf-8') as csvfile:
                reader = csv.DictReader(csvfile)
                for row in reader:
                    pair = RelatedNewsPair(
                        rep_oid_aid=row.get('repOidAid', ''),
                        related_oid_aid=row.get('relatedOidAid', ''),
                        similarity=float(row.get('similarity', 0.0)),
                        category=row.get('category', ''),
                        created_at=row.get('createdAt', '')
                    )
                    related_list.append(pair)
            
            logger.info(f"📁 연관뉴스 파일서버 조회 완료: {file_path} - 카테고리: {category}, 개수: {len(related_list)}")
            return related_list
            
        except Exception as e:
            logger.error(f"📁 연관뉴스 파일서버 조회 실패: {category}, 오류: {e}")
            return []
    
    def _get_latest_time_path(self) -> str:
        """
        가장 최신 시간대 디렉터리 경로 찾기
        """
        try:
            latest_path = None
            latest_time = None
            
            # am, pm 디렉터리 순회
            for period in ['am', 'pm']:
                period_path = f"{self.base_path}/{period}"
                if not os.path.exists(period_path):
                    continue
                
                # 해당 period의 모든 날짜 디렉터리 찾기
                pattern = f"{period_path}/*_{period}"
                for dir_path in glob.glob(pattern):
                    try:
                        dir_name = os.path.basename(dir_path)
                        # 2025-08-19_am 형태에서 시간 추출
                        date_str = dir_name.replace(f'_{period}', '')
                        hour = 6 if period == 'am' else 18
                        dir_time = datetime.strptime(f"{date_str} {hour:02d}:00:00", "%Y-%m-%d %H:%M:%S")
                        
                        if latest_time is None or dir_time > latest_time:
                            latest_time = dir_time
                            latest_path = dir_path
                    except Exception:
                        continue
            
            if latest_path:
                logger.info(f"📁 최신 시간대 경로: {latest_path}")
                return latest_path
            else:
                # 최신 경로가 없으면 현재 시간 경로 반환
                current_path = self._get_current_time_path()
                logger.info(f"📁 최신 경로 없음, 현재 시간 경로 사용: {current_path}")
                return current_path
                
        except Exception as e:
            logger.error(f"📁 최신 시간대 경로 조회 실패: {e}")
            return self._get_current_time_path()
    
    def get_all_categories_latest_data(self, stage: str) -> Dict[str, List[NewsDetail]]:
        """
        모든 카테고리의 최신 데이터 조회
        """
        categories = ["POLITICS", "ECONOMY", "SOCIETY", "LIFE", "INTERNATIONAL", "IT_SCIENCE", "VEHICLE", "TRAVEL_FOOD", "ART"]
        latest_time_path = self._get_latest_time_path()
        
        result = {}
        for category in categories:
            news_list = self.get_news_from_csv(category, stage, latest_time_path)
            if news_list:
                result[category] = news_list
        
        return result
