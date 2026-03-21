# 📄 DOCUMENT 1: PROJECT OVERVIEW

> [!IMPORTANT]
> **Architecture Update (March 2026):** The following services have been merged:
> - **Mentor Service + Group Service → User Service** (port 8082) — mentor onboarding, groups, and user profiles now live in one service
> - **Review Service → Session Service** (port 8085) — reviews and sessions share the same service and database
>
> The original 11-service design below reflects the initial architecture. See `service_architecture_summary.md` for the current 8-service topology.

## SkillSync — Peer Learning & Mentor Matching Platform

---

## 1.1 System Description

SkillSync is a **production-grade, multi-tenant platform** that bridges the gap between knowledge seekers and domain experts. It enables:

- **Real-time mentor discovery** with advanced filtering (skill, rating, experience, price, availability)
- **Structured session booking** with a full lifecycle state machine
- **Peer learning groups** for collaborative knowledge sharing
- **Post-session review & rating** system for quality assurance
- **Event-driven notifications** for real-time updates

The system is built on a **Spring Boot microservices architecture** with an **API Gateway**, **service discovery**, **event-driven messaging via RabbitMQ**, and a **React + TypeScript** frontend.

---

## 1.2 Problem Statement

### The Problem

Learners struggle to find **qualified, available mentors** who match their specific skill requirements. Existing platforms lack:

1. **Granular discovery** — No way to filter mentors by a combination of skill, price, experience, and real-time availability
2. **Session lifecycle management** — No structured workflow from request → acceptance → completion → review
3. **Peer learning** — No integrated mechanism for group-based collaborative learning
4. **Quality assurance** — No post-session rating system to surface high-quality mentors
5. **Scalability** — Monolithic architectures fail under high user load

### The Solution

SkillSync addresses every gap with a purpose-built, microservices-based platform that handles the full mentoring lifecycle — from discovery to review — while supporting horizontal scalability for millions of concurrent users.

---

## 1.3 User Roles

### Role Matrix

| Capability | ROLE_LEARNER | ROLE_MENTOR | ROLE_ADMIN |
|---|:---:|:---:|:---:|
| Register / Login | ✅ | ✅ | ✅ |
| Create / Edit Profile | ✅ | ✅ | ✅ |
| Search Mentors | ✅ | ❌ | ✅ |
| Request Sessions | ✅ | ❌ | ❌ |
| Accept / Reject Sessions | ❌ | ✅ | ❌ |
| Set Availability | ❌ | ✅ | ❌ |
| Create Mentor Profile | ❌ | ✅ | ❌ |
| Rate Mentors | ✅ | ❌ | ❌ |
| Join Peer Groups | ✅ | ✅ | ❌ |
| Create Peer Groups | ✅ | ✅ | ❌ |
| Manage Users | ❌ | ❌ | ✅ |
| Approve Mentors | ❌ | ❌ | ✅ |
| Moderate Groups | ❌ | ❌ | ✅ |
| View Analytics | ❌ | ❌ | ✅ |

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
  User (ROLE_LEARNER) → Submits Mentor Application
    → Application includes: bio, experience, hourly_rate, skills[], certifications
    → Status: PENDING
  Admin reviews application
    → APPROVED → User gains ROLE_MENTOR, profile activated
    → REJECTED → User notified with rejection reason
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

  ┌──────────┐     Learner requests    ┌───────────┐
  │          │ ──────────────────────▶  │           │
  │  [INIT]  │                         │ REQUESTED │
  │          │                         │           │
  └──────────┘                         └─────┬─────┘
                                             │
                              ┌──────────────┼──────────────┐
                              │              │              │
                        Mentor accepts  Mentor rejects  Learner cancels
                              │              │              │
                              ▼              ▼              ▼
                        ┌──────────┐   ┌──────────┐   ┌──────────┐
                        │ ACCEPTED │   │ REJECTED │   │CANCELLED │
                        └────┬─────┘   └──────────┘   └──────────┘
                             │
                    ┌────────┼────────┐
                    │                 │
              Session done      Either cancels
                    │                 │
                    ▼                 ▼
              ┌──────────┐     ┌──────────┐
              │COMPLETED │     │CANCELLED │
              └──────────┘     └──────────┘
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
- Review includes: star rating (1–5), written comment
- One review per session (enforced)
- Mentor's average rating recalculated on each new review
- Reviews displayed on mentor profile (paginated, newest first)

