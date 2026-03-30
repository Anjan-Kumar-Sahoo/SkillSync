# ============================================================
#  Session Service - Production Dockerfile
# ============================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY skillsync-cache-common ./skillsync-cache-common
RUN mvn -f skillsync-cache-common/pom.xml clean install -DskipTests -B

COPY session-service/pom.xml ./session-service/
RUN mvn -f session-service/pom.xml dependency:go-offline -B || true
COPY session-service/src ./session-service/src
RUN mvn -f session-service/pom.xml clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=build /app/session-service/target/*.jar app.jar
RUN mkdir -p /app/logs && chmod -R 777 /app/logs
RUN chown -R appuser:appgroup /app
USER appuser
EXPOSE 8085
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
