# 📄 DOCUMENT 5: DEPLOYMENT + DEVOPS

> [!IMPORTANT]
> **Architecture Update (March 2026):** The following services have been merged and are removed from deployment topologies:
> - **Mentor Service + Group Service → User Service** (port 8082)
> - **Review Service → Session Service** (port 8085)
>
> **Payment Extraction (March 2026):** Payment has been extracted from User Service into a dedicated **Payment Service** (port 8086) with its own database (`skillsync_payment`).
>
> **CQRS + Redis Caching (March 2026):** All business services now require a **Redis 7.2** instance for distributed caching. The `docker-compose.yml` includes Redis as a service dependency with AOF persistence. See `doc6_cqrs_redis_architecture.md` for details.
>
> The original deployment diagrams below reflect the initial 11-service architecture. Real deployments use the current 9-service topology.

## SkillSync — Infrastructure, Deployment & Operations

---

## 5.1 Production Architecture

```
                            ┌──────────────────────────┐
                            │      DNS / CDN           │
                            │   (CloudFlare / AWS CF)  │
                            └────────────┬─────────────┘
                                         │
                                         │ HTTPS (443)
                                         ▼
                            ┌──────────────────────────┐
                            │       Nginx              │
                            │   Reverse Proxy          │
                            │   (SSL Termination)      │
                            │                          │
                            │  /           → React SPA │
                            │  /api/*      → Gateway   │
                            │  /ws/*       → WebSocket │
                            └──────┬─────────┬─────────┘
                                   │         │
                      ┌────────────┘         └────────────┐
                      │                                    │
                      ▼                                    ▼
         ┌──────────────────────┐            ┌──────────────────────┐
         │    React Frontend    │            │  Spring Cloud Gateway │
         │    (Static Files)    │            │       :8080           │
         │    Served by Nginx   │            │  ┌────────────────┐  │
         └──────────────────────┘            │  │ JWT Filter     │  │
                                             │  │ Rate Limiter   │  │
                                             │  │ Circuit Breaker│  │
                                             │  │ Load Balancer  │  │
                                             │  └────────────────┘  │
                                             └──────────┬───────────┘
                                                        │
                              ┌──────────────────────────┼──────────────────────────┐
                              │                          │                          │
                    ┌─────────┴─────────┐    ┌───────────┴─────────┐    ┌──────────┴──────────┐
                    │                   │    │                     │    │                     │
                    │  Auth Service     │    │  User Service       │    │  Mentor Service     │
                    │  :8081 (×2)       │    │  :8082 (×2)         │    │  :8083 (×2)         │
                    │                   │    │                     │    │                     │
                    └─────────┬─────────┘    └───────────┬─────────┘    └──────────┬──────────┘
                              │                          │                          │
                    ┌─────────┴─────────┐    ┌───────────┴─────────┐    ┌──────────┴──────────┐
                    │                   │    │                     │    │                     │
                    │  Skill Service    │    │  Session Service    │    │  Group Service      │
                    │  :8084 (×1)       │    │  :8085 (×2)         │    │  :8086 (×1)         │
                    │                   │    │                     │    │                     │
                    └─────────┬─────────┘    └───────────┬─────────┘    └──────────┬──────────┘
                              │                          │                          │
                    ┌─────────┴─────────┐    ┌───────────┴─────────┐               │
                    │                   │    │                     │               │
                    │  Review Service   │    │ Notification Service│               │
                    │  :8087 (×1)       │    │  :8088 (×2)         │               │
                    │                   │    │                     │               │
                    └───────────────────┘    └─────────────────────┘               │
                                                                                   │
                          ┌──────────────────────────────────────────────────────────┘
                          │
          ┌───────────────┼───────────────┐
          │               │               │
          ▼               ▼               ▼
    ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
   │ PostgreSQL  │ │  RabbitMQ   │ │   Eureka    │ │   Redis     │
   │ Cluster     │ │  Cluster    │ │   Server    │ │   7.2       │
   │ (Primary +  │ │ (Mirrored)  │ │   :8761     │ │   :6379     │
   │  Replica)   │ │             │ │             │ │ (AOF+LRU)   │
   └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘
```

