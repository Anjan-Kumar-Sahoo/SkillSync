# 📄 DOCUMENT 2: DATABASE + BACKEND DESIGN

## SkillSync — Database & Microservices Architecture

---

## 2.1 Database Design

### Database-per-Service Strategy

Each microservice owns its dedicated PostgreSQL database. No cross-service joins — all inter-service communication happens via REST APIs or RabbitMQ events.

| Service | Database | Schema |
|---|---|---|
| Auth Service | `skillsync_auth` | `auth` |
| User Service | `skillsync_user` | `users` |
| Mentor Service | `skillsync_mentor` | `mentors` |
| Skill Service | `skillsync_skill` | `skills` |
| Session Service | `skillsync_session` | `sessions` |
| Group Service | `skillsync_group` | `groups` |
| Review Service | `skillsync_review` | `reviews` |
| Notification Service | `skillsync_notification` | `notifications` |

---

### 2.1.1 Table Definitions

#### Auth Service — `skillsync_auth`

```sql
CREATE TABLE auth.users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'ROLE_LEARNER',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE auth.refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token           VARCHAR(512) NOT NULL UNIQUE,
    expires_at      TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_users_email ON auth.users(email);
CREATE INDEX idx_refresh_tokens_user_id ON auth.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON auth.refresh_tokens(token);
```

#### User Service — `skillsync_user`

```sql
CREATE TABLE users.profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE,  -- References auth.users.id (cross-service)
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    bio             TEXT,
    avatar_url      VARCHAR(500),
    phone           VARCHAR(20),
    location        VARCHAR(200),
    profile_complete_pct INT DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users.user_skills (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    skill_id        UUID NOT NULL,  -- References skills.skills.id (cross-service)
    proficiency     VARCHAR(20) DEFAULT 'BEGINNER', -- BEGINNER, INTERMEDIATE, ADVANCED
    UNIQUE(user_id, skill_id)
);

-- Indexes
CREATE INDEX idx_profiles_user_id ON users.profiles(user_id);
CREATE INDEX idx_user_skills_user_id ON users.user_skills(user_id);
CREATE INDEX idx_user_skills_skill_id ON users.user_skills(skill_id);
```

#### Mentor Service — `skillsync_mentor`

```sql
CREATE TABLE mentors.mentor_profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE,
    bio             TEXT NOT NULL,
    experience_years INT NOT NULL DEFAULT 0,
    hourly_rate     DECIMAL(10,2) NOT NULL,
    avg_rating      DECIMAL(3,2) DEFAULT 0.00,
    total_reviews   INT DEFAULT 0,
    total_sessions  INT DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, SUSPENDED
    rejection_reason TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mentors.mentor_skills (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentor_id       UUID NOT NULL REFERENCES mentors.mentor_profiles(id) ON DELETE CASCADE,
    skill_id        UUID NOT NULL,
    UNIQUE(mentor_id, skill_id)
);

CREATE TABLE mentors.availability_slots (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentor_id       UUID NOT NULL REFERENCES mentors.mentor_profiles(id) ON DELETE CASCADE,
    day_of_week     INT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6), -- 0=Sunday
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    is_active       BOOLEAN DEFAULT TRUE,
    CHECK (end_time > start_time)
);

-- Indexes
CREATE INDEX idx_mentor_profiles_user_id ON mentors.mentor_profiles(user_id);
CREATE INDEX idx_mentor_profiles_status ON mentors.mentor_profiles(status);
CREATE INDEX idx_mentor_profiles_rating ON mentors.mentor_profiles(avg_rating DESC);
CREATE INDEX idx_mentor_profiles_rate ON mentors.mentor_profiles(hourly_rate);
CREATE INDEX idx_mentor_skills_mentor_id ON mentors.mentor_skills(mentor_id);
CREATE INDEX idx_mentor_skills_skill_id ON mentors.mentor_skills(skill_id);
CREATE INDEX idx_availability_mentor_day ON mentors.availability_slots(mentor_id, day_of_week);
```

#### Skill Service — `skillsync_skill`

```sql
CREATE TABLE skills.skills (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL UNIQUE,
    category        VARCHAR(100) NOT NULL,
    description     TEXT,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE skills.categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL UNIQUE,
    parent_id       UUID REFERENCES skills.categories(id),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_skills_name ON skills.skills(name);
CREATE INDEX idx_skills_category ON skills.skills(category);
CREATE INDEX idx_skills_name_trgm ON skills.skills USING GIN (name gin_trgm_ops); -- Fuzzy search
```

#### Session Service — `skillsync_session`

```sql
CREATE TABLE sessions.sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentor_id       UUID NOT NULL,
    learner_id      UUID NOT NULL,
    topic           VARCHAR(255) NOT NULL,
    description     TEXT,
    session_date    TIMESTAMP NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 60,
    meeting_link    VARCHAR(500),
    status          VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    cancel_reason   TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_status CHECK (status IN ('REQUESTED','ACCEPTED','REJECTED','COMPLETED','CANCELLED')),
    CONSTRAINT chk_duration CHECK (duration_minutes BETWEEN 15 AND 240)
);

-- Indexes
CREATE INDEX idx_sessions_mentor_id ON sessions.sessions(mentor_id);
CREATE INDEX idx_sessions_learner_id ON sessions.sessions(learner_id);
CREATE INDEX idx_sessions_status ON sessions.sessions(status);
CREATE INDEX idx_sessions_date ON sessions.sessions(session_date);
CREATE INDEX idx_sessions_mentor_date ON sessions.sessions(mentor_id, session_date); -- Conflict detection
```

#### Group Service — `skillsync_group`

