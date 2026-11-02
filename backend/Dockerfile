# Multi-stage build: JDK 17 builds the Spring Boot fat jar, JRE 17 runs it (infrastructure.yml containerization).
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src/main/ src/main/
# Tests run outside the image (Testcontainers + specs/ live outside this build context).
RUN ./mvnw -B -q package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /workspace/target/api-*.jar /app/app.jar
# Entrypoint maps Render's DATABASE_URL / PORT onto the Spring settings (local compose passes DB_* directly).
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod 755 /app/docker-entrypoint.sh
# Keep the heap inside small containers (Render free plan = 512 MB RAM).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
USER app
# 8080 locally; on Render the app listens on $PORT (default 10000) via SERVER_PORT.
EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