### Feature 9: Notifications
- Event-driven via RabbitMQ
- Notification types:
  - `SESSION_REQUESTED` — Mentor notified of new request
  - `SESSION_ACCEPTED` / `SESSION_REJECTED` — Learner notified
  - `SESSION_REMINDER` — Both parties, 24h & 1h before
  - `MENTOR_APPROVED` / `MENTOR_REJECTED` — Applicant notified
  - `NEW_REVIEW` — Mentor notified of new review
  - `GROUP_JOINED` — Group owner notified
- Delivery: In-app (WebSocket) + email (future)
- Read/unread status tracking

---

## 1.5 Business Goals

| Goal | Metric | Target |
|---|---|---|
| User Acquisition | Monthly active users | 100K+ within Year 1 |
| Mentor Quality | Avg mentor rating | ≥ 4.0 / 5.0 |
| Session Completion | Booking → Completed rate | ≥ 75% |
| Platform Reliability | Uptime | 99.9% |
| Latency | API p95 response time | < 200ms |
| Scalability | Concurrent users | 50K+ |

---

## 1.6 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           CLIENT LAYER                                  │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │              React + TypeScript SPA                              │    │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐   │    │
│  │  │ Auth     │ │ Mentor   │ │ Session  │ │ Admin Dashboard  │   │    │
│  │  │ Module   │ │ Discovery│ │ Booking  │ │                  │   │    │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘   │    │
│  └─────────────────────────┬───────────────────────────────────────┘    │
│                            │ HTTPS / WSS                                │
└────────────────────────────┼────────────────────────────────────────────┘
                             │
┌────────────────────────────┼────────────────────────────────────────────┐
│                    GATEWAY LAYER                                        │
│  ┌─────────────────────────┴───────────────────────────────────────┐    │
│  │              Spring Cloud Gateway                                │    │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐   │    │
│  │  │ JWT      │ │ Rate     │ │ Route    │ │ Load Balancing   │   │    │
│  │  │ Validate │ │ Limiting │ │ Config   │ │                  │   │    │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘   │    │
│  └─────────────────────────┬───────────────────────────────────────┘    │
│                            │                                            │
└────────────────────────────┼────────────────────────────────────────────┘
                             │
┌────────────────────────────┼────────────────────────────────────────────┐
│                    SERVICE LAYER                                        │
│                            │                                            │
│  ┌────────────┐ ┌──────────────────────┐ ┌──────────────┐              │
│  │ Auth       │ │ User Service :8082   │ │ Skill        │              │
│  │ Service    │ │ (+ Mentor + Group)   │ │ Service      │              │
│  │ :8081      │ └──────────────────────┘ │ :8084        │              │
│  └────────────┘                          └──────────────┘              │
│                                                                        │
│  ┌──────────────────────┐                ┌──────────────┐              │
│  │ Session Service :8085│                │ Notification │              │
│  │ (+ Review)           │                │ Service      │              │
│  └──────────────────────┘                │ :8088        │              │
│                                          └──────────────┘              │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                    Eureka Service Discovery (:8761)              │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                             │
┌────────────────────────────┼────────────────────────────────────────────┐
│                    DATA & MESSAGING LAYER                               │
│                                                                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                  │
│  │ Auth DB  │ │ User DB  │ │Mentor DB │ │Skill DB  │  (PostgreSQL)    │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                  │
│  │Session DB│ │ Group DB │ │Review DB │ │Notif DB  │  (PostgreSQL)    │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘                  │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                    RabbitMQ Message Broker                        │    │
│  │  Exchanges: session.exchange, mentor.exchange, notification.*    │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 1.7 Key Workflows

