# =================
# 최적화된 멀티 스테이지 빌드 Dockerfile
# =================

# 1단계: 프로젝트 빌드 (JDK가 포함된 이미지 사용)
FROM openjdk:17-jdk-slim AS builder

# 보안: root가 아닌 사용자로 실행
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 작업 디렉토리 설정
WORKDIR /app

# 💡 개선점: 종속성 파일 먼저 복사하여 캐싱 활용
COPY build.gradle settings.gradle ./
COPY gradlew ./gradlew
COPY gradle ./gradle

# Gradle 래퍼 실행 권한 부여
RUN chmod +x ./gradlew

# 💡 개선점: 소스 코드 복사 전 종속성 먼저 다운로드
# 이렇게 하면 소스 코드만 변경 시 이 단계는 캐시를 사용합니다.
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src ./src

# 프로젝트 빌드 (daemon 비활성화로 메모리 절약)
RUN ./gradlew bootJar --no-daemon

# =================
# 2단계: 실행 환경 구성 (Chrome + JRE 포함)
# =================
FROM openjdk:17-slim

# 보안: root가 아닌 사용자로 실행
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 작업 디렉토리 설정
WORKDIR /app

# Chrome과 ChromeDriver 설치 (Selenium 크롤링용)
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    curl \
    unzip \
    && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && rm -rf /var/lib/apt/lists/*

# ChromeDriver 설치
RUN CHROME_VERSION=$(google-chrome --version | cut -d " " -f3 | cut -d "." -f1) \
    && CHROMEDRIVER_VERSION=$(curl -s "https://chromedriver.storage.googleapis.com/LATEST_RELEASE_${CHROME_VERSION}") \
    && wget -O /tmp/chromedriver.zip "https://chromedriver.storage.googleapis.com/${CHROMEDRIVER_VERSION}/chromedriver_linux64.zip" \
    && unzip /tmp/chromedriver.zip -d /tmp/ \
    && mv /tmp/chromedriver /usr/local/bin/chromedriver \
    && chmod +x /usr/local/bin/chromedriver \
    && rm /tmp/chromedriver.zip

# 빌드 단계(builder)에서 생성된 jar 파일만 복사
COPY --from=builder /app/build/libs/*.jar ./app.jar

# 파일 소유권 변경 (보안)
RUN chown -R appuser:appuser /app

# Chrome이 headless 모드에서 실행되도록 환경 변수 설정
ENV DISPLAY=:99
ENV CHROME_BIN=/usr/bin/google-chrome
ENV CHROME_PATH=/usr/bin/google-chrome

# JVM 최적화 설정
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# 포트 노출
EXPOSE 8083

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8083/api/crawler/health || exit 1

# 보안: root가 아닌 사용자로 전환
USER appuser

# 컨테이너 실행 시 이 명령어가 실행됨
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
