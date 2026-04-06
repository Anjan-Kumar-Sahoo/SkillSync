# Presentation Sync Note

Updated for final presentation on 2026-04-06. Start with docs/00_Presentation_Playbook.md for the guided narrative, then use this document for deep details.

---

# 01 Project Overview and Viva Prep



---

## Content from: doc1_project_overview.md

# ðŸ“„ DOCUMENT 1: PROJECT OVERVIEW

> [!IMPORTANT]
> **Architecture Update (March 2026):** The following services have been merged:
> - **Mentor Service + Group Service â†’ User Service** (port 8082) â€” mentor onboarding, groups, and user profiles now live in one service
> - **Review Service â†’ Session Service** (port 8085) â€” reviews and sessions share the same service and database
>
> **Payment Microservice Extraction (March 2026):** Razorpay payment processing has been extracted from User Service into a dedicated **Payment Service** (port 8086) with event-driven Saga orchestration via RabbitMQ. The Saga publishes `payment.business.action` events consumed by User Service to execute business actions (e.g., mentor approval), replacing direct service coupling.
>
> **CQRS + Redis Caching (March 2026):** All business services now implement the **CQRS pattern** (Command/Query separation) with **Redis 7.2** as a distributed cache layer for read optimization. See `doc6_cqrs_redis_architecture.md` for the full design.
> **Enterprise Hardening (March 2026):** Implemented a dedicated Mapper layer to decouple CQRS Command/Query services. Added tiered API Gateway Rate Limiting using resilience4j. Deployed a full observability stack with Prometheus, Grafana (auto-provisioned dashboards), and Loki (log aggregation) alongside Zipkin. CI/CD pipeline hardened with SonarQube integration and strict failure rules.
>
> ðŸš€ **Frontend Completed (March 2026):** The React 18 frontend is now fully scaffolded and operational.
> - **Tech**: React 18, Vite, TypeScript, Tailwind v4
> - **State Management**: Redux Toolkit for Auth (JWT token persistence), React Query for Data fetching
> - **Pages Built**: Auth (Login, Register), Learner Dashboard, Mentor Discovery, 
>   My Sessions (multi-tab mapping), Checkout (Razorpay SDK Flow), and Mentor Dashboard (availability logic).
>
> The original 11-service design below reflects the initial architecture. See `service_architecture_summary.md` for the current 9-service topology.

## SkillSync â€” Peer Learning & Mentor Matching Platform

---

## 1.1 System Description

SkillSync is a **production-grade, multi-tenant platform** that bridges the gap between knowledge seekers and domain experts. It enables:

- **Real-time mentor discovery** with advanced filtering (skill, rating, experience, price, availability)
- **Structured session booking** with a full lifecycle state machine
- **Peer learning groups** for collaborative knowledge sharing
- **Post-session review & rating** system for quality assurance
- **Event-driven notifications** for real-time updates

The system is built on a **Spring Boot microservices architecture** with an **API Gateway**, **service discovery**, **event-driven messaging via RabbitMQ**, **CQRS pattern with Redis distributed caching**, and a **React + TypeScript** frontend.

---

## 1.2 Problem Statement

### The Problem

Learners struggle to find **qualified, available mentors** who match their specific skill requirements. Existing platforms lack:

1. **Granular discovery** â€” No way to filter mentors by a combination of skill, price, experience, and real-time availability
2. **Session lifecycle management** â€” No structured workflow from request â†’ acceptance â†’ completion â†’ review
3. **Peer learning** â€” No integrated mechanism for group-based collaborative learning
4. **Quality assurance** â€” No post-session rating system to surface high-quality mentors
5. **Scalability** â€” Monolithic architectures fail under high user load

### The Solution

SkillSync addresses every gap with a purpose-built, microservices-based platform that handles the full mentoring lifecycle â€” from discovery to review â€” while supporting horizontal scalability for millions of concurrent users.

---

## 1.3 User Roles

### Role Matrix

| Capability | ROLE_LEARNER | ROLE_MENTOR | ROLE_ADMIN |
|---|:---:|:---:|:---:|
| Register / Login | âœ… | âœ… | âœ… |
| Create / Edit Profile | âœ… | âœ… | âœ… |
| Search Mentors | âœ… | âŒ | âœ… |
| Request Sessions | âœ… | âŒ | âŒ |
| Accept / Reject Sessions | âŒ | âœ… | âŒ |
| Set Availability | âŒ | âœ… | âŒ |
| Create Mentor Profile | âŒ | âœ… | âŒ |
| Rate Mentors | âœ… | âŒ | âŒ |
| Join Peer Groups | âœ… | âœ… | âŒ |
| Create Peer Groups | âœ… | âœ… | âŒ |
| Manage Users | âŒ | âŒ | âœ… |
| Approve Mentors | âŒ | âŒ | âœ… |
| Moderate Groups | âŒ | âŒ | âœ… |
| View Analytics | âŒ | âŒ | âœ… |

### Detailed Role Descriptions

**ROLE_LEARNER**
- Primary consumer of the platform
- Can browse the skill catalog, search/filter mentors, and initiate session requests
- Can join or create peer learning groups
- Must submit a review after each completed session (optional but encouraged via UX nudge)

**ROLE_MENTOR**
- Applies for mentor status via onboarding flow
- Profile goes through admin approval before activation
- Manages availability slots, accepts/rejects incoming session requests
- Receives ratings and reviews that affect their discovery ranking

**ROLE_ADMIN**
- Full platform oversight
- Approves/rejects mentor applications
- Can suspend/ban users, moderate group content
- Access to analytics dashboard (active users, sessions, revenue potential)

---

## 1.4 Core Features

### Feature 1: Authentication & Authorization
- User registration with **OTP email verification** (JavaMailSender)
- Login with JWT access + refresh token pair (Verified users only)
- Token refresh mechanism (access: 15min, refresh: 7 days)
- Role-based route guards (frontend) + method-level security (backend)
- Password hashing with BCrypt (strength 12)

