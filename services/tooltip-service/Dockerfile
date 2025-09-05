# =================
# 1단계: 프로젝트 빌드 (JDK가 포함된 이미지 사용)
# =================
FROM openjdk:17-jdk-slim AS builder
WORKDIR /app

# 종속성 파일 먼저 복사하여 Docker 빌드 캐시 활용
COPY build.gradle settings.gradle ./
COPY gradlew ./gradlew
COPY gradle ./gradle

# 소스 코드 복사 전 종속성 먼저 다운로드 (빌드 속도 향상)
RUN ./gradlew dependencies

# 나머지 소스 코드 복사
COPY src ./src

# Gradle을 사용하여 프로젝트를 빌드 (jar 파일 생성)
RUN ./gradlew bootJar

# =================
# 2단계: 실행 환경 구성 (JRE만 포함된 가벼운 이미지 사용)
# =================
FROM openjdk:17-slim
WORKDIR /app

# 빌드 단계(builder)에서 생성된 jar 파일만 복사
COPY --from=builder /app/build/libs/*.jar ./app.jar

# 컨테이너 실행 시 이 명령어가 실행됨
ENTRYPOINT ["java", "-jar", "app.jar"]

# ⭐️ 중요: 각 서비스의 실제 포트로 변경해주세요! ⭐️
# 예: gateway-service는 8000, user-service는 8081 등
EXPOSE 8086