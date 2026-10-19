package com.customcharge.persistencelayer.config.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@ConfigurationProperties(prefix = "mvp.persistence.rabbitmq")
public class RabbitMQProperties implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQProperties.class);
    private static final String DEFAULT_TOPOLOGY_LOCATION = "classpath:rabbitmq.json";

    private ApplicationContext applicationContext;

    private boolean enabled = true;
    private String exchangeName = "orchestration-exchange";
    private boolean durable = true;
    private List<QueueBinding> queues = new ArrayList<>();

    private String topologyConfigLocation = DEFAULT_TOPOLOGY_LOCATION;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void setQueues(List<QueueBinding> queues) {
        this.queues = queues != null ? queues : new ArrayList<>();
    }

    public void loadFromJsonIfPresent() {
        if (applicationContext == null) {
            log.warn("ApplicationContext not yet set, skipping RabbitMQ topology load from JSON");
            return;
        }
        if (topologyConfigLocation == null || topologyConfigLocation.isBlank()) {
            return;
        }
        try {
            Resource resource = applicationContext.getResource(topologyConfigLocation);
            if (!resource.exists()) {
                log.debug("RabbitMQ topology config not found at {}, using properties/YAML only", topologyConfigLocation);
                return;
            }
            ObjectMapper objectMapper = applicationContext.getBean(ObjectMapper.class);
            try (InputStream is = resource.getInputStream()) {
                TopologyConfigFile file = objectMapper.readValue(is, TopologyConfigFile.class);
                if (file.getExchangeName() != null && !file.getExchangeName().isBlank()) {
                    this.exchangeName = file.getExchangeName();
                }
                if (file.getDurable() != null) {
                    this.durable = file.getDurable();
                }
                if (file.getQueues() != null && !file.getQueues().isEmpty()) {
                    this.queues = new ArrayList<>(file.getQueues());
                    log.info("Loaded RabbitMQ topology from {}: exchange={}, queues={}",
                            topologyConfigLocation, exchangeName, queues.size());
                }
            }
        } catch (Exception e) {
            log.warn("Could not load RabbitMQ topology from {}: {}. Using properties/YAML only.",
                    topologyConfigLocation, e.getMessage());
        }
    }

    @Getter
    @Setter
    public static class QueueBinding {
        private String name;
        private String routingKey;
    }

    @Getter
    @Setter
    public static class TopologyConfigFile {
        private String exchangeName;
        private Boolean durable;
        private List<QueueBinding> queues = Collections.emptyList();
    }
}
