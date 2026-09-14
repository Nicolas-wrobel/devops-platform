# apps/backend/Dockerfile

# 1) Build
FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /src

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

# 2) Runtime
FROM eclipse-temurin:21.0.12_8-jre-alpine
WORKDIR /app

RUN apk upgrade --no-cache \
    && apk add --no-cache curl \
    && addgroup -S appgroup && adduser -S -G appgroup appuser

COPY --from=build --chown=appuser:appgroup /src/target/*.jar app.jar

USER appuser
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]