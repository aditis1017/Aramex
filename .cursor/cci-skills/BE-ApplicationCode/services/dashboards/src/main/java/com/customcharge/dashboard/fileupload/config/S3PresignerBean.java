package com.customcharge.dashboard.fileupload.config;

import com.customcharge.dashboard.constants.ValidationConstants;
import com.customcharge.persistencelayer.config.aws.S3Properties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@ConditionalOnProperty(prefix = "mvp.persistence.aws.s3", name = "enabled", havingValue = "true")
public class S3PresignerBean {
    @Bean
    @ConditionalOnMissingBean(S3Presigner.class)
    public S3Presigner s3Presigner(S3Properties p) {
        if (p.getRegion() == null || p.getRegion().isBlank()) {
            throw new IllegalStateException(ValidationConstants.S3_REGION_REQUIRED);
        }
        var b = S3Presigner.builder().region(Region.of(p.getRegion().trim()));
        if (p.getAccessKey() != null && !p.getAccessKey().isBlank()
                && p.getSecretKey() != null && !p.getSecretKey().isBlank()) {
            b.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(p.getAccessKey(), p.getSecretKey())));
        }
        if (p.getEndpoint() != null && !p.getEndpoint().isBlank()) {
            b.endpointOverride(URI.create(p.getEndpoint().trim()));
        }
        return b.build();
    }
}
