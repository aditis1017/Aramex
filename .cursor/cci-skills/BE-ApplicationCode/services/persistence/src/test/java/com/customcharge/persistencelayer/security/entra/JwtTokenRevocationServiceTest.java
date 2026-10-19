package com.customcharge.persistencelayer.security.entra;

import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenRevocationServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOps;

    private JwtTokenRevocationService service;
    private EntraOidcSecurityProperties properties;

    @BeforeEach
    void setUp() {
        properties = new EntraOidcSecurityProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-05-14T12:00:00Z"), ZoneOffset.UTC);
        service = new JwtTokenRevocationService(redis, properties, clock);
    }

    @Test
    void revoke_writesRedisKeysForDigestAndJti() {
        when(redis.opsForValue()).thenReturn(valueOps);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID("jti-abc")
                .expirationTime(Date.from(Instant.parse("2026-05-14T14:00:00Z")))
                .build();

        service.revoke("raw-token-value", claims);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps, org.mockito.Mockito.times(2)).set(keyCaptor.capture(), eq("1"), any(Duration.class));
        assertThat(keyCaptor.getAllValues().get(0)).startsWith("entra:revoked:token:");
        assertThat(keyCaptor.getAllValues()).contains("entra:revoked:jti:jti-abc");
    }

    @Test
    void isRevoked_trueWhenDigestKeyPresent() {
        String token = "same-token";
        String digestKey = EntraRedisConfigurationKeys.revokedTokenDigestKey(JwtTokenRevocationService.sha256Hex(token));
        when(redis.hasKey(digestKey)).thenReturn(true);

        assertThat(service.isRevoked(token, new JWTClaimsSet.Builder().build())).isTrue();
    }

    @Test
    void isRevoked_skippedWhenDisabled() {
        properties.setTokenRevocationEnabled(false);

        assertThat(service.isRevoked("token", new JWTClaimsSet.Builder().build())).isFalse();
        verify(redis, never()).hasKey(any());
    }
}
