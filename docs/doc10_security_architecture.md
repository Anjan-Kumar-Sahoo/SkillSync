# Document 10: Security Architecture

> Version: 1.0 | Date: 2026-04-01 | Status: Post-Audit

## 1. Security Boundaries

```
┌─────────────────────────────────────────────────────────────┐
│                      INTERNET                                │
│                                                              │
│  ┌──────────────┐            ┌──────────────────────────┐   │
│  │   Frontend    │            │    Cloudflare Proxy       │   │
│  │   (Vercel)    │───HTTPS───▶│  (SSL termination +      │   │
│  │   SPA React   │            │   DDoS protection)       │   │
│  └──────────────┘            └───────────┬──────────────┘   │
│                                          │ HTTP/HTTPS        │
│                              ┌───────────▼──────────────┐   │
│                              │    EC2 Host               │   │
│                              │  ┌────────────────────┐  │   │
│                              │  │  API Gateway (:80)  │  │   │
│                              │  │  (Spring Cloud)     │  │   │
│                              │  └────────┬───────────┘  │   │
│                              │           │ Docker net    │   │
│                              │  ┌────────▼───────────┐  │   │
│                              │  │   Microservices     │  │   │
│                              │  │   (internal only)   │  │   │
│                              │  └────────────────────┘  │   │
│                              └──────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 2. Authentication Flow

### 2.1 Standard Login (Email + Password)

```
Frontend                    Gateway                   Auth Service
   │                           │                          │
   │ POST /api/auth/login      │                          │
   │ {email, password}         │                          │
   │──────────────────────────▶│──────────────────────────▶│
   │                           │                          │ Authenticate
   │                           │                          │ Generate JWT
   │                           │                          │ Store RefreshToken in DB
   │                           │     AuthResponse +       │
   │  Set-Cookie: accessToken  │◀─── Set-Cookie headers ──│
   │  Set-Cookie: refreshToken │                          │
   │◀──────────────────────────│                          │
   │                           │                          │
   │ Store tokens in Redux +   │                          │
   │ localStorage (current)    │                          │
```

### 2.2 Cookie Attributes (Production)

| Attribute   | Value         | Rationale |
|-------------|---------------|-----------|
| `HttpOnly`  | `true`        | Prevents JS access (XSS mitigation) |
| `Secure`    | `true`        | Only sent over HTTPS |
| `SameSite`  | `None`        | Required for cross-subdomain cookies (api.mraks.dev → skillsync.mraks.dev) |
| `Domain`    | `.mraks.dev`  | Shared across all subdomains |
| `Path`      | `/`           | Available to all routes |
| `Max-Age`   | `900` / `604800` | 15 min (access) / 7 days (refresh) |

### 2.3 OAuth Login (Google)

```
Frontend                    Gateway                   Auth Service
   │                           │                          │
   │ Google OAuth flow         │                          │
   │ (Google handles auth)     │                          │
   │                           │                          │
   │ POST /api/auth/oauth-login│                          │
   │ {provider, providerId,    │                          │
   │  email, firstName, ...}   │                          │
   │──────────────────────────▶│──────────────────────────▶│
   │                           │                          │ Find or create user
   │                           │                          │ Generate JWT
   │                           │     OAuthResponse +      │
   │  Set-Cookie: accessToken  │◀─── Set-Cookie headers ──│
   │  Set-Cookie: refreshToken │                          │
   │◀──────────────────────────│                          │
   │                           │                          │
   │ If new user:              │                          │
   │ Redirect to /setup-password│                         │
```

### 2.4 Token Refresh (Silent)

```
Frontend (axios interceptor)    Gateway              Auth Service
   │                               │                      │
   │ Receives 401                  │                      │
   │                               │                      │
   │ POST /api/auth/refresh        │                      │
   │ {refreshToken}                │                      │
   │──────────────────────────────▶│─────────────────────▶│
   │                               │                      │ Validate
   │                               │                      │ Delete old token
   │                               │                      │ Generate new pair
   │                               │     New tokens       │
   │◀──────────────────────────────│◀─────────────────────│
   │                               │                      │
   │ Retry original request        │                      │
   │ with new token                │                      │
