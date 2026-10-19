package com.customcharge.shipment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@SpringBootApplication
@ComponentScan("com.customcharge")
@EnableScheduling
public class ShipmentApplication {

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	SecurityFilterChain kubernetesProbeSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher("/health")
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/health").permitAll()
						.anyRequest().permitAll())
				.csrf(AbstractHttpConfigurer::disable);
		return http.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(ShipmentApplication.class, args);
	}

}
