# Document 9: EC2 Gateway and NGINX Incident Fix Runbook

## Incident Summary

Observed production symptoms on AWS EC2:
- Containers running, but gateway remained in health state starting.
- NGINX marked unhealthy.
- Swagger and monitoring UIs reachable, but auth data flow (register and login) not reliably working end-to-end.

## Root Causes Identified

1. Healthcheck fragility in minimal containers
- Gateway and NGINX healthchecks depended on single binaries only.
- In slim images, utility availability can differ by tag, causing false negative health states even when the process is up.
- This blocked readiness gating and made ingress behavior inconsistent.

2. DNS naming inconsistency across ingress and observability
- NGINX upstream and Prometheus targets were not consistently aligned to a single naming convention.
- Mixed service-name and container-name usage increased the risk of DNS lookup mismatch in containerized runtime.

3. Missing explicit network aliases for critical components
- While Docker Compose often resolves service names automatically, explicit aliases were missing.
- This made behavior depend on implicit DNS behavior and deployment mode details.

## Fixes Applied

### 1. NGINX upstream alignment

Updated NGINX upstream target to gateway container DNS name.

File changed:
- Backend/nginx/nginx.conf

Change:
- upstream api_gateway now points to skillsync-gateway:8080

### 2. Docker Compose healthcheck hardening

Updated gateway and NGINX healthchecks to use fallback probes instead of one utility only.

File changed:
- Backend/docker-compose.yml

Change:
- Gateway healthcheck now tries readiness endpoint via wget, then curl, then TCP probe.
- NGINX healthcheck now tries local health endpoint via wget, then curl, then TCP probe.
- Gateway start period increased to 140s to avoid early flapping during dependent startup.

### 3. Explicit network aliases for deterministic DNS

Added explicit aliases on skillsync-net for key services.

File changed:
- Backend/docker-compose.yml

Aliases added for:
- eureka-server and skillsync-eureka
- config-server and skillsync-config
- api-gateway and skillsync-gateway
- auth-service and skillsync-auth
- user-service and skillsync-user
- skill-service and skillsync-skill
- session-service and skillsync-session
- notification-service and skillsync-notification
- payment-service and skillsync-payment
- skillsync-nginx

### 4. Prometheus target alignment

Standardized scrape targets to explicit container DNS names.

File changed:
- Backend/monitoring/prometheus/prometheus.yml

Updated targets:
- skillsync-gateway:8080
- skillsync-auth:8081
- skillsync-user:8082
- skillsync-skill:8084
- skillsync-session:8085
- skillsync-notification:8088
- skillsync-payment:8086
- skillsync-eureka:8761
- skillsync-config:8888

## Mandatory Debug Commands Used

Run these on EC2 from the Backend directory.

```bash
docker compose ps
docker logs skillsync-gateway --tail 300
docker logs skillsync-nginx --tail 300
curl -sS http://localhost:8761 | head
curl -sS http://localhost:8080/actuator/health
curl -sS http://localhost/actuator/health
```

Deep network and route checks:

```bash
docker exec -it skillsync-gateway sh -lc "wget -qO- http://localhost:8080/actuator/health || true"
docker exec -it skillsync-nginx sh -lc "nginx -t"
docker exec -it skillsync-nginx sh -lc "wget -qO- http://skillsync-gateway:8080/actuator/health || true"
docker exec -it skillsync-gateway sh -lc "wget -qO- http://skillsync-auth:8081/actuator/health || true"
```

Eureka registration checks:

```bash
curl -sS http://localhost:8761/eureka/apps | grep -E "AUTH-SERVICE|USER-SERVICE|SESSION-SERVICE|SKILL-SERVICE|PAYMENT-SERVICE|NOTIFICATION-SERVICE|API-GATEWAY|CONFIG-SERVER"
```

Auth flow checks via ingress:

```bash
curl -i -X POST http://localhost/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"EC2 Test","email":"ec2test@example.com","password":"P@ssword123"}'

curl -i -X POST http://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ec2test@example.com","password":"P@ssword123"}'
```

Prometheus target checks:

```bash
curl -sS http://localhost:9090/api/v1/targets | grep -E "skillsync-(gateway|auth|user|skill|session|payment|notification|config|eureka)"
```

## Controlled Redeploy Procedure

Do not restart blindly. Redeploy with these exact steps after config changes:

```bash
docker compose config > /tmp/skillsync.compose.resolved.yml
docker compose up -d --force-recreate api-gateway nginx prometheus
docker compose ps
```

If service images are rebuilt in CI:

```bash
docker compose pull
docker compose up -d
```

## Final Validation Checklist

1. docker compose ps shows gateway and nginx as healthy.
2. Eureka dashboard shows all required services as UP.
3. Login and register requests return success via NGINX endpoint.
4. Gateway Swagger remains accessible and functional.
5. Prometheus targets are UP for gateway and business services.
6. Grafana dashboards show live metrics and no empty panels for core services.

## Architecture Notes After Fix

Ingress path:
- Client -> NGINX (80) -> skillsync-gateway (8080) -> service discovery -> business services -> Postgres

Discovery and config path:
- Services -> skillsync-eureka (8761)
- Services and gateway -> skillsync-config (8888)

Observability path:
- Prometheus -> service /actuator/prometheus endpoints
- Grafana -> Prometheus datasource
- Zipkin receives traces from services and gateway