```sql
CREATE TABLE groups.learning_groups (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    max_members     INT DEFAULT 50,
    created_by      UUID NOT NULL,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE groups.group_members (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id        UUID NOT NULL REFERENCES groups.learning_groups(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'MEMBER', -- OWNER, ADMIN, MEMBER
    joined_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(group_id, user_id)
);

CREATE TABLE groups.group_skills (
    group_id        UUID NOT NULL REFERENCES groups.learning_groups(id) ON DELETE CASCADE,
    skill_id        UUID NOT NULL,
    PRIMARY KEY (group_id, skill_id)
);

CREATE TABLE groups.discussions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id        UUID NOT NULL REFERENCES groups.learning_groups(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL,
    parent_id       UUID REFERENCES groups.discussions(id),   -- Threaded replies
    content         TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_group_members_group ON groups.group_members(group_id);
CREATE INDEX idx_group_members_user ON groups.group_members(user_id);
CREATE INDEX idx_discussions_group ON groups.discussions(group_id);
CREATE INDEX idx_discussions_parent ON groups.discussions(parent_id);
CREATE INDEX idx_discussions_created ON groups.discussions(group_id, created_at DESC);
```

#### Review Service — `skillsync_review`

```sql
CREATE TABLE reviews.reviews (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID NOT NULL UNIQUE,  -- One review per session
    mentor_id       UUID NOT NULL,
    reviewer_id     UUID NOT NULL,
    rating          INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_reviews_mentor_id ON reviews.reviews(mentor_id);
CREATE INDEX idx_reviews_reviewer_id ON reviews.reviews(reviewer_id);
CREATE INDEX idx_reviews_session_id ON reviews.reviews(session_id);
CREATE INDEX idx_reviews_mentor_rating ON reviews.reviews(mentor_id, created_at DESC);
```

#### Notification Service — `skillsync_notification`

```sql
CREATE TABLE notifications.notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    type            VARCHAR(50) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    message         TEXT NOT NULL,
    data            JSONB,           -- Flexible payload
    is_read         BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_notifications_user_id ON notifications.notifications(user_id);
CREATE INDEX idx_notifications_user_unread ON notifications.notifications(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_created ON notifications.notifications(created_at DESC);
```

---

### 2.1.2 ER Diagram

```
                                    ┌──────────────────┐
                                    │   categories     │
                                    ├──────────────────┤
                                    │ id          (PK) │
                              ┌────▶│ name             │
                              │     │ parent_id   (FK) │──┐
                              │     └──────────────────┘  │
                              │            ▲              │
                              │            └──────────────┘ (self-ref)
                              │
┌──────────────┐         ┌────┴─────────────┐       ┌──────────────────┐
│ auth.users   │         │  skills.skills    │       │ mentor_skills    │
├──────────────┤         ├───────────────────┤       ├──────────────────┤
│ id      (PK) │         │ id          (PK)  │◀──────│ skill_id    (FK) │
│ email        │         │ name              │       │ mentor_id   (FK) │──┐
│ password_hash│         │ category          │       └──────────────────┘  │
│ role         │         │ description       │                             │
│ is_active    │         └───────────────────┘                             │
│ is_verified  │                                                           │
└──────┬───────┘                                                           │
       │                                                                   │
       │ user_id                                                           │
       ▼                                                                   │
┌──────────────────┐     ┌──────────────────┐      ┌──────────────────────┤
│ users.profiles   │     │ mentor_profiles  │◀─────┘                      │
├──────────────────┤     ├──────────────────┤                              │
│ id          (PK) │     │ id          (PK) │       ┌──────────────────┐  │
│ user_id     (FK) │     │ user_id     (FK) │──────▶│availability_slots│  │
│ first_name       │     │ bio              │       ├──────────────────┤  │
│ last_name        │     │ experience_years │       │ id          (PK) │  │
│ bio              │     │ hourly_rate      │       │ mentor_id   (FK) │  │
│ avatar_url       │     │ avg_rating       │       │ day_of_week      │  │
└──────────────────┘     │ status           │       │ start_time       │  │
                         └────────┬─────────┘       │ end_time         │  │
                                  │                 └──────────────────┘  │
                                  │                                       │
             ┌────────────────────┼──────────────────────┐               │
             │                    │                      │               │
             ▼                    ▼                      ▼               │
     ┌──────────────┐    ┌──────────────┐       ┌──────────────┐        │
     │   sessions   │    │   reviews    │       │notifications │        │
     ├──────────────┤    ├──────────────┤       ├──────────────┤        │
     │ id      (PK) │    │ id      (PK) │       │ id      (PK) │        │
     │ mentor_id    │    │ session_id   │       │ user_id      │        │
     │ learner_id   │    │ mentor_id    │       │ type         │        │
     │ topic        │    │ reviewer_id  │       │ title        │        │
     │ session_date │    │ rating       │       │ message      │        │
     │ status       │    │ comment      │       │ data (JSONB) │        │
     └──────────────┘    └──────────────┘       │ is_read      │        │
                                                └──────────────┘        │
     ┌──────────────────┐     ┌──────────────────┐                      │
     │ learning_groups  │     │ group_members    │                      │
     ├──────────────────┤     ├──────────────────┤                      │
     │ id          (PK) │◀────│ group_id    (FK) │                      │
     │ name             │     │ user_id          │                      │
     │ description      │     │ role             │                      │
     │ max_members      │     └──────────────────┘                      │
     │ created_by       │                                               │
     └────────┬─────────┘     ┌──────────────────┐                      │
              │               │  discussions     │                      │
              └──────────────▶├──────────────────┤                      │
                              │ id          (PK) │                      │
                              │ group_id    (FK) │                      │
                              │ user_id          │                      │
                              │ parent_id   (FK) │ (self-ref, threads)  │
                              │ content          │                      │
                              └──────────────────┘                      │
```

### 2.1.3 Indexing Strategy

