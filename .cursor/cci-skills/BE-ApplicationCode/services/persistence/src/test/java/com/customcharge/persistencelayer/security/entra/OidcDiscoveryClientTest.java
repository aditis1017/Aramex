package com.customcharge.persistencelayer.security.entra;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class OidcDiscoveryClientTest {

    private final MockWebServer server = new MockWebServer();

    @BeforeEach
    void start() throws Exception {
        server.start();
    }

    @AfterEach
    void shutdown() throws Exception {
        server.shutdown();
    }

    @Test
    void fetchDiscovery_parsesDocument() {
        String base = server.url("/tenant/v2.0").toString().replaceAll("/+$", "");
        server.enqueue(new MockResponse()
                .setBody("""
                        {
                          "issuer": "%s",
                          "jwks_uri": "%s",
                          "id_token_signing_alg_values_supported": ["RS256"]
                        }
                        """.formatted(base, server.url("/keys").toString()))
                .addHeader("Content-Type", "application/json"));

        EntraOidcSecurityProperties properties = new EntraOidcSecurityProperties();
        WebClient webClient = WebClient.builder().build();
        OidcDiscoveryClient client = new OidcDiscoveryClient(webClient, new ObjectMapper(), properties);

        OidcDiscoveryDocument doc = client.fetchDiscovery(base);

        assertThat(doc.issuer()).isEqualTo(base);
        assertThat(doc.jwksUri()).isEqualTo(URI.create(server.url("/keys").toString()));
        assertThat(doc.idTokenSigningAlgValuesSupported()).containsExactly("RS256");
    }
}
