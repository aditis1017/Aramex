package com.customcharge.persistencelayer.security.entra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import java.util.List;

/**
 * Stateless JWT security for servlet applications consuming this library.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableScheduling
@ConditionalOnEntraOidcSecurityEnabled
public class EntraOidcWebSecurityConfiguration {

    @Bean
    public EntraJwtAuthenticationFilter entraJwtAuthenticationFilter(
            EntraOidcSecurityProperties properties,
            EntraRedisConfigurationService secretsService,
            JwtValidationService jwtValidationService,
            JwtUserIdentityExtractor identityExtractor,
            EntraUserValidationService userValidationService,
            JwtTokenRevocationService tokenRevocationService,
            EntraSecurityResponseWriter responseWriter,
            ApplicationUserAuthoritiesService applicationUserAuthoritiesService) {
        return new EntraJwtAuthenticationFilter(
                properties,
                secretsService,
                jwtValidationService,
                identityExtractor,
                userValidationService,
                tokenRevocationService,
                responseWriter,
                applicationUserAuthoritiesService);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain entraSecurityFilterChain(
            HttpSecurity http,
            EntraJwtAuthenticationFilter entraJwtAuthenticationFilter,
            EntraAuthenticationEntryPoint authenticationEntryPoint,
            EntraAccessDeniedHandler accessDeniedHandler,
            EntraOidcSecurityProperties properties) throws Exception {

        List<String> publicPatterns = properties.getPublicPaths().stream()
                .filter(p -> p != null && !p.isBlank())
                .map(String::trim)
                .toList();

        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> {
                    if (!publicPatterns.isEmpty()) {
                        reg.requestMatchers(EntraPublicPathMatchers.fromPatterns(publicPatterns)).permitAll();
                    }
                    reg.anyRequest().authenticated();
                })
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(entraJwtAuthenticationFilter, AuthorizationFilter.class);

        return http.build();
    }
}