| Index Type | Use Case | Example |
|---|---|---|
| **B-Tree (default)** | Equality & range queries | `idx_sessions_date`, `idx_mentor_profiles_rate` |
| **Composite** | Multi-column queries | `idx_sessions_mentor_date` (conflict detection) |
| **Partial** | Filtered subsets | `idx_notifications_user_unread WHERE is_read = FALSE` |
| **GIN (trigram)** | Fuzzy text search | `idx_skills_name_trgm` for skill autocomplete |
| **Unique** | Constraint enforcement | `UNIQUE(mentor_id, skill_id)` on mentor_skills |

---

## 2.2 Microservices Design

### 2.2.1 Auth Service

**Port**: 8081

**Responsibilities**: User registration, email verification, login, JWT token management, password reset.

#### API Endpoints

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/auth/register` | PUBLIC | Register new user |
| POST | `/api/auth/login` | PUBLIC | Login, returns JWT pair |
| POST | `/api/auth/refresh` | PUBLIC | Refresh access token |
| POST | `/api/auth/logout` | AUTHENTICATED | Invalidate refresh token |
| POST | `/api/auth/forgot-password` | PUBLIC | Send password reset email |
| POST | `/api/auth/reset-password` | PUBLIC | Reset password with token |
| GET  | `/api/auth/verify/{token}` | PUBLIC | Email verification |
| GET  | `/api/auth/validate` | INTERNAL | Validate JWT (used by Gateway) |

#### DTOs

```java
// Request DTOs
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 100) String password,
    @NotBlank @Size(min = 2, max = 100) String firstName,
    @NotBlank @Size(min = 2, max = 100) String lastName
) {}

public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {}

public record RefreshTokenRequest(
    @NotBlank String refreshToken
) {}

// Response DTOs
public record AuthResponse(
    String accessToken,
    String refreshToken,
    long expiresIn,
    String tokenType,       // "Bearer"
    UserSummary user
) {}

public record UserSummary(
    UUID id,
    String email,
    String role,
    String firstName,
    String lastName
) {}
```

#### Business Logic
- Password must contain: ≥8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special char
- Access token TTL: 15 minutes
- Refresh token TTL: 7 days
- Max 5 active refresh tokens per user (FIFO eviction)
- Failed login lockout: 5 attempts → 15 min lockout
- Email verification required before first login

---

### 2.2.2 User Service

**Port**: 8082

**Responsibilities**: Profile CRUD, user skill tagging, avatar upload, profile completeness calculation.

#### API Endpoints

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/api/users/me` | AUTHENTICATED | Get own profile |
| PUT | `/api/users/me` | AUTHENTICATED | Update own profile |
| GET | `/api/users/{id}` | AUTHENTICATED | Get user profile by ID |
| POST | `/api/users/me/avatar` | AUTHENTICATED | Upload avatar |
| DELETE | `/api/users/me/avatar` | AUTHENTICATED | Remove avatar |
| GET | `/api/users/me/skills` | AUTHENTICATED | Get user's skills |
| POST | `/api/users/me/skills` | AUTHENTICATED | Add skill to profile |
| DELETE | `/api/users/me/skills/{skillId}` | AUTHENTICATED | Remove skill |
| GET | `/api/users` | ADMIN | List all users (paginated) |
| PUT | `/api/users/{id}/status` | ADMIN | Activate/deactivate user |

#### DTOs

```java
public record UpdateProfileRequest(
    @Size(min = 2, max = 100) String firstName,
    @Size(min = 2, max = 100) String lastName,
    @Size(max = 1000) String bio,
    @Size(max = 20) String phone,
    @Size(max = 200) String location
) {}

public record ProfileResponse(
    UUID id,
    UUID userId,
    String firstName,
    String lastName,
    String email,
    String bio,
    String avatarUrl,
    String phone,
    String location,
    int profileCompletePct,
    List<SkillSummary> skills,
    LocalDateTime createdAt
) {}

public record AddSkillRequest(
    @NotNull UUID skillId,
    @NotBlank String proficiency  // BEGINNER, INTERMEDIATE, ADVANCED
) {}
```

---

### 2.2.3 Mentor Service

**Port**: 8083

**Responsibilities**: Mentor onboarding, profile management, availability management, mentor search/discovery, admin approval workflow.

#### API Endpoints

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/mentors/apply` | LEARNER | Apply to become mentor |
| GET | `/api/mentors/search` | AUTHENTICATED | Search mentors with filters |
| GET | `/api/mentors/{id}` | AUTHENTICATED | Get mentor profile |
| PUT | `/api/mentors/me` | MENTOR | Update mentor profile |
| GET | `/api/mentors/me/availability` | MENTOR | Get own availability |
| POST | `/api/mentors/me/availability` | MENTOR | Add availability slot |
| DELETE | `/api/mentors/me/availability/{id}` | MENTOR | Remove availability slot |
| PUT | `/api/mentors/{id}/approve` | ADMIN | Approve mentor application |
| PUT | `/api/mentors/{id}/reject` | ADMIN | Reject mentor application |
| GET | `/api/mentors/pending` | ADMIN | List pending applications |

#### DTOs

```java
public record MentorApplicationRequest(
    @NotBlank @Size(min = 50, max = 2000) String bio,
    @Min(0) @Max(50) int experienceYears,
    @DecimalMin("5.00") @DecimalMax("500.00") BigDecimal hourlyRate,
    @NotEmpty @Size(max = 10) List<UUID> skillIds
) {}

public record MentorSearchRequest(
    UUID skillId,
    @Min(0) @Max(5) Double minRating,
    @Min(0) Integer minExperience,
    @Min(0) Integer maxExperience,
    @DecimalMin("0") BigDecimal minPrice,
    @DecimalMin("0") BigDecimal maxPrice,
    Integer dayOfWeek,
    @Min(0) int page,
    @Min(1) @Max(50) int size,
    String sortBy,    // "rating", "price", "experience"
    String sortDir    // "asc", "desc"
) {}

