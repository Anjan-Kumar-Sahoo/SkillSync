# ============================================================
#  Config Server - Production Dockerfile
# ============================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/logs && chmod -R 777 /app/logs
RUN chown -R appuser:appgroup /app
USER appuser
EXPOSE 8888
ENV JAVA_OPTS="-Xms256m -Xmx512m"
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8888/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
