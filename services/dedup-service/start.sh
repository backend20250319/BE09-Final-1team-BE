#!/bin/bash

# Python 중복제거 서비스 시작 스크립트

echo "🐍 Python 중복제거 서비스 시작..."

# 환경 변수 설정
export PYTHONPATH=/app
export ENVIRONMENT=local

# 가상환경 활성화 (있는 경우)
if [ -d "venv" ]; then
    echo "가상환경 활성화..."
    source venv/bin/activate
fi

# 의존성 설치
echo "의존성 설치 중..."
pip install -r requirements.txt

# 파일서버 디렉터리 확인
echo "파일서버 디렉터리 확인..."
python -c "
import os
fileserver_path = '/data/news-fileserver'
if not os.path.exists(fileserver_path):
    print(f'⚠️ 파일서버 디렉터리가 없습니다: {fileserver_path}')
    os.makedirs(fileserver_path, exist_ok=True)
    print(f'✅ 파일서버 디렉터리 생성: {fileserver_path}')
else:
    print(f'✅ 파일서버 디렉터리 확인: {fileserver_path}')
"

# 서비스 시작
echo "🚀 FastAPI 서버 시작..."
uvicorn app.main:app --host 0.0.0.0 --port 8084 --reload