### Feature 2: User Profile Management
- Create and update profile (name, bio, avatar, contact info)
- Skill association (learners tag their learning interests, mentors tag their expertise)
- Profile image upload to cloud storage (S3-compatible)
- Profile completeness tracker

### Feature 3: Mentor Onboarding
```
Workflow:
  User (ROLE_LEARNER) â†’ Submits Mentor Application
    â†’ Application includes: bio, experience, hourly_rate, skills[], certifications
    â†’ Status: PENDING
  User pays â‚¹9 Mentor Onboarding Fee via Razorpay
    â†’ Backend creates Razorpay order (amount: 900 paise)
    â†’ Frontend completes checkout
    â†’ Backend verifies payment signature
    â†’ On SUCCESS â†’ triggers mentor approval
  Admin reviews application (if manual approval required)
    â†’ APPROVED â†’ User gains ROLE_MENTOR, profile activated
    â†’ REJECTED â†’ User notified with rejection reason
```

### Feature 4: Skill Management
- Centralized, admin-managed skill catalog
- Skills organized by category (e.g., "Programming > Java", "Design > Figma")
- Skills linked to mentors via many-to-many relationship
- Skill-based search indexing for fast discovery

### Feature 5: Mentor Discovery
- Multi-criteria search with filters:
  - **Skill** (exact match or contains)
  - **Rating** (minimum threshold)
  - **Experience** (years range)
  - **Price** (hourly rate range)
  - **Availability** (specific date/time slots)
- Paginated results with sorting (rating desc, price asc, experience desc)
- Mentor cards with: avatar, name, top skills, rating, hourly rate, availability indicator

### Feature 6: Session Booking

