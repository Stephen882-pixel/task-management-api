FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

COPY credentials.json ./
COPY tokens/ ./tokens/ 2>/dev/null || true

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD java -cp app.jar org.springframework.boot.loader.JarLauncher \
    -c "select 1" || exit 1


EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]