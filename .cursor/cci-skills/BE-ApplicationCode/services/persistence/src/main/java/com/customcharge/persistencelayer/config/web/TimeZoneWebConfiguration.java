package com.customcharge.persistencelayer.config.web;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.customcharge.persistencelayer.serializer.InstantTimeZoneSerializer;
import com.customcharge.persistencelayer.web.TimeZoneHandlerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Instant;

@Configuration
@ConditionalOnWebApplication
public class TimeZoneWebConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TimeZoneHandlerInterceptor()).addPathPatterns("/**");
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer instantTimeZoneSerializationCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addSerializer(Instant.class, new InstantTimeZoneSerializer());
            builder.modulesToInstall(module);
        };
    }
}