```
State Machine:

  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”     Learner requests    â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
  â”‚          â”‚ â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶  â”‚           â”‚
  â”‚  [INIT]  â”‚                         â”‚ REQUESTED â”‚
  â”‚          â”‚                         â”‚           â”‚
  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜                         â””â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”˜
                                             â”‚
                              â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
                              â”‚              â”‚              â”‚
                        Mentor accepts  Mentor rejects  Learner cancels
                              â”‚              â”‚              â”‚
                              â–¼              â–¼              â–¼
                        â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”   â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”   â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
                        â”‚ ACCEPTED â”‚   â”‚ REJECTED â”‚   â”‚CANCELLED â”‚
                        â””â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”˜   â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜   â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                             â”‚
                    â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”
                    â”‚                 â”‚
              Session done      Either cancels
                    â”‚                 â”‚
                    â–¼                 â–¼
              â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”     â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
              â”‚COMPLETED â”‚     â”‚CANCELLED â”‚
              â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜     â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

- Session includes: mentor, learner, date/time, duration, topic, meeting link
- Calendar conflict detection (no double-booking)
- Automatic reminder notifications (24h and 1h before session)

### Feature 7: Peer Learning Groups
- Create groups with name, description, skill tags, max member count
- Join/leave groups freely
- Group discussion board (threaded messages)
- Group member list with roles (OWNER, MEMBER)
- Admin moderation capabilities

### Feature 8: Reviews & Ratings
- Learners submit reviews after COMPLETED sessions
- Review includes: star rating (1â€“5), written comment
- One review per session (enforced)
- Mentor's average rating recalculated on each new review
- Reviews displayed on mentor profile (paginated, newest first)

### Feature 9: Notifications
- Event-driven via RabbitMQ
- Notification types:
  - `SESSION_REQUESTED` â€” Mentor notified of new request
  - `SESSION_ACCEPTED` / `SESSION_REJECTED` â€” Learner notified
  - `SESSION_REMINDER` â€” Both parties, 24h & 1h before
  - `MENTOR_APPROVED` / `MENTOR_REJECTED` â€” Applicant notified
  - `PAYMENT_SUCCESS` â€” User notified of successful payment + business action
  - `PAYMENT_FAILED` â€” User notified of payment verification failure
  - `PAYMENT_COMPENSATED` â€” User notified when payment succeeded but business action failed
  - `NEW_REVIEW` â€” Mentor notified of new review
  - `GROUP_JOINED` â€” Group owner notified
- Delivery: In-app (WebSocket) + email (future)
- Read/unread status tracking

### Feature 10: Payment Gateway (Razorpay) â€” Dedicated Microservice with Event-Driven Saga
- Runs as a **standalone Payment Service** (port 8086, `com.skillsync.payment`)
- Two payment use cases:
  1. **Mentor Onboarding Fee** â€” â‚¹9 to apply as mentor
  2. **Session Booking Fee** â€” â‚¹9 to book a session with a mentor
- **Event-driven Saga orchestration:**
  - Payment Service creates Razorpay order (amount: 900 paise)
  - Frontend completes checkout via Razorpay SDK
  - Payment Service verifies HMAC-SHA256 signature â†’ marks VERIFIED
  - Saga orchestrator publishes `payment.business.action` event to RabbitMQ
  - User Service consumes the event and executes business action (mentor approval / session gate)
  - On success â†’ marks SUCCESS + publishes `payment.success` notification event
  - On failure â†’ triggers compensation + publishes `payment.compensated` event
- **Payment status lifecycle:** `CREATED â†’ VERIFIED â†’ SUCCESS_PENDING â†’ SUCCESS / COMPENSATED`
- **Reference mapping:** Every payment is linked to a business entity via `referenceId` + `referenceType`
- **Compensation strategy:** Business effects are reversed if saga fails (mentor revert, session gate)
- **Decoupling:** Payment Service has ZERO direct dependencies on User Service â€” all coordination is event-driven via RabbitMQ
- **Security:**
  - userId ALWAYS from JWT header (X-User-Id) â€” never from request params
  - Razorpay API key and secret from environment variables
  - Amount fixed server-side (never trust client-sent amounts)
  - Signature verification prevents tampering
  - Ownership validation on all payment queries
- **Notification events:** payment.success, payment.failed, payment.compensated via RabbitMQ â†’ Notification Service
- Edge cases: duplicate prevention (per reference), idempotent verification, amount mismatch detection

---

## 1.5 Business Goals

| Goal | Metric | Target |
|---|---|---|
| User Acquisition | Monthly active users | 100K+ within Year 1 |
| Mentor Quality | Avg mentor rating | â‰¥ 4.0 / 5.0 |
| Session Completion | Booking â†’ Completed rate | â‰¥ 75% |
| Platform Reliability | Uptime | 99.9% |
| Latency | API p95 response time | < 200ms |
| Scalability | Concurrent users | 50K+ |

---

## 1.6 High-Level Architecture

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                           CLIENT LAYER                                  â”‚
â”‚                                                                         â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”    â”‚
â”‚  â”‚              React + TypeScript SPA                              â”‚    â”‚
â”‚  â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”   â”‚    â”‚
â”‚  â”‚  â”‚ Auth     â”‚ â”‚ Mentor   â”‚ â”‚ Session  â”‚ â”‚ Admin Dashboard  â”‚   â”‚    â”‚
â”‚  â”‚  â”‚ Module   â”‚ â”‚ Discoveryâ”‚ â”‚ Booking  â”‚ â”‚                  â”‚   â”‚    â”‚
â”‚  â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜   â”‚    â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜    â”‚
â”‚                            â”‚ HTTPS / WSS                                â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                             â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                    GATEWAY LAYER                                        â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”    â”‚
â”‚  â”‚              Spring Cloud Gateway                                â”‚    â”‚
â”‚  â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”   â”‚    â”‚
â”‚  â”‚  â”‚ JWT      â”‚ â”‚ Rate     â”‚ â”‚ Route    â”‚ â”‚ Load Balancing   â”‚   â”‚    â”‚
â”‚  â”‚  â”‚ Validate â”‚ â”‚ Limiting â”‚ â”‚ Config   â”‚ â”‚                  â”‚   â”‚    â”‚
â”‚  â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜   â”‚    â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜    â”‚
â”‚                            â”‚                                            â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                             â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                    SERVICE LAYER                                        â”‚
â”‚                            â”‚                                            â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”              â”‚
â”‚  â”‚ Auth       â”‚ â”‚ User Service :8082   â”‚ â”‚ Skill        â”‚              â”‚
â”‚  â”‚ Service    â”‚ â”‚ (+ Mentor + Group)   â”‚ â”‚ Service      â”‚              â”‚
â”‚  â”‚ :8081      â”‚ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â”‚ :8084        â”‚              â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜                          â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜              â”‚
â”‚                                                                        â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”          â”‚
â”‚  â”‚ Session Service :8085â”‚  â”‚ Payment      â”‚  â”‚ Notification â”‚          â”‚
â”‚  â”‚ (+ Review)           â”‚  â”‚ Service      â”‚  â”‚ Service      â”‚          â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â”‚ :8086        â”‚  â”‚ :8088        â”‚          â”‚
â”‚                            â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜          â”‚
â”‚                                                                         â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”    â”‚
â”‚  â”‚                    Eureka Service Discovery (:8761)              â”‚    â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜    â”‚
â”‚                                                                         â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                             â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                    CACHING LAYER (Redis 7.2)                            â”‚
â”‚                                                                         â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”    â”‚
â”‚  â”‚  Cache-Aside Pattern: QueryService â†’ Redis â†’ PostgreSQL         â”‚    â”‚
â”‚  â”‚  CommandService â†’ PostgreSQL write â†’ Redis invalidation         â”‚    â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜    â”‚
â”‚                                                                         â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                             â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                    DATA & MESSAGING LAYER                               â”‚
â”‚                                                                         â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”                  â”‚
â”‚  â”‚ Auth DB  â”‚ â”‚ User DB  â”‚ â”‚Mentor DB â”‚ â”‚Skill DB  â”‚  (PostgreSQL)    â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜                  â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â” â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”                  â”‚
â”‚  â”‚Session DBâ”‚ â”‚ Group DB â”‚ â”‚Review DB â”‚ â”‚Notif DB  â”‚  (PostgreSQL)    â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜ â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜                  â”‚
â”‚                                                                         â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”    â”‚
â”‚  â”‚                    RabbitMQ Message Broker                        â”‚    â”‚
â”‚  â”‚  Exchanges: session, mentor, review, skill, payment              â”‚    â”‚
â”‚  â”‚  Saga Events: payment.business.action (Payment â†’ User Service)   â”‚    â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜    â”‚
â”‚                                                                         â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

---

## 1.7 Key Workflows

### Workflow 1: Mentor Discovery (with Redis Cache)

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”     GET /api/mentors/search         â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚          â”‚ â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶  â”‚              â”‚
â”‚ Learner  â”‚     ?skill=Java&minRating=4          â”‚ API Gateway  â”‚
â”‚ (React)  â”‚     &maxPrice=50&page=0              â”‚              â”‚
â”‚          â”‚ â—€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€  â”‚              â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜     Paginated mentor list            â””â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”˜
                                                         â”‚
                                                         â–¼
                                                  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
                                                  â”‚ User Service â”‚
                                                  â”‚ (MentorQuery â”‚
                                                  â”‚    Service)  â”‚
                                                  â”‚              â”‚
                                                  â”‚ 1. Parse     â”‚
                                                  â”‚    filters   â”‚
                                                  â”‚ 2. Check     â”‚
                                                  â”‚    Redis     â”‚
                                                  â”‚ 3. MISS â†’    â”‚
                                                  â”‚    Query DB  â”‚
                                                  â”‚    (indexed) â”‚
                                                  â”‚ 4. Cache in  â”‚
                                                  â”‚    Redis     â”‚
                                                  â”‚ 5. Paginate  â”‚
                                                  â”‚ 6. Return    â”‚
                                                  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

### Workflow 2: Session Booking Flow

```
Learner                API Gateway         Session Service       Mentor Service       RabbitMQ        Notification Service
  â”‚                        â”‚                     â”‚                     â”‚                  â”‚                    â”‚
  â”‚ POST /api/sessions     â”‚                     â”‚                     â”‚                  â”‚                    â”‚
  â”‚ {mentorId, date, topic}â”‚                     â”‚                     â”‚                  â”‚                    â”‚
  â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶â”‚                     â”‚                     â”‚                  â”‚                    â”‚
  â”‚                        â”‚ Validate JWT        â”‚                     â”‚                  â”‚                    â”‚
  â”‚                        â”‚ Route to service    â”‚                     â”‚                  â”‚                    â”‚
  â”‚                        â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶â”‚                     â”‚                  â”‚                    â”‚
  â”‚                        â”‚                     â”‚ Validate mentor     â”‚                  â”‚                    â”‚
  â”‚                        â”‚                     â”‚ exists & available  â”‚                  â”‚                    â”‚
  â”‚                        â”‚                     â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶â”‚                  â”‚                    â”‚
  â”‚                        â”‚                     â”‚â—€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”‚                  â”‚                    â”‚
  â”‚                        â”‚                     â”‚                     â”‚                  â”‚                    â”‚
  â”‚                        â”‚                     â”‚ Check time conflict â”‚                  â”‚                    â”‚
  â”‚                        â”‚                     â”‚ Create session      â”‚                  â”‚                    â”‚
  â”‚                        â”‚                     â”‚ (status=REQUESTED)  â”‚                  â”‚                    â”‚
  â”‚                        â”‚                     â”‚                     â”‚                  â”‚                    â”‚
  â”‚                        â”‚                     â”‚ Publish event       â”‚                  â”‚                    â”‚
  â”‚                        â”‚                     â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶  â”‚                    â”‚
  â”‚                        â”‚                     â”‚                     â”‚  SESSION_REQUESTEDâ”‚                    â”‚
  â”‚                        â”‚                     â”‚                     â”‚                  â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶â”‚
  â”‚                        â”‚                     â”‚                     â”‚                  â”‚                    â”‚
  â”‚                        â”‚  201 Created         â”‚                     â”‚                  â”‚  Notify mentor     â”‚
  â”‚â—€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”‚â—€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”‚                     â”‚                  â”‚  (WebSocket)       â”‚
  â”‚                        â”‚                     â”‚                     â”‚                  â”‚                    â”‚
