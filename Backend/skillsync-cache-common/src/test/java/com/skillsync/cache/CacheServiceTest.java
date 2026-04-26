package com.skillsync.cache;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RedisConnection redisConnection;

    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new CacheService(redisTemplate, new SimpleMeterRegistry(), "test-service");
    }

    @Test
    void shouldPrefixVersionToCacheKey() {
        assertEquals("v1:user:42", CacheService.vKey("user:42"));
    }

    @Test
    void shouldReturnCachedValueOnHit() {
        when(valueOperations.get("user:1")).thenReturn("cached-value");

        String value = cacheService.get("user:1", String.class);

        assertEquals("cached-value", value);
    }

    @Test
    void shouldReturnNullForNullSentinelValue() {
        when(valueOperations.get("user:2")).thenReturn("__NULL__");

        String value = cacheService.get("user:2", String.class);

        assertNull(value);
        assertTrue(cacheService.isNullSentinel("user:2"));
    }

    @Test
    void shouldHandleRedisGetExceptionGracefully() {
        when(valueOperations.get("user:3")).thenThrow(new RuntimeException("redis down"));

        String value = cacheService.get("user:3", String.class);

        assertNull(value);
    }

    @Test
    void shouldPutValueWithConfiguredTtl() {
        Duration ttl = Duration.ofMinutes(5);

        cacheService.put("session:1", "payload", ttl);

        verify(valueOperations).set("session:1", "payload", ttl);
    }

    @Test
    void shouldStoreNullSentinelWithShortTtl() {
        cacheService.putNull("missing:user:1");

        verify(valueOperations).set("missing:user:1", "__NULL__", Duration.ofSeconds(60));
    }

    @Test
    void shouldEvictSingleKey() {
        when(redisTemplate.delete("user:5")).thenReturn(true);

        cacheService.evict("user:5");

        verify(redisTemplate).delete("user:5");
    }

    @Test
    @SuppressWarnings("deprecation")
    void shouldEvictKeysByPatternUsingScan() {
        @SuppressWarnings("unchecked")
        Cursor<byte[]> cursor = mock(Cursor.class);
        when(redisConnection.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next())
                .thenReturn("user:10".getBytes(StandardCharsets.UTF_8))
                .thenReturn("user:11".getBytes(StandardCharsets.UTF_8));

        when(redisTemplate.execute(org.mockito.ArgumentMatchers.<RedisCallback<Object>>any())).thenAnswer(invocation -> {
            RedisCallback<Object> callback = invocation.getArgument(0);
            return callback.doInRedis(redisConnection);
        });

        cacheService.evictByPattern("user:*");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> keysCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(redisTemplate).delete(keysCaptor.capture());
        assertTrue(keysCaptor.getValue().contains("user:10"));
        assertTrue(keysCaptor.getValue().contains("user:11"));
        verify(cursor).close();
    }

    @Test
    void shouldLoadFromDbAndCacheValueOnCacheMiss() {
        when(valueOperations.get("profile:1")).thenReturn(null);
        AtomicBoolean fallbackCalled = new AtomicBoolean(false);

        String value = cacheService.getOrLoad(
                "profile:1",
                String.class,
                Duration.ofMinutes(10),
                () -> {
                    fallbackCalled.set(true);
                    return "db-value";
                }
        );

        assertEquals("db-value", value);
        assertTrue(fallbackCalled.get());
        verify(valueOperations).set("profile:1", "db-value", Duration.ofMinutes(10));
    }

    @Test
    void shouldStoreNullSentinelWhenDbFallbackReturnsNull() {
        when(valueOperations.get("profile:2")).thenReturn(null);

        String value = cacheService.getOrLoad(
                "profile:2",
                String.class,
                Duration.ofMinutes(10),
                () -> null
        );

        assertNull(value);
        verify(valueOperations).set(eq("profile:2"), eq("__NULL__"), eq(Duration.ofSeconds(60)));
    }

    @Test
    void shouldSkipDbFallbackWhenNullSentinelAlreadyExists() {
        when(valueOperations.get("profile:3")).thenReturn("__NULL__");

        String value = cacheService.getOrLoad(
                "profile:3",
                String.class,
                Duration.ofMinutes(5),
                () -> {
                    fail("DB fallback should not be invoked when null sentinel exists");
                    return "unused";
                }
        );

        assertNull(value);
        verify(valueOperations, never()).set(eq("profile:3"), any(), any(Duration.class));
    }

    @Test
    void shouldReturnFalseWhenNullSentinelCheckThrows() {
        when(valueOperations.get("profile:4")).thenThrow(new RuntimeException("read failed"));

        assertFalse(cacheService.isNullSentinel("profile:4"));
    }
}
