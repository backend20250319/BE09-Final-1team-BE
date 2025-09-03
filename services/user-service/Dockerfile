# =================
# 1단계: 프로젝트 빌드 (JDK가 포함된 이미지 사용)
# =================
FROM openjdk:17-jdk-slim AS builder
WORKDIR /app

# 💡 개선점: 종속성 파일 먼저 복사하여 캐싱 활용
COPY build.gradle settings.gradle ./
COPY gradlew ./gradlew
COPY gradle ./gradle

# 💡 개선점: 소스 코드 복사 전 종속성 먼저 다운로드
# 이렇게 하면 소스 코드만 변경 시 이 단계는 캐시를 사용합니다.
RUN ./gradlew dependencies

# 소스 코드 복사
COPY src ./src

# 프로젝트 빌드
RUN ./gradlew bootJar

# =================
# 2단계: 실행 환경 구성 (JRE만 포함된 가벼운 이미지 사용)
# =================
FROM openjdk:17-slim
WORKDIR /app

# 빌드 단계(builder)에서 생성된 jar 파일만 복사
COPY --from=builder /app/build/libs/*.jar ./app.jar

# 컨테이너 실행
ENTRYPOINT ["java", "-jar", "app.jar"]

# 서비스 포트 명시
EXPOSE 8081