```

### Workflow 3: Mentor Approval Flow

```
User                 API Gateway         Mentor Service          RabbitMQ        Notification Service
  â”‚                      â”‚                     â”‚                     â”‚                    â”‚
  â”‚ POST /api/mentors    â”‚                     â”‚                     â”‚                    â”‚
  â”‚ /apply               â”‚                     â”‚                     â”‚                    â”‚
  â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶â”‚                     â”‚                     â”‚                    â”‚
  â”‚                      â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶â”‚                     â”‚                    â”‚
  â”‚                      â”‚                     â”‚ Create mentor       â”‚                    â”‚
  â”‚                      â”‚                     â”‚ (status=PENDING)    â”‚                    â”‚
  â”‚â—€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”‚â—€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”‚                     â”‚                    â”‚
  â”‚                      â”‚                     â”‚                     â”‚                    â”‚
  â”‚                      â”‚                     â”‚                     â”‚                    â”‚

Admin                API Gateway         Mentor Service          RabbitMQ        Notification Service
  â”‚                      â”‚                     â”‚                     â”‚                    â”‚
  â”‚ PUT /api/mentors     â”‚                     â”‚                     â”‚                    â”‚
  â”‚ /{id}/approve        â”‚                     â”‚                     â”‚                    â”‚
  â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶â”‚                     â”‚                     â”‚                    â”‚
  â”‚                      â”‚ Validate ROLE_ADMIN â”‚                     â”‚                    â”‚
  â”‚                      â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶â”‚                     â”‚                    â”‚
  â”‚                      â”‚                     â”‚ Update status       â”‚                    â”‚
  â”‚                      â”‚                     â”‚ (APPROVED)          â”‚                    â”‚
  â”‚                      â”‚                     â”‚ Update user role    â”‚                    â”‚
  â”‚                      â”‚                     â”‚ Publish event       â”‚                    â”‚
  â”‚                      â”‚                     â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶â”‚                    â”‚
  â”‚                      â”‚                     â”‚                     â”‚ MENTOR_APPROVED    â”‚
  â”‚                      â”‚                     â”‚                     â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¶â”‚
  â”‚â—€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”‚â—€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”‚                     â”‚                    â”‚
  â”‚                      â”‚                     â”‚                     â”‚   Notify user      â”‚
