package com.customcharge.persistencelayer.config.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;

@Configuration
@ConditionalOnClass({
        SqsAsyncClient.class,
        S3Client.class,
        LambdaClient.class
})
@EnableConfigurationProperties({SQSProperties.class, S3Properties.class, LambdaProperties.class})
public class AWSClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AWSClientConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(SqsAsyncClient.class)
    @ConditionalOnProperty(prefix = "mvp.persistence.aws.sqs", name = "enabled", havingValue = "true")
    public SqsAsyncClient sqsAsyncClient(SQSProperties properties) {
        requireNonBlank(properties.getRegion(), "mvp.persistence.aws.sqs.region");
        var builder = SqsAsyncClient.builder().region(Region.of(properties.getRegion()));
        if (hasCredentials(properties.getAccessKey(), properties.getSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())));
            log.debug("SQS client configured with explicit credentials");
        }
        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            try {
                builder.endpointOverride(URI.create(properties.getEndpoint()));
                log.debug("SQS endpoint override: {}", properties.getEndpoint());
            } catch (Exception e) {
                log.warn("Invalid SQS endpoint '{}': {}", properties.getEndpoint(), e.getMessage());
            }
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean(S3Client.class)
    @ConditionalOnProperty(prefix = "mvp.persistence.aws.s3", name = "enabled", havingValue = "true")
    public S3Client s3Client(S3Properties properties) {
        requireNonBlank(properties.getRegion(), "mvp.persistence.aws.s3.region");
        var builder = S3Client.builder().region(Region.of(properties.getRegion()));
        if (hasCredentials(properties.getAccessKey(), properties.getSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())));
            log.debug("S3 client configured with explicit credentials");
        }
        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            try {
                builder.endpointOverride(URI.create(properties.getEndpoint()));
                log.debug("S3 endpoint override: {}", properties.getEndpoint());
            } catch (Exception e) {
                log.warn("Invalid S3 endpoint '{}': {}", properties.getEndpoint(), e.getMessage());
            }
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean(LambdaClient.class)
    @ConditionalOnProperty(prefix = "mvp.persistence.aws.lambda", name = "enabled", havingValue = "true")
    public LambdaClient lambdaClient(LambdaProperties properties) {

        requireNonBlank(properties.getRegion(), "mvp.persistence.aws.lambda.region");

        var builder = LambdaClient.builder()
                .region(Region.of(properties.getRegion()));

        if (hasCredentials(properties.getAccessKey(), properties.getSecretKey())) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(
                                    properties.getAccessKey(),
                                    properties.getSecretKey()
                            )
                    )
            );
        }
        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }
        return builder.build();
    }
    private static void requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must be set");
        }
    }

    private static boolean hasCredentials(String accessKey, String secretKey) {
        return accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank();
    }
}