**Scaling annotations** (×N) show the recommended minimum replica count for production.

---

## 5.2 Docker Setup

### 5.2.1 Dockerfile — Spring Boot Microservice (Multi-stage)

```dockerfile
# Dockerfile.service
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy parent POM and common module first (layer caching)
COPY pom.xml .
COPY skillsync-common/pom.xml ./skillsync-common/
COPY skillsync-common/src ./skillsync-common/src

# Copy service-specific POM and source
ARG SERVICE_NAME
COPY ${SERVICE_NAME}/pom.xml ./${SERVICE_NAME}/
COPY ${SERVICE_NAME}/src ./${SERVICE_NAME}/src

# Build only the target service (skip tests in CI — run separately)
RUN mvn clean package -pl ${SERVICE_NAME} -am -DskipTests \
    -Dmaven.repo.local=/app/.m2

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring

ARG SERVICE_NAME
WORKDIR /app

# Copy built artifact
COPY --from=builder /app/${SERVICE_NAME}/target/*.jar app.jar

# Security: Run as non-root
USER spring:spring

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:${SERVER_PORT}/actuator/health || exit 1

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -Djava.security.egd=file:/dev/./urandom"

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
```

### 5.2.2 Dockerfile — React Frontend (Multi-stage)

```dockerfile
# Dockerfile.frontend
# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files first (layer caching)
COPY package.json package-lock.json ./
RUN npm ci --prefer-offline

# Copy source and build
COPY . .

ARG VITE_API_BASE_URL
ARG VITE_WS_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_WS_URL=${VITE_WS_URL}

RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:1.25-alpine

# Remove default Nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom Nginx config
COPY nginx/nginx.conf /etc/nginx/conf.d/

# Copy built React app
COPY --from=builder /app/dist /usr/share/nginx/html

# Security headers script
RUN chown -R nginx:nginx /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget -qO- http://localhost:80/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
```

### 5.2.3 Nginx Configuration

```nginx
# nginx/nginx.conf
server {
    listen 80;
    server_name localhost;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https:; connect-src 'self' ws: wss: http://api-gateway:8080;" always;

    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml text/javascript image/svg+xml;

    # Serve React SPA
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;  # SPA fallback routing

        # Cache static assets aggressively
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }

    # Proxy API requests to Spring Cloud Gateway
    location /api/ {
        proxy_pass http://api-gateway:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Timeouts
        proxy_connect_timeout 10s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;

        # Error handling
        proxy_intercept_errors on;
        error_page 502 503 504 /50x.html;
    }

    # WebSocket proxy
    location /ws/ {
        proxy_pass http://notification-service:8088;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 86400s; # 24h for WebSocket connections
    }

    # Health check endpoint
    location /health {
        access_log off;
        return 200 "healthy";
        add_header Content-Type text/plain;
    }

    # Custom error pages
    location = /50x.html {
        root /usr/share/nginx/html;
        internal;
    }
}
```

### 5.2.4 Docker Compose — Full Stack

