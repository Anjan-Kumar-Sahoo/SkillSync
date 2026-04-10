# Presentation Sync Note

Updated for final presentation on 2026-04-06. Start with docs/00_Presentation_Playbook.md for the guided narrative, then use this document for deep details.

---

# 04 Security Auth and OAuth



---

## Content from: doc10_security_architecture.md

# Document 10: Security Architecture

> Version: 1.0 | Date: 2026-04-01 | Status: Post-Audit

## 1. Security Boundaries

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                      INTERNET                                â”‚
â”‚                                                              â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”            â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”   â”‚
â”‚  â”‚   Frontend    â”‚            â”‚    Cloudflare Proxy       â”‚   â”‚
â”‚  â”‚   (Vercel)    â”‚â”€â”€â”€HTTPSâ”€â”€â”€â–¶â”‚  (SSL termination +      â”‚   â”‚
â”‚  â”‚   SPA React   â”‚            â”‚   DDoS protection)       â”‚   â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜            â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜   â”‚
â”‚                                          â”‚ HTTP/HTTPS        â”‚
â”‚                              â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”   â”‚
â”‚                              â”‚    EC2 Host               â”‚   â”‚
â”‚                              â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”‚   â”‚
â”‚                              â”‚  â”‚  API Gateway (:80)  â”‚  â”‚   â”‚
â”‚                              â”‚  â”‚  (Spring Cloud)     â”‚  â”‚   â”‚
â”‚                              â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â”‚   â”‚
â”‚                              â”‚           â”‚ Docker net    â”‚   â”‚
â”‚                              â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”‚   â”‚
â”‚                              â”‚  â”‚   Microservices     â”‚  â”‚   â”‚
â”‚                              â”‚  â”‚   (internal only)   â”‚  â”‚   â”‚
â”‚                              â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â”‚   â”‚
â”‚                              â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜   â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

## 2. Authentication Flow

### 2.1 Standard Login (Email + Password)

```
Frontend                    Gateway                   Auth Service
   â”‚                           â”‚                          â”‚
   â”‚ POST /api/auth/login      â”‚                          â”‚
   â”‚ {email, password}         â”‚                          â”‚
   â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶â”‚
   â”‚                           â”‚                          â”‚ Authenticate
   â”‚                           â”‚                          â”‚ Generate JWT
   â”‚                           â”‚                          â”‚ Store RefreshToken in DB
   â”‚                           â”‚     AuthResponse +       â”‚
   â”‚  Set-Cookie: accessToken  â”‚â—€â”€â”€â”€ Set-Cookie headers â”€â”€â”‚
   â”‚  Set-Cookie: refreshToken â”‚                          â”‚
   â”‚â—€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”‚                          â”‚
   â”‚                           â”‚                          â”‚
   â”‚ Store tokens in Redux +   â”‚                          â”‚
   â”‚ localStorage (current)    â”‚                          â”‚
```

### 2.2 Cookie Attributes (Production)

| Attribute   | Value         | Rationale |
|-------------|---------------|-----------|
| `HttpOnly`  | `true`        | Prevents JS access (XSS mitigation) |
| `Secure`    | `true`        | Only sent over HTTPS |
| `SameSite`  | `None`        | Required for cross-subdomain cookies (api.mraks.dev â†’ skillsync.mraks.dev) |
| `Domain`    | `.mraks.dev`  | Shared across all subdomains |
| `Path`      | `/`           | Available to all routes |
| `Max-Age`   | `900` / `604800` | 15 min (access) / 7 days (refresh) |

### 2.3 OAuth Login (Google)

