# SkillSync Backend — Complete Testing Guide

> [!IMPORTANT]
> **Architecture Update (March 2026):** The following services have been merged:
> - **Mentor Service + Group Service → User Service** (port 8082)
> - **Review Service → Session Service** (port 8085)
>
> **CQRS + Redis Caching (March 2026):** All business services now use Redis 7.2 for distributed caching. Services require a running Redis instance on port 6379. If Redis is unavailable, services fall back to direct PostgreSQL queries with zero data loss.
>
> All code is now organized in **layered packages** with CQRS sub-packages (`service.command/`, `service.query/`, `cache/`) within each service.
>
> **Active services:**
> | # | Service | Port |
> |---|---------|------|
> | 1 | Eureka Server | 8761 |
> | 2 | Config Server | 8888 |
> | 3 | API Gateway | 8080 |
> | 4 | Auth Service | 8081 |
> | 5 | User Service (includes Mentor, Group & Payment) | 8082 |
> | 6 | Skill Service | 8084 |
> | 7 | Session Service (includes Review) | 8085 |
> | 8 | Notification Service | 8088 |

---

## 📋 STEP 1: Prerequisites

### 1.1 Create PostgreSQL Databases & Schemas

Open **psql** or **pgAdmin** and run:

```sql
-- Create databases
CREATE DATABASE skillsync_auth;
CREATE DATABASE skillsync_user;
CREATE DATABASE skillsync_skill;
CREATE DATABASE skillsync_session;
CREATE DATABASE skillsync_notification;

-- Create schemas inside each database
\c skillsync_auth
CREATE SCHEMA IF NOT EXISTS auth;

\c skillsync_user
CREATE SCHEMA IF NOT EXISTS users;
CREATE SCHEMA IF NOT EXISTS mentors;
CREATE SCHEMA IF NOT EXISTS groups;

\c skillsync_skill
CREATE SCHEMA IF NOT EXISTS skills;

\c skillsync_session
CREATE SCHEMA IF NOT EXISTS sessions;
CREATE SCHEMA IF NOT EXISTS reviews;

\c skillsync_notification
CREATE SCHEMA IF NOT EXISTS notifications;
```

> [!NOTE]
> If running via Docker, the `init-databases.sql` file handles this automatically on first container start. You only need to run the above when testing locally without Docker.

### 1.2 Install & Start RabbitMQ

Download from https://www.rabbitmq.com/download.html and start the service.  
Management UI: http://localhost:15672 (guest/guest)

> [!NOTE]
> If you don't have RabbitMQ, services that use it (User Service for mentor events, Session Service for session/review events, Notification Service) will still start but event publishing will fail silently. Auth and Skill services work fine without it.

### 1.3 Install & Start Redis

**Option A — Docker (Recommended):**
```powershell
docker run -d --name skillsync-redis -p 6379:6379 redis:7.2-alpine --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
```

**Option B — Manual install:**
Download from https://redis.io/download and start the service.

Verify: `redis-cli ping` should return `PONG`

> [!NOTE]
> If Redis is not running, all services will still start and function correctly. Read operations will fall back to PostgreSQL queries. Cache hit/miss metrics will show 0 hits. This is by design (graceful degradation).

---

## 📋 STEP 2: Start Services

### Option A — Docker (Recommended)

```powershell
cd f:\SkillSync
docker-compose up --build
```

This starts everything (Postgres, RabbitMQ, all services, Nginx). Wait for all containers to become healthy.  
Verify: **http://localhost:8761** (Eureka Dashboard — all services should be registered)

### Option B — Local (for development/debugging)

Open **separate terminals** for each service and run:

```powershell
# Terminal 1 — Eureka Server (MUST start first, wait until ready)
cd f:\SkillSync\eureka-server
mvn spring-boot:run

# Terminal 2 — Config Server
cd f:\SkillSync\config-server
mvn spring-boot:run

# Terminal 3 — API Gateway
cd f:\SkillSync\api-gateway
mvn spring-boot:run

# Terminal 4 — Auth Service
cd f:\SkillSync\auth-service
mvn spring-boot:run

# Terminal 5 — User Service (serves User + Mentor + Group APIs)
cd f:\SkillSync\user-service
mvn spring-boot:run

# Terminal 6 — Skill Service
cd f:\SkillSync\skill-service
mvn spring-boot:run

# Terminal 7 — Session Service (serves Session + Review APIs)
cd f:\SkillSync\session-service
mvn spring-boot:run

# Terminal 8 — Notification Service
cd f:\SkillSync\notification-service
mvn spring-boot:run
```