```yaml
# docker-compose.yml
version: '3.9'

services:
  # ============================================
  # INFRASTRUCTURE
  # ============================================

  postgres:
    image: postgres:16-alpine
    container_name: skillsync-postgres
    environment:
      POSTGRES_USER: skillsync
      POSTGRES_PASSWORD: ${DB_PASSWORD:-skillsync_dev}
      POSTGRES_MULTIPLE_DATABASES: >
        skillsync_auth,
        skillsync_user,
        skillsync_payment,
        skillsync_skill,
        skillsync_session,
        skillsync_notification
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./infrastructure/postgres/init-multiple-dbs.sh:/docker-entrypoint-initdb.d/init-multiple-dbs.sh
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U skillsync"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - skillsync-network

  rabbitmq:
    image: rabbitmq:3.12-management-alpine
    container_name: skillsync-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER:-skillsync}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASS:-skillsync_dev}
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    ports:
      - "5672:5672"    # AMQP
      - "15672:15672"  # Management UI
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "check_running"]
      interval: 30s
      timeout: 10s
      retries: 5
    networks:
      - skillsync-network

  # ============================================
  # SERVICE DISCOVERY
  # ============================================

  eureka-server:
    build:
      context: .
      dockerfile: Dockerfile.service
      args:
        SERVICE_NAME: eureka-server
    container_name: skillsync-eureka
    environment:
      SERVER_PORT: 8761
      SPRING_PROFILES_ACTIVE: docker
    ports:
      - "8761:8761"
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8761/actuator/health"]
      interval: 30s
      timeout: 5s
      start_period: 60s
      retries: 3
    networks:
      - skillsync-network

  # ============================================
  # API GATEWAY
  # ============================================

  api-gateway:
    build:
      context: .
      dockerfile: Dockerfile.service
      args:
        SERVICE_NAME: api-gateway
    container_name: skillsync-gateway
    environment:
      SERVER_PORT: 8080
      SPRING_PROFILES_ACTIVE: docker
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8080:8080"
    depends_on:
      eureka-server:
        condition: service_healthy
    networks:
      - skillsync-network

  # ============================================
  # MICROSERVICES
  # ============================================

  auth-service:
    build:
      context: .
      dockerfile: Dockerfile.service
      args:
        SERVICE_NAME: auth-service
    container_name: skillsync-auth
    environment:
      SERVER_PORT: 8081
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/skillsync_auth
      SPRING_DATASOURCE_USERNAME: skillsync
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-skillsync_dev}
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka
      JWT_SECRET: ${JWT_SECRET}
      JWT_ACCESS_EXPIRATION: 900000
      JWT_REFRESH_EXPIRATION: 604800000
    depends_on:
      postgres:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
    networks:
      - skillsync-network

  user-service:
    build:
      context: .
      dockerfile: Dockerfile.service
      args:
        SERVICE_NAME: user-service
    container_name: skillsync-user
    environment:
      SERVER_PORT: 8082
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/skillsync_user
      SPRING_DATASOURCE_USERNAME: skillsync
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-skillsync_dev}
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_USER:-skillsync}
      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_PASS:-skillsync_dev}
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
    networks:
      - skillsync-network

  payment-service:
    build:
      context: .
      dockerfile: Dockerfile.service
      args:
        SERVICE_NAME: payment-service
    container_name: skillsync-payment
    environment:
      SERVER_PORT: 8086
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/skillsync_payment
      SPRING_DATASOURCE_USERNAME: skillsync
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-skillsync_dev}
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_USER:-skillsync}
      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_PASS:-skillsync_dev}
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka
      RAZORPAY_API_KEY: ${RAZORPAY_API_KEY}
      RAZORPAY_API_SECRET: ${RAZORPAY_API_SECRET}
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
    networks:
      - skillsync-network

  skill-service:
    build:
      context: .
      dockerfile: Dockerfile.service
      args:
        SERVICE_NAME: skill-service
    container_name: skillsync-skill
    environment:
      SERVER_PORT: 8084
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/skillsync_skill
      SPRING_DATASOURCE_USERNAME: skillsync
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-skillsync_dev}
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka
    depends_on:
      postgres:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
    networks:
      - skillsync-network

  session-service:
    build:
      context: .
      dockerfile: Dockerfile.service
      args:
        SERVICE_NAME: session-service
    container_name: skillsync-session
    environment:
      SERVER_PORT: 8085
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/skillsync_session
      SPRING_DATASOURCE_USERNAME: skillsync
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-skillsync_dev}
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_USER:-skillsync}
      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_PASS:-skillsync_dev}
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
    networks:
      - skillsync-network

  notification-service:
    build:
      context: .
      dockerfile: Dockerfile.service
      args:
        SERVICE_NAME: notification-service
    container_name: skillsync-notification
    environment:
      SERVER_PORT: 8088
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/skillsync_notification
      SPRING_DATASOURCE_USERNAME: skillsync
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-skillsync_dev}
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_USER:-skillsync}
      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_PASS:-skillsync_dev}
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
    networks:
      - skillsync-network

  # ============================================
  # FRONTEND
  # ============================================

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile.frontend
      args:
        VITE_API_BASE_URL: http://localhost:8080
        VITE_WS_URL: http://localhost:8088/ws
    container_name: skillsync-frontend
    ports:
      - "3000:80"
    depends_on:
      - api-gateway
    networks:
      - skillsync-network

# ============================================
# VOLUMES & NETWORKS
# ============================================

volumes:
  postgres_data:
    driver: local
  rabbitmq_data:
    driver: local

networks:
  skillsync-network:
    driver: bridge
```

