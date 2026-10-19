package com.customcharge.chargescalculator.controller;

import com.customcharge.persistencelayer.dto.response.HealthResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final String applicationName;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    public HealthController(
            @Value("${spring.application.name}") String applicationName,
            ObjectProvider<BuildProperties> buildPropertiesProvider) {
        this.applicationName = applicationName;
        this.buildPropertiesProvider = buildPropertiesProvider;
    }

    @GetMapping
    public HealthResponse getHealth() {
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        return HealthResponse.builder()
                .status("UP")
                .timestamp(LocalDateTime.now())
                .application(applicationName)
                .applicationVersion(buildProperties != null ? buildProperties.getVersion() : null)
                .build();
    }
}
