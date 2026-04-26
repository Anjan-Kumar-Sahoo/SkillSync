package com.skillsync.cache;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RedisConfigTest {

    private final RedisConfig redisConfig = new RedisConfig();

    @Test
    void shouldCreateRedisTemplateWithExpectedSerializers() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        RedisTemplate<String, Object> template = redisConfig.redisTemplate(connectionFactory);

        assertNotNull(template);
        assertSame(connectionFactory, template.getConnectionFactory());
        assertInstanceOf(StringRedisSerializer.class, template.getKeySerializer());
        assertInstanceOf(StringRedisSerializer.class, template.getHashKeySerializer());
        assertInstanceOf(GenericJackson2JsonRedisSerializer.class, template.getValueSerializer());
        assertInstanceOf(GenericJackson2JsonRedisSerializer.class, template.getHashValueSerializer());
    }

    @Test
    void shouldSerializeAndDeserializeJavaTimePayload() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisTemplate<String, Object> template = redisConfig.redisTemplate(connectionFactory);

        Object payload = new HashMap<>(Map.of("createdAt", LocalDateTime.of(2026, 4, 16, 10, 30)));
        @SuppressWarnings("unchecked")
        RedisSerializer<Object> serializer = (RedisSerializer<Object>) template.getValueSerializer();
        byte[] serialized = serializer.serialize(payload);

        assertNotNull(serialized);
        Object deserialized = serializer.deserialize(serialized);
        assertNotNull(deserialized);
        assertInstanceOf(Map.class, deserialized);
    }
}
