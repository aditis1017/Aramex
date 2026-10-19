package com.customcharge.persistencelayer.security.entra;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntraRedisConfigurationServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOps;

    private EntraRedisConfigurationService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        EntraOidcSecurityProperties properties = new EntraOidcSecurityProperties();
        properties.setRetryMaxAttempts(1);
        service = new EntraRedisConfigurationService(redis, properties);
    }

    @Test
    void refreshFromRedis_success() {
        when(valueOps.get(EntraRedisConfigurationKeys.TENANT_ID)).thenReturn("tid");
        when(valueOps.get(EntraRedisConfigurationKeys.CLIENT_ID)).thenReturn("cid");
        when(valueOps.get(EntraRedisConfigurationKeys.ISSUER_URL)).thenReturn("https://login.example/tenant/v2.0");

        Optional<EntraRuntimeSecrets> secrets = service.refreshFromRedis();

        assertThat(secrets).isPresent();
        assertThat(secrets.get().tenantId()).isEqualTo("tid");
        assertThat(secrets.get().clientId()).isEqualTo("cid");
    }

    @Test
    void requireCurrentSecrets_throwsWhenIncomplete() {
        when(valueOps.get(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.requireCurrentSecrets())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Entra configuration");
    }

    @Test
    void refreshFromRedis_handlesRedisFailureWithEmptyCache() {
        when(valueOps.get(anyString())).thenThrow(new QueryTimeoutException("timeout"));

        Optional<EntraRuntimeSecrets> secrets = service.refreshFromRedis();

        assertThat(secrets).isEmpty();
    }
}
