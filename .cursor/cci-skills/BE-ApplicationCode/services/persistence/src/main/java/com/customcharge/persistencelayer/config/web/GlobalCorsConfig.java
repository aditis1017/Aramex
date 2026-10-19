package com.customcharge.persistencelayer.config.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class GlobalCorsConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    public GlobalCorsConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    /**
     * Used by Spring Security ({@code http.cors()}) so browser preflight (OPTIONS) receives CORS headers
     * before JWT authentication runs.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", buildCorsConfiguration());
        return source;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        CorsConfiguration config = buildCorsConfiguration();
        var registration = registry.addMapping("/**");
        if (config.getAllowedOrigins() != null) {
            registration.allowedOrigins(config.getAllowedOrigins().toArray(new String[0]));
        }
        if (config.getAllowedMethods() != null) {
            registration.allowedMethods(config.getAllowedMethods().toArray(new String[0]));
        }
        if (config.getAllowedHeaders() != null) {
            registration.allowedHeaders(config.getAllowedHeaders().toArray(new String[0]));
        }
        registration.allowCredentials(Boolean.TRUE.equals(config.getAllowCredentials()));
    }

    private CorsConfiguration buildCorsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = corsProperties.getAllowedOrigins();
        if (origins != null && !origins.isEmpty()) {
            config.setAllowedOrigins(origins);
        }
        applyWildcardOrValues(
                corsProperties.getAllowedMethods(),
                config::setAllowedMethods,
                config::addAllowedMethod);
        applyWildcardOrValues(
                corsProperties.getAllowedHeaders(),
                config::setAllowedHeaders,
                config::addAllowedHeader);
        config.setAllowCredentials(Boolean.TRUE.equals(corsProperties.getAllowCredentials()));
        return config;
    }

    private static void applyWildcardOrValues(
            List<String> values,
            java.util.function.Consumer<List<String>> setter,
            java.util.function.Consumer<String> adder) {
        if (values == null || values.isEmpty()) {
            return;
        }
        if (values.size() == 1 && "*".equals(values.get(0))) {
            adder.accept(CorsConfiguration.ALL);
        } else {
            setter.accept(values);
        }
    }
}