```

---

## 1.8 Technology Summary

| Layer | Technology | Purpose |
|---|---|---|
| Frontend | React 18 + TypeScript | Single Page Application |
| State Management | Redux Toolkit | Global state, auth, caching |
| API Client | Axios + React Query | HTTP calls, caching, retry |
| Styling | Tailwind CSS | Utility-first CSS framework |
| Realtime | WebSocket (SockJS + STOMP) | Live notifications |
| API Gateway | Spring Cloud Gateway | Routing, JWT validation, rate limiting |
| Service Discovery | Eureka Server | Dynamic service registration |
| Backend Services | Spring Boot 3.x | Microservice framework |
| Security | Spring Security + JWT | Authentication & authorization |
| Messaging | RabbitMQ | Async event-driven communication |
| Caching | Redis 7.2 | Distributed cache layer (Cache-Aside pattern) |
| Architecture Pattern | CQRS | Command/Query separation for read optimization |
| Payment Gateway | Razorpay (Java SDK 1.4.8) | Mentor fee + session booking payments |
| Database | PostgreSQL | Per-service relational storage (Source of Truth) |
| ORM | Spring Data JPA / Hibernate | Object-relational mapping |
| Documentation | Swagger / OpenAPI 3.0 | Automated API documentation UI |
| Logging | Logback / SLF4J | Rolling file and console logging |
| Build Tool | Maven | Dependency management, build |
| Containerization | Docker + Docker Compose | Packaging & orchestration |
| CI/CD | GitHub Actions | Build, test, deploy pipeline |

---

> [!NOTE]
> This document serves as the foundational reference for all subsequent design documents.
> All architectural decisions flow from the principles outlined here.


---

## Content from: skillsync_viva_preparation.md

# SkillSync â€” Complete Viva Preparation Guide

> **Everything you need to know â€” architecture, layers, entities, design decisions, technologies, flows, and Swagger UI testing.**

---

## ðŸ“‹ Table of Contents

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
15. [CQRS + Redis (Cache Details)](#15-cqrs--redis-cache-details)
16. [Frontend & React Q&A](#16-frontend--react-qa)

---

## 1. How to Use Swagger UI

### What is Swagger UI?
An **interactive API docs tool** auto-generated from your controllers using `springdoc-openapi`. It lets you see all endpoints, view request/response schemas, and **execute API calls directly from the browser**.

### Swagger UI â€” Single Entry Point (API Gateway)

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
5. **Click "Try it out"** â†’ fill JSON body + required headers
6. **Click "Execute"** â†’ see response, curl command, status code

### Testing Order (all via Gateway at 8080)

```
 1. Auth Service      â†’ POST /api/auth/register (learner)
 2. Auth Service      â†’ POST /api/auth/verify-otp
 3. Auth Service      â†’ POST /api/auth/register (mentor user)
 4. Auth Service      â†’ POST /api/auth/verify-otp
 5. Auth Service      â†’ POST /api/auth/register (admin)
 6. Auth Service      â†’ POST /api/auth/verify-otp
 7. Auth Service      â†’ PUT /api/auth/users/3/role?role=ROLE_ADMIN
 8. Skill Service     â†’ POST /api/skills (Java, Spring Boot, React, Python)
 9. User Service      â†’ PUT /api/users/me (+ Authorization: Bearer <token>)
10. User Service      â†’ POST /api/users/me/skills
11. User Service      â†’ POST /api/mentors/apply
12. Session Service   â†’ POST /api/sessions (book session)
13. Payment Service   â†’ POST /api/payments/create-order (Pay SESSION_BOOKING)
14. Payment Service   â†’ POST /api/payments/verify (Verifies â†’ publishes event)
14. User Service      â†’ POST /api/mentors/me/availability
15. Session Service   â†’ POST /api/sessions
16. Session Service   â†’ PUT /api/sessions/1/accept
17. Session Service   â†’ PUT /api/sessions/1/complete
18. Session Service   â†’ POST /api/reviews
19. Notification Svc  â†’ GET /api/notifications
20. User Service      â†’ POST /api/groups
21. User Service      â†’ POST /api/groups/1/discussions
```

---

## 2. Architecture & Layers

### Services Overview

| # | Service | Port | Database |
|---|---------|------|----------|
| 1 | Eureka Server | 8761 | â€” |
| 2 | Config Server | 8888 | â€” |
| 3 | API Gateway | 8080 | â€” |
| 4 | Auth Service | 8081 | skillsync_auth |
| 5 | User Service | 8082 | skillsync_user |
| 6 | Payment Service | 8086 | skillsync_payment |
| 7 | Skill Service | 8084 | skillsync_skill |
| 8 | Session Service | 8085 | skillsync_session |
| 9 | Notification Service | 8088 | skillsync_notification |

### Layered Package Structure (per service)

```
com.skillsync.<service>/
â”œâ”€â”€ controller/   â† REST endpoints (HTTP entry points)
â”œâ”€â”€ service/
â”‚   â”œâ”€â”€ command/  â† Write operations + cache invalidation (CQRS)
â”‚   â””â”€â”€ query/   â† Read operations + cache-aside from Redis (CQRS)
â”œâ”€â”€ cache/        â† RedisConfig + CacheService (generic cache wrapper)
â”œâ”€â”€ repository/   â† Data access (Spring Data JPA)
â”œâ”€â”€ entity/       â† JPA entities (DB tables)
â”œâ”€â”€ dto/          â† Data Transfer Objects (Java Records)
â”œâ”€â”€ enums/        â† Enums (Roles, Statuses)
â”œâ”€â”€ config/       â† Configuration (OpenAPI, RabbitMQ, WebSocket)
â”œâ”€â”€ exception/    â† Global error handling
â”œâ”€â”€ event/        â† RabbitMQ event POJOs
â”œâ”€â”€ consumer/     â† RabbitMQ message listeners
â”œâ”€â”€ feign/        â† Inter-service HTTP clients
â””â”€â”€ security/     â† JWT (Auth only)
```

### Request Flow

```
Read:  Client â†’ Gateway (JWT) â†’ Controller â†’ QueryService â†’ Redis â†’ (miss?) â†’ Repository â†’ PostgreSQL â†’ cache in Redis
Write: Client â†’ Gateway (JWT) â†’ Controller â†’ CommandService â†’ Repository â†’ PostgreSQL â†’ evict from Redis
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
| attempts | Max 5 wrong tries â†’ brute-force protection |

**RefreshToken** (table: `auth.refresh_tokens`)
- `@ManyToOne` â†’ AuthUser; token String (unique); 7-day expiry
- Max 5 per user (FIFO eviction) to prevent abuse

### Services
- **AuthService**: register (hash pw + send OTP), login (check isVerified, block if not), refreshToken, updateUserRole
- **OtpService**: generateAndSendOtp, verifyOtp (track attempts), scheduled cleanup every hour
- **EmailService**: sends OTP via JavaMail

### Flow
```
Register â†’ hash password â†’ save AuthUser â†’ generate OTP â†’ send email
Verify OTP â†’ validate code + attempts â†’ isVerified = true
Login â†’ authenticate â†’ check isVerified â†’ issue JWT + refresh token
```

---

## 4. User Service

**Purpose:** User profiles, mentor applications, groups, discussions. Merged from 3 services.

**3 schemas:** `users`, `mentors`, `groups`

> **Note:** Payment processing has been extracted to a dedicated **Payment Service** (port 8086). User Service consumes `payment.business.action` events via RabbitMQ to trigger business actions.

### Entities

