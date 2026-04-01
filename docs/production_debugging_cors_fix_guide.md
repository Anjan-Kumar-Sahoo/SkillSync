# Production Debugging & CORS Fix Guide

Date: 2026-04-01

This guide captures the verified production failures, root causes, exact config fixes, and validation commands for SkillSync.

## 1. Incident Symptoms

- Swagger UI loaded but API execution failed with `Failed to fetch` and `Undocumented`.
- Frontend login/register/OAuth requests failed in production.
- Public domain API checks returned Vercel `404 NOT_FOUND`.

## 2. Root Causes (Verified)

### Root Cause A: Public domain traffic was not reaching EC2 ingress

Evidence:

```bash
curl -i https://skillsync.mraks.dev/auth/actuator/health
```

Observed: `Server: Vercel`, `X-Vercel-Error: NOT_FOUND`.

Impact:

- Browser requests from frontend to `/api/*` never reached EC2 NGINX/Gateway.
- Swagger paths on production domain also resolved to Vercel instead of backend.

### Root Cause B: Swagger service docs used internal Docker server URLs

Evidence:

```bash
curl -s http://35.153.59.2/service-docs/auth-service/v3/api-docs
```

Observed in JSON:

```json
"servers": [{ "url": "http://172.18.0.17:8081", "description": "Generated server url" }]
```

Impact:

- Swagger "Try it out" attempted internal container IPs unreachable from browser.
- Produced `Failed to fetch` behavior even when docs loaded.

### Root Cause C: Gateway lacked compatibility routes required by production checks

Expected checks required `/auth/**`, `/user/**`, `/session/**`, `/oauth2/**`, `/login/**`.

Before fix, gateway only exposed `/api/auth/**`, `/api/users/**`, `/api/sessions/**`, etc.

Impact:

- `curl .../auth/actuator/health` could not be routed correctly.
- Legacy or compatibility clients using non-`/api` prefixes failed.

## 3. Fixes Applied

## 3.1 NGINX (Ingress)

File: `Backend/nginx/nginx.conf`

Changes:

- All gateway upstream proxy targets now use `http://skillsync-gateway:8080` directly.
- Added explicit ingress paths for:
  - `/auth/`
  - `/user/`
  - `/session/`
  - `/oauth2/`
  - `/login/`
- Kept catch-all and existing Swagger routes proxied to gateway.

## 3.2 API Gateway Routes + Global CORS

File: `Backend/api-gateway/src/main/resources/application.properties`

Changes:

- Added compatibility routes:
  - `/auth/actuator/**` -> auth-service `/actuator/**`
  - `/auth/**` -> auth-service `/api/auth/**`
  - `/user/**` -> user-service `/api/users/**`
  - `/session/**` -> session-service `/api/sessions/**`
  - `/oauth2/**` -> auth-service passthrough
  - `/login/**` -> auth-service passthrough
- Added Spring Cloud Gateway global CORS:
  - Allowed origins include `https://skillsync.mraks.dev`
  - Methods: `GET, POST, PUT, DELETE, OPTIONS, PATCH`
  - Headers: `*`
  - Credentials: `true`
- Added `server.forward-headers-strategy=framework` for reverse-proxy-aware URL generation.

## 3.3 Gateway CORS Bean Defaults

File: `Backend/api-gateway/src/main/java/com/skillsync/apigateway/config/CorsConfig.java`

Changes:

- Default allowed origins now include `https://skillsync.mraks.dev`.
- Trimmed origin entries from env to avoid whitespace mismatch.
- Kept localhost wildcard patterns for local/dev compatibility.

## 3.4 Swagger/OpenAPI Public Server URL Fix

Files updated:

- `Backend/auth-service/src/main/java/com/skillsync/auth/config/OpenApiConfig.java`
- `Backend/user-service/src/main/java/com/skillsync/user/config/OpenApiConfig.java`
- `Backend/skill-service/src/main/java/com/skillsync/skill/config/OpenApiConfig.java`
- `Backend/session-service/src/main/java/com/skillsync/session/config/OpenApiConfig.java`
- `Backend/notification-service/src/main/java/com/skillsync/notification/config/OpenApiConfig.java`
- `Backend/payment-service/src/main/java/com/skillsync/payment/config/OpenApiConfig.java`

Changes:

- Added explicit relative OpenAPI server:

```java
new Server().url("/")
```

Impact:

- Swagger `Try it out` now targets the same host origin that served Swagger UI (EC2 IP or proxied host), instead of private Docker IPs.

