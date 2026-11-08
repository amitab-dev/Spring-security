# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy the Maven wrapper and pom first so dependencies are cached
# and only re-downloaded when pom.xml changes
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy the source and build the jar
COPY src src
RUN ./mvnw package -DskipTests -B

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a non-root user
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Respect container memory limits
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# exec makes java PID 1 so it receives SIGTERM and shuts down gracefully
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