**Profile** (`users.profiles`) â€” firstName, lastName, bio, phone, location, profileCompletePct (calculated: 5 fields Ã— 20%)

**UserSkill** (`users.user_skills`) â€” userId + skillId + proficiency (BEGINNER/INTERMEDIATE/ADVANCED). Junction table for many-to-many.

**MentorProfile** (`mentors.mentor_profiles`)
- status: PENDING â†’ APPROVED/REJECTED/SUSPENDED
- `@OneToMany` â†’ MentorSkill (skills taught), AvailabilitySlot (free hours)
- avgRating, totalReviews, totalSessions (aggregate counters)

**MentorSkill** (`mentors.mentor_skills`) â€” `@ManyToOne` â†’ MentorProfile + skillId

**AvailabilitySlot** (`mentors.availability_slots`) â€” `@ManyToOne` â†’ MentorProfile, dayOfWeek, startTime, endTime

**LearningGroup** (`groups.learning_groups`) â€” name, maxMembers, `@OneToMany` â†’ GroupMember

**GroupMember** (`groups.group_members`) â€” `@ManyToOne` â†’ LearningGroup, userId, role (OWNER/ADMIN/MEMBER), unique(group_id, user_id)

**Discussion** (`groups.discussions`) â€” `@ManyToOne` â†’ LearningGroup, authorId, content, **self-referencing** `parent` â†’ Discussion (for threaded replies). parent=null means top-level post.

### Services
- **UserService**: createOrUpdateProfile, addSkill. Uses **Feign â†’ SkillService** to fetch skill names.
- **MentorService**: apply (PENDING), approveMentor (â†’ APPROVED + **Feign â†’ AuthService** to change role + **RabbitMQ event**), rejectMentor, addAvailability
- **GroupService**: createGroup (auto OWNER), joinGroup (checks full), postDiscussion (members only, threaded)

### Inter-Service Communication
- `AuthServiceClient` (Feign) â†’ updates role to ROLE_MENTOR on approval
- `SkillServiceClient` (Feign) â†’ gets skill name/category for display
- RabbitMQ: publishes `MentorApprovedEvent`, `MentorRejectedEvent`

---

## 5. Skill Service

**Purpose:** Centralized skill catalog. Referenced by User + Session services.

### Entities
**Skill** (`skills.skills`) â€” name (unique), category, description, isActive
**Category** (`skills.categories`) â€” name, self-referencing `parent` (tree structure for nested categories)

### Why separate service?
Single source of truth for skill names. Prevents inconsistency if skills were defined in each service independently.

---

## 6. Session Service

**Purpose:** Session booking lifecycle + reviews/ratings.

**2 schemas:** `sessions`, `reviews`

### Entities

**Session** (`sessions.sessions`) â€” mentorId, learnerId, topic, sessionDate, durationMinutes, status, cancelReason

**SessionStatus** â€” **State Machine Enum**:
```
REQUESTED â†’ ACCEPTED, REJECTED, CANCELLED
ACCEPTED  â†’ COMPLETED, CANCELLED
REJECTED  â†’ (terminal)
COMPLETED â†’ (terminal)
CANCELLED â†’ (terminal)
```
The enum has `canTransitionTo()` method with an `ALLOWED_TRANSITIONS` map. Enforces valid state changes at domain level.

**Review** (`reviews.reviews`) â€” sessionId (**unique** â€” one review per session), mentorId, reviewerId, rating (1-5), comment

### Services
- **SessionService**: createSession (validates: not self, 24h future, no conflicts), accept/reject/complete/cancel (ownership + state transition validation). Publishes events to RabbitMQ for each state change.
- **ReviewService**: submitReview (only learner of COMPLETED session), getMentorRatingSummary (avg, total, distribution)

### Events Published
Session events â†’ `session.exchange` with keys: `session.requested/accepted/rejected/cancelled/completed`
Review events â†’ `review.exchange` with key: `review.submitted`

---

## 7. Notification Service

**Purpose:** Listens to RabbitMQ events â†’ saves notifications â†’ pushes via WebSocket.

### Entity
**Notification** (`notifications.notifications`) â€” userId, type, title, message, data (TEXT), isRead

### Consumer Layer (RabbitMQ Listeners)
- **SessionEventConsumer**: 5 handlers (requestedâ†’notify mentor, accepted/rejected/completedâ†’notify learner, cancelledâ†’notify both)
- **MentorEventConsumer**: approvedâ†’notify user, rejectedâ†’notify user with reason
- **ReviewEventConsumer**: submittedâ†’notify mentor with star count

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
Event published â†’ RabbitMQ â†’ Consumer receives â†’ NotificationService.createAndPush()
  â†’ Saves to DB + WebSocketService.pushToUser() â†’ Real-time push to frontend
```

---

## 8. API Gateway, Eureka, Config Server

### API Gateway (port 8080)
- Routes requests by path pattern to services via Eureka (`lb://service-name`)
- **JwtAuthenticationFilter**: extracts JWT â†’ validates â†’ adds X-User-Id/Email/Role headers
- Auth & Skill routes: **no** JWT required. All others: JWT required.
- WebSocket route: `lb:ws://notification-service` for `/ws/**`
- **Swagger UI**: Aggregated at `http://localhost:8080/swagger-ui.html` â€” proxies `/v3/api-docs` from each service. Individual service ports are NOT exposed.

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

**Why NOT Records for Entities?** JPA needs mutable objects (setters), no-args constructor, proxy-based lazy loading â€” all incompatible with records.

---

## 10. Why WebSocket?

**Problem:** Without WebSocket, frontend must **poll** every few seconds â†’ wasteful.

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

**Problem:** Without broker, SessionService must directly call NotificationService â†’ tight coupling, failure cascades.

**Solution:** Publish event to RabbitMQ â†’ it delivers to consumers asynchronously.

| Benefit | Explanation |
|---------|------------|
| Loose coupling | Publisher doesn't know about consumers |
| Resilience | If consumer is down, messages queue up |
| Scalability | Multiple consumers can share a queue |
| Extensibility | New consumer = zero changes to publisher |
| Async | Main operation returns immediately |

