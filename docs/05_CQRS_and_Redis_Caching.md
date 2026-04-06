# Presentation Sync Note

Updated for final presentation on 2026-04-06. Start with docs/00_Presentation_Playbook.md for the guided narrative, then use this document for deep details.

---

# 05 CQRS and Redis Caching



---

## Content from: doc6_cqrs_redis_architecture.md

# ðŸ“„ DOCUMENT 6: CQRS + REDIS CACHING ARCHITECTURE

## SkillSync â€” Command Query Responsibility Segregation & Distributed Caching

---

## 6.1 What is CQRS?

**Command Query Responsibility Segregation (CQRS)** is an architectural pattern that separates read and write operations into distinct service classes.

```
Traditional (single service):
  Controller â†’ Service â†’ Repository â†’ DB

CQRS (split services):
  Controller â†’ CommandService â”€â”€â†’ Repository â†’ DB  (writes + cache invalidation)
               QueryService   â”€â”€â†’ Redis â†’ DB        (reads + cache-aside)
  
  Both Services â”€â”€> Mapper â”€â”€> DTO (Decoupled mapping logic)
```

### Why CQRS for SkillSync?

| Reason | Explanation |
|--------|-------------|
| **Read/write ratio** | Mentor discovery, skill browsing, and session lookups are read-heavy (~80% reads) |
| **Optimization independence** | Read paths can be cached aggressively without affecting write correctness |
| **Scalability** | Query services can be scaled independently of command services |
| **Testability** | Smaller, focused service classes are easier to unit test |
| **Single Responsibility** | Each service has one job: either mutate state or retrieve state |

---

## 6.2 Why Redis?

**Redis** (Remote Dictionary Server) is an in-memory key-value data store used as SkillSync's distributed cache layer.

### Role in Architecture

```
PostgreSQL = Source of Truth (persistent, ACID-compliant)
Redis      = Read Optimization Layer (ephemeral, fast, TTL-managed)
```

> [!IMPORTANT]
> Redis is **NOT** a primary database. It is a **read cache only**. All writes go directly to PostgreSQL. If Redis is unavailable, the system falls back to direct DB queries with zero data loss.

### Why Redis over alternatives?

| Alternative | Why Redis wins |
|------------|---------------|
| In-process cache (Caffeine) | Not shared across service replicas; inconsistent under load balancing |
| Memcached | No data structures, no persistence, no pub/sub |
| Hazelcast | Heavier footprint, more complex clustering |
| Spring `@Cacheable` | Requires annotation-driven approach; less control over TTL and invalidation |

---

## 6.3 Cache Strategy: Cache-Aside

SkillSync uses the **Cache-Aside** (Lazy Loading) pattern:

### Read Path (QueryService)

```
Client â†’ QueryService:
  1. Check Redis for key (e.g., "v1:user:profile:100")
  2. HIT â†’ Return cached response immediately
  3. MISS â†’ Query PostgreSQL
  4. Store result in Redis with domain-specific TTL
  5. Return response to client
```

### Write Path (CommandService)

```
Client â†’ CommandService:
  1. Execute database write (INSERT/UPDATE/DELETE)
  2. Evict relevant Redis cache keys
  3. (Optional) Publish RabbitMQ event for cross-service invalidation
  4. Return response to client
```

### Why Cache-Aside?

| Decision | Rationale |
|----------|-----------|
| **Why not Write-Through?** | Adds latency to every write; SkillSync's write frequency doesn't justify it |
| **Why not Write-Behind?** | Risks data loss if Redis crashes before flush; PostgreSQL is our source of truth |
| **Why not Read-Through?** | Cache-Aside gives us explicit control over what gets cached and TTL per domain |

---

## 6.4 Cache Key Namespace & TTL Strategy

| Service | Domain | Key Pattern | TTL | Rationale |
|---------|--------|------------|-----|-----------|
| User | Profile | `v1:user:profile:{userId}` | 10 min | Moderate change frequency |
| User | Mentor | `v1:user:mentor:{mentorId}` | 10 min | Discovery queries are frequent |
| User | Mentor (by user) | `v1:user:mentor:user:{userId}` | 10 min | Alternate lookup path |
| User | Group | `v1:user:group:{groupId}` | 10 min | Group details rarely change |
| Skill | Single Skill | `v1:skill:{skillId}` | 1 hour | Skills almost never change |
| Skill | All Skills | `v1:skill:all:*` | 1 hour | Catalog browsing cache |
| Session | Session | `v1:session:{sessionId}` | 5 min | State transitions happen frequently |
| Session | Review | `v1:review:{reviewId}` | 5 min | Post-session, immutable after submission |
| Session | Rating Summary | `v1:review:mentor:{id}:summary` | 5 min | Aggregated rating data |
| Notification | Unread Count | `v1:notification:unread:{userId}` | 2 min | High-frequency polling from frontend |

### TTL Design Principles

1. **Shorter TTL for volatile data** â€” Sessions change status frequently (5 min)
2. **Longer TTL for stable data** â€” Skills rarely change (1 hour)
3. **Very short TTL for counters** â€” Unread notification count (2 min)
4. **No caching for sensitive data** â€” Auth tokens, OTPs, payment secrets are **never** cached

---

## 6.5 CQRS Implementation Per Service

### User Service

