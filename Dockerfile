# 1. 빌드 스테이지 (AS build 를 반드시 추가해야 합니다!)
FROM amazoncorretto:21-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew bootJar

# 2. 실행 스테이지
FROM amazoncorretto:21-alpine
WORKDIR /app

# 위에서 정의한 'build' 스테이지로부터 파일을 가져옵니다.
COPY --from=build /app/build/libs/*1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]