```
Frontend                    Gateway                   Auth Service
   â”‚                           â”‚                          â”‚
   â”‚ Google OAuth flow         â”‚                          â”‚
   â”‚ (Google handles auth)     â”‚                          â”‚
   â”‚                           â”‚                          â”‚
   â”‚ POST /api/auth/oauth-loginâ”‚                          â”‚
   â”‚ {provider, providerId,    â”‚                          â”‚
   â”‚  email, firstName, ...}   â”‚                          â”‚
   â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶â”‚
   â”‚                           â”‚                          â”‚ Find or create user
   â”‚                           â”‚                          â”‚ Generate JWT
   â”‚                           â”‚     OAuthResponse +      â”‚
   â”‚  Set-Cookie: accessToken  â”‚â—€â”€â”€â”€ Set-Cookie headers â”€â”€â”‚
   â”‚  Set-Cookie: refreshToken â”‚                          â”‚
   â”‚â—€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”‚                          â”‚
   â”‚                           â”‚                          â”‚
   â”‚ If new user:              â”‚                          â”‚
   â”‚ Redirect to /setup-passwordâ”‚                         â”‚
```

### 2.4 Token Refresh (Silent)

```
Frontend (axios interceptor)    Gateway              Auth Service
   â”‚                               â”‚                      â”‚
   â”‚ Receives 401                  â”‚                      â”‚
   â”‚                               â”‚                      â”‚
   â”‚ POST /api/auth/refresh        â”‚                      â”‚
   â”‚ {refreshToken}                â”‚                      â”‚
   â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶â”‚
   â”‚                               â”‚                      â”‚ Validate
   â”‚                               â”‚                      â”‚ Delete old token
   â”‚                               â”‚                      â”‚ Generate new pair
   â”‚                               â”‚     New tokens       â”‚
   â”‚â—€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”‚â—€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”‚
   â”‚                               â”‚                      â”‚
   â”‚ Retry original request        â”‚                      â”‚
   â”‚ with new token                â”‚                      â”‚
```

## 3. Authorization Model

### 3.1 Gateway-Level (Route Protection)

| Route Pattern | JWT Required | Filter |
|---------------|-------------|--------|
| `/api/auth/**` | âŒ No | None (auth service handles internally) |
| `/api/users/**` | âœ… Yes | `JwtAuthenticationFilter` |
| `/api/mentors/**` | âœ… Yes | `JwtAuthenticationFilter` |
| `/api/sessions/**` | âœ… Yes | `JwtAuthenticationFilter` |
| `/api/notifications/**` | âœ… Yes | `JwtAuthenticationFilter` |
| `/api/payments/**` | âœ… Yes | `JwtAuthenticationFilter` |
| `/api/skills/**` | âŒ No | None (public browsing) |
| `/ws/**` | âŒ No | WebSocket (separate auth) |
| `/health` | âŒ No | Public health check |
| `/actuator/**` | âŒ No | Public (restrict in production) |

### 3.2 Service-Level (X-User-Id Trust Model)

The gateway extracts JWT claims and forwards as headers:
- `X-User-Id` â€” user's database ID
- `X-User-Email` â€” user's email
- `X-User-Role` â€” user's role (ROLE_LEARNER, ROLE_MENTOR, ROLE_ADMIN)

Downstream services **trust these headers implicitly**. This is secure ONLY because:
1. Services are not exposed outside Docker network
2. Gateway is the sole ingress point
3. Gateway strips and rewrites these headers from JWT

> âš ï¸ **KNOWN RISK**: Gateway does NOT currently strip incoming X-User-Id headers before processing. An attacker could add this header to bypass auth IF a route lacks the JWT filter.

## 4. Secrets Management

| Secret | Storage | Rotation Policy |
|--------|---------|-----------------|
| JWT Secret | `.env` on EC2 | Manual (NO rotation currently) |
| DB Password | `.env` on EC2 | Manual |
| RabbitMQ Password | `.env` on EC2 | Manual |
| Gmail App Password | `.env` on EC2 | Manual |
| Docker Hub Credentials | GitHub Secrets | Manual |
| EC2 SSH Key | GitHub Secrets | Manual |

> âš ï¸ **RECOMMENDATION**: Migrate to AWS Secrets Manager for automated rotation and audit logging.

## 5. Rate Limiting

