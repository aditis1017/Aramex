package com.customcharge.persistencelayer.security.entra;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Clock;

/**
 * Low-level beans for Entra OIDC HTTP clients (kept as {@code @Bean} because of {@link Qualifier} wiring).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnEntraOidcSecurityEnabled
class EntraHttpConfiguration {

    @Bean
    @Qualifier("entraSecurityClock")
    Clock entraSecurityClock() {
        return Clock.systemUTC();
    }

    @Bean
    @Qualifier("entraSecurityWebClient")
    WebClient entraSecurityWebClient(EntraOidcSecurityProperties properties) {
        int connectTimeoutMs = (int) Math.min(properties.getHttpConnectTimeout().toMillis(), Integer.MAX_VALUE);
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(properties.getHttpReadTimeout());
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
