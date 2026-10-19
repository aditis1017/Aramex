package com.customcharge.persistencelayer.config.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.customcharge.persistencelayer.accountRedis.AccountRedisStringCache;
import com.customcharge.persistencelayer.accountRedis.DefaultAccountRedisStringCache;
import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.cache.RedisCacheService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;

import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Single Redis connection for the persistence layer (cache, account Redis, Entra OIDC config/JWKS cache).
 * Configure via {@code mvp.persistence.redis.*} only — do not add a separate Redis connection for Entra.
 */
@Configuration
@EnableConfigurationProperties(RedisProperties.class)
@ConditionalOnPersistenceRedisEnabled
public class RedisConfiguration {

    public static final String REDIS_CONNECTION_FACTORY_BEAN = "redisConnectionFactory";

    @Bean(name = REDIS_CONNECTION_FACTORY_BEAN)
    public RedisConnectionFactory redisConnectionFactory(RedisProperties properties) {

        RedisStandaloneConfiguration config =
                new RedisStandaloneConfiguration(
                        properties.getHost(),
                        properties.getPort());

        config.setDatabase(properties.getDatabase());

        if (properties.getPassword() != null && !properties.getPassword().isEmpty()) {
            config.setPassword(RedisPassword.of(properties.getPassword()));
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder =
                LettuceClientConfiguration.builder();
        if (properties.getCommandTimeoutMs() > 0) {
            clientBuilder.commandTimeout(Duration.ofMillis(properties.getCommandTimeoutMs()));
        }

        return new LettuceConnectionFactory(config, clientBuilder.build());
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();

        return template;
    }

    /**
     * Shared string template for account cache, Entra OIDC config ({@code entra.*} keys), and JWKS cache ({@code entra:jwks}).
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate(
            @org.springframework.beans.factory.annotation.Qualifier(REDIS_CONNECTION_FACTORY_BEAN)
            RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public AccountRedisStringCache accountRedisStringCache(StringRedisTemplate stringRedisTemplate) {
        return new DefaultAccountRedisStringCache(stringRedisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(CacheService.class)
    public CacheService cacheService(
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper) {
        return new RedisCacheService(redisTemplate, objectMapper);
    }
}
