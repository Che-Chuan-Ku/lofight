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
# scripts/build-pages.mjs writes the D41 static pages (guide / bosses / faq / about / updates / privacy, robots.txt, sitemap.xml) into dist/
COPY scripts/ scripts/
RUN npm run build

# Stage 2: Maven 3.9.9 + JDK 17 builds the Spring Boot fat jar (infrastructure.yml containerization).
# The image ships Maven itself, so the build no longer depends on the wrapper fetching the Maven tarball from Maven
# Central (a Render build failed exactly there on 2026-11-04); only the project dependencies are downloaded.
# Stage 2a: pom.xml with the project <version> pinned to a constant. Every release bumps that version, which used to
# invalidate the dependency layer below and re-download everything on each deploy; copying this normalised pom instead
# keeps the layer cached until a dependency really changes (BuildKit keys COPY --from on file content).
FROM maven:3.9.9-eclipse-temurin-17 AS pom
COPY backend/pom.xml /pom.xml
RUN sed -i '/<artifactId>api<\/artifactId>/{n;s#<version>[^<]*</version>#<version>0-deps</version>#;}' /pom.xml \
    && grep -q '<version>0-deps</version>' /pom.xml
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY backend/maven-settings.xml /root/.m2/settings.xml
COPY --from=pom /pom.xml ./pom.xml
RUN mvn -B -q -Daether.connector.http.retryHandler.count=5 dependency:go-offline
COPY backend/pom.xml ./pom.xml
COPY backend/src/main/ src/main/
# Tests run outside the image (Testcontainers + specs/ live outside this build).
RUN mvn -B -q -o package -Dmaven.test.skip=true || mvn -B -q package -Dmaven.test.skip=true

# Stage 3: JRE 17 runtime.
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 app && mkdir -p /app/web /app/static && chown app /app/static
COPY --from=build /workspace/target/api-*.jar /app/app.jar
# Page template keeps the __GOOGLE_CLIENT_ID__ placeholder; docker-entrypoint.sh renders it into /app/static
# (served at / and /index.html) with the runtime GOOGLE_CLIENT_ID. The rest of dist/ (D41 static pages, robots.txt,
# sitemap.xml) is copied as-is into /app/static by the entrypoint.
COPY --from=web /web/dist/ /app/web/
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
