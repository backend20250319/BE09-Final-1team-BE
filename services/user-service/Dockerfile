# 1단계: 빌드 환경 (JDK 포함)
FROM openjdk:17-jdk-slim AS builder

WORKDIR /workspace/app

# Gradle 관련 파일들을 먼저 복사하여 의존성 레이어를 캐싱합니다.
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성을 먼저 다운로드합니다.
RUN ./gradlew dependencies

# 나머지 소스 코드를 복사합니다.
COPY src src

# 애플리케이션을 빌드합니다.
RUN ./gradlew bootJar

# 2단계: 최종 실행 환경 (JRE만 포함하여 가볍게 만듦)
FROM openjdk:17-jre-slim

WORKDIR /app

# 빌드 단계에서 생성된 JAR 파일만 최종 이미지로 복사합니다.
COPY --from=builder /workspace/app/build/libs/*.jar app.jar

# 애플리케이션이 사용할 포트를 노출합니다. (필요에 따라 변경)
EXPOSE 8081

# 컨테이너가 시작될 때 JAR 파일을 실행합니다.
ENTRYPOINT ["java","-jar","/app/app.jar"]
