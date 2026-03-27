# SkillSync — Complete Viva Preparation Guide

> **Everything you need to know — architecture, layers, entities, design decisions, technologies, flows, and Swagger UI testing.**

---

## 📋 Table of Contents

1. [How to Use Swagger UI](#1-how-to-use-swagger-ui)
2. [Architecture & Layers](#2-architecture--layers)
3. [Auth Service](#3-auth-service)
4. [User Service](#4-user-service)
5. [Skill Service](#5-skill-service)
6. [Session Service](#6-session-service)
7. [Notification Service](#7-notification-service)
8. [API Gateway, Eureka, Config Server](#8-api-gateway-eureka-config-server)
9. [Why Java Records?](#9-why-java-records)
10. [Why WebSocket?](#10-why-websocket)
11. [Why RabbitMQ?](#11-why-rabbitmq)
12. [End-to-End Flow](#12-end-to-end-flow)
13. [Key Design Decisions](#13-key-design-decisions)
14. [Viva Q&A](#14-viva-qa)
15. [CQRS + Redis Q&A](#15-cqrs--redis-qa)

---

## 1. How to Use Swagger UI

### What is Swagger UI?
An **interactive API docs tool** auto-generated from your controllers using `springdoc-openapi`. It lets you see all endpoints, view request/response schemas, and **execute API calls directly from the browser**.

### Swagger UI — Single Entry Point (API Gateway)

> **IMPORTANT:** Swagger UI is available **only** through the API Gateway at port **8080**. Individual service ports are NOT exposed.

**URL:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Use the **dropdown at the top-right** to switch between services:

| Dropdown Option | APIs Shown |
|----------------|-----------|
| Auth Service | Register, Login, OTP, Token Refresh, Role Update |
| User Service (Users + Mentors + Groups) | Profiles, Skills, Mentor Apply/Approve, Groups, Discussions |
| Payment Service | Create Order, Verify Payment, Payment History |
| Skill Service | Skill CRUD, Search |
| Session Service (Sessions + Reviews) | Session Booking, Accept/Reject/Complete, Reviews, Ratings |
| Notification Service | Get Notifications, Unread Count, Mark Read |

### How to Test Step-by-Step

1. **Start all services** (Docker or individually)
2. **Open** `http://localhost:8080/swagger-ui.html` in browser
3. **Select a service** from the dropdown (e.g., "Auth Service")
4. **Expand an endpoint** (e.g., `POST /api/auth/register`)
5. **Click "Try it out"** → fill JSON body + required headers
6. **Click "Execute"** → see response, curl command, status code

### Testing Order (all via Gateway at 8080)

```
 1. Auth Service      → POST /api/auth/register (learner)
 2. Auth Service      → POST /api/auth/verify-otp
 3. Auth Service      → POST /api/auth/register (mentor user)
 4. Auth Service      → POST /api/auth/verify-otp
 5. Auth Service      → POST /api/auth/register (admin)
 6. Auth Service      → POST /api/auth/verify-otp
 7. Auth Service      → PUT /api/auth/users/3/role?role=ROLE_ADMIN
 8. Skill Service     → POST /api/skills (Java, Spring Boot, React, Python)
 9. User Service      → PUT /api/users/me (+ Authorization: Bearer <token>)
10. User Service      → POST /api/users/me/skills
11. User Service      → POST /api/mentors/apply
12. Payment Service   → POST /api/payments/create-order (Pay MENTOR_FEE)
13. Payment Service   → POST /api/payments/verify (Verifies → publishes event → User Service approves mentor)
14. User Service      → POST /api/mentors/me/availability
15. Session Service   → POST /api/sessions
16. Session Service   → PUT /api/sessions/1/accept
17. Session Service   → PUT /api/sessions/1/complete
18. Session Service   → POST /api/reviews
19. Notification Svc  → GET /api/notifications
20. User Service      → POST /api/groups
21. User Service      → POST /api/groups/1/discussions
```

---

## 2. Architecture & Layers

### Services Overview

| # | Service | Port | Database |
|---|---------|------|----------|
| 1 | Eureka Server | 8761 | — |
| 2 | Config Server | 8888 | — |
| 3 | API Gateway | 8080 | — |
| 4 | Auth Service | 8081 | skillsync_auth |
| 5 | User Service | 8082 | skillsync_user |
| 6 | Payment Service | 8086 | skillsync_payment |
| 7 | Skill Service | 8084 | skillsync_skill |
| 8 | Session Service | 8085 | skillsync_session |
| 9 | Notification Service | 8088 | skillsync_notification |

### Layered Package Structure (per service)

```
com.skillsync.<service>/
├── controller/   ← REST endpoints (HTTP entry points)
├── service/
│   ├── command/  ← Write operations + cache invalidation (CQRS)
│   └── query/   ← Read operations + cache-aside from Redis (CQRS)
├── cache/        ← RedisConfig + CacheService (generic cache wrapper)
├── repository/   ← Data access (Spring Data JPA)
├── entity/       ← JPA entities (DB tables)
├── dto/          ← Data Transfer Objects (Java Records)
├── enums/        ← Enums (Roles, Statuses)
├── config/       ← Configuration (OpenAPI, RabbitMQ, WebSocket)
├── exception/    ← Global error handling
├── event/        ← RabbitMQ event POJOs
├── consumer/     ← RabbitMQ message listeners
├── feign/        ← Inter-service HTTP clients
└── security/     ← JWT (Auth only)
```

### Request Flow

```
Read:  Client → Gateway (JWT) → Controller → QueryService → Redis → (miss?) → Repository → PostgreSQL → cache in Redis
Write: Client → Gateway (JWT) → Controller → CommandService → Repository → PostgreSQL → evict from Redis
```

### Tech Stack

| Tech | Purpose |
|------|---------|
| Java 17, Spring Boot 3 | Core framework |
| Spring Cloud Gateway | API Gateway (reactive) |
| Eureka | Service discovery |
| Spring Cloud Config | Centralized config |
| Spring Data JPA + Hibernate | ORM |
| PostgreSQL | Database (one per service, source of truth) |
| **Redis 7.2** | **Distributed cache (Cache-Aside pattern)** |
| **CQRS** | **Command/Query separation for read optimization** |
| RabbitMQ | Async event messaging + cross-service cache sync |
| WebSocket (STOMP) | Real-time push |
| OpenFeign | Declarative inter-service HTTP |
| JWT + BCrypt | Auth & password hashing |
| Lombok | Boilerplate reduction |
| springdoc-openapi | Swagger UI |

---

## 3. Auth Service

**Purpose:** Registration, email verification (OTP), login, JWT management, role assignment.

### Entities

**AuthUser** (table: `auth.users`)
| Field | Type | Why |
|-------|------|-----|
| email | String (unique) | Login identifier |
| passwordHash | String | BCrypt-hashed, never plain text |
| role | Enum: ROLE_LEARNER/MENTOR/ADMIN | Authorization |
| isVerified | boolean | Email verification gate |
| isActive | boolean | Account enable/disable |

**OtpToken** (table: `auth.otp_tokens`)
| Field | Purpose |
|-------|---------|
| otp | 6-digit code (SecureRandom) |
| expiresAt | 5-minute expiry |
| used | Prevents reuse |
| attempts | Max 5 wrong tries → brute-force protection |

**RefreshToken** (table: `auth.refresh_tokens`)
- `@ManyToOne` → AuthUser; token String (unique); 7-day expiry
- Max 5 per user (FIFO eviction) to prevent abuse

### Services
- **AuthService**: register (hash pw + send OTP), login (check isVerified, block if not), refreshToken, updateUserRole
- **OtpService**: generateAndSendOtp, verifyOtp (track attempts), scheduled cleanup every hour
- **EmailService**: sends OTP via JavaMail

### Flow
```
Register → hash password → save AuthUser → generate OTP → send email
Verify OTP → validate code + attempts → isVerified = true
Login → authenticate → check isVerified → issue JWT + refresh token
```

---

## 4. User Service

**Purpose:** User profiles, mentor applications, groups, discussions. Merged from 3 services.

**3 schemas:** `users`, `mentors`, `groups`

> **Note:** Payment processing has been extracted to a dedicated **Payment Service** (port 8086). User Service consumes `payment.business.action` events via RabbitMQ to trigger business actions.

### Entities

**Profile** (`users.profiles`) — firstName, lastName, bio, phone, location, profileCompletePct (calculated: 5 fields × 20%)

**UserSkill** (`users.user_skills`) — userId + skillId + proficiency (BEGINNER/INTERMEDIATE/ADVANCED). Junction table for many-to-many.

**MentorProfile** (`mentors.mentor_profiles`)
- status: PENDING → APPROVED/REJECTED/SUSPENDED
- `@OneToMany` → MentorSkill (skills taught), AvailabilitySlot (free hours)
- avgRating, totalReviews, totalSessions (aggregate counters)

**MentorSkill** (`mentors.mentor_skills`) — `@ManyToOne` → MentorProfile + skillId

**AvailabilitySlot** (`mentors.availability_slots`) — `@ManyToOne` → MentorProfile, dayOfWeek, startTime, endTime

**LearningGroup** (`groups.learning_groups`) — name, maxMembers, `@OneToMany` → GroupMember

**GroupMember** (`groups.group_members`) — `@ManyToOne` → LearningGroup, userId, role (OWNER/ADMIN/MEMBER), unique(group_id, user_id)

**Discussion** (`groups.discussions`) — `@ManyToOne` → LearningGroup, authorId, content, **self-referencing** `parent` → Discussion (for threaded replies). parent=null means top-level post.

### Services
- **UserService**: createOrUpdateProfile, addSkill. Uses **Feign → SkillService** to fetch skill names.
- **MentorService**: apply (PENDING), approveMentor (→ APPROVED + **Feign → AuthService** to change role + **RabbitMQ event**), rejectMentor, addAvailability
- **GroupService**: createGroup (auto OWNER), joinGroup (checks full), postDiscussion (members only, threaded)

### Inter-Service Communication
- `AuthServiceClient` (Feign) → updates role to ROLE_MENTOR on approval
- `SkillServiceClient` (Feign) → gets skill name/category for display
- RabbitMQ: publishes `MentorApprovedEvent`, `MentorRejectedEvent`

---

## 5. Skill Service

**Purpose:** Centralized skill catalog. Referenced by User + Session services.

### Entities
**Skill** (`skills.skills`) — name (unique), category, description, isActive
**Category** (`skills.categories`) — name, self-referencing `parent` (tree structure for nested categories)

### Why separate service?
Single source of truth for skill names. Prevents inconsistency if skills were defined in each service independently.

---

## 6. Session Service

**Purpose:** Session booking lifecycle + reviews/ratings.

**2 schemas:** `sessions`, `reviews`

### Entities

**Session** (`sessions.sessions`) — mentorId, learnerId, topic, sessionDate, durationMinutes, status, cancelReason

**SessionStatus** — **State Machine Enum**:
```
REQUESTED → ACCEPTED, REJECTED, CANCELLED
ACCEPTED  → COMPLETED, CANCELLED
REJECTED  → (terminal)
COMPLETED → (terminal)
CANCELLED → (terminal)
```
The enum has `canTransitionTo()` method with an `ALLOWED_TRANSITIONS` map. Enforces valid state changes at domain level.

**Review** (`reviews.reviews`) — sessionId (**unique** — one review per session), mentorId, reviewerId, rating (1-5), comment

### Services
- **SessionService**: createSession (validates: not self, 24h future, no conflicts), accept/reject/complete/cancel (ownership + state transition validation). Publishes events to RabbitMQ for each state change.
- **ReviewService**: submitReview (only learner of COMPLETED session), getMentorRatingSummary (avg, total, distribution)

### Events Published
Session events → `session.exchange` with keys: `session.requested/accepted/rejected/cancelled/completed`
Review events → `review.exchange` with key: `review.submitted`

---

## 7. Notification Service

**Purpose:** Listens to RabbitMQ events → saves notifications → pushes via WebSocket.

### Entity
**Notification** (`notifications.notifications`) — userId, type, title, message, data (TEXT), isRead

### Consumer Layer (RabbitMQ Listeners)
- **SessionEventConsumer**: 5 handlers (requested→notify mentor, accepted/rejected/completed→notify learner, cancelled→notify both)
- **MentorEventConsumer**: approved→notify user, rejected→notify user with reason
- **ReviewEventConsumer**: submitted→notify mentor with star count

### RabbitMQ Config
- 3 Topic Exchanges: `session.exchange`, `mentor.exchange`, `review.exchange`
- 8 durable queues bound by routing keys
- Jackson JSON message converter

### WebSocket Config
```
Endpoint: /ws/notifications (with SockJS fallback)
Client subscribes to: /user/{userId}/queue/notifications
Server pushes via: SimpMessagingTemplate.convertAndSendToUser()
```

### Flow
```
Event published → RabbitMQ → Consumer receives → NotificationService.createAndPush()
  → Saves to DB + WebSocketService.pushToUser() → Real-time push to frontend
```

---

## 8. API Gateway, Eureka, Config Server

### API Gateway (port 8080)
- Routes requests by path pattern to services via Eureka (`lb://service-name`)
- **JwtAuthenticationFilter**: extracts JWT → validates → adds X-User-Id/Email/Role headers
- Auth & Skill routes: **no** JWT required. All others: JWT required.
- WebSocket route: `lb:ws://notification-service` for `/ws/**`
- **Swagger UI**: Aggregated at `http://localhost:8080/swagger-ui.html` — proxies `/v3/api-docs` from each service. Individual service ports are NOT exposed.

### Eureka Server (port 8761)
- Service registry. All services register at startup. Gateway resolves `lb://auth-service` to actual host:port.

### Config Server (port 8888)
- Centralized config from Git/local files. Services fetch config at startup via `spring.config.import`.

---

## 9. Why Java Records?

All DTOs use Java Records (`record ClassName(fields) {}`):

| Benefit | Explanation |
|---------|------------|
| **Immutable** | Fields are final, no setters. DTOs should never change after creation |
| **No boilerplate** | Auto-generates constructor, getters, equals, hashCode, toString |
| **Thread-safe** | Immutable = inherently thread-safe |
| **Semantic** | `record` keyword = "this is a data carrier" |
| **Validation works** | Supports `@NotNull`, `@Size`, `@Min`, etc. |

**Why NOT Records for Entities?** JPA needs mutable objects (setters), no-args constructor, proxy-based lazy loading — all incompatible with records.

---

## 10. Why WebSocket?

**Problem:** Without WebSocket, frontend must **poll** every few seconds → wasteful.

**Solution:** WebSocket = persistent bidirectional connection. Server pushes only when there's a new notification.

| Technology | Role |
|-----------|------|
| WebSocket | Full-duplex TCP communication |
| STOMP | Message framing sub-protocol |
| SockJS | Fallback for old browsers |
| SimpMessagingTemplate | Spring's API to send user-specific messages |

**vs HTTP Polling:** Lower latency, less bandwidth, real-time updates, no wasted requests.

---

## 11. Why RabbitMQ?

**Problem:** Without broker, SessionService must directly call NotificationService → tight coupling, failure cascades.

**Solution:** Publish event to RabbitMQ → it delivers to consumers asynchronously.

| Benefit | Explanation |
|---------|------------|
| Loose coupling | Publisher doesn't know about consumers |
| Resilience | If consumer is down, messages queue up |
| Scalability | Multiple consumers can share a queue |
| Extensibility | New consumer = zero changes to publisher |
| Async | Main operation returns immediately |

**Topic Exchange:** Routes by routing key patterns. `session.requested` → `notification.session.requested.queue`.

---

## 12. End-to-End Flow

```
1. Register → Auth saves user + sends OTP email
2. Verify OTP → Auth marks isVerified=true
3. Login → Auth returns JWT (15min access + 7day refresh)
4. Create skills → Skill Service saves skill catalog
5. Update profile → User Service creates Profile
6. Apply as mentor → User Service creates PENDING MentorProfile
7. Pay mentor fee → Payment Service creates Razorpay order, verifies signature
   → On SUCCESS: Payment Service publishes payment.business.action event
   → User Service consumes event → sets status=APPROVED
   → Feign call to Auth: role=ROLE_MENTOR
   → RabbitMQ event → Notification Service: "Approved!" push
8. Book session → Session Service validates + saves REQUESTED session
   → RabbitMQ event → Notification: "New request" to mentor
9. Mentor accepts → status=ACCEPTED → Notification to learner
10. Mentor completes → status=COMPLETED → Notification to learner
11. Learner reviews → Review saved + RabbitMQ → Notification to mentor
12. Check notifications → REST API or real-time via WebSocket
```

---

## 13. Key Design Decisions

| Decision | Why |
|----------|-----|
| Database per service | Data isolation, independent schema evolution |
| Schemas within DB | Logical separation (users/mentors/groups) |
| Short-lived JWT (15min) + refresh (7day) | Security + convenience |
| Gateway-level auth | Centralized; services just read X-User-Id |
| Feign for sync calls | When response is needed (role update) |
| RabbitMQ for async | Notifications don't need sync response |
| **CQRS pattern** | **Separate read/write for independent optimization and scaling** |
| **Redis Cache-Aside** | **Read optimization without affecting write correctness** |
| **Graceful degradation** | **Redis down → fallback to DB, zero data loss** |
| **Event-driven cache sync** | **Cross-service cache invalidation via RabbitMQ** |
| State machine enum | Domain-level enforcement of valid transitions |
| Soft references (userId as Long) | No cross-DB foreign keys in microservices |
| Payment as dedicated service | **Separation of Concerns:** payment logic, Razorpay SDK, and saga orchestration are isolated in their own service with event-driven coordination |
| Event-driven Saga | Payment → RabbitMQ → User Service decouples payment lifecycle from business actions, enabling independent deployment/scaling |
| @CreatedDate/@LastModifiedDate | Auto timestamp management |
| Profile completeness % | Gamification (5 fields × 20%) |
| Builder pattern (Lombok) | Fluent, readable object construction |
| Self-referencing entities | Tree structures: Discussion threads, Category hierarchy |

---

## 14. Viva Q&A

**Q: What is a microservice?** A: Small, independently deployable service with its own DB. Communicates via REST + message queues.

**Q: Why microservices?** A: Independent scaling, fault isolation, team independence, technology flexibility.

**Q: Why Eureka?** A: Service discovery. Services find each other by name, not hardcoded URLs. Enables load balancing.

**Q: Why API Gateway?** A: Single entry point. Centralized auth, routing, CORS. Clients don't need to know every service address.

**Q: How does JWT work?** A: Server signs token with secret → client sends in header → Gateway validates signature → extracts user info → forwards to services.

**Q: Why BCrypt?** A: One-way hash with salt. Even if DB is breached, passwords can't be reversed.

**Q: Why OTP attempts limit?** A: Brute-force protection. After 5 wrong attempts, OTP is invalidated.

**Q: Why userId is Long not FK?** A: Cross-service FKs impossible (different DBs). Soft reference.

**Q: What is self-referencing ManyToOne?** A: Discussion.parent → Discussion. Creates tree for threaded replies.

**Q: Feign vs RabbitMQ?** A: Feign = sync (need response). RabbitMQ = async (fire & forget).

**Q: What is Topic Exchange?** A: Routes messages by routing key pattern. Enables flexible message routing.

**Q: RabbitMQ down?** A: Main operations work. publishEvent() is try-catch. Notifications delay until broker recovers.

**Q: WebSocket vs Polling?** A: Persistent connection, server pushes only when needed. Less bandwidth, real-time.

**Q: Why Records for DTOs?** A: Immutable, no boilerplate, thread-safe, semantic clarity.

**Q: Why NOT Records for Entities?** A: JPA needs mutability, no-args constructor, proxy support.

**Q: What is SessionStatus state machine?** A: Enum with ALLOWED_TRANSITIONS map. canTransitionTo() prevents invalid changes.

**Q: What does ddl-auto=update do?** A: Hibernate auto-creates/alters tables to match entities. Dev only.

**Q: What is @ControllerAdvice?** A: Global exception handler for all controllers.

**Q: Why was Payment extracted into its own service?** A: To enforce **Separation of Concerns**. Payment/Razorpay integration, saga orchestration, and the Payment entity are now isolated. Communication is event-driven via RabbitMQ (`payment.business.action` events consumed by User Service), enabling independent deployment, scaling, and testing.

**Q: How does the event-driven Saga work?** A: After payment verification, the Saga Orchestrator publishes a `payment.business.action` event to RabbitMQ. User Service's `PaymentEventConsumer` listens on `payment.business.action.queue` and calls `MentorCommandService.approveMentor()`. This replaces the old direct Feign/import coupling.

**Q: Builder Pattern?** A: Fluent object construction: `.builder().field(val).build()`. Lombok generates it.

**Q: What is @EntityListeners(AuditingEntityListener)?** A: Auto-fills @CreatedDate and @LastModifiedDate timestamps.

**Q: How does Gateway route?** A: `lb://service-name` → asks Eureka for address → forwards request.

**Q: Why use `lb://service-name` instead of `spring.cloud.gateway.discovery.locator.enabled=true`?**
A: Four reasons:
1. **Clean URLs**: The locator creates ugly URLs tightly coupled to service names (e.g., `/USER-SERVICE/api/users`). We want clean, semantic paths (`/api/users`).
2. **Security Control**: We need to selectively apply our `JwtAuthenticationFilter`. Auth Service is public, User Service is secured. Auto-discovery exposes *everything* uniformly, making selective filtering difficult.
3. **Merged Routes**: We map `/api/mentors/**` and `/api/groups/**` to the User Service. Auto-discovery cannot infer this logical routing.
4. **Encapsulation**: The frontend shouldn't know our internal microservice names. The Gateway hides our architecture.

> **Viva Tip:** For any feature, explain: WHAT it does → WHY we chose it → HOW it works → ALTERNATIVES considered.

---

## 15. CQRS + Redis Q&A

**Q: What is CQRS?** A: Command Query Responsibility Segregation. Separates write operations (CommandService) from read operations (QueryService) into distinct classes for independent optimization.

**Q: Why CQRS?** A: SkillSync is read-heavy (~80% reads). CQRS lets us cache reads aggressively via Redis without affecting write consistency. Each side can be scaled and optimized independently.

**Q: What is Redis used for?** A: Redis is a distributed in-memory cache. We use the Cache-Aside pattern: QueryService checks Redis first, falls back to PostgreSQL on miss, then stores the result in Redis.

**Q: Why Redis over in-process cache (Caffeine)?** A: In-process cache isn't shared across service replicas. With multiple instances behind a load balancer, each would have a different cache. Redis provides a single shared cache.

**Q: What happens if Redis goes down?** A: Graceful degradation. The CacheService wraps all Redis calls in try-catch. On failure, reads fall back to PostgreSQL. Zero data loss because PostgreSQL is the source of truth.

**Q: How does cache invalidation work?** A: CommandService evicts relevant Redis keys after every write (INSERT/UPDATE/DELETE). For cross-service invalidation, we use RabbitMQ events (e.g., `review.submitted` triggers mentor cache eviction in User Service).

**Q: What TTLs do you use?** A: Domain-specific — Skills: 1 hour (rarely change), Sessions: 5 min (state changes frequently), Notifications: 2 min (high poll rate), Users: 10 min (moderate updates).

**Q: Why not cache Auth Service?** A: Security. Passwords, OTPs, JWT tokens, and refresh tokens must never be stored in a shared cache. Auth queries are simple PK lookups and are already fast.

**Q: Cache-Aside vs Write-Through?** A: Cache-Aside gives us explicit control. Write-Through adds latency to every write. Since reads vastly outnumber writes, Cache-Aside is the right choice.

**Q: How do you test caching?** A: Unit tests mock CacheService to verify hit/miss/invalidation. Integration tests use Redis Testcontainers to test the full cache lifecycle (miss → populate → hit → invalidate → miss).

---

## 16. Frontend & React Q&A

**Q: Why React 18 instead of Angular/Vue?** A: React offers a massive ecosystem, concurrent rendering features, and integrates perfectly with our chosen stack (TypeScript, Redux Toolkit, TanStack Query). The component-based atomic design suits our complex dashboards well.

**Q: What is Redux Toolkit used for?** A: Global client state. Specifically, we use it for keeping track of the Auth State (JWT, refresh token, user role) globally across all components and Router guards.

**Q: Why use TanStack Query alongside Redux?** A: Redux is for synchronous client state (Auth, UI toggles). TanStack Query (React Query) is meant for asynchronous **server state**. It automatically handles caching, background refetching, pagination variables, and loading/error states for our API endpoints, removing hundreds of lines of boilerplate.

**Q: How do you handle JWT Tokens on the frontend?** A: We use an Axios interceptor. The `request` interceptor injects the Bearer token natively. The `response` interceptor watches for 401 Unauthorized errors.

**Q: How does the Silent Token Refresh work?** A: If a 401 is thrown, the Axios interceptor pauses all outgoing API requests into a queue, calls the `/api/auth/refresh` endpoint using the persisted refresh token, updates the Redux store, and then replays the queued requests. This creates a seamless, uninterrupted UX.

**Q: How did you implement Razorpay on the frontend?** A: When a user books a session, we first hit `/api/payments/order` to get an order ID asynchronously. Then we instantiate `new window.Razorpay(options)` which opens the official checkout modal over our app. Upon capturing the `razorpay_signature` in the success handler, we send it to our backend to natively link payment success to the session booking.

**Q: How do you protect routes?** A: We use React Router v6 with custom Wrapper Guards (`AuthGuard`, `GuestGuard`, `RoleGuard`). These evaluate the Redux state before rendering children. If unauthorized, they trigger a `<Navigate to="/login" replace />`.
