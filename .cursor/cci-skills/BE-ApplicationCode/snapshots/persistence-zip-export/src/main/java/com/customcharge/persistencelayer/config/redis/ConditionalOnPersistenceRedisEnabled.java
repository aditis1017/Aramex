package com.customcharge.persistencelayer.config.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Persistence-layer Redis ({@link RedisConfiguration}) is enabled via {@code mvp.persistence.redis.*}.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(prefix = "mvp.persistence.redis", name = "enabled", havingValue = "true")
public @interface ConditionalOnPersistenceRedisEnabled {
}
