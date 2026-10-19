package com.customcharge.persistencelayer.security.entra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches OIDC discovery metadata using {@link WebClient}.
 */
@Service
@ConditionalOnEntraOidcSecurityEnabled
public class OidcDiscoveryClient {

    private static final Logger log = LoggerFactory.getLogger(OidcDiscoveryClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final EntraOidcSecurityProperties properties;

    public OidcDiscoveryClient(
            @Qualifier("entraSecurityWebClient") WebClient entraSecurityWebClient,
            ObjectMapper objectMapper,
            EntraOidcSecurityProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.webClient = entraSecurityWebClient;
    }

    public OidcDiscoveryDocument fetchDiscovery(String issuerBaseUrl) {
        String normalized = issuerBaseUrl.trim().replaceAll("/+$", "");
        String discoveryUrl = normalized + "/.well-known/openid-configuration";
        log.debug("Entra OIDC discovery HTTP GET url={}", discoveryUrl);
        try {
            String json = ResilientExecutor.executeWithRetry(
                    "OIDC discovery GET",
                    properties.getRetryMaxAttempts(),
                    properties.getRetryBackoff(),
                    () -> webClient.get()
                            .uri(discoveryUrl)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block(properties.getHttpReadTimeout()));
            if (json == null || json.isBlank()) {
                throw new IllegalStateException("Empty discovery response");
            }
            OidcDiscoveryDocument document = parse(json);
            log.debug("Entra OIDC discovery OK issuer={} jwksUri={}", document.issuer(), document.jwksUri());
            return document;
        } catch (WebClientResponseException ex) {
            log.debug("Entra OIDC discovery HTTP error status={}", ex.getStatusCode().value());
            throw new IllegalStateException("OIDC discovery HTTP error: " + ex.getStatusCode(), ex);
        }
    }

    private OidcDiscoveryDocument parse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String issuer = text(root, "issuer");
            String jwks = text(root, "jwks_uri");
            List<String> algs = new ArrayList<>();
            if (root.has("id_token_signing_alg_values_supported") && root.get("id_token_signing_alg_values_supported").isArray()) {
                for (JsonNode n : root.get("id_token_signing_alg_values_supported")) {
                    if (n.isTextual()) {
                        algs.add(n.asText());
                    }
                }
            }
            return new OidcDiscoveryDocument(issuer, URI.create(jwks), algs);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Invalid OIDC discovery JSON", ex);
        }
    }

    private static String text(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) {
            throw new IllegalArgumentException("Missing or invalid field in discovery document: " + field);
        }
        return n.asText();
    }
}
