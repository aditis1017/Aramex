package com.customcharge.persistencelayer.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new RedisCacheService(redisTemplate, new ObjectMapper());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void get_shouldReturnEmpty_whenRedisThrows() {
        when(valueOperations.get("key")).thenThrow(
                new IllegalStateException("LettuceConnectionFactory has been STOPPED"));

        assertThat(cacheService.get("key", String.class)).isEmpty();
    }

    @Test
    void put_shouldNotThrow_whenRedisThrows() {
        doThrow(new IllegalStateException("LettuceConnectionFactory has been STOPPED"))
                .when(valueOperations)
                .set("key", "value", Duration.ofMinutes(1));

        cacheService.put("key", "value", Duration.ofMinutes(1));

        verify(valueOperations).set("key", "value", Duration.ofMinutes(1));
    }
}
