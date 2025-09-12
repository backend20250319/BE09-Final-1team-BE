# =================================================================
# 1단계: Gradle을 사용하여 애플리케이션을 빌드하는 'builder' 스테이지
# =================================================================
FROM gradle:jdk17 AS builder
WORKDIR /home/gradle/src

# 전체 소스 코드를 빌드 스테이지로 복사합니다.
COPY . .

# Gradle 빌드를 실행하여 JAR 파일을 생성합니다.
RUN gradle build --no-daemon -x test


# =================================================================
# 2단계: 실제 실행 환경을 구성하는 최종 스테이지
# =================================================================
FROM openjdk:17-slim
WORKDIR /app

# 빌드 스테이지(builder)에서 생성된 JAR 파일만 최종 이미지로 복사합니다.
COPY --from=builder /home/gradle/src/build/libs/*.jar app.jar

# 각 서비스에 맞는 포트를 노출합니다.
EXPOSE 8761 # <-- 이 포트는 각 서비스에 맞게 수정해주세요.

# 컨테이너가 시작될 때 JAR 파일을 실행합니다. (Cgroup 오류 해결 옵션 추가)
ENTRYPOINT ["java", "-Djava.security.properties=/dev/null", "-jar", "/app/app.jar"]