# Single deployable: the Vite front end and the Spring Boot API in one image (one Render web service, one URL).
# Build context is the REPO ROOT (render.yaml dockerContext: . / docker-compose build.context: ..);
# the root .dockerignore keeps .env*, node_modules, specs, tests and other non-build files out of the context.

# Stage 1: front end -> dist/index.html (single file). Node matches .node-version.
FROM node:22.12.0-alpine AS web
WORKDIR /web
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY index.html vite.config.js ./
COPY src/ src/
RUN npm run build

# Stage 2: JDK 17 builds the Spring Boot fat jar (infrastructure.yml containerization).
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY backend/src/main/ src/main/
# Tests run outside the image (Testcontainers + specs/ live outside this build).
RUN ./mvnw -B -q package -Dmaven.test.skip=true

# Stage 3: JRE 17 runtime.
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 app && mkdir -p /app/web /app/static && chown app /app/static
COPY --from=build /workspace/target/api-*.jar /app/app.jar
# Page template keeps the __GOOGLE_CLIENT_ID__ placeholder; docker-entrypoint.sh renders it into /app/static
# (served at / and /index.html) with the runtime GOOGLE_CLIENT_ID.
COPY --from=web /web/dist/index.html /app/web/index.html
# Entrypoint maps Render's DATABASE_URL / PORT onto the Spring settings (local compose passes DB_* directly).
COPY backend/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod 755 /app/docker-entrypoint.sh
# Keep the heap inside small containers (Render free plan = 512 MB RAM).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC" \
    SPRING_WEB_RESOURCES_STATIC_LOCATIONS="file:/app/static/"
USER app
# 8080 locally; on Render the app listens on $PORT (default 10000) via SERVER_PORT.
EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
