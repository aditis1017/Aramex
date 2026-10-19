package com.customcharge.persistencelayer.config.aws;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mvp.persistence.aws.lambda")
public class LambdaProperties {

    private boolean enabled = false;
    private String region;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String functionName;

}