### Verify Eureka Dashboard
Open: **http://localhost:8761**  
You should see all services registered.

---

## 📋 STEP 3: Test APIs (via Gateway at `localhost:8080`)

> [!IMPORTANT]
> All requests go through the **API Gateway** at port **8080**.  
> Use Postman, Insomnia, or `curl`. The examples below use `curl`.

---

### 🔐 3.1 AUTH SERVICE

#### Register User 1 (Learner)
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "learner@test.com",
    "password": "password123",
    "firstName": "Anjan",
    "lastName": "Sahoo"
  }'
```

#### Verify OTP (Check the registered email inbox for the OTP)
```bash
curl -X POST http://localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "learner@test.com",
    "otp": "<OTP_FROM_EMAIL>" 
  }'
```
> [!IMPORTANT]
> **Email verification is mandatory.** The OTP is always sent via real email to the registered address — even during testing. Check the email inbox (including spam/junk folder) for the OTP code. Login will be **blocked** until the email is verified.
>
> If you attempt to login without verifying your email, the service will automatically re-send a new OTP and reject the login with a clear error message.

**Expected Response** (save the `accessToken`!):
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "expiresIn": 900,
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "email": "learner@test.com",
    "role": "ROLE_LEARNER",
    "firstName": "Anjan",
    "lastName": "Sahoo"
  }
}
```

#### Register User 2 (will become Mentor)
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "mentor@test.com",
    "password": "password123",
    "firstName": "Anshul",
    "lastName": "Kumar"
  }'

# IMPORTANT: Verify OTP from email inbox before logging in!
```

#### Verify OTP for Mentor
```bash
curl -X POST http://localhost:8080/api/auth/verify-otp \\
  -H "Content-Type: application/json" \\
  -d '{
    "email": "mentor@test.com",
    "otp": "<OTP_FROM_EMAIL>"
  }'
```

#### Register Admin User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@test.com",
    "password": "password123",
    "firstName": "Admin",
    "lastName": "User"
  }'
```

#### Verify OTP for Admin
```bash
curl -X POST http://localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@test.com",
    "otp": "<OTP_FROM_EMAIL>"
  }'
```

Then manually promote to admin (direct call to Auth Service):  
```bash
curl -X PUT "http://localhost:8080/api/auth/users/3/role?role=ROLE_ADMIN"
```

#### Login (only works AFTER email verification)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "learner@test.com",
    "password": "password123"
  }'
```

> [!WARNING]
> Login will fail with `"Email not verified"` error if OTP verification has not been completed. The service will auto-resend a new OTP on each failed login attempt due to unverified email.

#### Refresh Token
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<REFRESH_TOKEN_FROM_LOGIN>"
  }'
```

#### Validate Token
```bash
curl -X GET http://localhost:8080/api/auth/validate \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

---

### 🎯 3.2 SKILL SERVICE (create skills first — other services depend on these)

#### Create Skills
```bash
curl -X POST http://localhost:8080/api/skills \
  -H "Content-Type: application/json" \
  -d '{"name": "Java", "category": "Programming", "description": "Java programming language"}'

curl -X POST http://localhost:8080/api/skills \
  -H "Content-Type: application/json" \
  -d '{"name": "Spring Boot", "category": "Framework", "description": "Spring Boot framework"}'

curl -X POST http://localhost:8080/api/skills \
  -H "Content-Type: application/json" \
  -d '{"name": "React", "category": "Frontend", "description": "React.js library"}'

curl -X POST http://localhost:8080/api/skills \
  -H "Content-Type: application/json" \
  -d '{"name": "Python", "category": "Programming", "description": "Python programming language"}'
```

#### Get All Skills
```bash
curl http://localhost:8080/api/skills
```

#### Get Skill by ID
```bash
curl http://localhost:8080/api/skills/1
```

#### Search Skills
```bash
curl "http://localhost:8080/api/skills/search?q=java"
```

#### Update Skill
```bash
curl -X PUT http://localhost:8080/api/skills/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "Java SE", "category": "Programming", "description": "Java Standard Edition"}'
```

---

### 👤 3.3 USER SERVICE

> [!NOTE]
> The Gateway passes `X-User-Id` header after JWT validation. For direct testing, pass it manually.

#### Update My Profile (as Learner, userId=1)
```bash
curl -X PUT http://localhost:8080/api/users/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1" \
  -d '{
    "firstName": "Anjan",
    "lastName": "Sahoo",
    "bio": "Full stack developer passionate about learning",
    "phone": "9876543210",
    "location": "India"
  }'
