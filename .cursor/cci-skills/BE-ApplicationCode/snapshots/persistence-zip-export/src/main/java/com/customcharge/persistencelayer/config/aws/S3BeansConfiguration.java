package com.customcharge.persistencelayer.config.aws;

import com.customcharge.persistencelayer.util.aws.DirectS3UploadStrategy;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import com.customcharge.persistencelayer.util.aws.LambdaS3UploadStrategy;
import com.customcharge.persistencelayer.util.aws.S3Helper;
import com.customcharge.persistencelayer.util.aws.S3StorageService;
import com.customcharge.persistencelayer.util.aws.S3UploadStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@AutoConfigureAfter(AWSClientConfiguration.class)
@ConditionalOnClass({S3Client.class})
@ConditionalOnProperty(prefix = "mvp.persistence.aws.s3", name = "enabled", havingValue = "true")
public class S3BeansConfiguration {

    @Bean
    @ConditionalOnBean(S3Client.class)
    @ConditionalOnMissingBean
    public S3Helper s3Helper(S3Client s3Client, S3Properties s3Properties) {
        return new S3Helper(s3Client, s3Properties);
    }

    @Bean("directS3UploadStrategy")
    @ConditionalOnBean(S3Helper.class)
    @ConditionalOnMissingBean(name = "directS3UploadStrategy")
    public S3UploadStrategy directS3UploadStrategy(S3Helper s3Helper) {
        return new DirectS3UploadStrategy(s3Helper);
    }

    @Bean("lambdaS3UploadStrategy")
    @ConditionalOnBean(LambdaClient.class)
    @ConditionalOnMissingBean(name = "lambdaS3UploadStrategy")
    public S3UploadStrategy lambdaS3UploadStrategy(LambdaClient lambdaClient) {
        return new LambdaS3UploadStrategy(lambdaClient);
    }

    @Bean("lambdaS3UploadStrategy")
    @ConditionalOnMissingBean(name = "lambdaS3UploadStrategy")
    public S3UploadStrategy unsupportedLambdaS3UploadStrategy() {
        return (bucket, key, data) -> {
            throw new UnsupportedOperationException(
                    "Lambda S3 upload is not available. Set mvp.persistence.aws.lambda.enabled=true to enable.");
        };
    }

    @Bean
    @ConditionalOnBean(name = "directS3UploadStrategy")
    @ConditionalOnMissingBean
    public S3StorageService s3StorageService(S3Helper s3Helper,
                                              S3UploadStrategy directS3UploadStrategy,
                                              S3UploadStrategy lambdaS3UploadStrategy) {
        return new S3StorageService(s3Helper, directS3UploadStrategy, lambdaS3UploadStrategy);
    }
}
