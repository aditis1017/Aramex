package com.customcharge.shipment.ingest.adapter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "shipment.ingest.adapter")
public class IngestAdapterProperties {

    /**
     * When true, Rabbit ingest is handled only by {@code ingest.adapter} (forward to SQS).
     * When false, {@code ShipmentDirectRabbitIngestListener} processes Rabbit directly.
     */
    private boolean enabled;

    private boolean enabledsqs;

    /**
     * SQS queue name/url passed to {@link com.customcharge.persistencelayer.messaging.MessagePublisher}.
     */
    private String targetSqsQueue = "custom-charges-shipment-ingest";
}
