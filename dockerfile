# ─── 1) Build stage ────────────────────────────────────────────────────────────
FROM gradle:7.5.1-jdk17 AS builder

# 작업 디렉토리 설정
WORKDIR /home/gradle/project

COPY --chown=gradle:gradle . .

# RUN gradle dependencies --no-daemon

# JAR 빌드
RUN gradle clean bootJar --no-daemon

# ─── 2) Run stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
