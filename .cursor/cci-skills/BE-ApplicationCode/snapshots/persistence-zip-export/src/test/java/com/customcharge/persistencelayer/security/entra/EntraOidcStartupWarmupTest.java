package com.customcharge.persistencelayer.security.entra;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.net.URI;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntraOidcStartupWarmupTest {

    @Mock
    private EntraRedisConfigurationService secretsService;

    @Mock
    private OidcDiscoveryService discoveryService;

    @Mock
    private JwksCacheService jwksCacheService;

    private EntraOidcStartupWarmup warmup;

    @BeforeEach
    void setUp() {
        warmup = new EntraOidcStartupWarmup(secretsService, discoveryService, jwksCacheService);
    }

    @Test
    void run_skipsWhenSecretsMissing() {
        when(secretsService.getCurrentSecrets()).thenReturn(Optional.empty());

        warmup.run(mock(ApplicationArguments.class));

        verifyNoInteractions(discoveryService, jwksCacheService);
    }

    @Test
    void run_prefetchesJwksWhenSecretsPresent() {
        EntraRuntimeSecrets secrets =
                new EntraRuntimeSecrets("tid", "cid", "https://login.example.com/tenant/v2.0");
        when(secretsService.getCurrentSecrets()).thenReturn(Optional.of(secrets));
        URI jwks = URI.create("https://login.example.com/tenant/discovery/v2.0/keys");
        when(discoveryService.getDiscovery("https://login.example.com/tenant/v2.0"))
                .thenReturn(OidcDiscoveryDocument.fromIssuerAndJwks("https://login.example.com/tenant/v2.0", jwks));
        when(jwksCacheService.getJwksJson(jwks.toString())).thenReturn("{\"keys\":[]}");

        warmup.run(mock(ApplicationArguments.class));

        verify(jwksCacheService).getJwksJson(jwks.toString());
    }
}