| Category | Limit | Scope |
|----------|-------|-------|
| OTP / Forgot Password | 5 req/min | Per IP |
| Login / OAuth | 10 req/min | Per IP |
| Payment Create/Verify | 10 req/min | Per User |
| Actuator | 60 req/min | Per IP |
| Authenticated APIs | 100 req/min | Per User |

Implementation: In-memory `ConcurrentHashMap` sliding window.

> âš ï¸ **LIMITATION**: Single-instance only. Requires Redis backend for multi-instance deployments.

## 6. Known Security Gaps (Post-Audit)

| # | Gap | Status | Priority |
|---|-----|--------|----------|
| 1 | `updateUserRole` lacks RBAC | âŒ Open | P0 |
| 2 | CSRF protection missing | âŒ Open | P0 |
| 3 | JWT secret is weak/predictable | âŒ Open | P0 |
| 4 | `setupPassword` unauthenticated | âŒ Open | P0 |
| 5 | No security response headers | âŒ Open | P1 |
| 6 | Tokens in localStorage | âŒ Open | P2 |
| 7 | Internal routes exposed via gateway | âŒ Open | P1 |
| 8 | Debug ports exposed on EC2 | âŒ Open | P3 |

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
| All others | â€” | Deny | Block direct access |


---

## Content from: doc7_auth_oauth_email_system.md

# ðŸ” SkillSync Auth & Notification System (V3)

This document outlines the architecture and implementation details for the 
**Registration Rollback**, **Password Reset with OTP**, **OAuth Integration**, 
the **Asynchronous Email Pipeline**, and the **Email Retry Mechanism**.

---

## ðŸ”‘ 1. Authentication Upgrades & Rate Limiting

### 1.0 API Gateway Rate Limiting
To protect against brute force attacks, OTP spamming, and credential stuffing, an API Gateway `RateLimitingFilter` enforces tiered limits across auth endpoints (**before** JWT validation):
- **OTP endpoints** (`/api/auth/otp`, `/verify-otp`, `/forgot-password`): 5 requests / min
- **Auth endpoints** (`/api/auth/login`, `/oauth-login`, `/setup-password`): 10 requests / min
- **General APIs**: 100 requests / min (bucketed by `X-User-Id` if authenticated, IP otherwise)

### 1.1 Registration Rollback (Security Enhancement)
To prevent "Ghost Users" (unverified accounts cluttering the DB), SkillSync now implements a strict rollback policy.

- **Trigger**: If OTP verification for the `REGISTRATION` flow fails due to:
  - Max attempts (5) reached.
  - OTP expiry (5 minutes).
- **Action**: 
  - Delete partial user record from `auth.users`.
  - Delete related OTP records.
- **Goal**: Ensure the system remains clean and the user can start a fresh registration immediately.

### 1.2 Password Reset with OTP
Users can now securely reset forgotten passwords via a multi-step flow.

1.  **Request**: `POST /api/auth/forgot-password`
    - Generates a `PASSWORD_RESET` type OTP.
    - Sends an email to the user.
2.  **Verify/Apply**: `POST /api/auth/reset-password`
  - **Forgot-password mode** payload: `email`, `otp`, `newPassword`, `confirmPassword`.
  - Validates OTP attempts and expiry.
  - Hashes the new password using BCrypt.
  - **Security**: Invalidates all active refresh tokens for the user in `auth.refresh_tokens`.
  - **Cache**: Evicts the `user:profile:<userId>` cache in Redis to ensure consistency across the system.

### 1.2.1 Authenticated Password Change (Same Endpoint)

SkillSync also supports in-app password updates without creating a separate API.

1.  **Authenticated request**: `POST /api/auth/reset-password`
  - Payload: `currentPassword`, `newPassword`, `confirmPassword`.
  - Auth context resolved from JWT (`Authorization` header or `accessToken` cookie).
2.  **Validation**
  - Current password must match stored hash.
  - New password must differ from current password.
  - `newPassword` and `confirmPassword` must match.
