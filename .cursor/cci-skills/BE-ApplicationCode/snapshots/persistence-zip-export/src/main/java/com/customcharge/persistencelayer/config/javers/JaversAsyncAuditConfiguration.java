package com.customcharge.persistencelayer.config.javers;

import com.customcharge.persistencelayer.observability.JaversAuditMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.javers.core.Javers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableConfigurationProperties(JaversProperties.class)
@ConditionalOnProperty(prefix = "mvp.persistence.javers.async", name = "enabled", havingValue = "true")
public class JaversAsyncAuditConfiguration {

    @Bean(destroyMethod = "shutdown")
    Executor javersAuditExecutor(JaversProperties javersProperties) {
        int poolSize = Math.max(1, javersProperties.getAsync().getPoolSize());
        AtomicInteger threadIndex = new AtomicInteger();
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("javers-audit-" + threadIndex.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(poolSize, threadFactory);
    }

    @Bean
    JaversAuditMetrics javersAuditMetrics(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        MeterRegistry registry = meterRegistryProvider.getIfAvailable(SimpleMeterRegistry::new);
        return new JaversAuditMetrics(registry);
    }

    @Bean
    JaversAuditAsyncCommitter javersAuditAsyncCommitter(
            Javers javers,
            AuditCommitResolver auditCommitResolver,
            Executor javersAuditExecutor,
            JaversAuditMetrics javersAuditMetrics) {
        return new JaversAuditAsyncCommitter(
                javers, auditCommitResolver, javersAuditExecutor, javersAuditMetrics);
    }
}
