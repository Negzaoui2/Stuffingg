# syntax=docker/dockerfile:1.7

# ============================================================
# Stage 1 : BUILD (Maven + JDK 21)
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# 1) Cache des dépendances : on copie d'abord uniquement le pom.xml
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 2) Copie du code source et build
COPY src ./src
RUN mvn -B -q clean package -DskipTests \
 && mkdir -p target/extracted \
 && cp target/*.jar target/extracted/app.jar

# ============================================================
# Stage 2 : RUNTIME (JRE 21, léger, non-root)
# ============================================================
FROM eclipse-temurin:21-jre AS runtime

# curl pour le HEALTHCHECK
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# Utilisateur non-root
RUN groupadd --system --gid 1001 spring \
 && useradd  --system --uid 1001 --gid spring --home /app --shell /usr/sbin/nologin spring

WORKDIR /app

COPY --from=build --chown=spring:spring /workspace/target/extracted/app.jar /app/app.jar

USER spring

ENV SERVER_PORT=8086 \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8086

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -fsS "http://localhost:${SERVER_PORT}/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
