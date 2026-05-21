# syntax=docker/dockerfile:1

FROM node:20-alpine AS frontend-build
ARG VITE_SITE_URL=http://localhost:8080
ARG VITE_YANDEX_METRIKA_ID=109312391
ENV VITE_SITE_URL=$VITE_SITE_URL
ENV VITE_YANDEX_METRIKA_ID=$VITE_YANDEX_METRIKA_ID
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM gradle:8.10.2-jdk17 AS backend-build
WORKDIR /app/backend
COPY backend/ ./
COPY --from=frontend-build /app/frontend/dist ./src/main/resources/static/
RUN gradle bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /data

COPY --from=backend-build /app/backend/build/libs/*.jar /app/app.jar

ENV SERVER_PORT=8080 \
    DATABASE_PATH=/data/favorites.db \
    APP_PROJECT_ROOT=/app \
    TELEGRAM_USER_API_ENABLED=false \
    JAVA_OPTS=""

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- "http://127.0.0.1:${SERVER_PORT}/api/health" | grep -q ok || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