```
com.skillsync.user
  +-- cache/
  â”‚   â”œâ”€â”€ RedisConfig.java          â† Lettuce client + Jackson JSON serialization
  â”‚   â””â”€â”€ CacheService.java         â† Generic cache wrapper with graceful degradation
  +-- service/
      +-- command/
      â”‚   â”œâ”€â”€ UserCommandService     â† Profile CRUD + cache invalidation
      â”‚   â”œâ”€â”€ MentorCommandService   â† Mentor approval + Saga integration + cache invalidation
      â”‚   â””â”€â”€ GroupCommandService    â† Group operations + cache invalidation
      +-- query/
          â”œâ”€â”€ UserQueryService       â† Cache-aside profile reads
          â”œâ”€â”€ MentorQueryService     â† Cache-aside mentor reads (search, discovery)
          â””â”€â”€ GroupQueryService      â† Cache-aside group reads
  +-- mapper/
      â”œâ”€â”€ UserMapper                 â† Dedicated static mapping methods
      â”œâ”€â”€ MentorMapper               â† Dedicated static mapping methods
      â”œâ”€â”€ GroupMapper                â† Dedicated static mapping methods
      â””â”€â”€ PaymentMapper              â† Dedicated static mapping methods
```

### Skill Service

```
com.skillsync.skill
  +-- cache/
  â”‚   â”œâ”€â”€ RedisConfig.java
  â”‚   â””â”€â”€ CacheService.java
  +-- config/
  â”‚   â””â”€â”€ RabbitMQConfig.java       â† Skill event exchange
  +-- event/
  â”‚   â””â”€â”€ SkillEvent.java           â† Event DTO for cross-service sync
  +-- service/
      +-- command/
      â”‚   â””â”€â”€ SkillCommandService   â† Skill CRUD + cache invalidation + event publishing
      +-- query/
          â””â”€â”€ SkillQueryService     â† Cache-aside skill reads (autocomplete, catalog)
  +-- mapper/
      â””â”€â”€ SkillMapper               â† Dedicated static mapping methods
```

### Session Service

```
com.skillsync.session
  +-- cache/
  â”‚   â”œâ”€â”€ RedisConfig.java
  â”‚   â””â”€â”€ CacheService.java
  +-- service/
      +-- command/
      â”‚   â”œâ”€â”€ SessionCommandService â† Session lifecycle + cache invalidation
      â”‚   â””â”€â”€ ReviewCommandService  â† Review submission + cache invalidation + event publishing
      +-- query/
          â”œâ”€â”€ SessionQueryService   â† Cache-aside session reads
          â””â”€â”€ ReviewQueryService    â† Cache-aside review reads (rating summary, distribution)
  +-- mapper/
      â”œâ”€â”€ SessionMapper             â† Dedicated static mapping methods
      â””â”€â”€ ReviewMapper              â† Dedicated static mapping methods
```

### Notification Service

```
com.skillsync.notification
  +-- cache/
  â”‚   â”œâ”€â”€ RedisConfig.java
  â”‚   â””â”€â”€ CacheService.java
  +-- service/
      +-- command/
      â”‚   â””â”€â”€ NotificationCommandService â† Create + push + cache invalidation
      +-- query/
          â””â”€â”€ NotificationQueryService   â† Cache-aside unread count
  +-- mapper/
      â””â”€â”€ NotificationMapper             â† Dedicated static mapping methods
```

---

## 6.6 Event-Driven Cache Synchronization

### Cross-Service Cache Invalidation via RabbitMQ

When a review is submitted in Session Service, it must invalidate the mentor's cached rating in User Service:

```
Session Service                    RabbitMQ                    User Service
     â”‚                                â”‚                            â”‚
     â”‚  ReviewSubmittedEvent          â”‚                            â”‚
     â”‚  {mentorId, rating, reviewId}  â”‚                            â”‚
     â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–ºâ”‚                            â”‚
     â”‚                                â”‚  review.submitted          â”‚
     â”‚                                â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–ºâ”‚
     â”‚                                â”‚                            â”‚
     â”‚                                â”‚       ReviewEventCacheSyncConsumer:
     â”‚                                â”‚       1. Update mentor avgRating
     â”‚                                â”‚       2. Evict v1:user:mentor:{mentorId}
     â”‚                                â”‚       3. Evict v1:user:mentor:user:{userId}
     â”‚                                â”‚                            â”‚
```

### Skill Event Sync

When a skill is created/updated/deactivated, the Skill Service publishes to `skill.exchange`:

```
Skill Service                      RabbitMQ
     â”‚                                â”‚
     â”‚  SkillEvent                    â”‚
     â”‚  {skillId, action, name}       â”‚
     â”‚â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–ºâ”‚
     â”‚                                â”‚
     â”‚                 (Future: consumed by User Service
     â”‚                  to invalidate skill-related caches)
```

---

## 6.7 Saga + Cache Consistency

The `PaymentSagaOrchestrator` integrates with the CQRS layer to ensure cache consistency during the Saga lifecycle:

```
PaymentSagaOrchestrator
     â”‚
     â”œâ”€â”€ SUCCESS PATH:
     â”‚   1. transitionToSuccessPending()        â†’ DB update
     â”‚   2. MentorCommandService.approveMentor() â†’ DB update + cache evict
     â”‚   3. markPaymentSuccess()                â†’ DB update
     â”‚   4. Publish payment.success event       â†’ RabbitMQ
     â”‚
     â””â”€â”€ COMPENSATION PATH:
         1. MentorCommandService.revertMentorApproval() â†’ DB revert + cache evict
         2. markPaymentCompensated()                    â†’ DB update
         3. Publish payment.compensated event           â†’ RabbitMQ
```

> [!IMPORTANT]
> Cache invalidation happens on **both** success and compensation paths. This ensures that stale cached mentor profiles are never served after a payment saga completes or rolls back.

---

## 6.8 Graceful Degradation

The `CacheService` wrapper catches all Redis exceptions and logs them, allowing the system to fall back to direct PostgreSQL queries:

```java
public <T> T get(String key, Class<T> type) {
    try {
        Object value = redisTemplate.opsForValue().get(key);
        return type.cast(value);
    } catch (Exception e) {
        log.warn("Redis GET failed for key '{}': {}", key, e.getMessage());
        return null;  // Triggers DB fallback in QueryService
    }
}
```

