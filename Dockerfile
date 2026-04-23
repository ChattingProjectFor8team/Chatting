# 1. 빌드 스테이지
FROM amazoncorretto:21-alpine AS build
WORKDIR /app

# 🌟 핵심 1: 소스코드를 빼고, Gradle 설정 파일들만 먼저 복사합니다.
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 🌟 핵심 2: 소스코드가 없어도 라이브러리들을 미리 다 다운로드 받습니다.
# 코드가 수정되어도 이 단계는 '캐시'되어 1초 만에 지나갑니다!
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

# 🌟 핵심 3: 진짜 소스코드는 라이브러리를 다 받은 후에 복사합니다.
COPY src src

# 실제 빌드 (이미 도커 안에 라이브러리가 다 있어서 1~2분 안에 끝납니다)
RUN ./gradlew bootJar -x test --no-daemon

# 2. 실행 스테이지
FROM amazoncorretto:21-alpine
WORKDIR /app

# 빌드 스테이지에서 생성된 jar 파일을 복사
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# 컨테이너 실행
ENTRYPOINT ["java", "-jar", "app.jar"]

