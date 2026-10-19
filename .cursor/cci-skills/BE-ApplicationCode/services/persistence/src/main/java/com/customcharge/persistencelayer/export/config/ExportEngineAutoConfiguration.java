package com.customcharge.persistencelayer.export.config;

import com.customcharge.persistencelayer.config.aws.S3Properties;
import com.customcharge.persistencelayer.config.datasource.DataSourceConfiguration;
import com.customcharge.persistencelayer.config.jpa.JpaConfiguration;
import com.customcharge.persistencelayer.export.registry.DefaultExportPipelineRegistry;
import com.customcharge.persistencelayer.export.registry.ExportPipeline;
import com.customcharge.persistencelayer.export.registry.ExportPipelineRegistry;
import com.customcharge.persistencelayer.export.repository.ExportJobRepository;
import com.customcharge.persistencelayer.export.service.ExportJobProgressService;
import com.customcharge.persistencelayer.export.service.ExportJobRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "mvp.persistence.export", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ExportEngineProperties.class)
@AutoConfigureAfter({DataSourceConfiguration.class, JpaConfiguration.class})
public class ExportEngineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ExportJobProgressService.class)
    public ExportJobProgressService exportJobProgressService(ExportJobRepository exportJobRepository) {
        return new ExportJobProgressService(exportJobRepository);
    }

    @Bean
    public ExportPipelineRegistry exportPipelineRegistry(List<ExportPipeline> pipelines) {
        return new DefaultExportPipelineRegistry(pipelines);
    }

    @Bean
    public ExportJobRunner exportJobRunner(
            ExportPipelineRegistry exportPipelineRegistry,
            ObjectMapper objectMapper,
            ExportJobProgressService exportJobProgressService,
            ExportJobRepository exportJobRepository,
            ObjectProvider<S3Client> s3Client,
            ObjectProvider<S3Properties> s3Properties,
            ApplicationEventPublisher applicationEventPublisher,
            ExportEngineProperties exportEngineProperties) {
        return new ExportJobRunner(
                exportPipelineRegistry,
                objectMapper,
                exportJobProgressService,
                exportJobRepository,
                s3Client,
                s3Properties,
                applicationEventPublisher,
                exportEngineProperties);
    }

    @Bean(name = "exportJobLauncherExecutor")
    public ThreadPoolTaskExecutor exportJobLauncherExecutor(ExportEngineProperties props) {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("export-job-");
        ex.setCorePoolSize(props.getLauncherCorePoolSize());
        ex.setMaxPoolSize(props.getLauncherMaxPoolSize());
        ex.setQueueCapacity(props.getLauncherQueueCapacity());
        ex.initialize();
        return ex;
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "exportSseScheduler")
    public ScheduledExecutorService exportSseScheduler() {
        return Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "export-sse");
            t.setDaemon(true);
            return t;
        });
    }

    @Bean
    @ConditionalOnClass(S3Presigner.class)
    @ConditionalOnBean(S3Client.class)
    @ConditionalOnMissingBean(S3Presigner.class)
    public S3Presigner exportS3Presigner(S3Client s3Client, ObjectProvider<S3Properties> s3Properties) {
        S3Presigner.Builder builder = S3Presigner.builder().s3Client(s3Client);
        S3Properties props = s3Properties.getIfAvailable();
        if (props != null && props.getRegion() != null && !props.getRegion().isBlank()) {
            builder.region(Region.of(props.getRegion()));
        }
        return builder.build();
    }
}
