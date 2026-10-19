package com.customcharge.shipment.ingest.worker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "shipment.ingest.worker")
public class IngestWorkerProperties {

    private String sqsQueue = "custom-charges-shipment-ingest";
    private int listenerConcurrency = 5;
}