```

#### Get My Profile
```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1"
```

#### Add Skills to Profile
```bash
curl -X POST http://localhost:8080/api/users/me/skills \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1" \
  -d '{"skillId": 1, "proficiency": "INTERMEDIATE"}'

curl -X POST http://localhost:8080/api/users/me/skills \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1" \
  -d '{"skillId": 3, "proficiency": "BEGINNER"}'
```

#### Get Profile by ID
```bash
curl http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>"
```

---

### 🎓 3.4 MENTOR APIs (served by User Service on port 8082)

#### Apply to Become Mentor (as User 2)
```bash
curl -X POST http://localhost:8080/api/mentors/apply \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2" \
  -d '{
    "bio": "Experienced Java developer with 5 years in Spring Boot microservices. Passionate about teaching and mentoring junior developers in backend development.",
    "experienceYears": 5,
    "hourlyRate": 25.00,
    "skillIds": [1, 2]
  }'
```

#### View Pending Applications (Admin)
```bash
curl http://localhost:8080/api/mentors/pending \
  -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>" \
  -H "X-User-Id: 3"
```

#### Approve Mentor (Admin — mentorId=1)
```bash
curl -X PUT http://localhost:8080/api/mentors/1/approve \
  -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>" \
  -H "X-User-Id: 3"
```

#### Get Mentor Profile
```bash
curl http://localhost:8080/api/mentors/1 \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>"
```

#### Search Approved Mentors
```bash
curl http://localhost:8080/api/mentors/search \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>"
```

#### Add Availability (as Mentor, userId=2)
```bash
curl -X POST http://localhost:8080/api/mentors/me/availability \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2" \
  -d '{"dayOfWeek": 1, "startTime": "09:00", "endTime": "17:00"}'

curl -X POST http://localhost:8080/api/mentors/me/availability \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2" \
  -d '{"dayOfWeek": 3, "startTime": "10:00", "endTime": "18:00"}'
```

---

### 📅 3.5 SESSION APIs (served by Session Service on port 8085)

#### Book a Session (Learner books with Mentor)
```bash
curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1" \
  -d '{
    "mentorId": 2,
    "topic": "Spring Boot Microservices",
    "description": "Want to learn about building microservices with Spring Boot",
    "sessionDate": "2026-04-01T10:00:00",
    "durationMinutes": 60
  }'
```

> [!WARNING]
> `sessionDate` must be at least **24 hours in the future**. Adjust the date accordingly!

#### Get Session by ID
```bash
curl http://localhost:8080/api/sessions/1 \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1"
```

#### Get My Sessions (as Learner)
```bash
curl http://localhost:8080/api/sessions/learner \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1"
```

#### Get My Sessions (as Mentor)
```bash
curl http://localhost:8080/api/sessions/mentor \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

#### Accept Session (Mentor accepts)
```bash
curl -X PUT http://localhost:8080/api/sessions/1/accept \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

#### Complete Session (Mentor completes)
```bash
curl -X PUT http://localhost:8080/api/sessions/1/complete \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

#### Reject Session (test with a second session)
```bash
# First create another session
curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1" \
  -d '{
    "mentorId": 2,
    "topic": "React Basics",
    "description": "Want to learn React fundamentals",
    "sessionDate": "2026-04-02T14:00:00",
    "durationMinutes": 45
  }'

# Reject it
curl -X PUT "http://localhost:8080/api/sessions/2/reject?reason=Schedule%20conflict" \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

---

### ⭐ 3.6 REVIEW APIs (served by Session Service on port 8085 — Session must be COMPLETED first)

#### Submit Review (Learner reviews completed session)
```bash
curl -X POST http://localhost:8080/api/reviews \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1" \
  -d '{
    "sessionId": 1,
    "rating": 5,
    "comment": "Excellent session! The mentor explained Spring Boot microservices concepts very clearly with practical examples."
  }'