**Topic Exchange:** Routes by routing key patterns. `session.requested` â†’ `notification.session.requested.queue`.

---

## 12. End-to-End Flow

```
1. Register â†’ Auth saves user + sends OTP email
2. Verify OTP â†’ Auth marks isVerified=true
3. Login â†’ Auth returns JWT (15min access + 7day refresh)
4. Create skills â†’ Skill Service saves skill catalog
5. Update profile â†’ User Service creates Profile
6. Apply as mentor â†’ User Service creates PENDING MentorProfile
7. Pay mentor fee â†’ Payment Service creates Razorpay order, verifies signature
   â†’ On SUCCESS: Payment Service publishes payment.business.action event
   â†’ User Service consumes event â†’ sets status=APPROVED
   â†’ Feign call to Auth: role=ROLE_MENTOR
   â†’ RabbitMQ event â†’ Notification Service: "Approved!" push
8. Book session â†’ Session Service validates + saves REQUESTED session
   â†’ RabbitMQ event â†’ Notification: "New request" to mentor
9. Mentor accepts â†’ status=ACCEPTED â†’ Notification to learner
10. Mentor completes â†’ status=COMPLETED â†’ Notification to learner
11. Learner reviews â†’ Review saved + RabbitMQ â†’ Notification to mentor
12. Check notifications â†’ REST API or real-time via WebSocket
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
| **Graceful degradation** | **Redis down â†’ fallback to DB, zero data loss** |
| **Event-driven cache sync** | **Cross-service cache invalidation via RabbitMQ** |
| State machine enum | Domain-level enforcement of valid transitions |
| Soft references (userId as Long) | No cross-DB foreign keys in microservices |
| Payment as dedicated service | **Separation of Concerns:** payment logic, Razorpay SDK, and saga orchestration are isolated in their own service with event-driven coordination |
| Event-driven Saga | Payment â†’ RabbitMQ â†’ User Service decouples payment lifecycle from business actions, enabling independent deployment/scaling |
| @CreatedDate/@LastModifiedDate | Auto timestamp management |
| Profile completeness % | Gamification (5 fields Ã— 20%) |
| Builder pattern (Lombok) | Fluent, readable object construction |
| Self-referencing entities | Tree structures: Discussion threads, Category hierarchy |

---

## 14. Viva Q&A

**Q: What is a microservice?** A: Small, independently deployable service with its own DB. Communicates via REST + message queues.

**Q: Why microservices?** A: Independent scaling, fault isolation, team independence, technology flexibility.

**Q: Why Eureka?** A: Service discovery. Services find each other by name, not hardcoded URLs. Enables load balancing.

**Q: Why API Gateway?** A: Single entry point. Centralized auth, routing, CORS. Clients don't need to know every service address.

**Q: How does JWT work?** A: Server signs token with secret â†’ client sends in header â†’ Gateway validates signature â†’ extracts user info â†’ forwards to services.

**Q: Why BCrypt?** A: One-way hash with salt. Even if DB is breached, passwords can't be reversed.

**Q: Why OTP attempts limit?** A: Brute-force protection. After 5 wrong attempts, OTP is invalidated.

**Q: Why userId is Long not FK?** A: Cross-service FKs impossible (different DBs). Soft reference.

**Q: What is self-referencing ManyToOne?** A: Discussion.parent â†’ Discussion. Creates tree for threaded replies.

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

**Q: How does Gateway route?** A: `lb://service-name` â†’ asks Eureka for address â†’ forwards request.

**Q: Why use `lb://service-name` instead of `spring.cloud.gateway.discovery.locator.enabled=true`?**
A: Four reasons:
1. **Clean URLs**: The locator creates ugly URLs tightly coupled to service names (e.g., `/USER-SERVICE/api/users`). We want clean, semantic paths (`/api/users`).
2. **Security Control**: We need to selectively apply our `JwtAuthenticationFilter`. Auth Service is public, User Service is secured. Auto-discovery exposes *everything* uniformly, making selective filtering difficult.
3. **Merged Routes**: We map `/api/mentors/**` and `/api/groups/**` to the User Service. Auto-discovery cannot infer this logical routing.
4. **Encapsulation**: The frontend shouldn't know our internal microservice names. The Gateway hides our architecture.

> **Viva Tip:** For any feature, explain: WHAT it does â†’ WHY we chose it â†’ HOW it works â†’ ALTERNATIVES considered.

---

## 15. CQRS + Redis (Cache Details)

### ðŸ§© Why CQRS?
**Command Query Responsibility Segregation** separates operations that **change state** (Commands) from those that **read state** (Queries).
- **Command side**: Optimizes for write performance, consistency, and execution of business logic (PostgreSQL).
- **Query side**: Optimizes for read performance using indexed searches and aggressive Redis caching.
- **Benefit**: SkillSync is read-heavy (~80:20 ratio). CQRS allows us to scale the Query side independently and apply complex caching logic without complicating the write logic.

### ðŸ¢ The 3 Cache Layers

| Layer | Type | Implementation | Purpose |
|-------|------|----------------|---------|
| **L1** | **In-Memory Application** | `ConcurrentHashMap` + `ReentrantLock` | **Stampede Protection**: Prevents multiple threads from hitting the DB at once for the same missing key. |
| **L2** | **Distributed Cache** | **Redis 7.2** (In-Memory) | **Global Shared Cache**: High-speed data sharing across all microservice instances. |
| **L3** | **Persistence** | **PostgreSQL** | **Source of Truth**: Used as fallback during a cache miss or if Redis is unavailable. |

### ðŸ› ï¸ How we use Caching â€” Scenarios

1.  **Cache-Aside (Lazy Loading)**:
    - `QueryService.getOrLoad()`: Application checks Redis â†’ (miss?) â†’ gets from PostgreSQL â†’ updates Redis â†’ returns.