public record MentorProfileResponse(
    UUID id,
    UUID userId,
    String firstName,
    String lastName,
    String avatarUrl,
    String bio,
    int experienceYears,
    BigDecimal hourlyRate,
    double avgRating,
    int totalReviews,
    int totalSessions,
    String status,
    List<SkillSummary> skills,
    List<AvailabilitySlot> availability
) {}

public record AvailabilitySlotRequest(
    @Min(0) @Max(6) int dayOfWeek,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime
) {}
```

#### Business Logic
- Only ROLE_LEARNER can apply (checked via JWT role claim)
- Application requires ≥50 char bio, ≥1 skill, hourly rate $5–$500
- Admin sees paginated pending applications, newest first
- On approval: user role updated to ROLE_MENTOR via inter-service call to Auth Service
- On rejection: reason stored, user can re-apply after 30 days
- Search uses composite query with dynamic WHERE clauses via Spring Data Specifications

---

### 2.2.4 Skill Service

**Port**: 8084

**Responsibilities**: Centralized skill catalog, category management, skill search with autocomplete.

#### API Endpoints

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/api/skills` | PUBLIC | List all skills (paginated) |
| GET | `/api/skills/{id}` | PUBLIC | Get skill by ID |
| GET | `/api/skills/search` | PUBLIC | Search skills (autocomplete) |
| GET | `/api/skills/categories` | PUBLIC | List all categories |
| POST | `/api/skills` | ADMIN | Create new skill |
| PUT | `/api/skills/{id}` | ADMIN | Update skill |
| DELETE | `/api/skills/{id}` | ADMIN | Deactivate skill |
| POST | `/api/skills/categories` | ADMIN | Create category |

---

### 2.2.5 Session Service

**Port**: 8085

**Responsibilities**: Session lifecycle management, conflict detection, state transitions, event publishing.

#### API Endpoints

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/sessions` | LEARNER | Request a session |
| GET | `/api/sessions/{id}` | AUTHENTICATED | Get session details |
| GET | `/api/sessions/me` | AUTHENTICATED | Get my sessions (as learner or mentor) |
| PUT | `/api/sessions/{id}/accept` | MENTOR | Accept session request |
| PUT | `/api/sessions/{id}/reject` | MENTOR | Reject session request |
| PUT | `/api/sessions/{id}/cancel` | AUTHENTICATED | Cancel session |
| PUT | `/api/sessions/{id}/complete` | MENTOR | Mark session complete |
| GET | `/api/sessions/mentor/{mentorId}` | AUTHENTICATED | Get mentor's sessions |

#### DTOs

```java
public record CreateSessionRequest(
    @NotNull UUID mentorId,
    @NotBlank @Size(max = 255) String topic,
    @Size(max = 2000) String description,
    @NotNull @Future LocalDateTime sessionDate,
    @Min(15) @Max(240) int durationMinutes
) {}