```

#### Get Mentor Reviews
```bash
curl http://localhost:8080/api/reviews/mentor/2 \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>"
```

#### Get Mentor Rating Summary
```bash
curl http://localhost:8080/api/reviews/mentor/2/summary \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>"
```

**Expected Response:**
```json
{
  "mentorId": 2,
  "averageRating": 5.0,
  "totalReviews": 1,
  "ratingDistribution": { "5": 1 }
}
```

#### Get My Submitted Reviews
```bash
curl http://localhost:8080/api/reviews/me \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1"
```

---

### 👥 3.7 GROUP APIs (served by User Service on port 8082)

#### Create a Learning Group
```bash
curl -X POST http://localhost:8080/api/groups \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1" \
  -d '{
    "name": "Spring Boot Study Group",
    "description": "A group for learning Spring Boot together",
    "maxMembers": 10
  }'
```

#### Join Group (User 2 joins)
```bash
curl -X POST http://localhost:8080/api/groups/1/join \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

#### Get All Groups
```bash
curl http://localhost:8080/api/groups \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>"
```

#### Get Group by ID
```bash
curl http://localhost:8080/api/groups/1 \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>"
```

#### Post Discussion
```bash
curl -X POST http://localhost:8080/api/groups/1/discussions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 1" \
  -d '{"content": "Hey everyone! What topics should we cover first?", "parentId": null}'
```

#### Reply to Discussion
```bash
curl -X POST http://localhost:8080/api/groups/1/discussions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2" \
  -d '{"content": "I suggest we start with Spring Boot basics and then move to microservices!", "parentId": 1}'
```

#### Get Group Discussions
```bash
curl http://localhost:8080/api/groups/1/discussions \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>"
```

---

### 🔔 3.8 NOTIFICATION SERVICE

#### Get My Notifications
```bash
curl http://localhost:8080/api/notifications \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

> [!TIP]
> If RabbitMQ is running, you should see notifications like "New Session Request", "Mentor Application Approved!" etc. that were automatically created by the event consumers.

#### Get Unread Count
```bash
curl http://localhost:8080/api/notifications/unread/count \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

#### Mark Notification as Read
```bash
curl -X PUT http://localhost:8080/api/notifications/1/read \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>"
```

#### Mark All as Read
```bash
curl -X PUT http://localhost:8080/api/notifications/read-all \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

---

### 💳 3.9 PAYMENT APIs (served by User Service on port 8082)

> [!IMPORTANT]
> Payment uses **Razorpay test credentials** by default. No real money is charged.
> The create-order endpoint creates a Razorpay order, and the verify endpoint validates the payment signature.

#### Create Payment Order (as Learner — Mentor Fee)
```bash
curl -X POST http://localhost:8080/api/payments/create-order \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2" \
  -d '{
    "type": "MENTOR_FEE",
    "referenceId": 1
  }'
```

**Expected Response:**
```json
{
  "orderId": "order_XXXXXXXXXX",
  "amount": 900,
  "currency": "INR",
  "status": "CREATED",
  "keyId": "rzp_test_SUxK0KnvPwKuAT"
}
```

> [!NOTE]
> In a real frontend flow, the `orderId` and `keyId` are passed to Razorpay's checkout SDK.
> The verify endpoint below is called **after** the user completes payment on the frontend.

#### Verify Payment (after Razorpay checkout)
```bash
curl -X POST http://localhost:8080/api/payments/verify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2" \
  -d '{
    "razorpayOrderId": "order_XXXXXXXXXX",
    "razorpayPaymentId": "pay_XXXXXXXXXX",
    "razorpaySignature": "SIGNATURE_FROM_RAZORPAY"
  }'
```

#### Get My Payments
```bash
curl http://localhost:8080/api/payments/my-payments \
  -H "Authorization: Bearer <LEARNER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

#### Check Payment Status (inter-service)
```bash
curl "http://localhost:8080/api/payments/check?userId=2&type=MENTOR_FEE"
```

---

## 📋 STEP 4: End-to-End Flow (Recommended Test Order)

Follow this order for the complete happy path:

```
 1. Register learner@test.com
 2. Verify OTP for learner (check EMAIL INBOX — OTP is always sent via real email)
 3. Register mentor@test.com
 4. Verify OTP for mentor (check EMAIL INBOX)
 5. Register admin@test.com
 6. Verify OTP for admin (check EMAIL INBOX)
 7. Promote admin (PUT /api/auth/users/3/role?role=ROLE_ADMIN)
 7. Create 4 skills (Java, Spring Boot, React, Python)
 8. Update learner profile + add skills
 9. Mentor applies (POST /api/mentors/apply)
10. >>> PAY MENTOR FEE (POST /api/payments/create-order with type=MENTOR_FEE)
11. >>> VERIFY PAYMENT (POST /api/payments/verify -- triggers auto-approval)
12. OR: Admin approves mentor (PUT /api/mentors/1/approve)
13. Login mentor again (to get updated ROLE_MENTOR token)
14. Add mentor availability
15. Learner books session with mentor
16. Mentor accepts session
17. Mentor completes session
18. Learner submits review
19. Check mentor rating summary
20. Create a group and post discussions
21. Check notifications (mentor should have: approval + session request + review alerts)
22. Check payment history (GET /api/payments/my-payments)
```

