# 🔐 SkillSync Auth & Notification System (V3)

This document outlines the architecture and implementation details for the 
**Registration Rollback**, **Password Reset with OTP**, **OAuth Integration**, 
the **Asynchronous Email Pipeline**, and the **Email Retry Mechanism**.

---

## 🔑 1. Authentication Upgrades & Rate Limiting

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
2.  **Verify**: `POST /api/auth/reset-password`
    - Payload: `email`, `otp`, `newPassword`.
    - Validates OTP attempts and expiry.
    - Hashes the new password using BCrypt.
    - **Security**: Invalidates all active refresh tokens for the user in `auth.refresh_tokens`.
    - **Cache**: Evicts the `user:profile:<userId>` cache in Redis to ensure consistency across the system.

### 1.3 OAuth + JWT Hybrid Flow (CORRECTED V3)

SkillSync supports **Google OAuth** login with different behavior for new vs existing users.

> [!IMPORTANT]
> The OAuth flow has been corrected in V3 to ensure proper UX:
> - **New users MUST set a password** before full access
> - **Existing users get direct login** with no password prompt

#### 1.3.1 OAuth Registration (New User)

When a user logs in via OAuth and does NOT have an existing account:

```text
Frontend → Google OAuth → sends profile to POST /api/auth/oauth-login
  ↓
Backend: User not found by provider+providerId nor by email
  ↓
Create user:
  - provider = "google"
  - providerId = "google-id-123"  
  - isVerified = true (verified by OAuth provider)
  - passwordSet = false (MUST set password)
  ↓
Response: OAuthResponse { passwordSetupRequired: true }
  ↓
Frontend: REDIRECT to Password Setup Screen
  ↓
POST /api/auth/setup-password { email, password }
  ↓
User can now login with email + password
```

#### 1.3.2 OAuth Login (Existing Verified User)

When a user already exists AND is verified:

```text
Frontend → Google OAuth → sends profile to POST /api/auth/oauth-login
  ↓
Backend: User FOUND by provider+providerId or by email, and isVerified=true
  ↓
Response: OAuthResponse { passwordSetupRequired: false }
  ↓
Direct login — NO password prompt, NO forced reset
```

#### 1.3.3 Edge Cases

| Scenario | Behavior |
|----------|----------|
| User exists but NOT verified | OAuth login BLOCKED. Error: "Please verify your email first" |
| OAuth email matches existing local account | Link OAuth provider to existing account (if verified) |
| User already has password set | `passwordSetupRequired: false` |
| Duplicate OAuth call for same user | Idempotent — returns existing user |

#### 1.3.4 User Lookup Priority

1. `findByProviderAndProviderId(provider, providerId)` — exact OAuth match
2. `findByEmail(email)` — fallback to email match
3. If neither found → create new user

#### 1.3.5 New Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/auth/oauth-login` | POST | OAuth login (returns `OAuthResponse`) |
| `/api/auth/setup-password` | POST | Set password for OAuth users |

#### 1.3.6 New DTOs

**OAuthResponse** — extends standard auth response:
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

## 📧 2. Asynchronous Email Pipeline

The **Notification Service** uses a hybrid **WebSocket + Email** architecture.

### 2.1 Architecture
The system follows an event-driven pattern via RabbitMQ:
```text
Event (Session/Payment/Mentor) 
  → Multi-Queue Consumer 
  → DB Save 
  → WebSocket Push (Real-time UI)
  → Email Service (Async Delivery)
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
  ↓
Publish EmailRetryEvent to email.retry.queue
  ↓
EmailRetryConsumer picks up event
  ↓
Attempt resend with exponential backoff
  ↓
On success: done
On failure (retryCount < 3): re-publish with retryCount++
On failure (retryCount >= 3): log PERMANENT FAILURE → message goes to email.dlq
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

## 🏗️ 3. Redis Caching & Consistency

- **Invalidation Strategy**:
  - `AuthService` now has direct access to `CacheService` (via `skillsync-cache-common`).
  - Upon **Password Reset**, **Password Setup**, or **OAuth linking**, the `user:profile:<userId>` key is evicted.
  - This prevents `User Query Service` from serving outdated profile data or verified statuses.
- **Key Format**: All keys use `CacheService.vKey(...)` → `v1:<domain>:<entity>:<id>`

---

## 🧪 4. Testing & Validation

Test scenarios:

### OAuth Flow Tests
1. **New user → password setup required**: `passwordSetupRequired: true`
2. **Existing verified user → direct login**: `passwordSetupRequired: false`, no user creation
3. **Unverified user → blocked**: Exception thrown, no OAuth login
4. **Password setup → success**: Password set, cache evicted
5. **Duplicate password setup → rejected**: Error if password already set

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

## 🐳 5. DevOps Configuration

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

## 🌐 6. Production OAuth + API Domain Checklist

For production domain deployments (`https://skillsync.mraks.dev`):

1. Google Cloud Console must include this exact origin:
  - `https://skillsync.mraks.dev`
2. If authorization-code redirect flow is used, add this exact redirect URI:
  - `https://skillsync.mraks.dev/auth/oauth2/code/google`
3. API Gateway and OpenAPI must advertise the public base URL:
  - `APP_PUBLIC_BASE_URL=https://skillsync.mraks.dev`
4. CORS allowed origins must include the production frontend domain.

Use `docs/production_debugging_cors_fix_guide.md` for full route, CORS, and Swagger validation commands.