### Failure Scenarios

| Scenario | Behavior |
|----------|----------|
| Redis down | All reads fall back to PostgreSQL; zero data loss |
| Redis slow | Timeouts handled; fallback to DB |
| Redis full (maxmemory) | LRU eviction policy removes oldest keys |
| Redis reconnection | Automatic via Lettuce client pool |
| Network partition | Cache misses handled gracefully; DB is source of truth |

---

## 6.9 Redis Infrastructure Configuration

### Docker Compose

```yaml
redis:
  image: redis:7.2-alpine
  container_name: skillsync-redis
  command: >
    redis-server
    --appendonly yes
    --maxmemory 256mb
    --maxmemory-policy allkeys-lru
  ports:
    - "6379:6379"
  volumes:
    - redis_data:/data
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 10s
    timeout: 5s
    retries: 5
```

### Spring Boot Configuration (per service)

```properties
# Redis connection
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}

# Connection pool (Lettuce)
spring.data.redis.lettuce.pool.max-active=10
spring.data.redis.lettuce.pool.max-idle=5
spring.data.redis.lettuce.pool.min-idle=2
spring.data.redis.lettuce.pool.max-wait=2000ms
spring.data.redis.timeout=3000ms

# Domain-specific TTLs
cache.ttl.default=600
```

### Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `REDIS_HOST` | `localhost` | Redis server hostname |
| `REDIS_PORT` | `6379` | Redis server port |

---

## 6.10 Monitoring & Observability

### Actuator Endpoints

All services expose cache metrics via Spring Boot Actuator:

```
GET /actuator/health      â†’ Includes Redis health
GET /actuator/metrics     â†’ Cache hit/miss ratios
GET /actuator/caches      â†’ Registered cache names
```

### Key Metrics to Monitor

| Metric | What It Tells You |
|--------|-------------------|
| `cache.gets{result=hit}` | Cache hit count â€” should be high |
| `cache.gets{result=miss}` | Cache miss count â€” triggers DB query |
| `cache.evictions` | Number of evictions (LRU or explicit) |
| `cache.puts` | Number of items added to cache |
| Redis `used_memory` | Memory usage â€” should stay below 256MB |
| Redis `connected_clients` | Connection pool usage |

---

## 6.11 Security Exclusions

> [!CAUTION]
> The following are **explicitly excluded** from Redis caching:
> - **Auth Service** â€” Entirely excluded. No caching of JWT tokens, passwords, OTPs, or verification codes
> - **Payment secrets** â€” Razorpay API keys, signatures, and order secrets are never cached
> - **Session tokens** â€” Refresh tokens and access tokens are managed in PostgreSQL only
> - **User passwords** â€” BCrypt hashes are never stored in Redis

---

## 6.12 Trade-offs & Design Decisions

| Decision | Trade-off | Mitigation |
|----------|-----------|------------|
| Cache-Aside over Write-Through | Eventual consistency (brief stale reads) | Short TTLs + aggressive invalidation |
| Redis over in-process cache | Network hop adds ~1ms latency | Still 10-100x faster than PostgreSQL query |
| Per-key TTL over global TTL | Configuration complexity | Domain-specific TTLs match data volatility |
| `SCAN` for pattern eviction | Cursor-based iteration | Used in CacheService.evictByPattern() for safe production use |
| No caching for Auth Service | Every auth check hits DB | Auth queries are simple PK lookups (fast) |
| Graceful degradation over fail-fast | Silent cache failures may go unnoticed | Logging + Actuator metrics + Prometheus alerting |

---

## 6.13 Key Versioning Rules

> [!IMPORTANT]
> **ALL** cache keys MUST use `CacheService.vKey(...)` to generate versioned keys.
> Direct string construction of keys is **STRICTLY PROHIBITED**.

### Rules

1. **Always use `CacheService.vKey("domain:entity:id")`** â†’ produces `v1:domain:entity:id`
2. **Never hardcode the `v1:` prefix** â€” the version is managed centrally in `CacheService.KEY_VERSION`
3. **Key format**: `v1:<service-domain>:<entity-type>:<identifier>`
4. **Pattern eviction**: Use `CacheService.evictByPattern("v1:domain:*")` for bulk eviction

### Current Version

```java
private static final String KEY_VERSION = "v1";
```

### Migration

To migrate cache keys to a new version:
1. Update `KEY_VERSION` in `CacheService.java`
2. Deploy all services â€” old keys expire naturally via TTL
3. No manual Redis flush required

### Audit Status

âœ… **All services verified** â€” No hardcoded cache keys found. All keys use `CacheService.vKey()`.

---

## 6.14 Observability Integration

Cache operations are fully observable via the metrics and tracing stack:

- **Micrometer Counters**: `cache.operations{result=hit|miss|evict|error}` 
- **Prometheus Endpoint**: `/actuator/prometheus` on all services
- **Zipkin Tracing**: Cache operations are included in distributed traces
- **Structured Logging**: All cache operations log with `traceId` and `spanId`

For full details, see **[doc8_observability.md](doc8_observability.md)**.

---

## 6.15 Future Enhancements

1. **Redis Sentinel/Cluster** â€” For high availability in production
2. **Cache warming** â€” Pre-populate hot data on service startup
3. **Read replicas** â€” Separate Redis read replicas for query-heavy services
4. **Grafana dashboard** â€” Real-time cache hit/miss ratios and memory usage via Prometheus
5. **Distributed locking** â€” Redis-based distributed locks for concurrent write scenarios

---

> [!NOTE]
> This document is the authoritative reference for all CQRS and Redis caching architecture decisions in SkillSync.
> All services follow the patterns described here. Any deviations must be documented with rationale.


