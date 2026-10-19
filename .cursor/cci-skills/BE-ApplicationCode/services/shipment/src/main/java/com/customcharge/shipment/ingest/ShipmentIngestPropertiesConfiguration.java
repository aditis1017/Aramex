package com.customcharge.shipment.ingest;

import com.customcharge.shipment.ingest.adapter.config.IngestAdapterProperties;
import com.customcharge.shipment.ingest.worker.config.IngestWorkerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({IngestAdapterProperties.class, IngestWorkerProperties.class})
public class ShipmentIngestPropertiesConfiguration {
}
