# =================
# 1단계: 프로젝트 빌드 (JDK가 포함된 이미지 사용)
# =================
FROM openjdk:17-jdk-slim AS builder

# 작업 디렉토리 설정
WORKDIR /app

# 프로젝트 파일 전체 복사
COPY build.gradle settings.gradle ./
COPY gradlew ./gradlew
COPY gradle ./gradle

# Gradle을 사용하여 프로젝트를 빌드 (jar 파일 생성)
RUN ./gradlew dependencies

# 소스 코드 복사
COPY src ./src

# 프로젝트 빌드
RUN ./gradlew bootJar
# =================
# 2단계: 실행 환경 구성 (Chrome + JRE 포함)
# =================
FROM openjdk:17-slim

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

# Chrome이 headless 모드에서 실행되도록 환경 변수 설정
ENV DISPLAY=:99
ENV CHROME_BIN=/usr/bin/google-chrome
ENV CHROME_PATH=/usr/bin/google-chrome

# 포트 노출
EXPOSE 8083

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8083/api/crawler/health || exit 1

# 컨테이너 실행 시 이 명령어가 실행됨
ENTRYPOINT ["java", "-jar", "app.jar"]