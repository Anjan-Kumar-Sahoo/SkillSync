# 📄 DOCUMENT 6: CQRS + REDIS CACHING ARCHITECTURE

## SkillSync — Command Query Responsibility Segregation & Distributed Caching

---

## 6.1 What is CQRS?

**Command Query Responsibility Segregation (CQRS)** is an architectural pattern that separates read and write operations into distinct service classes.

```
Traditional (single service):
  Controller → Service → Repository → DB

CQRS (split services):
  Controller → CommandService ──→ Repository → DB  (writes + cache invalidation)
               QueryService   ──→ Redis → DB        (reads + cache-aside)
  
  Both Services ──> Mapper ──> DTO (Decoupled mapping logic)
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
Client → QueryService:
  1. Check Redis for key (e.g., "v1:user:profile:100")
  2. HIT → Return cached response immediately
  3. MISS → Query PostgreSQL
  4. Store result in Redis with domain-specific TTL
  5. Return response to client
```

### Write Path (CommandService)

```
Client → CommandService:
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

1. **Shorter TTL for volatile data** — Sessions change status frequently (5 min)
2. **Longer TTL for stable data** — Skills rarely change (1 hour)
3. **Very short TTL for counters** — Unread notification count (2 min)
4. **No caching for sensitive data** — Auth tokens, OTPs, payment secrets are **never** cached

---

## 6.5 CQRS Implementation Per Service

### User Service

```
com.skillsync.user
  +-- cache/
  │   ├── RedisConfig.java          ← Lettuce client + Jackson JSON serialization
  │   └── CacheService.java         ← Generic cache wrapper with graceful degradation
  +-- service/
      +-- command/
      │   ├── UserCommandService     ← Profile CRUD + cache invalidation
      │   ├── MentorCommandService   ← Mentor approval + Saga integration + cache invalidation
      │   └── GroupCommandService    ← Group operations + cache invalidation
      +-- query/
          ├── UserQueryService       ← Cache-aside profile reads
          ├── MentorQueryService     ← Cache-aside mentor reads (search, discovery)
          └── GroupQueryService      ← Cache-aside group reads
  +-- mapper/
      ├── UserMapper                 ← Dedicated static mapping methods
      ├── MentorMapper               ← Dedicated static mapping methods
      ├── GroupMapper                ← Dedicated static mapping methods
      └── PaymentMapper              ← Dedicated static mapping methods
```

### Skill Service

```
com.skillsync.skill
  +-- cache/
  │   ├── RedisConfig.java
  │   └── CacheService.java
  +-- config/
  │   └── RabbitMQConfig.java       ← Skill event exchange
  +-- event/
  │   └── SkillEvent.java           ← Event DTO for cross-service sync
  +-- service/
      +-- command/
      │   └── SkillCommandService   ← Skill CRUD + cache invalidation + event publishing
      +-- query/
          └── SkillQueryService     ← Cache-aside skill reads (autocomplete, catalog)
  +-- mapper/
      └── SkillMapper               ← Dedicated static mapping methods
```

### Session Service

```
com.skillsync.session
  +-- cache/
  │   ├── RedisConfig.java
  │   └── CacheService.java
  +-- service/
      +-- command/
      │   ├── SessionCommandService ← Session lifecycle + cache invalidation
      │   └── ReviewCommandService  ← Review submission + cache invalidation + event publishing
      +-- query/
          ├── SessionQueryService   ← Cache-aside session reads
          └── ReviewQueryService    ← Cache-aside review reads (rating summary, distribution)
  +-- mapper/
      ├── SessionMapper             ← Dedicated static mapping methods
      └── ReviewMapper              ← Dedicated static mapping methods
```

### Notification Service

```
com.skillsync.notification
  +-- cache/
  │   ├── RedisConfig.java
  │   └── CacheService.java
  +-- service/
      +-- command/
      │   └── NotificationCommandService ← Create + push + cache invalidation
      +-- query/
          └── NotificationQueryService   ← Cache-aside unread count
  +-- mapper/
      └── NotificationMapper             ← Dedicated static mapping methods
