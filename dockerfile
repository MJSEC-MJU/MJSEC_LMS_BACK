# ─── 1) Build stage ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17.0.21_9-jdk-jammy@sha256:76751c16a2a07ec0960e659a2603491f8c79d95592173f6c0df8bf8ebab257cc AS builder

# 작업 디렉토리 설정
WORKDIR /home/gradle/project

COPY . .

# RUN gradle dependencies --no-daemon

# JAR 빌드
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
    && ./gradlew clean bootJar --no-daemon

# ─── 2) Run stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17.0.21_9-jre-jammy@sha256:aa6381384f8db99762fa7d0d3c2b5de58874e5f12c96df4c13a1455376ae0ed1

WORKDIR /app

COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