### 5.2.5 PostgreSQL Multi-DB Init Script

```bash
#!/bin/bash
# infrastructure/postgres/init-multiple-dbs.sh
set -e
set -u

function create_user_and_database() {
    local database=$1
    echo "Creating database '$database'"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        CREATE DATABASE $database;
        GRANT ALL PRIVILEGES ON DATABASE $database TO $POSTGRES_USER;
EOSQL
}

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
    echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
    for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
        db=$(echo $db | xargs)  # trim whitespace
        create_user_and_database $db
    done
    echo "Multiple databases created"
fi
```

---

## 5.3 CI/CD Pipeline (GitHub Actions)

### 5.3.1 Complete Pipeline

```yaml
# .github/workflows/ci-cd.yml
name: SkillSync CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  JAVA_VERSION: '21'
  NODE_VERSION: '20'
  DOCKER_REGISTRY: ghcr.io
  IMAGE_PREFIX: ${{ github.repository_owner }}/skillsync

jobs:
  # ============================================
  # STAGE 1: BACKEND BUILD + TEST
  # ============================================

  backend-build:
    name: Backend Build & Test
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
          POSTGRES_DB: skillsync_test
        ports:
          - 5432:5432
        options: >-
          --health-cmd "pg_isready -U test"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      rabbitmq:
        image: rabbitmq:3.12-management-alpine
        ports:
          - 5672:5672
        options: >-
          --health-cmd "rabbitmq-diagnostics check_running"
          --health-interval 30s
          --health-timeout 10s
          --health-retries 5

    strategy:
      matrix:
        service:
          - auth-service
          - user-service
          - payment-service
          - skill-service
          - session-service
          - notification-service

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Java ${{ env.JAVA_VERSION }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'maven'

      - name: Build common module
        run: mvn install -pl skillsync-common -DskipTests

      - name: Run tests for ${{ matrix.service }}
        run: |
          mvn test -pl ${{ matrix.service }} -am \
            -Dspring.profiles.active=test \
            -Dspring.datasource.url=jdbc:postgresql://localhost:5432/skillsync_test \
            -Dspring.datasource.username=test \
            -Dspring.datasource.password=test

      - name: Generate coverage report
        run: mvn jacoco:report -pl ${{ matrix.service }}

      - name: Check coverage threshold (80%)
        run: |
          COVERAGE=$(python3 -c "
          import xml.etree.ElementTree as ET
          tree = ET.parse('${{ matrix.service }}/target/site/jacoco/jacoco.xml')
          root = tree.getroot()
          for counter in root.findall('.//counter[@type=\"LINE\"]'):
              missed = int(counter.get('missed', 0))
              covered = int(counter.get('covered', 0))
              total = missed + covered
              if total > 0:
                  print(f'{covered * 100 // total}')
                  break
          ")
          echo "Coverage: ${COVERAGE}%"
          if [ "${COVERAGE}" -lt 80 ]; then
            echo "::error::Coverage ${COVERAGE}% is below 80% threshold for ${{ matrix.service }}"
            exit 1
          fi

      - name: SonarQube Quality Gate
        # Placeholder for SonarQube integration (runs static analysis)
        run: echo "SonarQube analysis goes here"

      - name: Upload Test Report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: surefire-report-${{ matrix.service }}
          path: ${{ matrix.service }}/target/surefire-reports/

      - name: Upload coverage artifact
        uses: actions/upload-artifact@v4
        with:
          name: coverage-${{ matrix.service }}
          path: ${{ matrix.service }}/target/site/jacoco/

      - name: Build JAR
        run: mvn package -pl ${{ matrix.service }} -am -DskipTests

      - name: Upload JAR artifact
        uses: actions/upload-artifact@v4
        with:
          name: jar-${{ matrix.service }}
          path: ${{ matrix.service }}/target/*.jar

  # ============================================
  # STAGE 2: FRONTEND BUILD + TEST
  # ============================================

  frontend-build:
    name: Frontend Build & Test
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Node.js ${{ env.NODE_VERSION }}
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        run: cd frontend && npm ci

      - name: Lint
        run: cd frontend && npm run lint

      - name: Type check
        run: cd frontend && npm run type-check

      - name: Unit tests with coverage
        run: cd frontend && npm run test -- --coverage --watchAll=false --ci

      - name: Check coverage threshold (75%)
        run: |
          cd frontend
          COVERAGE=$(node -e "
            const report = require('./coverage/coverage-summary.json');
            console.log(Math.round(report.total.lines.pct));
          ")
          echo "Coverage: ${COVERAGE}%"
          if [ "${COVERAGE}" -lt 75 ]; then
            echo "::error::Frontend coverage ${COVERAGE}% is below 75% threshold"
            exit 1
          fi

      - name: Build production bundle
        run: cd frontend && npm run build
        env:
          VITE_API_BASE_URL: ${{ vars.API_BASE_URL }}
          VITE_WS_URL: ${{ vars.WS_URL }}

      - name: Upload build artifact
        uses: actions/upload-artifact@v4
        with:
          name: frontend-build
          path: frontend/dist/

  # ============================================
  # STAGE 3: E2E TESTS
  # ============================================

  e2e-tests:
    name: E2E Tests
    runs-on: ubuntu-latest
    needs: [backend-build, frontend-build]
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}

      - name: Install Playwright
        run: cd frontend && npm ci && npx playwright install --with-deps

      - name: Start services with Docker Compose
        run: docker compose -f docker-compose.test.yml up -d --wait
        timeout-minutes: 5

      - name: Run E2E tests
        run: cd frontend && npx playwright test
        env:
          PLAYWRIGHT_BASE_URL: http://localhost:3000

      - name: Upload Playwright report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: frontend/playwright-report/

      - name: Stop services
        if: always()
        run: docker compose -f docker-compose.test.yml down -v

  # ============================================
  # STAGE 4: BUILD & PUSH DOCKER IMAGES
  # ============================================

  docker-build:
    name: Build & Push Docker Images
    runs-on: ubuntu-latest
    needs: [backend-build, frontend-build]
    if: github.event_name == 'push' && (github.ref == 'refs/heads/main' || github.ref == 'refs/heads/develop')

    permissions:
      contents: read
      packages: write

    strategy:
      matrix:
        service:
          - auth-service
          - user-service
          - skill-service
          - session-service
          - notification-service
          - api-gateway
          - eureka-server

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Login to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.DOCKER_REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Determine tag
        id: tag
        run: |
          if [ "${{ github.ref }}" = "refs/heads/main" ]; then
            echo "tag=latest" >> $GITHUB_OUTPUT
            echo "env=production" >> $GITHUB_OUTPUT
          else
            echo "tag=develop" >> $GITHUB_OUTPUT
            echo "env=staging" >> $GITHUB_OUTPUT
          fi

      - name: Build and push ${{ matrix.service }}
        uses: docker/build-push-action@v5
        with:
          context: .
          file: Dockerfile.service
          push: true
          tags: |
            ${{ env.DOCKER_REGISTRY }}/${{ env.IMAGE_PREFIX }}/${{ matrix.service }}:${{ steps.tag.outputs.tag }}
            ${{ env.DOCKER_REGISTRY }}/${{ env.IMAGE_PREFIX }}/${{ matrix.service }}:${{ github.sha }}
          build-args: |
            SERVICE_NAME=${{ matrix.service }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  docker-build-frontend:
    name: Build & Push Frontend Image
    runs-on: ubuntu-latest
    needs: [frontend-build]
    if: github.event_name == 'push' && (github.ref == 'refs/heads/main' || github.ref == 'refs/heads/develop')

    permissions:
      contents: read
      packages: write

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.DOCKER_REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push frontend
        uses: docker/build-push-action@v5
        with:
          context: ./frontend
          file: ./frontend/Dockerfile.frontend
          push: true
          tags: |
            ${{ env.DOCKER_REGISTRY }}/${{ env.IMAGE_PREFIX }}/frontend:${{ github.sha }}
          build-args: |
            VITE_API_BASE_URL=${{ vars.API_BASE_URL }}
            VITE_WS_URL=${{ vars.WS_URL }}

  # ============================================
  # STAGE 5: DEPLOY
  # ============================================

  deploy:
    name: Deploy to ${{ needs.docker-build.outputs.env || 'staging' }}
    runs-on: ubuntu-latest
    needs: [docker-build, docker-build-frontend, e2e-tests]
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    environment: production

    steps:
      - name: Deploy to production server
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.DEPLOY_HOST }}
          username: ${{ secrets.DEPLOY_USER }}
          key: ${{ secrets.DEPLOY_SSH_KEY }}
          script: |
            cd /opt/skillsync
            
            # Pull latest images
            docker compose pull
            
            # Rolling update (zero downtime)
            docker compose up -d --remove-orphans
            
            # Wait for health checks
            sleep 30
            
            # Verify services are healthy
            docker compose ps --filter "health=healthy" | grep -c "healthy"
            
            # Prune old images
            docker image prune -f
```

