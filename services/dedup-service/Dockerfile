# =================
# 최적화된 Python 서비스 Dockerfile
# =================

# Python 3.11 기반 이미지 (slim 버전으로 용량 최적화)
FROM python:3.11-slim AS builder

# 보안: root가 아닌 사용자로 실행
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 작업 디렉토리 설정
WORKDIR /app

# 시스템 패키지 업데이트 및 필수 도구 설치
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Python 의존성 파일 복사 및 설치
COPY requirements.txt .

# pip 업그레이드 및 의존성 설치 (캐시 최적화)
RUN pip install --no-cache-dir --upgrade pip setuptools wheel
RUN pip install --no-cache-dir -r requirements.txt

# =================
# 2단계: 실행 환경 구성
# =================
FROM python:3.11-slim

# 보안: root가 아닌 사용자로 실행
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 작업 디렉토리 설정
WORKDIR /app

# 런타임에 필요한 패키지만 설치
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 빌드 단계에서 설치된 Python 패키지들 복사
COPY --from=builder /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

# 전체 프로젝트 복사 (app/ 디렉토리 포함)
COPY . .

# 파일 소유권 변경 (보안)
RUN chown -R appuser:appuser /app

# 환경 변수 설정
ENV PYTHONPATH=/app
ENV ENVIRONMENT=docker
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

# 포트 노출
EXPOSE 8084

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8084/health || exit 1

# 보안: root가 아닌 사용자로 전환
USER appuser

# 애플리케이션 실행 (로컬 환경과 동일한 방식)
CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8084", "--workers", "1"]
