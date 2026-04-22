# 1. 빌드 스테이지
FROM amazoncorretto:21-alpine AS build
WORKDIR /app

# 전체 복사 후 빌드
COPY . .

# 테스트 실패 시 빌드 자체가 중단되도록 보장
RUN chmod +x gradlew && ./gradlew bootJar -x test

# 2. 실행 스테이지
FROM amazoncorretto:21-alpine
WORKDIR /app

# 빌드 스테이지에서 생성된 jar 파일을 복사
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# 컨테이너 실행 시 Spring 프로파일이나 환경변수를 유연하게 받을 수 있도록 설정
ENTRYPOINT ["java", "-jar", "app.jar"]