```

## 3. Authorization Model

### 3.1 Gateway-Level (Route Protection)

| Route Pattern | JWT Required | Filter |
|---------------|-------------|--------|
| `/api/auth/**` | ❌ No | None (auth service handles internally) |
| `/api/users/**` | ✅ Yes | `JwtAuthenticationFilter` |
| `/api/mentors/**` | ✅ Yes | `JwtAuthenticationFilter` |
| `/api/sessions/**` | ✅ Yes | `JwtAuthenticationFilter` |
| `/api/notifications/**` | ✅ Yes | `JwtAuthenticationFilter` |
| `/api/payments/**` | ✅ Yes | `JwtAuthenticationFilter` |
| `/api/skills/**` | ❌ No | None (public browsing) |
| `/ws/**` | ❌ No | WebSocket (separate auth) |
| `/health` | ❌ No | Public health check |
| `/actuator/**` | ❌ No | Public (restrict in production) |

### 3.2 Service-Level (X-User-Id Trust Model)

The gateway extracts JWT claims and forwards as headers:
- `X-User-Id` — user's database ID
- `X-User-Email` — user's email
- `X-User-Role` — user's role (ROLE_LEARNER, ROLE_MENTOR, ROLE_ADMIN)

Downstream services **trust these headers implicitly**. This is secure ONLY because:
1. Services are not exposed outside Docker network
2. Gateway is the sole ingress point
3. Gateway strips and rewrites these headers from JWT

> ⚠️ **KNOWN RISK**: Gateway does NOT currently strip incoming X-User-Id headers before processing. An attacker could add this header to bypass auth IF a route lacks the JWT filter.

## 4. Secrets Management

| Secret | Storage | Rotation Policy |
|--------|---------|-----------------|
| JWT Secret | `.env` on EC2 | Manual (NO rotation currently) |
| DB Password | `.env` on EC2 | Manual |
| RabbitMQ Password | `.env` on EC2 | Manual |
| Gmail App Password | `.env` on EC2 | Manual |
| Docker Hub Credentials | GitHub Secrets | Manual |
| EC2 SSH Key | GitHub Secrets | Manual |

> ⚠️ **RECOMMENDATION**: Migrate to AWS Secrets Manager for automated rotation and audit logging.

## 5. Rate Limiting

| Category | Limit | Scope |
|----------|-------|-------|
| OTP / Forgot Password | 5 req/min | Per IP |
| Login / OAuth | 10 req/min | Per IP |
| Payment Create/Verify | 10 req/min | Per User |
| Actuator | 60 req/min | Per IP |
| Authenticated APIs | 100 req/min | Per User |

Implementation: In-memory `ConcurrentHashMap` sliding window.

> ⚠️ **LIMITATION**: Single-instance only. Requires Redis backend for multi-instance deployments.

## 6. Known Security Gaps (Post-Audit)

| # | Gap | Status | Priority |
|---|-----|--------|----------|
| 1 | `updateUserRole` lacks RBAC | ❌ Open | P0 |
| 2 | CSRF protection missing | ❌ Open | P0 |
| 3 | JWT secret is weak/predictable | ❌ Open | P0 |
| 4 | `setupPassword` unauthenticated | ❌ Open | P0 |
| 5 | No security response headers | ❌ Open | P1 |
| 6 | Tokens in localStorage | ❌ Open | P2 |
| 7 | Internal routes exposed via gateway | ❌ Open | P1 |
| 8 | Debug ports exposed on EC2 | ❌ Open | P3 |

See `docs/skillsync_production_audit_report.md` for detailed fixes.

## 7. Deployment Security

### 7.1 CI/CD Security
- SSH key stored in GitHub Secrets (encrypted)
- Docker Hub credentials in GitHub Secrets
- SonarCloud token in GitHub Secrets
- `.env.example` committed (no secrets), `.env` excluded

### 7.2 EC2 Security Groups (Recommended)

| Port | Protocol | Source | Purpose |
|------|----------|--------|---------|
| 22 | TCP | Your IP only | SSH |
| 80 | TCP | Cloudflare IPs | HTTP redirect |
| 443 | TCP | Cloudflare IPs | HTTPS |
| All others | — | Deny | Block direct access |