---

## Content from: cqrs_redis_audit_report.md

# ðŸ” CQRS + Redis Architecture â€” Deep Technical Audit Report

> **Auditor:** Senior Backend Engineer / System Reviewer
> **Date:** 2026-03-25
> **Scope:** All 4 cached services (User, Skill, Session, Notification) + Saga integration
> **Files reviewed:** 40+ source files across services, configs, tests, Docker, and documentation

---

## PART 1: CQRS VALIDATION

### âœ… Strict Separation â€” PASS

| Service | CommandService(s) | QueryService(s) | Separation Clean? |
|---------|-------------------|------------------|--------------------|
| User | `UserCommandService`, `MentorCommandService`, `GroupCommandService` | `UserQueryService`, `MentorQueryService`, `GroupQueryService` | âœ… Yes |
| Skill | `SkillCommandService` | `SkillQueryService` | âœ… Yes |
| Session | `SessionCommandService`, `ReviewCommandService` | `SessionQueryService`, `ReviewQueryService` | âœ… Yes |
| Notification | `NotificationCommandService` | `NotificationQueryService` | âœ… Yes |

**Finding:** No mixing of read/write logic. All `CommandService` classes handle writes + cache invalidation. All `QueryService` classes handle reads + cache-aside. The `static mapToResponse()` pattern is correctly shared between command and query services.

### âœ… Controller Usage â€” PASS

Every controller injects **both** `CommandService` and `QueryService` and delegates correctly:
- GET endpoints â†’ QueryService
- POST/PUT/DELETE endpoints â†’ CommandService
- Clear `// â”€â”€â”€ QUERIES â”€â”€â”€` / `// â”€â”€â”€ COMMANDS â”€â”€â”€` section markers

