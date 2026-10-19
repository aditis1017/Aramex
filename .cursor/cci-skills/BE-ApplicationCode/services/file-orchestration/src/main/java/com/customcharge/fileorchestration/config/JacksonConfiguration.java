package com.customcharge.fileorchestration.config;

import com.customcharge.persistencelayer.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonUtils.buildObjectMapper();
    }
}
