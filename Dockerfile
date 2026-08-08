# 배포용 Dockerfile — Gradle 래퍼로 bootJar를 빌드하고 JRE 이미지에서 실행한다.
# H2 인메모리 + 기동 시 시드 적재 구조라 외부 DB 없이 컨테이너 1개로 데모가 돈다.

# 1) 빌드
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
# Windows 체크아웃(CRLF) 대비 줄바꿈 정리
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# 2) 런타임
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# 저사양 VM(1GB급) 대비 힙 상한. APP_JWT_SECRET 을 주입해 데모 시크릿을 덮어쓴다.
ENV JAVA_TOOL_OPTIONS="-Xmx256m"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