2.  **Explicit Invalidation (Write-through Eviction)**:
    - `CommandService.save/delete()`: Main operation updates PostgreSQL â†’ `cacheService.evict(key)` or `evictByPattern(pattern)`.
3.  **Cross-Service Cache Sync (Event-Driven)**:
    - **Scenario**: A review is submitted in `Session Service`.
    - **Sync**: `Session Service` publishes `review.submitted` event â†’ `User Service` consumes it â†’ updates Mentor's `avgRating` in DB â†’ **evicts** Mentor's profile from Redis. This ensures the profile reflects the latest rating immediately across services.

### ðŸ›¡ï¸ Cache Safety Strategies

| Strategy | Purpose | Implementation |
|----------|---------|----------------|
| **Stampede Protection** | Avoids overwhelming DB on popular key expiration | `ReentrantLock` in `CacheService` blocks redundant DB hits. |
| **Penetration Protection** | Avoids DB hits for non-existent keys (e.g., searching for missing ID) | **Null Sentinel**: Caches a `__NULL__` string for 60 seconds. |
| **Stampede/Penetration Combo** | `getOrLoad` combined logic | Ensures only 1 thread loads and non-existent IDs are also stopped. |
| **Key Versioning** | Avoids "poisoning" cache during schema updates | Prefixing all keys with `v1:` (e.g., `v1:user:profile:100`). |
| **Graceful Degradation** | Prevents service crash if Redis fails | Try-catch blocks wrap all Redis operations; fallback to DB. |

---

## 16. CQRS + Redis Q&A (Viva Questions)

**Q: What is the main reason for using Redis in SkillSync?**  
A: To reduce database load and latency for read-heavy operations like viewing mentor profiles, fetching skills, and getting recent notifications.

**Q: Explain the "Cache-Aside" pattern.**  
A: It means the *application* is responsible for reading/writing the cache. The app first tries to read from the cache; if it misses, it reads from the DB and manually updates the cache for future requests.

**Q: What happens if 1000 users request a mentor profile at the exact moment the cache expires?**  
A: This is called a **Cache Stampede**. We handle this using **Stampede Protection** (locking) in `CacheService`. Only the first request is allowed to hit the DB; others wait for the lock and then read from the newly refreshed cache.

**Q: How do you prevent hitting the DB repeatedly for a Skill ID that doesn't exist?**  
A: This is **Cache Penetration**. We handle it using a **Null Sentinel**. If the DB returns null, we store a special string (`__NULL__`) in Redis with a short TTL. Subsequent requests for that non-existent ID read the sentinel from the cache instead of hitting the DB.

**Q: Why not use Hibernate L1/L2 cache instead of a custom Redis wrapper?**  
A: Hibernate cache is limited to a single JVM. In a microservices environment, we need a **distributed cache** (Redis) that is shared across all instances to maintain consistency.

**Q: How do you handle cache consistency when data changes?**  
A: We use **Eventual Consistency** with **Explicit Eviction**. Every command (Write) explicitly calls `evict()` in the same transaction or immediately after. For cross-service changes (like ratings), we use **RabbitMQ events** to trigger evictions in other services.

**Q: Why use `SCAN` instead of `KEYS` to find keys to delete?**  
A: The `KEYS` command is blocking and can freeze Redis in production. `SCAN` is an iterative, non-blocking command that allows us to find and delete keys without affecting performance.

**Q: What is a "Versioned Key"?**  
A: We prefix keys with `v1:`. If we change the DTO structure in a new deployment, we can update the prefix to `v2:`. This prevents the new code from reading incompatible data cached by the old version.

**Q: Is Redis used for session management or just data caching?**  
A: In SkillSync, it's used for **data caching** (Profiles, Skills, etc.) and potentially for **rate limiting** or **OTP storage** (though OTPs are in Postgres for durability in this current version). Session info is in Postgres for persistence.

**Q: Can you explain the TTL (Time-To-Live) strategy you chose?**  
A: We use **layered TTLs** based on data volatility:
- **Skills**: 1 hour (rarely changes).
- **Mentor Profiles**: 10 mins (changes occasionally).
- **Session Status**: 5 mins (high frequency updates).
- **Null Sentinels**: 1 min (short duration to prevent abuse).

---

## 17. Frontend & React Q&A

**Q: Why React 18 instead of Angular/Vue?** A: React offers a massive ecosystem, concurrent rendering features, and integrates perfectly with our chosen stack (TypeScript, Redux Toolkit, TanStack Query). The component-based atomic design suits our complex dashboards well.

**Q: What is Redux Toolkit used for?** A: Global client state. Specifically, we use it for keeping track of the Auth State (JWT, refresh token, user role) globally across all components and Router guards.

**Q: Why use TanStack Query alongside Redux?** A: Redux is for synchronous client state (Auth, UI toggles). TanStack Query (React Query) is meant for asynchronous **server state**. It automatically handles caching, background refetching, pagination variables, and loading/error states for our API endpoints, removing hundreds of lines of boilerplate.

**Q: How do you handle JWT Tokens on the frontend?** A: We use an Axios interceptor. The `request` interceptor injects the Bearer token natively. The `response` interceptor watches for 401 Unauthorized errors.

**Q: How does the Silent Token Refresh work?** A: If a 401 is thrown, the Axios interceptor pauses all outgoing API requests into a queue, calls the `/api/auth/refresh` endpoint using the persisted refresh token, updates the Redux store, and then replays the queued requests. This creates a seamless, uninterrupted UX.

**Q: How did you implement Razorpay on the frontend?** A: When a user books a session, we first hit `/api/payments/order` to get an order ID asynchronously. Then we instantiate `new window.Razorpay(options)` which opens the official checkout modal over our app. Upon capturing the `razorpay_signature` in the success handler, we send it to our backend to natively link payment success to the session booking.

**Q: How do you protect routes?** A: We use React Router v6 with custom Wrapper Guards (`AuthGuard`, `GuestGuard`, `RoleGuard`). These evaluate the Redux state before rendering children. If unauthorized, they trigger a `<Navigate to="/login" replace />`.