---

## 5.4 Environment Configuration

### 5.4.1 Spring Profiles

```yaml
# application.yml (shared/base)
spring:
  application:
    name: ${SERVICE_NAME}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        default_schema: ${DB_SCHEMA:public}
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true

server:
  port: ${SERVER_PORT:8080}
  shutdown: graceful

management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  health:
    db:
      enabled: true
    rabbit:
      enabled: true

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka}
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${random.uuid}
```

```yaml
# application-dev.yml
spring:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  datasource:
    url: jdbc:postgresql://localhost:5432/${DB_NAME}
    username: skillsync
    password: skillsync_dev

logging:
  level:
    com.skillsync: DEBUG
    org.hibernate.SQL: DEBUG
```

```yaml
# application-docker.yml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  rabbitmq:
    host: ${SPRING_RABBITMQ_HOST:rabbitmq}
    port: ${SPRING_RABBITMQ_PORT:5672}
    username: ${SPRING_RABBITMQ_USERNAME:skillsync}
    password: ${SPRING_RABBITMQ_PASSWORD:skillsync_dev}

logging:
  level:
    com.skillsync: INFO
```

```yaml
# application-prod.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # NEVER auto-update in prod
    show-sql: false
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000

logging:
  level:
    root: WARN
    com.skillsync: INFO
```