3.  **Post-update hardening**
  - Refresh tokens are invalidated.
  - Profile cache is evicted.

### 1.3 OAuth + JWT Hybrid Flow (CORRECTED V3)

SkillSync supports **Google OAuth** login with different behavior for new vs existing users.

> [!IMPORTANT]
> The OAuth flow has been corrected in V3 to ensure proper UX:
> - **New users MUST set a password** before full access
> - **Existing users get direct login** with no password prompt

#### 1.3.1 OAuth Registration (New User)

When a user logs in via OAuth and does NOT have an existing account:

```text
Frontend â†’ Google OAuth â†’ sends profile to POST /api/auth/oauth-login
  â†“
Backend: User not found by provider+providerId nor by email
  â†“
Create user:
  - provider = "google"
  - providerId = "google-id-123"  
  - isVerified = true (verified by OAuth provider)
  - passwordSet = false (MUST set password)
  â†“
Response: OAuthResponse { passwordSetupRequired: true }
  â†“
Frontend: REDIRECT to Password Setup Screen
  â†“
POST /api/auth/setup-password { email, password }
  â†“
User can now login with email + password
```

#### 1.3.2 OAuth Login (Existing Verified User)

When a user already exists AND is verified:

```text
Frontend â†’ Google OAuth â†’ sends profile to POST /api/auth/oauth-login
  â†“
Backend: User FOUND by provider+providerId or by email, and isVerified=true
  â†“
Response: OAuthResponse { passwordSetupRequired: false }
  â†“
Direct login â€” NO password prompt, NO forced reset
```

#### 1.3.3 Edge Cases

| Scenario | Behavior |
|----------|----------|
| User exists but NOT verified | OAuth login BLOCKED. Error: "Please verify your email first" |
| OAuth email matches existing local account | Link OAuth provider to existing account (if verified) |
| User already has password set | `passwordSetupRequired: false` |
| Duplicate OAuth call for same user | Idempotent â€” returns existing user |

#### 1.3.4 User Lookup Priority

1. `findByProviderAndProviderId(provider, providerId)` â€” exact OAuth match
2. `findByEmail(email)` â€” fallback to email match
3. If neither found â†’ create new user

#### 1.3.5 New Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/auth/oauth-login` | POST | OAuth login (returns `OAuthResponse`) |
| `/api/auth/setup-password` | POST | Set password for OAuth users |

#### 1.3.6 New DTOs

**OAuthResponse** â€” extends standard auth response:
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "abc...",
  "expiresIn": 900,
  "tokenType": "Bearer",
  "user": { "id": 10, "email": "user@gmail.com", ... },
  "passwordSetupRequired": true
}
```

**SetupPasswordRequest**:
```json
{
  "email": "user@gmail.com",
  "password": "mySecurePassword123"
}
```

---

## ðŸ“§ 2. Asynchronous Email Pipeline

The **Notification Service** uses a hybrid **WebSocket + Email** architecture.

### 2.1 Architecture
The system follows an event-driven pattern via RabbitMQ:
```text
Event (Session/Payment/Mentor) 
  â†’ Multi-Queue Consumer 
  â†’ DB Save 
  â†’ WebSocket Push (Real-time UI)
  â†’ Email Service (Async Delivery)
```

### 2.2 Template System
We use **Thymeleaf HTML templates** for rich, premium email experiences.

- **Templates Location**: `src/main/resources/templates/emails/`
- **Dynamic Content**: Uses placeholders for `recipientName`, `sessionTitle`, `amount`, etc.
- **Premium Design**: SkillSync branded, responsive, and includes clear Action Buttons.

### 2.3 Reliability & Failure Handling

- **Non-blocking**: Emails are sent asynchronously (`@Async`) to ensure RabbitMQ message acknowledgement is fast.
- **Fault Tolerance**: Wrapped in try-catch blocks to prevent email failures from crashing the notification pipeline.
- **Structured Logging**: Every email attempt logs:
  - `to` (recipient)
  - `emailType` (template name)
  - `error` (failure reason, if any)

Example log output:
```
[EMAIL] Failed to send | to=user@test.com | emailType=session-booked | error=Connection refused
```

### 2.4 Email Retry Mechanism (NEW V3)

> [!IMPORTANT]
> Failed emails are no longer silently lost. The retry system ensures delivery with exponential backoff.

#### Architecture

```text
EmailService.sendEmail() fails
  â†“
