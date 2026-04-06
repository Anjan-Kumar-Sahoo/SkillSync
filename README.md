# SkillSync

SkillSync is a production-focused mentor-learning platform with role-based workflows, microservices architecture, and a React frontend.

## What this project demonstrates
- End-to-end product flow: authentication, mentor discovery, session lifecycle, profile, payments
- Microservices patterns: gateway-based auth, service discovery, config server, event-driven messaging
- Reliability patterns: CQRS read/write split, Redis cache, resilient fallbacks, observability stack
- Production deployment practice: Dockerized services and environment-driven configuration

## Architecture at a glance
- Frontend: React + TypeScript + Redux Toolkit + React Query
- Backend: API Gateway + 8 domain/infra services (Spring Boot)
- Infrastructure: PostgreSQL, Redis, RabbitMQ, Zipkin, Prometheus, Grafana, Loki
- Deployment: Docker Compose on EC2

## Roles
- Learner: discover mentors, request sessions, join groups, review completed sessions
- Mentor: manage availability, accept/reject requests, track sessions and profile
- Admin: manage users and platform governance features

## Core flows
- Registration with OTP verification
- Forgot password with OTP reset flow
- Mentor onboarding and payment-linked workflows
- Session request -> accept/reject -> complete -> review

## Current docs map
- Project presentation guide: docs/00_Presentation_Playbook.md
- Project overview: docs/01_Project_Overview_and_Viva_Prep.md
- System and DB architecture: docs/02_System_and_Database_Architecture.md
- Frontend architecture and API contract: docs/03_Frontend_Design_and_API_Contract.md
- Security and auth: docs/04_Security_Auth_and_OAuth.md
- CQRS and Redis: docs/05_CQRS_and_Redis_Caching.md
- Deployment and infra: docs/06_Deployment_DevOps_and_Infrastructure.md
- Testing strategy: docs/07_Testing_and_QA_Strategy.md
- Observability: docs/08_Observability_and_Monitoring.md
- Payment architecture: docs/09_Payment_Implementation_Guide.md
- Production incidents and fixes: docs/10_Production_Readiness_and_Incident_Reports.md
- Frontend implementation details: docs/11_Frontend_Complete_Implementation.md

## UI documentation for presentation
- UI DOCS/BE-ARCHITECTURE.html
- UI DOCS/FE-ARCHITECTURE.html
- UI DOCS/PAYMENT_SAGA.html
- UI DOCS/DEPLOYMENT.html

## Manual deployment workflow (current)
1. Push code to GitHub
2. Build and push changed Docker image(s) to Docker Hub
3. SSH into EC2 and run:
   - git pull
   - docker compose pull
   - docker compose up -d --remove-orphans

## Recent implementation highlights
- Added dedicated reset-password page for OTP + new password
- Upgraded password setup UX with visibility toggle and live constraints
- Removed learner-only mentor search action from mentor context
- Updated profile avatar editing path via avatarUrl persistence
