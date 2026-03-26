package com.skillsync.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Production-grade Redis cache wrapper.
 * Provides Cache-Aside, Stampede protection, Penetration protection,
 * Versioned keys, and Micrometer metrics.
 */
@Service
@Slf4j
public class CacheService {

    private static final String KEY_VERSION = "v1";
    private static final Duration NULL_SENTINEL_TTL = Duration.ofSeconds(60);
    private static final String NULL_SENTINEL = "__NULL__";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter cacheEvictCounter;
    private final Counter cacheErrorCounter;

    private final ConcurrentHashMap<String, ReentrantLock> keyLocks = new ConcurrentHashMap<>();

    public CacheService(RedisTemplate<String, Object> redisTemplate, 
                        MeterRegistry meterRegistry,
                        @Value("${spring.application.name:unknown-service}") String serviceName) {
        this.redisTemplate = redisTemplate;
        this.cacheHitCounter = Counter.builder("cache.operations")
                .tag("result", "hit").tag("service", serviceName).register(meterRegistry);
        this.cacheMissCounter = Counter.builder("cache.operations")
                .tag("result", "miss").tag("service", serviceName).register(meterRegistry);
        this.cacheEvictCounter = Counter.builder("cache.operations")
                .tag("result", "evict").tag("service", serviceName).register(meterRegistry);
        this.cacheErrorCounter = Counter.builder("cache.operations")
                .tag("result", "error").tag("service", serviceName).register(meterRegistry);
    }

    public static String vKey(String rawKey) {
        return KEY_VERSION + ":" + rawKey;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                if (NULL_SENTINEL.equals(value)) {
                    log.debug("Cache HIT (null-sentinel): {}", key);
                    cacheHitCounter.increment();
                    return null;
                }
                log.debug("Cache HIT: {}", key);
                cacheHitCounter.increment();
                return (T) value;
            }
            log.debug("Cache MISS: {}", key);
            cacheMissCounter.increment();
        } catch (Exception e) {
            log.warn("Redis GET failed for key={}: {}. Falling back to DB.", key, e.getMessage());
            cacheErrorCounter.increment();
        }
        return null;
    }

    public boolean isNullSentinel(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return NULL_SENTINEL.equals(value);
        } catch (Exception e) { return false; }
    }

    public void put(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Cache PUT: {} (TTL={}s)", key, ttl.getSeconds());
        } catch (Exception e) {
            log.warn("Redis PUT failed for key={}: {}", key, e.getMessage());
            cacheErrorCounter.increment();
        }
    }

    public void putNull(String key) {
        try {
            redisTemplate.opsForValue().set(key, NULL_SENTINEL, NULL_SENTINEL_TTL);
            log.debug("Cache PUT (null-sentinel): {}", key);
        } catch (Exception e) {
            log.warn("Redis PUT null-sentinel failed for key={}: {}", key, e.getMessage());
            cacheErrorCounter.increment();
        }
    }

    public void evict(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.info("[CACHE] EVICT: {} (deleted={})", key, deleted);
            cacheEvictCounter.increment();
        } catch (Exception e) {
            log.warn("Redis EVICT failed for key={}: {}", key, e.getMessage());
            cacheErrorCounter.increment();
        }
    }

    public void evictByPattern(String pattern) {
        try {
            List<String> keys = new ArrayList<>();
            redisTemplate.execute((org.springframework.data.redis.connection.RedisConnection connection) -> {
                try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(100).build())) {
                    while (cursor.hasNext()) {
                        keys.add(new String(cursor.next()));
                    }
                } catch (Exception ex) {
                    log.warn("Error inside SCAN loop: {}", ex.getMessage());
                }
                return null;
            });
            
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("[CACHE] EVICT pattern SCAN: {} ({} keys)", pattern, keys.size());
                cacheEvictCounter.increment(keys.size());
            }
        } catch (Exception e) {
            log.warn("Redis EVICT pattern failed for pattern={}: {}", pattern, e.getMessage());
            cacheErrorCounter.increment();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> dbFallback) {
        T cached = get(key, type);
        if (cached != null) return cached;
        if (isNullSentinel(key)) return null;

        ReentrantLock lock = keyLocks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            cached = get(key, type);
            if (cached != null) return cached;
            if (isNullSentinel(key)) return null;

            T value = dbFallback.get();
            if (value != null) { put(key, value, ttl); }
            else { putNull(key); }
            return value;
        } finally {
            lock.unlock();
            keyLocks.remove(key, lock);
        }
    }
}
