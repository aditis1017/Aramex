package com.customcharge.shipment.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@ConditionalOnProperty(prefix = "mvp.persistence.redis", name = "enabled", havingValue = "true")
class ShipmentClusterMessageConsumptionStore {

    private static final String KEY_PREFIX = "customcharge:shipment:messages:consumed:minute:";
    private static final Duration KEY_TTL = Duration.ofHours(2);

    private final StringRedisTemplate redisTemplate;

    ShipmentClusterMessageConsumptionStore(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    void incrementCurrentMinute() {
        if (redisTemplate == null) {
            return;
        }
        String key = clusterKeyForMinute(currentMinuteBucket());
        Long value = redisTemplate.opsForValue().increment(key);
        if (value != null && value == 1L) {
            redisTemplate.expire(key, KEY_TTL);
        }
    }

    long countForMinute(long minuteBucket) {
        if (redisTemplate == null) {
            return -1L;
        }
        String value = redisTemplate.opsForValue().get(clusterKeyForMinute(minuteBucket));
        if (value == null || value.isBlank()) {
            return 0L;
        }
        return Long.parseLong(value);
    }

    private static long currentMinuteBucket() {
        return Instant.now().getEpochSecond() / 60L;
    }

    private static String clusterKeyForMinute(long minuteBucket) {
        return KEY_PREFIX + minuteBucket;
    }
}
