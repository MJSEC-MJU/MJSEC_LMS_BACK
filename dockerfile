# ─── 1) Build stage ────────────────────────────────────────────────────────────
FROM amazoncorretto:17-al2023@sha256:9f2fde515fa060e6218b680d0f5045940d915af43af266145586344bddb30320 AS builder

# 작업 디렉토리 설정
WORKDIR /home/gradle/project

COPY . .

# RUN gradle dependencies --no-daemon

# JAR 빌드
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
    && ./gradlew clean bootJar --no-daemon

# ─── 2) Run stage ─────────────────────────────────────────────────────────────
FROM amazoncorretto:17-al2023@sha256:9f2fde515fa060e6218b680d0f5045940d915af43af266145586344bddb30320

WORKDIR /app

COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
