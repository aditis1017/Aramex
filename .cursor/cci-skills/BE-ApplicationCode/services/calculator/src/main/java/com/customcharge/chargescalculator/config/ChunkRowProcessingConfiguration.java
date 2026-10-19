package com.customcharge.chargescalculator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;

@Configuration
public class ChunkRowProcessingConfiguration {

    @Bean(name = "chunkRowExecutor")
    public Executor chunkRowExecutor(CustomChargeProperties customChargeProperties) {
        int maxConcurrency = Math.max(1, customChargeProperties.getChunk().getMaxRowConcurrency());
        return new BoundedVirtualThreadExecutor(maxConcurrency);
    }
}
