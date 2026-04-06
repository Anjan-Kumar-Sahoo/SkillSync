# SkillSync Presentation Playbook

Last updated: 2026-04-06
Audience: Viva panel, technical review, architecture walkthrough

## 1. One-line Pitch
SkillSync is a role-based mentor-learning platform built with Spring Boot microservices and a React frontend, designed for secure authentication, reliable session workflows, and production-ready observability.

## 2. Current System Snapshot
- Frontend: React + TypeScript + Redux Toolkit + React Query
- Backend: Spring Boot microservices behind API Gateway
- Infra: Docker Compose on EC2, Redis, RabbitMQ, PostgreSQL, Prometheus, Grafana, Loki, Zipkin
- Auth: JWT via Gateway-propagated user headers
- Roles: Learner, Mentor, Admin

## 3. Service Topology (9 backend services)
- api-gateway
- auth-service
- user-service
- skill-service
- session-service
- payment-service
- notification-service
- config-server
- eureka-server

## 4. Core User Flows
### 4.1 Registration
Email -> OTP verify -> account active -> login

### 4.2 Forgot Password
Forgot password -> reset OTP email -> reset-password page -> OTP + new password -> login

### 4.3 Session Lifecycle
Learner requests -> Mentor accepts/rejects -> Session completes -> Learner review

### 4.4 Mentor Approval Payment
User initiates mentor/payment flow -> payment verify -> event-driven business action

## 5. Concepts to Explain Clearly
- API Gateway handles token verification and forwards X-User-Id/X-User-Role
- CQRS pattern for read/write separation in services
- Redis cache-aside with graceful DB fallback
- RabbitMQ for async events and decoupled service communication
- Transactional reliability in payment workflow

## 6. Frontend Architecture Summary
- Route-driven page modules
- Reusable layout system (PageLayout, AuthLayout, ProtectedRoute)
- Redux for auth/session UI state
- React Query for server state, mutation, and cache invalidation
- Axios interceptors for auth and error flow normalization

## 7. Recent Updates (presentation-relevant)
- Dedicated reset-password page added
- Setup password now uses a single password field with visibility toggle and live constraints
- Mentor UI no longer shows learner-only mentor-search actions in session empty state
- Profile update now supports avatarUrl persistence path

## 8. Deployment Mode Right Now
CI minutes exhausted -> manual deployment workflow in use:
1. Push code to GitHub
2. Build and push changed Docker images to Docker Hub
3. SSH EC2 -> git pull -> docker compose pull -> docker compose up -d

## 9. 10-minute Presentation Sequence
- Minute 1: Problem and product
- Minute 2: Roles and business flows
- Minute 3-4: Backend architecture and service boundaries
- Minute 5: Security model and auth propagation
- Minute 6: Frontend architecture and route/state design
- Minute 7: Payment and event-driven reliability
- Minute 8: Observability and production readiness
- Minute 9: Demo (forgot password + profile + mentor dashboard)
- Minute 10: Challenges, fixes, and roadmap

## 10. Q and A Fast Answers
- Why microservices? Domain isolation, independent scaling, clear ownership
- Why Gateway auth offload? Centralized security and consistent header contracts
- Why Redis + CQRS? Faster reads and safer write isolation
- Why event-driven? Loose coupling and resilient async workflows
- Why Docker Compose on EC2? Simple production topology for current project stage
