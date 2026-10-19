package com.customcharge.persistencelayer.accountRedis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

@Slf4j
@RequiredArgsConstructor
public class DefaultAccountRedisStringCache implements AccountRedisStringCache {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Optional<String> get(String key) {
        try {
            String v = stringRedisTemplate.opsForValue().get(key);
            if (v == null || v.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(v);
        } catch (Exception ex) {
            log.warn("Redis account cache read failed for key {}: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, String value) {

        log.info("Attempting to write to Redis");
        log.info("Redis key = {}", key);
        log.info("Redis value = {}", value);

        stringRedisTemplate.opsForValue().set(key, value);

        String storedValue = stringRedisTemplate.opsForValue().get(key);

        log.info("Redis stored value = {}", storedValue);

        java.util.Set<String> keys = stringRedisTemplate.keys("*");

        log.info("All redis keys = {}", keys);

        Long ttl = stringRedisTemplate.getExpire(key);

        log.info("Redis TTL for key {} = {}", key, ttl);

        RedisConnection connection =
                Objects.requireNonNull(stringRedisTemplate.getConnectionFactory())
                        .getConnection();

        log.info("Redis DB Size = {}", connection.dbSize());

        Properties serverInfo = connection.info("server");

        log.info("Redis server info = {}", serverInfo);

        if (log.isDebugEnabled()) {
            log.debug("Redis account cache put key={}", key);
        }
    }

    @Override
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }
}
