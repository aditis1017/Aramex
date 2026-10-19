package com.customcharge.persistencelayer.config.aws;

import com.customcharge.persistencelayer.messaging.TraceSqsInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnClass(SqsAsyncClient.class)
@ConditionalOnProperty(prefix = "mvp.persistence.aws.sqs", name = "enabled", havingValue = "true")
public class SQSTopologyConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SQSTopologyConfiguration.class);

    @EventListener(ContextRefreshedEvent.class)
    public void createQueuesOnStartup(ContextRefreshedEvent event) {
        ApplicationContext ctx = event.getApplicationContext();
        SqsAsyncClient sqsClient;
        try {
            sqsClient = ctx.getBean(SqsAsyncClient.class);
        } catch (Exception e) {
            log.debug("SqsAsyncClient not available, skipping SQS queue creation");
            return;
        }
        SQSProperties properties = ctx.getBean(SQSProperties.class);
        if (!properties.isCreateQueuesOnStartup()) {
            log.debug("SQS queue creation on startup disabled; skipping topology setup");
            return;
        }

        for (SQSProperties.QueueConfig qc : properties.getQueues()) {
            if (qc.getName() == null || qc.getName().isBlank()) {
                continue;
            }
            createQueueIfNotExists(sqsClient, qc);
        }
    }

    private void createQueueIfNotExists(SqsAsyncClient sqsClient, SQSProperties.QueueConfig qc) {
        String queueName = qc.getName();
        if (qc.isFifo() && !queueName.endsWith(".fifo")) {
            queueName = queueName + ".fifo";
        }

        Map<QueueAttributeName, String> attributes = buildQueueAttributes(qc);

        try {
            CreateQueueRequest.Builder requestBuilder = CreateQueueRequest.builder()
                    .queueName(queueName);
            if (!attributes.isEmpty()) {
                requestBuilder.attributes(attributes);
            }
            sqsClient.createQueue(requestBuilder.build()).join();
            log.info("Created SQS queue: {}", queueName);
        } catch (Exception e) {
            if (isAlreadyExistsError(e)) {
                log.debug("SQS queue already exists: {}", queueName);
            } else {
                log.warn("Could not create SQS queue '{}': {}", queueName, e.getMessage());
            }
        }
    }

    private Map<QueueAttributeName, String> buildQueueAttributes(SQSProperties.QueueConfig qc) {
        Map<QueueAttributeName, String> attrs = new HashMap<>();
        if (qc.getDelaySeconds() != null && qc.getDelaySeconds() >= 0) {
            attrs.put(QueueAttributeName.DELAY_SECONDS, String.valueOf(qc.getDelaySeconds()));
        }
        if (qc.getMaximumMessageSize() != null && qc.getMaximumMessageSize() > 0) {
            attrs.put(QueueAttributeName.MAXIMUM_MESSAGE_SIZE, String.valueOf(qc.getMaximumMessageSize()));
        }
        if (qc.getMessageRetentionPeriod() != null && qc.getMessageRetentionPeriod() > 0) {
            attrs.put(QueueAttributeName.MESSAGE_RETENTION_PERIOD, String.valueOf(qc.getMessageRetentionPeriod()));
        }
        if (qc.getReceiveMessageWaitTimeSeconds() != null && qc.getReceiveMessageWaitTimeSeconds() >= 0) {
            attrs.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, String.valueOf(qc.getReceiveMessageWaitTimeSeconds()));
        }
        if (qc.getVisibilityTimeout() != null && qc.getVisibilityTimeout() > 0) {
            attrs.put(QueueAttributeName.VISIBILITY_TIMEOUT, String.valueOf(qc.getVisibilityTimeout()));
        }
        if (qc.isFifo()) {
            attrs.put(QueueAttributeName.FIFO_QUEUE, "true");
        }
        return attrs;
    }

    private static boolean isAlreadyExistsError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (msg.contains("already exists")) {
            return true;
        }
        Throwable cause = e.getCause();
        return cause != null && cause.getMessage() != null
                && cause.getMessage().toLowerCase().contains("already exists");
    }

    @Bean
    public MessageChannel sqsMessageChannel() {
        ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel();
        channel.addInterceptor(new TraceSqsInterceptor());
        return channel;
    }
}
