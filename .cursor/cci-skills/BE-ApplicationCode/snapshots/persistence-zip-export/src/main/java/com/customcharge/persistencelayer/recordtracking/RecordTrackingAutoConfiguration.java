package com.customcharge.persistencelayer.recordtracking;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@EnableConfigurationProperties(RecordTrackingProperties.class)
@ConditionalOnProperty(prefix = "customcharge.record-tracking", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackageClasses = RecordTrackingLogger.class)
public class RecordTrackingAutoConfiguration {
}