**Exceptions (by design):**
- [PaymentController](file:///f:/SkillSync/user-service/src/main/java/com/skillsync/user/controller/PaymentController.java) uses `PaymentService` (non-CQRS) â€” **acceptable** because payments are security-critical and should NOT be cached.

### âš ï¸ Minor: CommandService depends on QueryService

- [UserCommandService L9](file:///f:/SkillSync/user-service/src/main/java/com/skillsync/user/service/command/UserCommandService.java#L9) imports `UserQueryService` for `mapToResponse()`.
- [MentorCommandService L15](file:///f:/SkillSync/user-service/src/main/java/com/skillsync/user/service/command/MentorCommandService.java#L15) imports `MentorQueryService` for `mapToResponse()`.
- Pattern: Command â†’ save â†’ then call `QueryService.mapToResponse()` for the return value.

**Verdict:** This is a **data coupling** (shared mapper only), NOT a logic coupling. The `mapToResponse()` methods are `static` and pure functions. **Acceptable** but could be cleaner with a dedicated `Mapper` class.

---

## PART 2: REDIS CACHING VALIDATION

### âœ… Cache-Aside Pattern â€” CORRECTLY IMPLEMENTED

The `getOrLoad()` method in [CacheService](file:///f:/SkillSync/user-service/src/main/java/com/skillsync/user/cache/CacheService.java#L190-L221) implements production-grade cache-aside:

```
1. Fast path: check Redis â†’ HIT â†’ return
2. Check null sentinel â†’ penetration protection
3. Acquire per-key lock â†’ stampede protection
4. Double-check after lock acquisition
5. Load from DB â†’ cache result (or cache null sentinel)
6. Release lock + cleanup
```

**Bonus features above basic cache-aside:**
- âœ… **Stampede protection** via `ConcurrentHashMap<String, ReentrantLock>` â€” prevents thundering herd
- âœ… **Cache penetration protection** via `__NULL__` sentinel with 60s TTL â€” prevents DB hammering for non-existent IDs
- âœ… **Versioned keys** (`v1:` prefix) â€” enables cache migration without flushing
- âœ… **Micrometer metrics** â€” `cache.operations{result=hit|miss|evict|error}` with service tag

### âœ… Cache Keys â€” CONSISTENT + NAMESPACED

| Service | Key Pattern | Versioned? | Namespaced? |
|---------|------------|-----------|------------|
| User | `v1:user:profile:{userId}` | âœ… | âœ… |
| User | `v1:user:mentor:{mentorId}` | âœ… | âœ… |
| User | `v1:user:group:{groupId}` | âœ… | âœ… |
| Skill | `v1:skill:{skillId}` | âœ… | âœ… |
| Session | `v1:session:{sessionId}` | âœ… | âœ… |
| Session | `v1:review:{reviewId}` | âœ… | âœ… |
| Session | `v1:review:mentor:{id}:summary` | âœ… | âœ… |
| Notification | `v1:notification:unread:{userId}` | âœ… | âœ… |

### âœ… TTLs â€” ALL DEFINED

| Domain | TTL | Configured In | Appropriate? |
|--------|-----|--------------|-------------|
| Profile | 600s (10 min) | `cache.ttl.profile=600` | âœ… |
| Mentor | 600s (10 min) | `cache.ttl.mentor=600` | âœ… |
| Group | 600s (10 min) | `cache.ttl.group=600` | âœ… |
| Skill | 3600s (1 hour) | `cache.ttl.skill=3600` | âœ… Stable data |
| Session | 300s (5 min) | `cache.ttl.session=300` | âœ… Volatile state |
| Review | 300s (5 min) | `cache.ttl.review=300` | âœ… |
| Notification | 120s (2 min) | `cache.ttl.notification=120` | âœ… High poll rate |
| Null sentinel | 60s | Hardcoded | âœ… Short-lived |

**No infinite TTLs found.** âœ…

---

## PART 3: CACHE INVALIDATION AUDIT (CRITICAL)

### User Service

| Operation | Cache Keys Evicted | Status |
|-----------|-------------------|--------|
| `createOrUpdateProfile` | `v1:user:profile:{userId}`, `v1:user:profile:id:{profileId}` | âœ… |
| `addSkill` | `v1:user:profile:{userId}` | âœ… |
| `removeSkill` | `v1:user:profile:{userId}` | âœ… |
| `approveMentor` | `v1:user:mentor:{id}`, `v1:user:mentor:user:{userId}`, `v1:user:mentor:search:*`, `v1:user:mentor:pending:*` | âœ… |
| `rejectMentor` | Same as approve | âœ… |
| `revertMentorApproval` | Same as approve | âœ… |
| `addAvailability` | All mentor caches | âœ… |
| `updateAvgRating` | All mentor caches | âœ… |
| `createGroup` | `v1:user:group:all:*` | âœ… |
| `joinGroup` | `v1:user:group:{groupId}` | âœ… |
| `leaveGroup` | `v1:user:group:{groupId}` | âœ… |
| `postDiscussion` | `v1:user:group:{groupId}:discussions:*` | âœ… |

### âŒ CRITICAL: `removeAvailability` â€” MISSING CACHE INVALIDATION

**File:** [MentorCommandService.java:147-149](file:///f:/SkillSync/user-service/src/main/java/com/skillsync/user/service/command/MentorCommandService.java#L147-L149)

```java
public void removeAvailability(Long slotId) {
    availabilitySlotRepository.deleteById(slotId);
    // âŒ NO cache invalidation!
}
```

**Impact:** After removing an availability slot, cached mentor profiles will still show the deleted slot until TTL expires (10 min). This is a **data inconsistency bug**.

**Fix:** Need to look up the mentor profile for the slot, then call `invalidateMentorCaches(profile.getId(), profile.getUserId())`.

### âš ï¸ MINOR: `apply()` (mentor application) â€” NO LIST CACHE INVALIDATION

**File:** [MentorCommandService.java:40-65](file:///f:/SkillSync/user-service/src/main/java/com/skillsync/user/service/command/MentorCommandService.java#L40-L65)

The `apply()` method does NOT invalidate `v1:user:mentor:pending:*` caches. A newly submitted application won't immediately appear in the pending list until TTL expires.

**Impact:** Low â€” pending applications page could show stale data for up to 10 minutes.

### Skill Service

| Operation | Cache Keys Evicted | Status |
|-----------|-------------------|--------|
| `createSkill` | `v1:skill:all:*`, `v1:skill:search:*` | âœ… |
| `updateSkill` | `v1:skill:{id}`, `v1:skill:all:*`, `v1:skill:search:*` | âœ… |
| `deactivateSkill` | `v1:skill:{id}`, `v1:skill:all:*`, `v1:skill:search:*` | âœ… |

### Session Service

| Operation | Cache Keys Evicted | Status |
|-----------|-------------------|--------|
| `createSession` | `v1:session:{id}`, `v1:session:learner:{id}:*`, `v1:session:mentor:{id}:*` | âœ… |
| `acceptSession` | Same pattern | âœ… |
| `rejectSession` | Same pattern | âœ… |
| `cancelSession` | Same pattern | âœ… |
| `completeSession` | Same pattern | âœ… |
| `submitReview` | `v1:review:mentor:{id}:*`, `v1:review:mentor:{id}:summary`, `v1:review:user:{id}:*` | âœ… |
| `deleteReview` | `v1:review:{id}`, all mentor + user review caches | âœ… |

### âš ï¸ MINOR: `submitReview` has redundant eviction

[ReviewCommandService.java:56-58](file:///f:/SkillSync/session-service/src/main/java/com/skillsync/session/service/command/ReviewCommandService.java#L56-L58):
```java
cacheService.evictByPattern(CacheService.vKey("review:mentor:" + mentorId + ":*"));  // Pattern covers summary
cacheService.evict(CacheService.vKey("review:mentor:" + mentorId + ":summary"));      // â† Redundant
```
The `evictByPattern("review:mentor:{id}:*")` already covers `review:mentor:{id}:summary`. Not a bug, just unnecessary double eviction.

### Notification Service â€” âœ… ALL OPERATIONS COVERED

---

## PART 4: EVENT-DRIVEN CACHE SYNC

### âœ… ReviewEventCacheSyncConsumer â€” CORRECTLY IMPLEMENTED

**File:** [ReviewEventCacheSyncConsumer.java](file:///f:/SkillSync/user-service/src/main/java/com/skillsync/user/consumer/ReviewEventCacheSyncConsumer.java)

- Listens on `user.review.submitted.queue`
- Calls `mentorCommandService.updateAvgRating()` which writes to DB + invalidates cache
- **Idempotent:** Uses `avgRating` and `totalReviews` from event (recalculated at source), so duplicate events produce the same result âœ…

### âœ… Notification Consumers â€” ALL USE `NotificationCommandService`

All 4 notification consumers ([MentorEventConsumer](file:///f:/SkillSync/notification-service/src/main/java/com/skillsync/notification/consumer/MentorEventConsumer.java), [SessionEventConsumer](file:///f:/SkillSync/notification-service/src/main/java/com/skillsync/notification/consumer/SessionEventConsumer.java), [ReviewEventConsumer](file:///f:/SkillSync/notification-service/src/main/java/com/skillsync/notification/consumer/ReviewEventConsumer.java), [PaymentEventConsumer](file:///f:/SkillSync/notification-service/src/main/java/com/skillsync/notification/consumer/PaymentEventConsumer.java)) correctly use `NotificationCommandService.createAndPush()` which handles cache invalidation.

### âš ï¸ MINOR: No consumer for Skill events

`SkillCommandService` publishes to `skill.exchange` with routing keys `skill.created`, `skill.updated`, but **no service consumes these events**. The exchange declaration exists but the events go nowhere.

**Impact:** No functional impact currently. This is future infrastructure.

### âš ï¸ Event Ordering

RabbitMQ does NOT guarantee strict ordering across consumers. If two reviews are submitted in quick succession:
- Event 1: `avgRating=4.5, totalReviews=10`
- Event 2: `avgRating=4.3, totalReviews=11`

If Event 2 is processed before Event 1, the mentor will have `avgRating=4.5, totalReviews=10` (stale). 

**Mitigation:** The current implementation is self-correcting â€” the next review submission will recalculate from the database. **Acceptable at current scale.**

---

## PART 5: SAGA + CACHE CONSISTENCY

### âœ… Success Path â€” CACHE CONSISTENT

```
PaymentSagaOrchestrator.executeSaga()
  â†’ transitionToSuccessPending()     â†’ DB write (no cache for payments âœ…)
  â†’ executeMentorOnboarding()        â†’ MentorCommandService.approveMentor()
                                        â†’ DB write + invalidateMentorCaches() âœ…
  â†’ markPaymentSuccess()             â†’ DB write
  â†’ publishPaymentEvent()            â†’ RabbitMQ notification
```

### âœ… Compensation Path â€” CACHE CONSISTENT

```
PaymentSagaOrchestrator.compensate()
  â†’ compensateMentorOnboarding()     â†’ MentorCommandService.revertMentorApproval()
                                        â†’ DB revert + invalidateMentorCaches() âœ…
  â†’ markPaymentCompensated()         â†’ DB write
  â†’ publishPaymentEvent()            â†’ RabbitMQ notification
```

**Both paths invalidate cache.** âœ… No DBâ†”cache inconsistency possible.

### âœ… PaymentService â€” NO CACHING (CORRECT)

`PaymentService` and `PaymentSagaOrchestrator` do NOT inject `CacheService` for their own payment data reads. Payment data is **never cached** â€” write-through to PostgreSQL only. This is the correct security decision.

---

## PART 6: FAILURE HANDLING

### âœ… Redis Down â€” GRACEFUL DEGRADATION

Every method in `CacheService` wraps Redis calls in `try-catch`:
```java
try {
    Object value = redisTemplate.opsForValue().get(key);
    ...
} catch (Exception e) {
    log.warn("Redis GET failed for key={}: {}. Falling back to DB.", key, e.getMessage());
    cacheErrorCounter.increment();
}
return null;  // QueryService sees null â†’ queries DB
```

**All 4 CacheService implementations follow this pattern.** System continues with DB-only reads on Redis failure. âœ…

### âœ… Cache Stampede Protection â€” IMPLEMENTED

The `getOrLoad()` method uses `ConcurrentHashMap<String, ReentrantLock>` for per-key locking. On a cache miss, only ONE thread hits the database; others wait and read the cached result. âœ…

### âš ï¸ MINOR: Stampede protection is per-JVM only

The `keyLocks` map is in-process. If you run multiple replicas of a service, each replica has its own lock map. Under high concurrency with N replicas, up to N threads (one per replica) could hit the database simultaneously on a cache miss.

**Mitigation:** For current scale, this is acceptable. For true distributed locking, use Redis-based locks (`SETNX` or Redisson).

### âš ï¸ MINOR: No circuit breaker on Redis

If Redis is slow (not down), every cache call will wait for the 3000ms timeout before falling back. This could cascade into slow responses system-wide.

**Recommendation:** Add a circuit breaker (Resilience4j) around Redis calls, or reduce `spring.data.redis.timeout` to 500msâ€“1000ms.

---

## PART 7: PERFORMANCE REVIEW

### âœ… Paginated Queries â€” NOT CACHED (CORRECT DECISION)

```java
// UserQueryService
public Page<Profile> getAllProfiles(Pageable pageable) {
    return profileRepository.findAll(pageable); // No cache
}

// MentorQueryService
public Page<MentorProfileResponse> searchMentors(Pageable pageable) {
    return mentorProfileRepository.findByStatus(MentorStatus.APPROVED, pageable).map(...); // No cache
}
```

Paginated results have too many parameter combinations (page, size, sort) to cache effectively. This is the right design decision. âœ…

### âš ï¸ MINOR: `searchSkills()` â€” NOT CACHED

```java
public List<SkillResponse> searchSkills(String query) {
    return skillRepository.searchByName(query).stream()...;
}
```

Search results are not cached. For autocomplete-style queries, this could be a performance concern at scale.

### âš ï¸ MINOR: `KEYS` command for pattern eviction

```java
Set<String> keys = redisTemplate.keys(pattern);
```

`KEYS` is O(N) and blocks Redis during execution. At current scale this is fine, but with 10k+ keys it could cause latency spikes.

**Recommendation:** Migrate to `SCAN` cursor-based iteration at scale.

### âœ… Payload Size â€” REASONABLE

Cached objects are DTOs (records), not entities. No lazy-loaded JPA collections are serialized. JSON serialization with type info is used. âœ…

---

## PART 8: CODE QUALITY

### âœ… Proper Abstraction

All services use `CacheService` as the cache wrapper. No direct `RedisTemplate` calls in business logic. âœ…

### âš ï¸ MINOR: Code Duplication â€” CacheService Ã— 4

The `CacheService` class is **identically duplicated** across 4 services (user, skill, session, notification), differing only in the `service` tag for metrics:

```java
.tag("service", "user-service")   // Only this changes
```

**Total duplication:** ~220 lines Ã— 4 = ~880 lines of duplicated code.

**Recommendation:** Extract to a shared `skillsync-cache-common` Maven module. Services would only configure the service tag.

### âš ï¸ MINOR: RedisConfig Ã— 4

Similarly, `RedisConfig.java` is duplicated 4 times. Same recommendation as above.

### âœ… Clean Architecture

All services follow the same consistent pattern:
```
controller/ â†’ service/command/ â†’ repository â†’ DB + cache evict
           â†’ service/query/  â†’ cache â†’ (miss) â†’ repository â†’ DB â†’ cache put
```

---

## PART 9: SECURITY CHECK

### âœ… Auth Service â€” NO CACHING

No `CacheService`, `RedisConfig`, or Redis dependency exists in `auth-service`. Confirmed by search. âœ…

### âœ… Payment Data â€” NOT CACHED

`PaymentService` does NOT use `CacheService`. Payment amounts, Razorpay secrets, order IDs, and signatures are never stored in Redis. âœ…

### âœ… JWT/OTP â€” NOT CACHED

`auth-service` manages JWT tokens, refresh tokens, and OTPs entirely in PostgreSQL. No Redis involvement. âœ…

### âœ… No Sensitive Data in Cached DTOs

Cached DTOs (`ProfileResponse`, `MentorProfileResponse`, `SkillResponse`, `SessionResponse`, `ReviewResponse`, `NotificationResponse`) contain only display-level data. No passwords, tokens, or payment secrets. âœ…

---

## PART 10: TESTING COVERAGE

### âœ… Cache Hit/Miss Tests â€” PRESENT

| Service | Test File | Cache Hit | Cache Miss | Cache Invalidation |
|---------|-----------|-----------|------------|-------------------|
| User | `UserServiceTest.java` | âŒ missing | âœ… | âœ… |
| Skill | `SkillServiceTest.java` | âŒ missing | âœ… | âœ… |
| Session | `SessionServiceTest.java` | âŒ missing | âœ… | âœ… |
| Notification | `NotificationServiceTest.java` | âŒ missing | âœ… | âœ… |

### âš ï¸ Cache HIT scenario NOT explicitly tested

All tests verify cache miss â†’ DB fetch â†’ cache put. But none test: **cache HIT â†’ return from Redis â†’ DB NOT called**. This is a gap.

### âš ï¸ Test keys don't use versioned prefix

Test expectations use bare keys like `"user:profile:100"` instead of `"v1:user:profile:100"`. Since the actual code uses `CacheService.vKey()` which prepends `v1:`, **tests may not be verifying the correct key format**.

**This is potentially a test accuracy issue.** The tests mock `cacheService.get("user:profile:100", ...)` but the actual code calls `cacheService.get("v1:user:profile:100", ...)`.

### âŒ No Redis Failure Tests

No test verifies that the system degrades gracefully when `CacheService` throws exceptions.

### âŒ No Event-Driven Cache Sync Tests

`ReviewEventCacheSyncConsumer` has no dedicated test class.

### âŒ No Saga + Cache Consistency Tests

No test verifies that cache is invalidated during both saga success and compensation paths.

---

## PART 11: DEVOPS VALIDATION

### âœ… Redis Container Config â€” PRODUCTION-GRADE

```yaml
redis:
  image: redis:7.2-alpine
  command: redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
  volumes:
    - redis-data:/data
```

- âœ… AOF persistence enabled
- âœ… Memory limit with LRU eviction
- âœ… Health check configured
- âœ… Named volume for data persistence
- âœ… Alpine image for minimal footprint

### âœ… Service Dependencies â€” CORRECT

All 4 cached services include:
```yaml
depends_on:
  redis:
    condition: service_healthy
```

Services wait for Redis health check before starting. âœ…

### âœ… Auth Service â€” NO Redis Dependency

`auth-service` does NOT depend on Redis in Docker Compose. Correct. âœ…

### âœ… Connection Pool Configuration â€” CONSISTENT

All services configure Lettuce pool with appropriate values:
- `max-active=16` (user/skill/session), `8` (notification)
- `max-idle=8`/`4`, `min-idle=2`/`1`
- `timeout=3000ms`

---

## PART 12: DOCUMENTATION AUDIT

### âœ… doc6 Created â€” Comprehensive CQRS + Redis Architecture Doc

[doc6_cqrs_redis_architecture.md](file:///f:/SkillSync/docs/doc6_cqrs_redis_architecture.md) covers:
- Cache-aside pattern explanation âœ…
- Cache key namespace + TTL table âœ…
- Event-driven sync flows âœ…
- Saga + cache consistency âœ…
- Graceful degradation âœ…
- Security exclusions âœ…
- Trade-offs document âœ…

### âš ï¸ Documentation vs Implementation Gaps

| Topic | doc6 Says | Actual Implementation |
|-------|-----------|----------------------|
| Cache keys | `user:profile:{userId}` | Actual: `v1:user:profile:{userId}` â€” **missing `v1:` prefix in docs** |
| CacheService API | `put(key, value, ttlSeconds)` | Actual: `put(key, value, Duration)` â€” **TTL param is Duration, not long** |
| Stampede protection | Not mentioned in doc6 | Implemented via `getOrLoad()` with ReentrantLock |
| Null sentinel caching | Not mentioned | Implemented via `putNull()` |
| Micrometer metrics | Mentioned briefly | Fully implemented with tagged counters |

### âš ï¸ doc6 underrepresents the actual implementation quality

The documentation describes a simpler version than what's actually implemented. The code is MORE sophisticated than the docs suggest (stampede protection, null sentinels, versioned keys, per-key locking).

---

## ðŸ“Š SUMMARY SCORECARD

### 1. âœ… What Is Implemented Correctly

- CQRS pattern â€” clean command/query separation across all services
- Cache-aside pattern â€” with stampede protection, penetration protection, versioned keys
- Graceful degradation â€” Redis failures never crash the API
- Micrometer metrics â€” hit/miss/evict/error counters per service
- Event-driven cache sync â€” `ReviewEventCacheSyncConsumer` idempotent updates
- Saga + cache consistency â€” both success and compensation paths invalidate cache
- Security exclusions â€” Auth, payment, JWT, OTP data never cached
- Docker config â€” Redis with AOF, LRU, health checks, named volumes
- All TTLs defined â€” domain-specific, no infinite TTLs
- Controller delegation â€” clean command/query separation at API layer

### 2. âš ï¸ Minor Issues (9 total)

| # | Issue | Severity | Location |
|---|-------|----------|----------|
| 1 | `CacheService` duplicated 4Ã— (~880 lines) | Minor | All services |
| 2 | `RedisConfig` duplicated 4Ã— | Minor | All services |
| 3 | CommandService imports QueryService (for mapper) | Minor | All services |
| 4 | `searchSkills()` not cached | Minor | SkillQueryService |
| 5 | Redundant `evict()` after `evictByPattern()` in ReviewCommandService | Minor | ReviewCommandService:56-58 |
| 6 | `KEYS` command used for pattern eviction | Minor | CacheService.evictByPattern() |
| 7 | Stampede protection is per-JVM only | Minor | CacheService.getOrLoad() |
| 8 | No circuit breaker on Redis | Minor | CacheService |
| 9 | Event ordering not guaranteed (RabbitMQ) | Minor | ReviewEventCacheSyncConsumer |

### 3. âŒ Critical Issues (3 total)

| # | Issue | Severity | Location | Fix Required |
|---|-------|----------|----------|-------------|
| **C1** | **`removeAvailability()` missing cache invalidation** | ðŸ”´ HIGH | [MentorCommandService:147-149](file:///f:/SkillSync/user-service/src/main/java/com/skillsync/user/service/command/MentorCommandService.java#L147-L149) | Must look up mentor from slot, then call `invalidateMentorCaches()` |
| **C2** | **Test keys don't use `v1:` prefix** | ðŸŸ¡ MEDIUM | All `*ServiceTest.java` | Tests verify `"user:profile:100"` but code uses `"v1:user:profile:100"` â€” tests may pass but are not testing real behavior |
| **C3** | **`apply()` doesn't invalidate pending list cache** | ðŸŸ¡ MEDIUM | [MentorCommandService:40-65](file:///f:/SkillSync/user-service/src/main/java/com/skillsync/user/service/command/MentorCommandService.java#L40-L65) | Add `cacheService.evictByPattern(CacheService.vKey("user:mentor:pending:*"))` |

### 4. ðŸš€ Improvements Recommended

| # | Improvement | Priority | Effort |
|---|-------------|----------|--------|
| 1 | Extract `CacheService` to shared Maven module | High | 2 hours |
| 2 | Add cache HIT test scenarios (verify DB never called) | High | 1 hour |
| 3 | Fix test key prefixes to match `v1:` versioned keys | High | 30 min |
| 4 | Add `ReviewEventCacheSyncConsumer` unit test | Medium | 1 hour |
| 5 | Add saga + cache integration test | Medium | 2 hours |
| 6 | Add Redis failure/degradation test | Medium | 1 hour |
| 7 | Reduce Redis timeout from 3000ms to 500-1000ms | Medium | 5 min |
| 8 | Add circuit breaker (Resilience4j) around Redis | Low | 2 hours |
| 9 | Migrate `KEYS` to `SCAN` for pattern eviction | Low | 1 hour |
| 10 | Extract `mapToResponse()` to dedicated Mapper classes | Low | 1 hour |
| 11 | Update doc6 to document stampede protection + null sentinels + versioned keys | Medium | 30 min |

### 5. ðŸ“Š Production Readiness Score

| Category | Score | Notes |
|----------|-------|-------|
| CQRS Separation | 9/10 | Clean, consistent, well-structured |
| Cache Strategy | 9/10 | Beyond basic cache-aside (stampede + penetration protection) |
| Cache Invalidation | 7/10 | Missing `removeAvailability()` + `apply()` invalidation |
| Event-Driven Sync | 8/10 | Working, idempotent, but no tests |
| Saga Consistency | 10/10 | Both paths covered, correct implementation |
| Failure Handling | 8/10 | Graceful degradation âœ…, but no circuit breaker |
| Security | 10/10 | Auth, payment, JWT all excluded from caching |
| Testing | 6/10 | Key prefix mismatch, missing hit tests, no event tests |
| DevOps | 9/10 | Proper Docker config, health checks, dependencies |
| Docs | 7/10 | Good but underrepresents actual implementation quality |
| **Overall** | **8.3/10** | |

### 6. ðŸ§  Final Verdict

## **NEEDS MINOR WORK** â€” Ready for demo/evaluation after fixing 3 issues

The implementation is architecturally sound and exceeds typical production quality in several areas (stampede protection, null sentinel caching, versioned keys, Micrometer metrics). The CQRS separation is clean and consistent.

**Before production/demo:**
1. Fix `removeAvailability()` cache invalidation (5 min fix)
2. Fix `apply()` missing pending list invalidation (2 min fix)
3. Fix test key prefixes to match `v1:` versioning (30 min)

**These 3 fixes bring the system to a solid 9/10 production readiness score.**

---

### Documentation Recommendations Based on Findings

| Document | Action Needed |
|----------|--------------|
| **doc6** | Update to document: versioned keys (`v1:` prefix), stampede protection (`getOrLoad()` with `ReentrantLock`), null sentinel caching (`putNull()`), Micrometer metrics counters |
| **doc4** | Fix test examples to use `CacheService.vKey()` prefix; add cache HIT test example |
| **backend_testing_guide** | Add missing test scenarios: cache hit, Redis failure, event sync |
| **doc2** | Update CacheService code sample to show `Duration` API instead of `long ttlSeconds`; add `getOrLoad()` signature |

