# Stage 1: build do jar com Maven Wrapper
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

# Stage 2: runtime enxuto (JRE + usuário non-root)
FROM eclipse-temurin:21-jre
WORKDIR /app
# curl serve ao healthcheck da API no compose (GET /actuator/health)
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/* \
  && useradd -m -u 1001 appuser
COPY --from=build /app/target/treino-*.jar /app/app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