### Workflow 1: Mentor Discovery

```
┌──────────┐     GET /api/mentors/search         ┌──────────────┐
│          │ ──────────────────────────────────▶  │              │
│ Learner  │     ?skill=Java&minRating=4          │ API Gateway  │
│ (React)  │     &maxPrice=50&page=0              │              │
│          │ ◀──────────────────────────────────  │              │
└──────────┘     Paginated mentor list            └──────┬───────┘
                                                         │
                                                         ▼
                                                  ┌──────────────┐
                                                  │   Mentor     │
                                                  │   Service    │
                                                  │              │
                                                  │ 1. Parse     │
                                                  │    filters   │
                                                  │ 2. Query DB  │
                                                  │    (indexed) │
                                                  │ 3. Join with │
                                                  │    skills    │
                                                  │ 4. Paginate  │
                                                  │ 5. Return    │
                                                  └──────────────┘
```

### Workflow 2: Session Booking Flow

```
Learner                API Gateway         Session Service       Mentor Service       RabbitMQ        Notification Service
  │                        │                     │                     │                  │                    │
  │ POST /api/sessions     │                     │                     │                  │                    │
  │ {mentorId, date, topic}│                     │                     │                  │                    │
  │───────────────────────▶│                     │                     │                  │                    │
  │                        │ Validate JWT        │                     │                  │                    │
  │                        │ Route to service    │                     │                  │                    │
  │                        │────────────────────▶│                     │                  │                    │
  │                        │                     │ Validate mentor     │                  │                    │
  │                        │                     │ exists & available  │                  │                    │
  │                        │                     │────────────────────▶│                  │                    │
  │                        │                     │◀────────────────────│                  │                    │
  │                        │                     │                     │                  │                    │
  │                        │                     │ Check time conflict │                  │                    │
  │                        │                     │ Create session      │                  │                    │
  │                        │                     │ (status=REQUESTED)  │                  │                    │
  │                        │                     │                     │                  │                    │
  │                        │                     │ Publish event       │                  │                    │
  │                        │                     │─────────────────────────────────────▶  │                    │
  │                        │                     │                     │  SESSION_REQUESTED│                    │
  │                        │                     │                     │                  │───────────────────▶│
  │                        │                     │                     │                  │                    │
  │                        │  201 Created         │                     │                  │  Notify mentor     │
  │◀───────────────────────│◀────────────────────│                     │                  │  (WebSocket)       │
  │                        │                     │                     │                  │                    │
```

### Workflow 3: Mentor Approval Flow

```
User                 API Gateway         Mentor Service          RabbitMQ        Notification Service
  │                      │                     │                     │                    │
  │ POST /api/mentors    │                     │                     │                    │
  │ /apply               │                     │                     │                    │
  │─────────────────────▶│                     │                     │                    │
  │                      │────────────────────▶│                     │                    │
  │                      │                     │ Create mentor       │                    │
  │                      │                     │ (status=PENDING)    │                    │
  │◀─────────────────────│◀────────────────────│                     │                    │
  │                      │                     │                     │                    │
  │                      │                     │                     │                    │

Admin                API Gateway         Mentor Service          RabbitMQ        Notification Service
  │                      │                     │                     │                    │
  │ PUT /api/mentors     │                     │                     │                    │
  │ /{id}/approve        │                     │                     │                    │
  │─────────────────────▶│                     │                     │                    │
  │                      │ Validate ROLE_ADMIN │                     │                    │
  │                      │────────────────────▶│                     │                    │
  │                      │                     │ Update status       │                    │
  │                      │                     │ (APPROVED)          │                    │
  │                      │                     │ Update user role    │                    │
  │                      │                     │ Publish event       │                    │
  │                      │                     │────────────────────▶│                    │
  │                      │                     │                     │ MENTOR_APPROVED    │
  │                      │                     │                     │───────────────────▶│
  │◀─────────────────────│◀────────────────────│                     │                    │
  │                      │                     │                     │   Notify user      │
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
| Database | PostgreSQL | Per-service relational storage |
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