### 5.4.2 Environment Variables Reference

```bash
# .env.example (copy to .env and fill in values)

# ============ DATABASE ============
DB_PASSWORD=your_strong_password_here

# ============ RABBITMQ ============
RABBITMQ_USER=skillsync
RABBITMQ_PASS=your_strong_password_here

# ============ JWT ============
JWT_SECRET=your-256-bit-secret-key-here-must-be-at-least-32-chars
JWT_ACCESS_EXPIRATION=900000         # 15 minutes
JWT_REFRESH_EXPIRATION=604800000     # 7 days

# ============ RAZORPAY (Payments) ============
RAZORPAY_API_KEY=rzp_test_your_key_here
RAZORPAY_API_SECRET=your_razorpay_secret_here

# ============ FRONTEND ============
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=http://localhost:8088/ws

# ============ DEPLOYMENT ============
DEPLOY_HOST=your-server-ip
DEPLOY_USER=deploy
```

---

## 5.5 Scaling Strategy

### 5.5.1 Horizontal Scaling Matrix

| Service | Min Replicas | Max Replicas | Scale Trigger | Priority |
|---|---|---|---|---|
| API Gateway | 2 | 4 | CPU > 70% | Critical |
| Auth Service | 2 | 6 | RPS > 500 | Critical |
| User Service | 2 | 4 | CPU > 70% | High |
| Mentor Service | 2 | 4 | CPU > 70% | High |
| Skill Service | 1 | 2 | Memory > 80% | Low |
| Session Service | 2 | 6 | RPS > 300 | Critical |
| Group Service | 1 | 3 | CPU > 70% | Medium |
| Review Service | 1 | 3 | CPU > 70% | Medium |
| Notification Service | 2 | 4 | Queue depth > 1000 | High |
| Observability Stack | 1 | 1 | N/A (Stateful) | Infrastructure |

