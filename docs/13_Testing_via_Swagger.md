# Testing via Swagger

This guide explains how to test SkillSync APIs with Swagger UI, how to obtain and reuse tokens, how `X-User-Id` is produced, and which endpoints are open versus protected across the backend services.

## 1. Best Swagger Entry Points

### Option A: Single aggregated Swagger UI through the gateway
Use the gateway Swagger UI when you want one place to test all services:

- `https://skillsync.mraks.dev/swagger-ui.html`
- Locally, if the gateway is running on port `8080`: `http://localhost:8080/swagger-ui.html`

The gateway is configured to proxy each service's OpenAPI document:

- Auth Service: `/service-docs/auth-service/v3/api-docs`
- User Service: `/service-docs/user-service/v3/api-docs`
- Skill Service: `/service-docs/skill-service/v3/api-docs`
- Session Service: `/service-docs/session-service/v3/api-docs`
- Notification Service: `/service-docs/notification-service/v3/api-docs`
- Payment Service: `/service-docs/payment-service/v3/api-docs`

### Option B: Per-service Swagger UI
If you run a service directly, its Swagger UI is available on that service's own port and `/swagger-ui/index.html`. In production, the public gateway base URL is used in the OpenAPI server config, so the UI points at the gateway host.

## 2. Token Model

SkillSync uses two tokens:

- Access token: short-lived JWT used for API authorization.
- Refresh token: long-lived JWT used only to get a new access token.

### Important rule
- `X-User-Id` is not a token.
- `X-User-Id` is derived from the access token by the API gateway and forwarded to downstream services.
- You should not guess or manually invent `X-User-Id` unless you are testing a service directly without the gateway.

## 3. How to Get Access Token and Refresh Token

### 3.1 Login
Call:

- `POST /api/auth/login`

Typical request body:

```json
{
  "email": "user@example.com",
  "password": "Password@123"
}
```

The response includes:

- `accessToken`
- `refreshToken`
- `expiresIn`
- `tokenType`
- `user`

The service also sets cookies:

- `accessToken`
- `refreshToken`

### 3.2 Registration flow
If the account is new, you may need to use:

- `POST /api/auth/initiate-registration`
- `POST /api/auth/verify-otp`
- `POST /api/auth/complete-registration`

Some flows also support:

- `POST /api/auth/register`
- `POST /api/auth/setup-password`

### 3.3 Refresh token flow
If the access token expires, call:

- `POST /api/auth/refresh`

The refresh endpoint accepts either:

- the `refreshToken` cookie, or
- a JSON body:

```json
{
  "refreshToken": "<refresh-token>"
}
```

Use the refresh token to get a fresh access token. Do not use the refresh token as a bearer token for normal API calls.

## 4. How `X-User-Id` Works

The API gateway reads the access token, extracts the JWT subject, and injects these headers into downstream requests:

- `X-User-Id`
- `X-User-Email`
- `X-User-Role`

This means:

- When using the gateway, you only need the access token.
- When testing a service directly, you may need to add `X-User-Id` manually for endpoints that expect it.

### Gateway behavior
The gateway also removes any incoming spoofed `X-User-Id`, `X-User-Email`, and `X-User-Role` headers before adding trusted values from the JWT.

## 5. Swagger Testing Workflow

### For protected endpoints
1. Open Swagger UI.
2. Click `Authorize`.
3. Paste the access token as a Bearer token.

Example value:

```text
Bearer <access-token>
```

4. Execute the endpoint.

### For endpoints that rely on cookies
Some auth endpoints use cookies, especially:

- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `POST /api/auth/setup-password`

In Swagger UI, cookies may not always be present automatically depending on how you entered the API. If needed, copy the refresh token from the login response and send it in the request body for `/refresh`.

### For direct-service testing without the gateway
If you hit a service directly instead of going through the gateway, add headers manually:

- `Authorization: Bearer <access-token>`
- `X-User-Id: <numeric-user-id>`
- `X-User-Email: <email>` if the endpoint or service expects it
- `X-User-Role: ROLE_LEARNER | ROLE_MENTOR | ROLE_ADMIN` if required

