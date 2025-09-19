# 1단계: 최종 실행 환경 설정
# 소스 코드를 다시 빌드하지 않으므로, 가벼운 JRE(Java Runtime Environment) 이미지를 사용합니다.
FROM openjdk:17-slim

# 작업 디렉토리를 지정합니다.
WORKDIR /app

# Jenkins가 이미 빌드한 JAR 파일을 복사합니다.
# Dockerfile은 'services/user-service' 폴더에서 실행되므로,
# 그 안의 'build/libs/' 경로에서 *.jar 파일을 찾습니다.
COPY build/libs/*.jar app.jar

# 애플리케이션이 사용할 포트를 노출합니다. (필요에 따라 변경)
EXPOSE 8083

# 컨테이너가 시작될 때 JAR 파일을 실행합니다.
ENTRYPOINT ["java","-jar","/app/app.jar"]

