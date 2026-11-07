# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Run stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /app/uploads && chown -R spring:spring /app
USER spring:spring
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar
ENV STORAGE_PATH=/app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
