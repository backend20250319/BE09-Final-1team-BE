# syntax=docker/dockerfile:1
FROM python:3.11-slim

# 환경변수
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

# 필요한 시스템 패키지 설치
RUN apt-get update && apt-get install -y --no-install-recommends curl tzdata && rm -rf /var/lib/apt/lists/*

# 파이썬 의존성
COPY requirements.txt .
RUN pip install -r requirements.txt

# 소스 복사
COPY . .

# Flask API 포트
EXPOSE 5000

# 헬스체크용 엔드포인트(/health) 있는 경우
HEALTHCHECK --interval=10s --timeout=2s --retries=12 \
  CMD curl -fsS http://localhost:5000/health || exit 1

# gunicorn으로 실행 (main.py 안에 app = create_app() 있다고 가정)
CMD ["gunicorn","main:app","--bind","0.0.0.0:5000","--workers","2","--worker-class","gthread","--threads","4","--timeout","120","--access-logfile","-","--error-logfile","-"]
