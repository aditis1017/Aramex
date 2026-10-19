package com.customcharge.persistencelayer.security.entra;

import com.customcharge.persistencelayer.config.redis.ConditionalOnPersistenceRedisEnabled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Entra OIDC beans load only when Entra security is enabled and persistence Redis
 * ({@code mvp.persistence.redis.*} / {@link com.customcharge.persistencelayer.config.redis.RedisConfiguration})
 * is active. Entra reads {@code entra.tenant.id}, {@code entra.client.id}, and {@code entra.issuer.url} from the
 * same {@link org.springframework.data.redis.core.StringRedisTemplate} as the rest of the library.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(prefix = "mvp.persistence.security.entra", name = "enabled", havingValue = "true")
@ConditionalOnPersistenceRedisEnabled
@ConditionalOnClass(name = "org.springframework.security.web.access.AccessDeniedHandler")
public @interface ConditionalOnEntraOidcSecurityEnabled {
}
