package com.customcharge.dashboard.config;

import com.customcharge.persistencelayer.messaging.MessagePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * When {@code mvp.persistence.aws.sqs.enabled=false} (e.g. local {@code dev} profile),
 * persistence-layer {@code MessagingConfiguration} does not run and no {@code sqsPublisher}
 * bean exists. File upload and other services still require that bean — register a no-op
 * implementation so the context starts; messages are not sent to AWS.
 */
@Configuration
@Slf4j
public class NoOpSqsPublisherConfiguration {

    @Bean("sqsPublisher")
    @ConditionalOnProperty(prefix = "mvp.persistence.aws.sqs", name = "enabled", havingValue = "false")
    public MessagePublisher noopSqsPublisher() {
        return (destination, payload) -> log.debug(
                "SQS disabled (mvp.persistence.aws.sqs.enabled=false); not sending. queue={} payloadType={}",
                destination,
                payload != null ? payload.getClass().getSimpleName() : "null");
    }
}
