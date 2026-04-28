FROM amazoncorretto:21-alpine
WORKDIR /app
# 밖에서 빌드된 결과물을 복사만 함
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