---

## 📋 STEP 5: Run Unit Tests (CQRS & Redis Validation)

Run the full test suite to guarantee cache correctness, fallback mechanics, and Saga consistency without hitting a live Redis instance.

```bash
# Run all tests across all services
mvn test
```

### Critical Test Coverages You Must Validate:

1. **Cache HIT Tests (`shouldReturnFromCache`)**
   - Validates that reading an existing key bypasses the PostgreSQL database entirely by verifying `repository.findById()` is called exactly `0` times (`never()`).
   
2. **Redis Failure Fallback Tests (`shouldFallbackToDbOnRedisFailure`)**
   - Injects a `RuntimeException("Redis connection refused")` when `cacheService.get()` is invoked.
   - Asserts the resilient design: the query seamlessly delegates to the database repository and returns accurate data without throwing 500s.

3. **Event-Driven Cache Invalidation Tests (`ReviewEventCacheSyncConsumerTest`)**
   - Proves RabbitMQ events reliably trigger cache evictions (e.g., `updateAvgRating()` is invoked on `ReviewSubmittedEvent`).

4. **Saga Consistency Tests (`PaymentSagaOrchestratorTest`)**
   - Verifies the `SUCCESS` branch approves the mentor and executes cache invalidation via `approveMentor()`.
   - Verifies the `COMPENSATION` branch reverts the approval and equally executes cache invalidation via `revertMentorApproval()`.

---

## 📋 STEP 6: Swagger UI (API Gateway — Single Entry Point)

> [!IMPORTANT]
> Swagger UI is available **only** through the API Gateway. Individual service ports are **not exposed**.

Open: **http://localhost:8080/swagger-ui.html**

Use the **dropdown at the top** to select which service to view/test:

| Dropdown Option | APIs Shown |
|----------------|-----------|
| Auth Service | Register, Login, OTP, Token Refresh, Role Update |
| User Service (Users + Mentors + Groups + Payments) | Profiles, Skills, Mentor Apply/Approve, Groups, Discussions, Payment Orders, Verification |
| Skill Service | Skill CRUD, Search |
| Session Service (Sessions + Reviews) | Session Booking, Accept/Reject/Complete, Reviews, Ratings |
| Notification Service | Get Notifications, Unread Count, Mark Read |

> [!NOTE]
> When testing APIs that require authentication, pass the `X-User-Id` header directly (bypass JWT). For APIs needing a JWT token via the Gateway, first call `/api/auth/login` to get a token, then use it in the `Authorization` header as `Bearer <token>`.

---

## 🔧 Troubleshooting

| Issue | Solution |
|-------|----------|
| Service can't start | Check if the PostgreSQL database exists and the schema is created |
| `Connection refused` to Eureka | Start Eureka Server first and wait 30 seconds |
| `401 Unauthorized` via Gateway | Check the JWT token is valid and not expired (15 min lifetime) |
| RabbitMQ connection failed | Make sure RabbitMQ is running on port 5672 |
| Redis connection failed | Make sure Redis is running on port 6379 (`redis-cli ping`) |
| Cache not working (all misses) | Verify Redis is running and check `application.properties` for `spring.data.redis.host` |
| Stale data after update | Cache invalidation may be delayed; wait for TTL expiry or verify `evict()` is called |
| `Table not found` errors | JPA `ddl-auto=update` creates tables automatically, but schemas must exist first |
| Port already in use | Kill the process: `netstat -ano | findstr :PORT` then `taskkill /PID <PID> /F` |
| Docker: database init fails | Delete the postgres volume (`docker volume rm skillsync_postgres-data`) and rebuild |
| Docker: service can't reach postgres | Check if healthcheck passed — services wait for `service_healthy` condition |
| Swagger UI empty | Ensure all services are registered in Eureka and healthy |
| Payment create-order fails | Check Razorpay credentials in `application.properties` or env vars |
| Signature verification fails | Ensure you pass the exact `razorpaySignature` from Razorpay checkout response |

