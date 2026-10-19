package com.customcharge.shipment.ingest.adapter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "shipment.ingest.adapter", name = "enabled", havingValue = "true")
public class IngestAdapterConfiguration {
}
