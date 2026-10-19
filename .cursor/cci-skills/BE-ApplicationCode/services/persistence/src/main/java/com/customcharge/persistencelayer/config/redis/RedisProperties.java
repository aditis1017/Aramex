package com.customcharge.persistencelayer.config.redis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mvp.persistence.redis")
public class RedisProperties {
    private boolean enabled = true;
    private String host = "localhost";
    private int port = 6379;
    private String password;
    private int database = 0;
    /** Max time to wait for a Redis command; 0 uses Lettuce default. */
    private int commandTimeoutMs = 500;
    /** TCP connect timeout; 0 uses Lettuce default. */
    private int connectTimeoutMs = 2000;
}