public record SessionResponse(
    UUID id,
    UUID mentorId,
    UUID learnerId,
    String mentorName,
    String learnerName,
    String topic,
    String description,
    LocalDateTime sessionDate,
    int durationMinutes,
    String meetingLink,
    String status,
    String cancelReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

public record RejectSessionRequest(
    @NotBlank @Size(max = 500) String reason
) {}

public record SessionFilterRequest(
    String status,
    LocalDate fromDate,
    LocalDate toDate,
    @Min(0) int page,
    @Min(1) @Max(50) int size
) {}
```

#### Business Logic & Validation
- **Conflict Detection**: Before creating session → check mentor's existing ACCEPTED sessions for time overlap
- **State Machine Enforcement**:
  - `REQUESTED → ACCEPTED` (mentor only)
  - `REQUESTED → REJECTED` (mentor only)
  - `REQUESTED → CANCELLED` (learner only)
  - `ACCEPTED → COMPLETED` (mentor only, after session_date)
  - `ACCEPTED → CANCELLED` (either party)
- Invalid transitions throw `InvalidStateTransitionException`
- Session date must be ≥24 hours in the future
- Duration: 15–240 minutes
- On state change → publish event to RabbitMQ

#### Events Published

| Event | Trigger | Payload |
|---|---|---|
| `SESSION_REQUESTED` | Learner creates session | sessionId, mentorId, learnerId, date |
| `SESSION_ACCEPTED` | Mentor accepts | sessionId, mentorId, learnerId, date |
| `SESSION_REJECTED` | Mentor rejects | sessionId, mentorId, learnerId, reason |
| `SESSION_CANCELLED` | Either cancels | sessionId, cancelledBy, reason |
| `SESSION_COMPLETED` | Mentor completes | sessionId, mentorId, learnerId |

---

### 2.2.6 Group Service

**Port**: 8086

**Responsibilities**: Group CRUD, membership management, threaded discussions.

#### API Endpoints

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/groups` | AUTHENTICATED | Create group |
| GET | `/api/groups` | AUTHENTICATED | List groups (paginated) |
| GET | `/api/groups/{id}` | AUTHENTICATED | Get group details |
| PUT | `/api/groups/{id}` | OWNER | Update group |
| DELETE | `/api/groups/{id}` | OWNER/ADMIN | Delete group |
| POST | `/api/groups/{id}/join` | AUTHENTICATED | Join group |
| DELETE | `/api/groups/{id}/leave` | MEMBER | Leave group |
| GET | `/api/groups/{id}/members` | MEMBER | List members |
| POST | `/api/groups/{id}/discussions` | MEMBER | Post discussion |
| GET | `/api/groups/{id}/discussions` | MEMBER | Get discussions |
| DELETE | `/api/groups/{id}/discussions/{dId}` | OWNER/ADMIN | Delete discussion |

---

### 2.2.7 Review Service

**Port**: 8087

**Responsibilities**: Review submission, rating aggregation, review retrieval.

#### API Endpoints

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/reviews` | LEARNER | Submit review for completed session |
| GET | `/api/reviews/mentor/{mentorId}` | AUTHENTICATED | Get mentor reviews |
| GET | `/api/reviews/{id}` | AUTHENTICATED | Get review by ID |
| GET | `/api/reviews/me` | AUTHENTICATED | Get my submitted reviews |
| DELETE | `/api/reviews/{id}` | ADMIN | Delete review |

#### DTOs

```java
public record CreateReviewRequest(
    @NotNull UUID sessionId,
    @Min(1) @Max(5) int rating,
    @Size(min = 10, max = 2000) String comment
) {}

public record ReviewResponse(
    UUID id,
    UUID sessionId,
    UUID mentorId,
    UUID reviewerId,
    String reviewerName,
    String reviewerAvatar,
    int rating,
    String comment,
    LocalDateTime createdAt
) {}

public record MentorRatingSummary(
    UUID mentorId,
    double averageRating,
    int totalReviews,
    Map<Integer, Integer> ratingDistribution  // {5: 42, 4: 28, 3: 10, 2: 5, 1: 2}
) {}
```

#### Business Logic
- Only the learner of a COMPLETED session can submit a review
- One review per session (enforced by unique constraint on session_id)
- After review submission → recalculate mentor's avg_rating via event to Mentor Service
- Rating distribution calculated on-read (or cached)

#### Events Published

| Event | Trigger | Payload |
|---|---|---|
| `REVIEW_SUBMITTED` | Learner submits review | reviewId, mentorId, rating |

---

### 2.2.8 Notification Service

**Port**: 8088

**Responsibilities**: Consume events from RabbitMQ, persist notifications, push via WebSocket.

#### API Endpoints

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/api/notifications` | AUTHENTICATED | Get my notifications |
| GET | `/api/notifications/unread/count` | AUTHENTICATED | Get unread count |
| PUT | `/api/notifications/{id}/read` | AUTHENTICATED | Mark as read |
| PUT | `/api/notifications/read-all` | AUTHENTICATED | Mark all as read |
| DELETE | `/api/notifications/{id}` | AUTHENTICATED | Delete notification |

#### RabbitMQ Consumers

```java
@RabbitListener(queues = "session.requested.queue")
public void handleSessionRequested(SessionRequestedEvent event) {
    // Create notification for MENTOR
    Notification notification = Notification.builder()
        .userId(event.getMentorId())
        .type("SESSION_REQUESTED")
        .title("New Session Request")
        .message("You have a new session request for " + event.getTopic())
        .data(Map.of("sessionId", event.getSessionId()))
        .build();
    notificationRepository.save(notification);
    webSocketService.pushToUser(event.getMentorId(), notification);
}

@RabbitListener(queues = "mentor.approved.queue")
public void handleMentorApproved(MentorApprovedEvent event) {
    // Create notification for USER whose mentor application was approved
    Notification notification = Notification.builder()
        .userId(event.getUserId())
        .type("MENTOR_APPROVED")
        .title("Mentor Application Approved!")
        .message("Congratulations! Your mentor application has been approved.")
        .build();
    notificationRepository.save(notification);
    webSocketService.pushToUser(event.getUserId(), notification);
}
```

---

## 2.3 UML Diagrams

### 2.3.1 Class Diagram — Session Entity

```
┌──────────────────────────────────────────────┐
│                  Session                      │
├──────────────────────────────────────────────┤
│ - id: UUID                                    │
│ - mentorId: UUID                              │
│ - learnerId: UUID                             │
│ - topic: String                               │
│ - description: String                         │
│ - sessionDate: LocalDateTime                  │
│ - durationMinutes: int                        │
│ - meetingLink: String                         │
│ - status: SessionStatus                       │
│ - cancelReason: String                        │
│ - createdAt: LocalDateTime                    │
│ - updatedAt: LocalDateTime                    │
├──────────────────────────────────────────────┤
│ + requestSession(dto): Session                │
│ + accept(): void                              │
│ + reject(reason: String): void                │
│ + cancel(reason: String): void                │
│ + complete(): void                            │
│ + isTransitionAllowed(target): boolean        │
├──────────────────────────────────────────────┤
│ «enum» SessionStatus                         │
│   REQUESTED, ACCEPTED, REJECTED,              │
│   COMPLETED, CANCELLED                        │
└──────────────────────────────────────────────┘
           │                        │
           │ uses                   │ uses
           ▼                        ▼
┌──────────────────┐    ┌───────────────────────┐
│ SessionService   │    │ SessionRepository     │
├──────────────────┤    ├───────────────────────┤
│ + create()       │    │ + findById()          │
│ + accept()       │    │ + findByMentorId()    │
│ + reject()       │    │ + findByLearnerId()   │
│ + cancel()       │    │ + findConflicting()   │
│ + complete()     │    │ + findByStatus()      │
│ + getById()      │    └───────────────────────┘
│ + getMySession() │
└──────────────────┘
           │
           │ publishes events
           ▼
┌──────────────────────────┐
│ SessionEventPublisher    │
├──────────────────────────┤
│ + publishRequested()     │
│ + publishAccepted()      │
│ + publishRejected()      │
│ + publishCancelled()     │
│ + publishCompleted()     │
└──────────────────────────┘
```

### 2.3.2 Class Diagram — Review Flow

```
┌─────────────────────────────┐
│      ReviewController       │
├─────────────────────────────┤
│ + submitReview()            │
│ + getMentorReviews()        │
│ + getMyReviews()            │
└────────────┬────────────────┘
             │
             ▼
┌─────────────────────────────┐    ┌───────────────────────┐
│      ReviewService          │───▶│ SessionServiceClient  │
├─────────────────────────────┤    │ (Feign / RestTemplate)│
│ + submitReview()            │    ├───────────────────────┤
│   - validate session exists │    │ + getSession(id)      │
│   - validate COMPLETED      │    │ + validateLearner()   │
│   - check duplicate         │    └───────────────────────┘
│   - save review             │
│   - publish event           │    ┌───────────────────────┐
│ + getMentorReviews()        │───▶│ ReviewRepository      │
│ + calculateAvgRating()      │    ├───────────────────────┤
│ + getRatingDistribution()   │    │ + findByMentorId()    │
└────────────┬────────────────┘    │ + findBySessionId()   │
             │                     │ + avgRatingByMentor() │
             │ publishes           └───────────────────────┘
             ▼
┌─────────────────────────────┐
│  ReviewEventPublisher       │
├─────────────────────────────┤
│ + publishReviewSubmitted()  │──── ▶ RabbitMQ
└─────────────────────────────┘       │
                                      ▼
                             ┌─────────────────────┐
                             │ Mentor Service       │
                             │ (Consumer)           │
                             │ - updateAvgRating()  │
                             └─────────────────────┘
```

### 2.3.3 Sequence Diagram — Session Booking Flow

```
 Learner          Gateway         SessionService       MentorService        RabbitMQ       NotificationSvc
   │                 │                  │                    │                  │                │
   │ POST /sessions  │                  │                    │                  │                │
   │ {mentorId,date} │                  │                    │                  │                │
   │────────────────▶│                  │                    │                  │                │
   │                 │ validate JWT     │                    │                  │                │
   │                 │ extract userId   │                    │                  │                │
   │                 │─────────────────▶│                    │                  │                │
   │                 │                  │                    │                  │                │
   │                 │                  │ GET /mentors/{id}  │                  │                │
   │                 │                  │───────────────────▶│                  │                │
   │                 │                  │                    │                  │                │
   │                 │                  │ MentorProfile      │                  │                │
   │                 │                  │◀───────────────────│                  │                │
   │                 │                  │                    │                  │                │
   │                 │                  │ validate:                             │                │
   │                 │                  │  - mentor APPROVED                    │                │
   │                 │                  │  - date ≥ 24h future                 │                │
   │                 │                  │  - no time conflict                  │                │
   │                 │                  │  - learner ≠ mentor                  │                │
   │                 │                  │                    │                  │                │
   │                 │                  │ save(REQUESTED)    │                  │                │
   │                 │                  │                    │                  │                │
   │                 │                  │ publish SESSION_REQUESTED             │                │
   │                 │                  │─────────────────────────────────────▶│                │
   │                 │                  │                    │                  │                │
   │                 │  201 Created     │                    │                  │ consume event  │
   │◀────────────────│◀─────────────────│                    │                  │───────────────▶│
   │                 │                  │                    │                  │                │
   │                 │                  │                    │                  │   push to      │
   │                 │                  │                    │                  │   mentor via   │
   │                 │                  │                    │                  │   WebSocket    │
```

### 2.3.4 Sequence Diagram — Mentor Approval Flow

```
 User            Gateway         MentorService        AuthService         RabbitMQ       NotificationSvc
   │                │                  │                    │                  │                │
   │ POST /mentors  │                  │                    │                  │                │
   │ /apply         │                  │                    │                  │                │
   │───────────────▶│                  │                    │                  │                │
   │                │ validate JWT     │                    │                  │                │
   │                │ role=LEARNER     │                    │                  │                │
   │                │─────────────────▶│                    │                  │                │
   │                │                  │ validate DTO       │                  │                │
   │                │                  │ save(PENDING)      │                  │                │
   │                │  202 Accepted    │                    │                  │                │
   │◀───────────────│◀─────────────────│                    │                  │                │
   │                │                  │                    │                  │                │

 Admin           Gateway         MentorService        AuthService         RabbitMQ       NotificationSvc
   │                │                  │                    │                  │                │
   │ PUT /mentors   │                  │                    │                  │                │
   │ /{id}/approve  │                  │                    │                  │                │
   │───────────────▶│                  │                    │                  │                │
   │                │ validate JWT     │                    │                  │                │
   │                │ role=ADMIN       │                    │                  │                │
   │                │─────────────────▶│                    │                  │                │
   │                │                  │ update(APPROVED)   │                  │                │
   │                │                  │                    │                  │                │
   │                │                  │ PUT /auth/users    │                  │                │
   │                │                  │ /{id}/role=MENTOR  │                  │                │
   │                │                  │───────────────────▶│                  │                │
   │                │                  │◀───────────────────│                  │                │
   │                │                  │                    │                  │                │
   │                │                  │ publish MENTOR_APPROVED               │                │
   │                │                  │─────────────────────────────────────▶│                │
   │                │                  │                    │                  │  notify user   │
   │                │  200 OK          │                    │                  │───────────────▶│
   │◀───────────────│◀─────────────────│                    │                  │                │
```

---

## 2.4 Exception Handling

### 2.4.1 Standard API Error Response

All error responses follow this structure:

```java
public record ApiErrorResponse(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    LocalDateTime timestamp,
    int status,
    String error,           // Error code (machine-readable)
    String message,         // Human-readable message
    String path,            // Request URI
    Map<String, String> details  // Optional: field-level errors
) {}
```

**Example Responses**:

```json
// Validation Error
{
  "timestamp": "2026-03-17T06:00:00.000Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/sessions",
  "details": {
    "sessionDate": "Session date must be in the future",
    "topic": "Topic must not be blank"
  }
}

// Authentication Failure
{
  "timestamp": "2026-03-17T06:00:00.000Z",
  "status": 401,
  "error": "AUTH_TOKEN_EXPIRED",
  "message": "Access token has expired",
  "path": "/api/users/me",
  "details": null
}

// Business Logic Error
{
  "timestamp": "2026-03-17T06:00:00.000Z",
  "status": 409,
  "error": "SESSION_CONFLICT",
  "message": "Mentor already has a session booked during this time slot",
  "path": "/api/sessions",
  "details": {
    "conflictingSessionId": "abc-123"
  }
}
```

### 2.4.2 Custom Exception Classes

```java
// Base exception
public abstract class SkillSyncException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Map<String, String> details;

    protected SkillSyncException(String message, String errorCode, 
                                  HttpStatus httpStatus, Map<String, String> details) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = details;
    }
    // getters
}

// Specific exceptions
public class ResourceNotFoundException extends SkillSyncException {
    public ResourceNotFoundException(String resource, String field, Object value) {
        super(
            String.format("%s not found with %s: %s", resource, field, value),
            "RESOURCE_NOT_FOUND",
            HttpStatus.NOT_FOUND,
            Map.of("resource", resource, "field", field, "value", value.toString())
        );
    }
}

public class DuplicateResourceException extends SkillSyncException {
    public DuplicateResourceException(String resource, String field, Object value) {
        super(
            String.format("%s already exists with %s: %s", resource, field, value),
            "DUPLICATE_RESOURCE",
            HttpStatus.CONFLICT,
            Map.of("resource", resource, "field", field)
        );
    }
}

public class InvalidStateTransitionException extends SkillSyncException {
    public InvalidStateTransitionException(String currentState, String targetState) {
        super(
            String.format("Cannot transition from %s to %s", currentState, targetState),
            "INVALID_STATE_TRANSITION",
            HttpStatus.UNPROCESSABLE_ENTITY,
            Map.of("currentState", currentState, "targetState", targetState)
        );
    }
}

public class UnauthorizedAccessException extends SkillSyncException { ... }
public class AuthTokenExpiredException extends SkillSyncException { ... }
public class AuthTokenInvalidException extends SkillSyncException { ... }
public class AccountLockedException extends SkillSyncException { ... }
public class SessionConflictException extends SkillSyncException { ... }
public class MentorNotApprovedException extends SkillSyncException { ... }
public class MaxMembersExceededException extends SkillSyncException { ... }
public class AlreadyReviewedException extends SkillSyncException { ... }
public class ServiceCommunicationException extends SkillSyncException { ... }
public class MessagePublishException extends SkillSyncException { ... }
public class RateLimitExceededException extends SkillSyncException { ... }
public class FileUploadException extends SkillSyncException { ... }
```

### 2.4.3 Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Handle all custom SkillSync exceptions
    @ExceptionHandler(SkillSyncException.class)
    public ResponseEntity<ApiErrorResponse> handleSkillSyncException(
            SkillSyncException ex, HttpServletRequest request) {
        log.warn("Business exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        ApiErrorResponse error = new ApiErrorResponse(
            LocalDateTime.now(ZoneOffset.UTC),
            ex.getHttpStatus().value(),
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            ex.getDetails()
        );
        return new ResponseEntity<>(error, ex.getHttpStatus());
    }

    // Handle Jakarta Bean Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("Validation failed: {}", fieldErrors);

        ApiErrorResponse error = new ApiErrorResponse(
            LocalDateTime.now(ZoneOffset.UTC),
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_ERROR",
            "Request validation failed",
            request.getRequestURI(),
            fieldErrors
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Handle constraint violations (path params, query params)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        Map<String, String> violations = new HashMap<>();
        ex.getConstraintViolations().forEach(v ->
            violations.put(v.getPropertyPath().toString(), v.getMessage())
        );

        ApiErrorResponse error = new ApiErrorResponse(
            LocalDateTime.now(ZoneOffset.UTC),
            HttpStatus.BAD_REQUEST.value(),
            "CONSTRAINT_VIOLATION",
            "Request constraint violation",
            request.getRequestURI(),
            violations
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Handle Spring Security authentication exceptions
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        
        ApiErrorResponse error = new ApiErrorResponse(
            LocalDateTime.now(ZoneOffset.UTC),
            HttpStatus.UNAUTHORIZED.value(),
            "AUTHENTICATION_FAILED",
            "Authentication failed: " + ex.getMessage(),
            request.getRequestURI(),
            null
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // Handle access denied (insufficient role)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        
        ApiErrorResponse error = new ApiErrorResponse(
            LocalDateTime.now(ZoneOffset.UTC),
            HttpStatus.FORBIDDEN.value(),
            "ACCESS_DENIED",
            "You do not have permission to perform this action",
            request.getRequestURI(),
            null
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // Handle inter-service communication failures
    @ExceptionHandler({FeignException.class, RestClientException.class})
    public ResponseEntity<ApiErrorResponse> handleServiceCommunicationError(
            Exception ex, HttpServletRequest request) {
        
        log.error("Service communication failed: {}", ex.getMessage(), ex);
        
        ApiErrorResponse error = new ApiErrorResponse(
            LocalDateTime.now(ZoneOffset.UTC),
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "SERVICE_UNAVAILABLE",
            "A dependent service is temporarily unavailable. Please retry.",
            request.getRequestURI(),
            null
        );
        return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Handle RabbitMQ publish failures
    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ApiErrorResponse> handleRabbitMQError(
            AmqpException ex, HttpServletRequest request) {
        
        log.error("RabbitMQ error: {}", ex.getMessage(), ex);

        // Note: The primary operation (e.g., session creation) should still succeed
        // Notification failure is logged and retried asynchronously
        ApiErrorResponse error = new ApiErrorResponse(
            LocalDateTime.now(ZoneOffset.UTC),
            HttpStatus.ACCEPTED.value(),
            "EVENT_PUBLISH_WARNING",
            "Operation completed but notification delivery may be delayed",
            request.getRequestURI(),
            null
        );
        return new ResponseEntity<>(error, HttpStatus.ACCEPTED);
    }

    // Catch-all for unexpected exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ApiErrorResponse error = new ApiErrorResponse(
            LocalDateTime.now(ZoneOffset.UTC),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please try again later.",
            request.getRequestURI(),
            null
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

### 2.4.4 Error Code Reference

| Error Code | HTTP Status | Scenario |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Request body validation failed |
| `CONSTRAINT_VIOLATION` | 400 | Path/query param constraint violated |
| `AUTHENTICATION_FAILED` | 401 | Invalid credentials |
| `AUTH_TOKEN_EXPIRED` | 401 | JWT access token expired |
| `AUTH_TOKEN_INVALID` | 401 | Malformed or tampered JWT |
| `ACCESS_DENIED` | 403 | Insufficient role/permissions |
| `ACCOUNT_LOCKED` | 403 | Too many failed login attempts |
| `RESOURCE_NOT_FOUND` | 404 | Entity not found |
| `DUPLICATE_RESOURCE` | 409 | Unique constraint violation |
| `SESSION_CONFLICT` | 409 | Time slot already booked |
| `INVALID_STATE_TRANSITION` | 422 | Invalid session state change |
| `MENTOR_NOT_APPROVED` | 422 | Mentor profile not yet approved |
| `MAX_MEMBERS_EXCEEDED` | 422 | Group is full |
| `ALREADY_REVIEWED` | 422 | Session already has a review |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `SERVICE_UNAVAILABLE` | 503 | Inter-service call failed |
| `EVENT_PUBLISH_WARNING` | 202 | RabbitMQ publish failed (non-critical) |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## 2.5 RabbitMQ Configuration

### Exchange & Queue Topology

```java
@Configuration
public class RabbitMQConfig {

    // Exchanges
    public static final String SESSION_EXCHANGE = "session.exchange";
    public static final String MENTOR_EXCHANGE = "mentor.exchange";
    public static final String REVIEW_EXCHANGE = "review.exchange";

    // Queues
    public static final String SESSION_REQUESTED_QUEUE = "session.requested.queue";
    public static final String SESSION_ACCEPTED_QUEUE = "session.accepted.queue";
    public static final String SESSION_REJECTED_QUEUE = "session.rejected.queue";
    public static final String SESSION_CANCELLED_QUEUE = "session.cancelled.queue";
    public static final String SESSION_COMPLETED_QUEUE = "session.completed.queue";
    public static final String MENTOR_APPROVED_QUEUE = "mentor.approved.queue";
    public static final String MENTOR_REJECTED_QUEUE = "mentor.rejected.queue";
    public static final String REVIEW_SUBMITTED_QUEUE = "review.submitted.queue";

    @Bean
    public TopicExchange sessionExchange() {
        return new TopicExchange(SESSION_EXCHANGE, true, false);
    }

    @Bean
    public Queue sessionRequestedQueue() {
        return QueueBuilder.durable(SESSION_REQUESTED_QUEUE)
            .withArgument("x-dead-letter-exchange", "dlx.exchange")
            .withArgument("x-dead-letter-routing-key", "dlx.session.requested")
            .build();
    }

    @Bean
    public Binding sessionRequestedBinding() {
        return BindingBuilder.bind(sessionRequestedQueue())
            .to(sessionExchange())
            .with("session.requested");
    }

    // ... similar for other queues

    // Dead Letter Queue for failed messages
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("dlx.queue")
            .withArgument("x-message-ttl", 300000) // 5 min retry delay
            .build();
    }
}
```

### RabbitMQ Error Handling

```java
@Configuration
public class RabbitMQErrorConfig {

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false); // Don't requeue failed messages
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
            .maxAttempts(3)
            .backOffOptions(1000, 2.0, 10000) // initial=1s, multiplier=2x, max=10s
            .recoverer(new RejectAndDontRequeueRecoverer()) // Send to DLQ after max retries
            .build());
        return factory;
    }
}
```

---

## 2.6 Logging Strategy

### Structured Logging with MDC

```java
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        
        MDC.put("correlationId", correlationId);
        MDC.put("service", "session-service"); // Set per service
        MDC.put("userId", extractUserIdFromJwt(request));
        
        response.setHeader("X-Correlation-ID", correlationId);
        
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

### Log Format (logback-spring.xml)

```xml
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>correlationId</includeMdcKeyName>
            <includeMdcKeyName>service</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON" />
    </root>

    <!-- Service-specific overrides -->
    <logger name="com.skillsync" level="DEBUG" />
    <logger name="org.springframework.security" level="WARN" />
    <logger name="org.hibernate.SQL" level="DEBUG" />
</configuration>
```

### Log Levels

| Level | Usage |
|---|---|
| `ERROR` | Unrecoverable failures, service down, data corruption |
| `WARN` | Business exceptions, validation failures, degraded performance |
| `INFO` | Request/response logs, state changes, key business events |
| `DEBUG` | SQL queries, method entry/exit, detailed flow tracing |
| `TRACE` | Full request/response bodies (development only) |

### Centralized Log Aggregation

```
Services → JSON stdout → Docker → ELK Stack (Elasticsearch + Logstash + Kibana)
```

Each log entry includes:
- `timestamp`
- `correlationId` (for tracing across services)
- `service` name
- `userId`
- `level`
- `message`
- `exception` (stack trace if applicable)

---

> [!NOTE]
> All microservices share a common `skillsync-common` library containing:
> - `ApiErrorResponse` DTO
> - All custom exception classes
> - `GlobalExceptionHandler`
> - `CorrelationIdFilter`
> - Common DTOs and utilities
