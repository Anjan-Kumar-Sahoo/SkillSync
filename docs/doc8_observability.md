# 📊 DOCUMENT 8: OBSERVABILITY — ZIPKIN + ACTUATOR + MICROMETER

## SkillSync — Distributed Tracing, Metrics, and Monitoring Architecture

---

## 8.1 Overview

SkillSync implements a full observability stack across all microservices:

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Metrics** | Micrometer + Prometheus | Application & cache performance metrics |
| **Dashboards**| Grafana | Visualization of metrics, auto-provisioned dashboards |
| **Tracing** | Micrometer Tracing + Zipkin | Distributed request tracing across services |
| **Health** | Spring Boot Actuator | Service health, readiness, and liveness checks |
| **Logging** | SLF4J + Loki + Promtail (optional) | Centralized, correlated logs with traceId/spanId aggregation |

```text
Request → API Gateway → auth-service → user-service → ...
              ↓               ↓              ↓
        [traceId propagated across all services]
              ↓               ↓              ↓
           Zipkin Server (collects all spans)
              ↓
        Zipkin UI (http://localhost:9411)
```

---

## 8.2 Zipkin & Prometheus/Grafana Infrastructure

### What is Zipkin?

Zipkin is a distributed tracing system that helps track requests as they flow across SkillSync's microservices. Each request is assigned a **traceId** that follows it through:

- API Gateway
- Auth Service
- User Service
- Session Service
- Skill Service
- Notification Service

### Docker Setup (Zipkin, Prometheus, Grafana, Loki)

```yaml
  zipkin:
    image: openzipkin/zipkin:latest
    container_name: skillsync-zipkin
    ports:
      - "9411:9411"

  prometheus:
    image: prom/prometheus:v2.45.0
    container_name: skillsync-prometheus
    volumes:
      - ./monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"

  grafana:
    image: grafana/grafana:10.0.3
    container_name: skillsync-grafana
    ports:
      - "3000:3000"
    volumes:
      - ./monitoring/grafana/provisioning:/etc/grafana/provisioning
      - ./monitoring/grafana/dashboards:/var/lib/grafana/dashboards
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=skillsync

  loki:
    image: grafana/loki:2.9.0
    container_name: skillsync-loki
    ports:
      - "3100:3100"
    command: -config.file=/etc/loki/local-config.yaml
```

### Spring Boot Configuration (ALL services)

```properties
# Tracing — 100% sampling for development, reduce in production
management.tracing.sampling.probability=1.0
management.zipkin.tracing.endpoint=http://${ZIPKIN_HOST:localhost}:9411/api/v2/spans
```

### Dependencies (via skillsync-cache-common)

All services that depend on `skillsync-cache-common` inherit these transitively:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

The API Gateway has these added directly to its `pom.xml`.

### Accessing Zipkin UI

```
http://localhost:9411
```

Use the Zipkin UI to:
- Search for traces by service name
- View request latency breakdown across services
- Identify slow spans or errors
- Correlate logs with trace IDs

---

## 8.3 Spring Boot Actuator

### Exposed Endpoints (ALL services)

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Service health status (includes Redis, DB, RabbitMQ health) |
| `GET /actuator/metrics` | Application metrics catalog |
| `GET /actuator/metrics/{metricName}` | Specific metric detail |
| `GET /actuator/prometheus` | Prometheus scrape endpoint for all metrics |
| `GET /actuator/info` | Application info |
| `GET /actuator/caches` | Registered cache names |

### Configuration

```properties
management.endpoints.web.exposure.include=health,info,metrics,caches,prometheus
management.metrics.tags.application=${spring.application.name}
```

---

## 8.4 Micrometer Metrics

### Custom Cache Metrics (from CacheService)

The shared `CacheService` in `skillsync-cache-common` registers these counters:

| Metric | Tags | Description |
|--------|------|-------------|
| `cache.operations` | `result=hit`, `service=<name>` | Cache hit count |
| `cache.operations` | `result=miss`, `service=<name>` | Cache miss count |
| `cache.operations` | `result=evict`, `service=<name>` | Cache eviction count |
| `cache.operations` | `result=error`, `service=<name>` | Redis error count |

### Querying Metrics

```bash
# Cache hit rate
curl http://localhost:8082/actuator/metrics/cache.operations?tag=result:hit

# Cache miss rate  
curl http://localhost:8082/actuator/metrics/cache.operations?tag=result:miss

# All metrics in Prometheus format
curl http://localhost:8082/actuator/prometheus
```

### Key Metrics to Monitor

| Metric | What It Tells You |
|--------|-------------------|
| `cache.operations{result=hit}` | Cache hit count — should be high |
| `cache.operations{result=miss}` | Cache miss count — triggers DB query |
| `cache.operations{result=evict}` | Eviction count (explicit invalidation) |
| `cache.operations{result=error}` | Redis errors — should be near zero |
| `http.server.requests` | API response time (auto-tracked by Actuator) |
| `jvm.memory.used` | JVM heap usage |
| `system.cpu.usage` | CPU utilization |