### 5.5.2 Database Scaling

```
                    ┌─────────────────┐
                    │   Primary       │
                    │   (Read/Write)  │
                    └────────┬────────┘
                             │
                    ┌────────┴────────┐
                    │  Streaming      │
                    │  Replication    │
                    ├────────┬────────┤
                    │        │        │
               ┌────┴───┐  ┌┴────────┴───┐
               │Replica │  │  Replica    │
               │(Read)  │  │  (Read)     │
               └────────┘  └─────────────┘
```

- **Read replicas** for Session Service and Mentor Service (high read load)
- **Connection pooling** via HikariCP (20 connections per service)
- **Indexes** on all foreign keys and frequently queried columns
- **Partitioning** on `sessions` table by `session_date` (future)

### 5.5.3 Caching Strategy

```
Request → API Gateway → Service → Check Redis Cache → DB (if cache miss)
                                        │
                                  Cache Hit → Return cached data
```

| Data | Cache TTL | Invalidation |
|---|---|---|
| Skill catalog | 1 hour | On admin update |
| Mentor search results | 30 seconds | On profile update |
| User profile | 5 minutes | On profile edit |
| Notification count | 10 seconds | On new notification |

---

## 5.6 Monitoring & Observability

### 5.6.1 Health Checks

```java
// Custom health indicator
@Component
public class RabbitMQHealthIndicator extends AbstractHealthIndicator {

    private final RabbitTemplate rabbitTemplate;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            rabbitTemplate.execute(channel -> {
                channel.queueDeclarePassive("session.requested.queue");
                return null;
            });
            builder.up()
                .withDetail("rabbitMQ", "Connected")
                .withDetail("queues", "Accessible");
        } catch (Exception e) {
            builder.down()
                .withDetail("rabbitMQ", "Connection failed")
                .withException(e);
        }
    }
}
```

### 5.6.2 Metrics (Prometheus + Grafana)

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'skillsync-services'
    metrics_path: '/actuator/prometheus'
    eureka_sd_configs:
      - server: 'http://eureka-server:8761/eureka'
    relabel_configs:
      - source_labels: [__meta_eureka_app_name]
        target_label: service
```

### Key Dashboards

| Dashboard | Metrics |
|---|---|
| **Service Health** | Up/down status, JVM memory, GC pauses, thread count |
| **API Performance** | Request rate, p50/p95/p99 latency, error rate by endpoint |
| **Business Metrics** | Sessions created/day, new users/day, reviews/day |
| **RabbitMQ** | Queue depth, publish/consume rate, dead letter count |
| **Database** | Connection pool usage, slow queries, replication lag |

### 5.6.3 Alerting Rules

```yaml
# alert-rules.yml
groups:
  - name: skillsync-alerts
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "{{ $labels.service }} is down"

      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error rate on {{ $labels.service }}"

      - alert: HighLatency
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "p95 latency > 500ms on {{ $labels.service }}"

      - alert: RabbitMQQueueBacklog
        expr: rabbitmq_queue_messages > 10000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Queue backlog > 10k messages"

      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "DB connection pool > 90% on {{ $labels.service }}"
```

---

> [!IMPORTANT]
> **Production Checklist** (before going live):
> - [ ] All environment variables set in production `.env`
> - [ ] JWT secret is ≥256 bits and securely stored
> - [ ] Database passwords are strong and rotated
> - [ ] Razorpay Production API Key/Secret configured
> - [ ] SSL certificates configured on Nginx
> - [ ] Rate limiting enabled on API Gateway
> - [ ] Health checks pass for all services
> - [ ] Monitoring dashboards configured
> - [ ] Alert rules configured and tested
> - [ ] Database backups scheduled (daily)
> - [ ] Log rotation configured
> - [ ] Firewall rules: only ports 80/443 exposed publicly