## 3.5 Frontend Production Routing & HTTPS Consistency

Files:

- `Frontend/vercel.json` (new)
- `Frontend/src/pages/LandingPage.tsx`
- `Frontend/vite.config.ts`

Changes:

- Added Vercel rewrites to proxy backend paths to EC2 NGINX:
  - `/api/*`, `/auth/*`, `/user/*`, `/session/*`, `/oauth2/*`, `/login/*`
  - `/swagger-ui*`, `/v3/api-docs*`, `/service-docs*`, `/actuator*`
- Removed hardcoded HTTP EC2 fallback from frontend display defaults.
- Swagger quick link updated to same-origin `/swagger-ui.html`.
- Dev server fallback remains local (`http://localhost:8080`) for local runs.

## 3.6 Environment Variables

Set in backend runtime `.env` on EC2:

```bash
ALLOWED_ORIGINS=https://skillsync.mraks.dev,https://skill-sync-sage.vercel.app,http://localhost:3000,http://localhost:5173
APP_BASE_URL=https://skillsync.mraks.dev
```

## 4. OAuth Production Checklist

Current frontend OAuth implementation uses Google token flow (`@react-oauth/google` + `userinfo`) and backend endpoint `/api/auth/oauth-login`.

Google Cloud Console settings:

1. Authorized JavaScript origins:
   - `https://skillsync.mraks.dev`
2. If Authorization Code flow is enabled later, add redirect URI exactly:
   - `https://skillsync.mraks.dev/auth/oauth2/code/google`

Important: keep backend and Google Console path strings exactly identical.

## 5. Validation Commands

Run after redeploying gateway/nginx/services.

### 5.1 Gateway health

```bash
curl -i http://localhost:8080/actuator/health
```

Expected: `200` with `{"status":"UP"}`.

### 5.2 Public API through domain

```bash
curl -i https://skillsync.mraks.dev/auth/actuator/health
curl -i https://skillsync.mraks.dev/api/auth/login
```

Expected:

- First command returns `200` health.
- Second command reaches auth-service route (status depends on method/body but must not be Vercel `404 NOT_FOUND`).

### 5.3 CORS preflight

```bash
curl -i -X OPTIONS https://skillsync.mraks.dev/api/auth/login \
  -H "Origin: https://skillsync.mraks.dev" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: content-type,authorization"
```

Expected headers include:

- `Access-Control-Allow-Origin: https://skillsync.mraks.dev`
- `Access-Control-Allow-Credentials: true`
- `Access-Control-Allow-Methods` includes `POST`

### 5.4 Swagger

Open:

- `https://skillsync.mraks.dev/swagger-ui.html`

Validate:

- Service docs load.
- "Try it out" calls use public domain (not `172.x.x.x`).
- No `Failed to fetch` for valid test calls.

### 5.5 Eureka registration

```bash
curl -s http://localhost:8761/eureka/apps | grep -E "API-GATEWAY|AUTH-SERVICE|USER-SERVICE|SESSION-SERVICE|SKILL-SERVICE|PAYMENT-SERVICE|NOTIFICATION-SERVICE|CONFIG-SERVER"
```

Expected: all required services present.

### 5.6 Internal service connectivity from gateway container

```bash
docker exec -it skillsync-gateway sh -lc "wget -qO- http://skillsync-auth:8081/actuator/health || true"
```

Expected: `{"status":"UP"}`.

## 6. Controlled Redeploy Sequence

Apply changes in this order:

1. Gateway
2. Auth/User/Session/Skill/Notification/Payment (relative OpenAPI server URL)
3. NGINX
4. Frontend (Vercel)

Commands:

```bash
cd /path/to/SkillSync/Backend
docker compose up -d --build api-gateway auth-service user-service skill-service session-service notification-service payment-service nginx
```

If images are from registry:

```bash
docker compose pull
docker compose up -d
```

## 7. Before vs After Summary

Before:

- Domain API checks returned Vercel `404 NOT_FOUND`.
- Swagger docs pointed to private Docker IP server URLs.
- Compatibility routes (`/auth`, `/user`, `/session`) were absent.

After:

- Domain routes are proxied to EC2 backend via Vercel rewrites.
- Gateway and NGINX support required path contracts.
- Swagger server URLs are pinned to public domain.
- CORS is explicitly configured in gateway global CORS and filter defaults.
- OAuth origin contract is documented for production.
