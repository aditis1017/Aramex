package com.customcharge.dashboard;

import com.customcharge.dashboard.config.DashboardEntityChargeProperties;
import com.customcharge.dashboard.fileupload.config.FileUploadProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@SpringBootApplication
@ComponentScan("com.customcharge")
@EnableConfigurationProperties({FileUploadProperties.class, DashboardEntityChargeProperties.class})
@EnableCaching
@EnableMethodSecurity
public class DashboardApplication {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain kubernetesProbeSecurityFilterChain(HttpSecurity http) throws Exception {

        http
            .securityMatcher("/health")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health").permitAll()
                .anyRequest().permitAll()
            )
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    public static void main(String[] args) {
        SpringApplication.run(DashboardApplication.class, args);
    }
}
