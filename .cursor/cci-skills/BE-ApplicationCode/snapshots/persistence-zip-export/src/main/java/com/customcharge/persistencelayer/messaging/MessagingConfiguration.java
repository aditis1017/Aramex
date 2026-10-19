package com.customcharge.persistencelayer.messaging;

import io.awspring.cloud.sqs.listener.QueueNotFoundStrategy;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "io.awspring.cloud.sqs.operations.SqsTemplate")
@ConditionalOnProperty(prefix = "mvp.persistence.aws.sqs", name = "enabled", havingValue = "true", matchIfMissing = false)
public class MessagingConfiguration {

    @Bean
    @ConditionalOnMissingBean(SqsTemplate.class)
    public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
        return SqsTemplate.builder()
                .sqsAsyncClient(sqsAsyncClient)
                .configure(options -> options.queueNotFoundStrategy(QueueNotFoundStrategy.FAIL))
                .build();
    }

    @Bean("sqsPublisher")
    @ConditionalOnMissingBean(name = "sqsPublisher")
    public MessagePublisher sqsMessagePublisher(SqsTemplate sqsTemplate) {
        return new SqsMessagePublisher(sqsTemplate);
    }

}