### Prometheus Integration & Target Scraping

All services expose `/actuator/prometheus` for Prometheus scraping.
Metrics are then visualized via **Grafana** (http://localhost:3000), using an auto-provisioned **Platform Overview Dashboard** that monitors JVM health, HTTP latency percentiles (p50/p99), error rates, and cache hits/misses.

```yaml
# Example Prometheus scrape config
scrape_configs:
  - job_name: 'skillsync-services'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['api-gateway:8080', 'auth-service:8081', 'user-service:8082', 'skill-service:8084', 'session-service:8085', 'notification-service:8088']
```

---

## 8.5 Structured Logging & Loki

### Log Pattern

All services use structured logging with traceId and spanId. Logs are automatically collected by **Grafana Loki** via Docker logging driver or Promtail for centralized aggregation.

```properties
logging.pattern.level=%5p [${spring.application.name},%X{traceId:-},%X{spanId:-}]
```

### Example Log Output

```
2026-03-26 14:30:25.123  INFO [user-service,abc123def456,789xyz] c.s.u.service.UserQueryService : Cache HIT: v1:user:profile:100
2026-03-26 14:30:25.125  INFO [session-service,abc123def456,321abc] c.s.s.service.SessionQueryService : Cache MISS: v1:session:200
```

### Benefits

1. **Trace Correlation**: Every log line includes the `traceId`, making it easy to follow a request across services
2. **Span Identification**: The `spanId` identifies the specific operation within the trace
3. **Service Identification**: The service name is included in every log line
4. **Grafana/Loki Integration**: Easily search `traceId="abc123def456"` in Grafana Explore to find all logs related to a request.

---

## 8.6 Environment Variables & Tooling Access

| Tool | Access URL | Default Login | Purpose |
|------|------------|---------------|---------|
| Zipkin | `http://localhost:9411` | (None) | Distributed trace analysis |
| Prometheus | `http://localhost:9090` | (None) | Raw metrics querying |
| Grafana | `http://localhost:3000` | `admin` / `skillsync` | Real-time monitoring dashboards |
| Loki | `http://localhost:3100` | (Via Grafana) | Log aggregation storage |
| Zipkin Port | `9411` | Zipkin server port (fixed) |

### .env Addition

```bash
ZIPKIN_HOST=zipkin
```

---

## 8.7 Service-Specific Configuration Summary

| Service | Actuator Port | Prometheus | Zipkin Tracing |
|---------|--------------|------------|----------------|
| API Gateway | 8080 | ✅ | ✅ |
| Auth Service | 8081 | ✅ | ✅ |
| User Service | 8082 | ✅ | ✅ |
| Skill Service | 8084 | ✅ | ✅ |
| Session Service | 8085 | ✅ | ✅ |
| Notification Service | 8088 | ✅ | ✅ |

---

## 8.8 Architecture Diagram

```text
┌──────────────┐
│   Client     │
└──────┬───────┘
       │ traceId: abc123
       ▼
┌──────────────┐    ┌──────────────┐
│  API Gateway │───►│  Zipkin      │
│  (port 8080) │    │  (port 9411) │
└──────┬───────┘    └──────▲───────┘
       │                   │
       ├─────────┬─────────┤
       ▼         ▼         │
┌──────────┐ ┌──────────┐  │
│  Auth    │ │  User    │──┘
│  Service │ │  Service │
└──────────┘ └──────────┘
       ├─────────┤
       ▼         ▼
┌──────────┐ ┌──────────────┐
│  Session │ │ Notification │
│  Service │ │  Service     │
└──────────┘ └──────────────┘
       │
       ▼
┌──────────┐
│  Skill   │
│  Service │
└──────────┘
```

All services report spans to Zipkin with the same `traceId`, enabling full request tracing.

---

## 8.9 Trade-offs & Design Decisions

| Decision | Trade-off | Mitigation |
|----------|-----------|------------|
| 100% sampling rate | More data sent to Zipkin | Reduce to 10-50% in production |
| In-memory Zipkin storage | Traces lost on restart | Use Elasticsearch backend for production |
| Structured logging (pattern-based) | Not JSON format | Can add `logstash-logback-encoder` for full JSON |
| Prometheus pull model | Requires Prometheus server | Simple to add; works with any Prometheus-compatible tool |

---

## 8.10 Future Enhancements

1. **Grafana Dashboard** — Visualize Prometheus metrics with Grafana
2. **ELK Stack** — Centralized log aggregation (Elasticsearch + Logstash + Kibana)
3. **JSON Structured Logging** — Add `logstash-logback-encoder` for machine-parseable logs
4. **Alert Rules** — Prometheus alerting for error rate spikes, high latency, Redis failures
5. **Elasticsearch for Zipkin** — Persistent trace storage for production
6. **Custom RabbitMQ Metrics** — Track event processing time and queue depth

---

> [!NOTE]
> This document is the authoritative reference for all observability and monitoring architecture decisions in SkillSync.
> All services follow the patterns described here. Any deviations must be documented with rationale.
