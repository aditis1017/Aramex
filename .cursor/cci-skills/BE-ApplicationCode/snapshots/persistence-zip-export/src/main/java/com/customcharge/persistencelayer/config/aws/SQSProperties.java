package com.customcharge.persistencelayer.config.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "mvp.persistence.aws.sqs")
public class SQSProperties implements InitializingBean, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(SQSProperties.class);
    private static final String DEFAULT_QUEUE_CONFIG_LOCATION = "classpath:sqs-queues.json";

    @Getter(AccessLevel.NONE)
    private ApplicationContext applicationContext;

    private boolean enabled = false;
    /**
     * When false (default), do not call SQS CreateQueue on startup; queues must already exist in AWS.
     */
    private boolean createQueuesOnStartup = false;
    private String region;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private List<QueueConfig> queues = new ArrayList<>();

    private String queueConfigLocation = DEFAULT_QUEUE_CONFIG_LOCATION;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void setQueues(List<QueueConfig> queues) {
        this.queues = queues != null ? queues : new ArrayList<>();
    }

    @Override
    public void afterPropertiesSet() {
        loadFromJsonIfPresent();
    }

    public void loadFromJsonIfPresent() {
        if (applicationContext == null) {
            log.warn("ApplicationContext not yet set, skipping SQS queue config load from JSON");
            return;
        }
        if (queueConfigLocation == null || queueConfigLocation.isBlank()) {
            return;
        }
        try {
            Resource resource = applicationContext.getResource(queueConfigLocation);
            if (!resource.exists()) {
                log.debug("SQS queue config not found at {}, using properties/YAML only", queueConfigLocation);
                return;
            }
            ObjectMapper objectMapper = applicationContext.getBean(ObjectMapper.class);
            try (InputStream is = resource.getInputStream()) {
                QueueConfigFile file = objectMapper.readValue(is, QueueConfigFile.class);
                if (file.getQueues() != null && !file.getQueues().isEmpty()) {
                    this.queues = new ArrayList<>(file.getQueues());
                    log.info("Loaded SQS queues from {}: {} queues", queueConfigLocation, queues.size());
                }
            }
        } catch (Exception e) {
            log.warn("Could not load SQS queue config from {}: {}. Using properties/YAML only.",
                    queueConfigLocation, e.getMessage());
        }
    }

    @Getter
    @Setter
    public static class QueueConfig {
        private String name;
        private Integer delaySeconds;
        private Integer maximumMessageSize;
        private Integer messageRetentionPeriod;
        private Integer receiveMessageWaitTimeSeconds;
        private Integer visibilityTimeout;
        private boolean fifo = false;
    }

    @Getter
    @Setter
    public static class QueueConfigFile {
        private List<QueueConfig> queues = Collections.emptyList();
    }
}
