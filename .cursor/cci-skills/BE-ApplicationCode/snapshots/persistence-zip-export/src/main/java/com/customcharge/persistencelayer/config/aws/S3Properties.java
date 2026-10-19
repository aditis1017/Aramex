package com.customcharge.persistencelayer.config.aws;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mvp.persistence.aws.s3")
public class S3Properties {

    private boolean enabled = false;
    private String region;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String defaultBucket;
}
