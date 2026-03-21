# SkillSync Backend — Complete Testing Guide

> [!IMPORTANT]
> **Architecture Update (March 2026):** The following services have been merged:
> - **Mentor Service + Group Service → User Service** (port 8082)
> - **Review Service → Session Service** (port 8085)
>
> You no longer need to run or test these standalone services. Their APIs are still available via the API Gateway using the same routes, but they are serviced by the merged backend services.

> **PostgreSQL:** `postgres` / `root` | **RabbitMQ:** `guest` / `guest`

---

## 📋 STEP 1: Prerequisites

### 1.1 Create PostgreSQL Databases

Open **psql** or **pgAdmin** and run:

```sql
CREATE DATABASE skillsync_auth;
CREATE DATABASE skillsync_user;
CREATE DATABASE skillsync_mentor;
CREATE DATABASE skillsync_skill;
CREATE DATABASE skillsync_session;
CREATE DATABASE skillsync_group;
CREATE DATABASE skillsync_review;
CREATE DATABASE skillsync_notification;
```

Also create the schemas inside each database:

```sql
-- Connect to each database and create schema
\c skillsync_auth
CREATE SCHEMA IF NOT EXISTS auth;

\c skillsync_user
CREATE SCHEMA IF NOT EXISTS users;

\c skillsync_mentor
CREATE SCHEMA IF NOT EXISTS mentors;

\c skillsync_skill
CREATE SCHEMA IF NOT EXISTS skills;

\c skillsync_session
CREATE SCHEMA IF NOT EXISTS sessions;

\c skillsync_group
CREATE SCHEMA IF NOT EXISTS groups;

\c skillsync_review
CREATE SCHEMA IF NOT EXISTS reviews;

\c skillsync_notification
CREATE SCHEMA IF NOT EXISTS notifications;
```

### 1.2 Install & Start RabbitMQ

Download from https://www.rabbitmq.com/download.html and start the service.  
Management UI: http://localhost:15672 (guest/guest)

> [!NOTE]
> If you don't have RabbitMQ, services that use it (Mentor, Session, Review, Notification) will still start but event publishing will fail silently. Auth, User, Skill, and Group services work fine without it.

---

## 📋 STEP 2: Start Services (in order)

Open **separate terminals** for each service and run:

```powershell
# Terminal 1 — Eureka Server (MUST start first, wait until ready)
cd f:\SkillSync\eureka-server
.\mvnw spring-boot:run

# Terminal 2 — Config Server
cd f:\SkillSync\config-server
.\mvnw spring-boot:run

# Terminal 3 — API Gateway
cd f:\SkillSync\api-gateway
.\mvnw spring-boot:run

# Terminal 4 — Auth Service
cd f:\SkillSync\auth-service
.\mvnw spring-boot:run

# Terminal 5 — User Service
cd f:\SkillSync\user-service
.\mvnw spring-boot:run

# Terminal 6 — Skill Service
cd f:\SkillSync\skill-service
.\mvnw spring-boot:run

# Terminal 7 — Mentor Service
cd f:\SkillSync\mentor-service
.\mvnw spring-boot:run

# Terminal 8 — Session Service
cd f:\SkillSync\session-service
.\mvnw spring-boot:run

# Terminal 9 — Group Service
cd f:\SkillSync\group-service
.\mvnw spring-boot:run

# Terminal 10 — Review Service
cd f:\SkillSync\review-service
.\mvnw spring-boot:run

# Terminal 11 — Notification Service
cd f:\SkillSync\notification-service
.\mvnw spring-boot:run
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

#### Verify OTP (Check your console/logs for the code)
```bash
curl -X POST http://localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "learner@test.com",
    "otp": "123456" 
  }'
```
> [!NOTE]
> For testing, the OTP is printed in the `auth-service` console logs. In production, check the email.
```

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

# Also verify OTP for mentor as shown above!
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

Then manually promote to admin (direct call to Auth Service):  
```bash
curl -X PUT "http://localhost:8081/api/auth/users/3/role?role=ROLE_ADMIN"
```

#### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "learner@test.com",
    "password": "password123"
  }'
```

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

### 🎓 3.4 MENTOR SERVICE

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

### 📅 3.5 SESSION SERVICE

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
    "sessionDate": "2026-03-20T10:00:00",
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
    "sessionDate": "2026-03-21T14:00:00",
    "durationMinutes": 45
  }'

# Reject it
curl -X PUT "http://localhost:8080/api/sessions/2/reject?reason=Schedule%20conflict" \
  -H "Authorization: Bearer <MENTOR_USER_ACCESS_TOKEN>" \
  -H "X-User-Id: 2"
```

---

### ⭐ 3.6 REVIEW SERVICE (Session must be COMPLETED first)

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

### 👥 3.7 GROUP SERVICE

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

## 📋 STEP 4: End-to-End Flow (Recommended Test Order)

Follow this order for the complete happy path:

```
1. Register learner@test.com
2. Verify OTP for learner (check console logs)
3. Register mentor@test.com
4. Verify OTP for mentor (check console logs)
5. Register admin@test.com
6. Promote admin (PUT /api/auth/users/3/role?role=ROLE_ADMIN)
7. Create 4 skills (Java, Spring Boot, React, Python)
8. Update learner profile + add skills
9. Mentor applies (POST /api/mentors/apply)
10. Admin approves mentor (PUT /api/mentors/1/approve)
11. Login mentor again (to get updated ROLE_MENTOR token)
12. Add mentor availability
13. Learner books session with mentor
14. Mentor accepts session
15. Mentor completes session
16. Learner submits review
17. Check mentor rating summary
18. Create a group and post discussions
19. Check notifications (mentor should have: approval + session request + review alerts)
```

---

## 📋 STEP 5: Direct Service Testing (bypass gateway)

You can also test services directly (bypassing JWT):

| Service | Direct URL |
|---------|-----------|
| Eureka Dashboard | http://localhost:8761 |
| Auth Service | http://localhost:8081/api/auth/... |
| User Service | http://localhost:8082/api/users/... |
| Mentor Service | http://localhost:8083/api/mentors/... |
| Skill Service | http://localhost:8084/api/skills/... |
| Session Service | http://localhost:8085/api/sessions/... |
| Group Service | http://localhost:8086/api/groups/... |
| Review Service | http://localhost:8087/api/reviews/... |
| Notification Service | http://localhost:8088/api/notifications/... |

When calling direct, pass `X-User-Id` header manually:
```bash
curl http://localhost:8082/api/users/me -H "X-User-Id: 1"
```

---

## 🔧 Troubleshooting

| Issue | Solution |
|-------|----------|
| Service can't start | Check if the PostgreSQL database exists and the schema is created |
| `Connection refused` to Eureka | Start Eureka Server first and wait 30 seconds |
| `401 Unauthorized` via Gateway | Check the JWT token is valid and not expired (15 min lifetime) |
| RabbitMQ connection failed | Make sure RabbitMQ is running on port 5672 |
| `Table not found` errors | JPA `ddl-auto=update` creates tables automatically, but schemas must exist |
| Port already in use | Kill the process: `netstat -ano | findstr :PORT` then `taskkill /PID <PID> /F` |