## 6. Open Endpoints By Service

These are the endpoints you can test without normal protected-route behavior. Some are public by design; others are internal but exposed in Swagger for service-to-service testing.

### 6.1 Auth Service
Base path: `/api/auth`

Open/public endpoints:

- `POST /api/auth/register`
- `POST /api/auth/initiate-registration`
- `POST /api/auth/complete-registration`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/verify-otp`
- `POST /api/auth/resend-otp`
- `POST /api/auth/forgot-password`
- `POST /api/auth/verify-password-reset-otp`
- `POST /api/auth/reset-password`
- `POST /api/auth/oauth-login`
- `POST /api/auth/setup-password`
- `GET /api/auth/validate`
- `GET /api/auth/me`

Internal endpoints:

- `GET /api/auth/internal/users/{id}`
- `GET /api/auth/internal/users`
- `GET /api/auth/internal/users/count`
- `DELETE /api/auth/internal/users/{id}`
- `PUT /api/auth/internal/users/{id}/role`
- `PUT /api/auth/internal/users/{id}/name`

Notes:
- The gateway blocks `/api/auth/internal/**` with HTTP 403.
- Use these only when testing auth-service directly or in internal service calls.

### 6.2 Skill Service
Base path: `/api/skills`

Public endpoints:

- `GET /api/skills`
- `GET /api/skills/{id}`
- `GET /api/skills/search?q=...`
- `GET /api/skills/batch?ids=1&ids=2&ids=3`

Protected endpoints:

- `POST /api/skills`
- `PUT /api/skills/{id}`
- `DELETE /api/skills/{id}`

This service is the easiest place to start if you want to verify Swagger access without login.

### 6.3 User Service
Base path: `/api/users`

Protected endpoints:

- `GET /api/users/me`
- `PUT /api/users/me`
- `POST /api/users/me/skills`
- `DELETE /api/users/me/skills/{skillId}`

Base path: `/api/mentors`

Protected endpoints:

- `GET /api/mentors/search`
- `GET /api/mentors/{id}`
- `GET /api/mentors/me`
- `GET /api/mentors/pending`
- `GET /api/mentors/me/availability`
- `POST /api/mentors/apply`
- `POST /api/mentors/me/availability`
- `DELETE /api/mentors/me/availability/{id}`
- `PUT /api/mentors/{id}/approve`
- `PUT /api/mentors/{id}/reject`

Base path: `/api/groups`

Protected endpoints:

- `GET /api/groups`
- `GET /api/groups/my`
- `GET /api/groups/{id}`
- `GET /api/groups/{id}/members`
- `GET /api/groups/{id}/discussions`
- `GET /api/groups/{id}/messages`
- `POST /api/groups`
- `PUT /api/groups/{id}`
- `DELETE /api/groups/{id}`
- `POST /api/groups/{id}/join`
- `POST /api/groups/{id}/message`
- `PUT /api/groups/{id}/message/{messageId}`
- `DELETE /api/groups/{id}/message/{messageId}`
- `DELETE /api/groups/{id}/leave`

Notes:
- In practice, user-service routes are gateway-protected.
- Mentor endpoints are routed through the gateway with JWT filtering, so they require authentication when tested through Swagger.

### 6.4 Session Service
Base path: `/api/sessions`

Protected endpoints:

- `GET /api/sessions/{id}`
- `GET /api/sessions/learner`
- `GET /api/sessions/mentor`
- `POST /api/sessions`
- `POST /api/sessions/{id}/accept`
- `POST /api/sessions/{id}/reject`
- `POST /api/sessions/{id}/cancel`
- `POST /api/sessions/{id}/complete`
- `POST /api/sessions/{id}/reschedule`
- `POST /api/sessions/{id}/review`
- `GET /api/sessions/count`
- `GET /api/sessions/mentor/{mentorId}/metrics`

Public endpoint:

- `GET /api/sessions/public/mentor/{mentorId}/booked`

Base path: `/api/reviews`

Protected endpoints:

- `GET /api/reviews/session/{sessionId}`
- `GET /api/reviews/mentor/{mentorId}`
- `POST /api/reviews`
- `PUT /api/reviews/{id}`
- `DELETE /api/reviews/{id}`

### 6.5 Notification Service
Base path: `/api/notifications`

Protected endpoints:

- `GET /api/notifications`
- `GET /api/notifications/unread/count`
- `PUT /api/notifications/{id}/read`
- `POST /api/notifications/read/{id}`
- `PUT /api/notifications/read-all`
- `DELETE /api/notifications/{id}`
- `DELETE /api/notifications/all`

This service is mostly useful after login because notifications are user-scoped.

### 6.6 Payment Service
Base path: `/api/payments`

Protected endpoints:

- `POST /api/payments/create-order`
- `POST /api/payments/verify`
- `GET /api/payments/my-payments`
- `GET /api/payments/order/{orderId}`
- `GET /api/payments/check`

Internal endpoint:

- `POST /internal/dlq/replay/{eventId}`
- `POST /internal/dlq/skip/{eventId}`
- `GET /internal/dlq/pending`

Notes:
- The create/verify endpoints are rate-limited more aggressively.
- Payment endpoints require authenticated user context and are not suitable for anonymous Swagger calls.

## 7. What Is Actually Open Through Swagger

If you are testing through the gateway, the truly open routes are:

- Auth service public endpoints under `/api/auth/**` except the internal admin-style routes.
- Skill service endpoints under `/api/skills/**`.
- Session public lookup endpoint: `GET /api/sessions/public/mentor/{mentorId}/booked`.

Everything else in the gateway route map is authenticated and should be tested with an access token.

## 8. Recommended Test Order In Swagger

If you want to test the system end-to-end, follow this order:

1. Open `Skill Service` and verify public browsing with `GET /api/skills`.
2. Open `Auth Service` and create or log in a user.
3. Copy the `accessToken` from the login response.
4. Use `Authorize` with `Bearer <accessToken>`.
5. Test `GET /api/auth/me` to confirm the token is valid.
6. Test `GET /api/users/me` or `GET /api/mentors/search` through the gateway.
7. Test `GET /api/sessions/public/mentor/{mentorId}/booked` for public session lookup.
8. Test payment and notification endpoints only after the authenticated profile flow is working.

## 9. Practical Examples

### Example: test a protected route through Swagger
1. Login with `POST /api/auth/login`.
2. Copy `accessToken` from the response.
3. Click `Authorize` in Swagger.
4. Paste:

```text
Bearer eyJhbGciOiJIUzI1NiJ9...
```

5. Call `GET /api/users/me`.

### Example: test refresh token manually
1. Call `POST /api/auth/login`.
2. Copy `refreshToken` from the response.
3. Call `POST /api/auth/refresh` with:

```json
{
  "refreshToken": "<refresh-token>"
}
```

4. Copy the new `accessToken` from the response.

### Example: test a direct service call without gateway
If you open a service directly and it expects `X-User-Id`, add:

```http
Authorization: Bearer <access-token>
X-User-Id: 1
```

Only do this when the service is not behind the gateway.

## 10. Troubleshooting

### Swagger returns 401
- Your access token is missing, expired, or not pasted as a Bearer token.
- Re-login and try again.

### Swagger returns 403 on internal auth routes
- This is expected for `/api/auth/internal/**` through the gateway.
- Use auth-service directly only for internal testing.

### `X-User-Id` is missing
- Use the gateway with a valid access token.
- If you are bypassing the gateway, add the header manually.

### Refresh token is not accepted
- Use the refresh token from the latest successful login.
- Old refresh tokens may already be invalidated after rotation.

### Swagger UI does not show all routes
- Open the aggregated gateway Swagger page.
- Make sure the gateway and service docs routes are running.

## 11. Short Reference

- Login token source: `POST /api/auth/login`
- Refresh token source: same login response, or cookie
- `X-User-Id`: extracted from access token by the gateway
- Public Swagger entry: `https://skillsync.mraks.dev/swagger-ui.html`
- Gateway docs proxy: `/service-docs/{service}/v3/api-docs`
