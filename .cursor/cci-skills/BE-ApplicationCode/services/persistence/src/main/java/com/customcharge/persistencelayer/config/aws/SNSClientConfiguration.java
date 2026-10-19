package com.customcharge.persistencelayer.config.aws;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(AWSClientConfiguration.class)
public class SNSClientConfiguration {
}