Publish EmailRetryEvent to email.retry.queue
  â†“
EmailRetryConsumer picks up event
  â†“
Attempt resend with exponential backoff
  â†“
On success: done
On failure (retryCount < 3): re-publish with retryCount++
On failure (retryCount >= 3): log PERMANENT FAILURE â†’ message goes to email.dlq
```

#### Retry Policy

| Parameter | Value |
|-----------|-------|
| Max retries | 3 |
| Base delay | 2 seconds |
| Backoff strategy | Exponential (2s, 4s, 8s) |
| Dead-letter queue | `email.dlq` |

#### RabbitMQ Infrastructure

| Component | Name |
|-----------|------|
| Exchange | `email.retry.exchange` (direct) |
| Queue | `email.retry.queue` |
| Dead-letter queue | `email.dlq` |

#### EmailRetryEvent

```json
{
  "to": "user@test.com",
  "subject": "Session Booked!",
  "templateName": "session-booked",
  "variables": { "recipientName": "John", ... },
  "retryCount": 0,
  "failureReason": "Connection timeout"
}
```

---

## ðŸ—ï¸ 3. Redis Caching & Consistency

- **Invalidation Strategy**:
  - `AuthService` now has direct access to `CacheService` (via `skillsync-cache-common`).
  - Upon **Password Reset**, **Password Setup**, or **OAuth linking**, the `user:profile:<userId>` key is evicted.
  - This prevents `User Query Service` from serving outdated profile data or verified statuses.
- **Key Format**: All keys use `CacheService.vKey(...)` â†’ `v1:<domain>:<entity>:<id>`

---

## ðŸ§ª 4. Testing & Validation

Test scenarios:

### OAuth Flow Tests
1. **New user â†’ password setup required**: `passwordSetupRequired: true`
2. **Existing verified user â†’ direct login**: `passwordSetupRequired: false`, no user creation
3. **Unverified user â†’ blocked**: Exception thrown, no OAuth login
4. **Password setup â†’ success**: Password set, cache evicted
5. **Duplicate password setup â†’ rejected**: Error if password already set

### Email Retry Tests
1. **Successful retry**: Email sent on retry attempt
2. **Failure triggers re-queue**: Retry count incremented, re-published
3. **Max retries respected**: No re-publish after 3 attempts

### Existing Tests (maintained)
1. **Rollback Test**: Verify user deletion after 5 failed OTP attempts
2. **Reset Test**: Verify old refresh tokens are deleted after password change
3. **Email Mock Test**: Verify `EmailService.sendEmail` is called during event consumption
4. **OAuth Link Test**: Verify local account correctly links to Google provider on login

---

## ðŸ³ 5. DevOps Configuration

Add these to your `.env` file before running:
```bash
# SMTP Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Zipkin (for distributed tracing)
ZIPKIN_HOST=zipkin
```

---

## ðŸŒ 6. Production OAuth + API Domain Checklist

For production domain deployments (`https://skillsync.mraks.dev`):

1. Google Cloud Console must include this exact origin:
  - `https://skillsync.mraks.dev`
2. If authorization-code redirect flow is used, add this exact redirect URI:
  - `https://skillsync.mraks.dev/auth/oauth2/code/google`
3. Swagger/OpenAPI now uses relative server URLs through gateway (`/`), so it works from EC2 IP ingress and proxied hosts without hardcoded domain mapping.
4. CORS allowed origins must include the production frontend domain.

Use `docs/production_debugging_cors_fix_guide.md` for full route, CORS, and Swagger validation commands.

