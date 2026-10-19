package com.customcharge.global_settings;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@SpringBootApplication
@ComponentScan({"com.customcharge"})
@EnableAsync
@EnableCaching
@EnableScheduling
public class GlobalSettingApplication {

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	SecurityFilterChain kubernetesProbeSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher("/health")
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/health").permitAll()
						.anyRequest().permitAll())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.csrf(AbstractHttpConfigurer::disable);
		return http.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(GlobalSettingApplication.class, args);
	}
}