```

---

## 6.6 Event-Driven Cache Synchronization

### Cross-Service Cache Invalidation via RabbitMQ

When a review is submitted in Session Service, it must invalidate the mentor's cached rating in User Service:

```
Session Service                    RabbitMQ                    User Service
     │                                │                            │
     │  ReviewSubmittedEvent          │                            │
     │  {mentorId, rating, reviewId}  │                            │
     │───────────────────────────────►│                            │
     │                                │  review.submitted          │
     │                                │───────────────────────────►│
     │                                │                            │
     │                                │       ReviewEventCacheSyncConsumer:
     │                                │       1. Update mentor avgRating
     │                                │       2. Evict v1:user:mentor:{mentorId}
     │                                │       3. Evict v1:user:mentor:user:{userId}
     │                                │                            │
```

### Skill Event Sync

When a skill is created/updated/deactivated, the Skill Service publishes to `skill.exchange`:

```
Skill Service                      RabbitMQ
     │                                │
     │  SkillEvent                    │
     │  {skillId, action, name}       │
     │───────────────────────────────►│
     │                                │
     │                 (Future: consumed by User Service
     │                  to invalidate skill-related caches)
```

---

## 6.7 Saga + Cache Consistency

The `PaymentSagaOrchestrator` integrates with the CQRS layer to ensure cache consistency during the Saga lifecycle:

```
PaymentSagaOrchestrator
     │
     ├── SUCCESS PATH:
     │   1. transitionToSuccessPending()        → DB update
     │   2. MentorCommandService.approveMentor() → DB update + cache evict
     │   3. markPaymentSuccess()                → DB update
     │   4. Publish payment.success event       → RabbitMQ
     │
     └── COMPENSATION PATH:
         1. MentorCommandService.revertMentorApproval() → DB revert + cache evict
         2. markPaymentCompensated()                    → DB update
         3. Publish payment.compensated event           → RabbitMQ
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
GET /actuator/health      → Includes Redis health
GET /actuator/metrics     → Cache hit/miss ratios
GET /actuator/caches      → Registered cache names
```

### Key Metrics to Monitor

| Metric | What It Tells You |
|--------|-------------------|
| `cache.gets{result=hit}` | Cache hit count — should be high |
| `cache.gets{result=miss}` | Cache miss count — triggers DB query |
| `cache.evictions` | Number of evictions (LRU or explicit) |
| `cache.puts` | Number of items added to cache |
| Redis `used_memory` | Memory usage — should stay below 256MB |
| Redis `connected_clients` | Connection pool usage |

---

## 6.11 Security Exclusions

> [!CAUTION]
> The following are **explicitly excluded** from Redis caching:
> - **Auth Service** — Entirely excluded. No caching of JWT tokens, passwords, OTPs, or verification codes
> - **Payment secrets** — Razorpay API keys, signatures, and order secrets are never cached
> - **Session tokens** — Refresh tokens and access tokens are managed in PostgreSQL only
> - **User passwords** — BCrypt hashes are never stored in Redis

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

1. **Always use `CacheService.vKey("domain:entity:id")`** → produces `v1:domain:entity:id`
2. **Never hardcode the `v1:` prefix** — the version is managed centrally in `CacheService.KEY_VERSION`
3. **Key format**: `v1:<service-domain>:<entity-type>:<identifier>`
4. **Pattern eviction**: Use `CacheService.evictByPattern("v1:domain:*")` for bulk eviction

### Current Version

```java
private static final String KEY_VERSION = "v1";
```

### Migration

To migrate cache keys to a new version:
1. Update `KEY_VERSION` in `CacheService.java`
2. Deploy all services — old keys expire naturally via TTL
3. No manual Redis flush required

### Audit Status

✅ **All services verified** — No hardcoded cache keys found. All keys use `CacheService.vKey()`.

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

1. **Redis Sentinel/Cluster** — For high availability in production
2. **Cache warming** — Pre-populate hot data on service startup
3. **Read replicas** — Separate Redis read replicas for query-heavy services
4. **Grafana dashboard** — Real-time cache hit/miss ratios and memory usage via Prometheus
5. **Distributed locking** — Redis-based distributed locks for concurrent write scenarios

---

> [!NOTE]
> This document is the authoritative reference for all CQRS and Redis caching architecture decisions in SkillSync.
> All services follow the patterns described here. Any deviations must be documented with rationale.
