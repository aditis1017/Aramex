package com.customcharge.shipment.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentIngestMetricsTest {

    private SimpleMeterRegistry registry;
    private ShipmentIngestMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new ShipmentIngestMetrics(registry);
    }

    @Test
    void recordRabbitReceived_shouldRegisterCounter() {
        metrics.recordRabbitReceived("direct");

        assertThat(registry.find(ShipmentIngestMetrics.PREFIX + ".rabbit.received").counter())
                .isNotNull();
        assertThat(registry.find(ShipmentIngestMetrics.PREFIX + ".rabbit.received")
                .tag("path", "direct")
                .counter()
                .count()).isEqualTo(1.0D);
    }

    @Test
    void recordMessageEntered_shouldIncrementEnteredAndEntityOutcome() {
        metrics.recordMessageEntered("AE");

        assertThat(registry.find(ShipmentIngestMetrics.PREFIX + ".messages.entered")
                .tag("destination_entity", "AE")
                .counter()
                .count()).isEqualTo(1.0D);
        assertThat(registry.find(ShipmentIngestMetrics.PREFIX + ".entity.outcome")
                .tags("destination_entity", "AE", "outcome", "entered")
                .counter()
                .count()).isEqualTo(1.0D);
    }

    @Test
    void recordEntityFailed_shouldIncrementFailedCountersOnlyForValidationReason() {
        metrics.recordEntityFailed("RUH", "incomplete");

        assertThat(registry.find(ShipmentIngestMetrics.PREFIX + ".entity.outcome")
                .tags("destination_entity", "RUH", "outcome", "failed")
                .counter()
                .count()).isEqualTo(1.0D);
        assertThat(registry.find(ShipmentIngestMetrics.PREFIX + ".validation.failed")
                .tags("reason", "incomplete", "destination_entity", "RUH")
                .counter()).isNull();

        metrics.recordEntityFailed("RUH", "validation");

        assertThat(registry.find(ShipmentIngestMetrics.PREFIX + ".validation.failed")
                .tags("reason", "validation", "destination_entity", "RUH")
                .counter()
                .count()).isEqualTo(1.0D);
    }

    @Test
    void recordEntityCreated_shouldIncrementCreatedOutcome() {
        metrics.recordEntityCreated("RUH");

        assertThat(registry.find(ShipmentIngestMetrics.PREFIX + ".entity.outcome")
                .tags("destination_entity", "RUH", "outcome", "created")
                .counter()
                .count()).isEqualTo(1.0D);
    }

    @Test
    void normalize_shouldReturnUnknownForBlankValues() {
        assertThat(ShipmentIngestMetrics.normalize(null)).isEqualTo("unknown");
        assertThat(ShipmentIngestMetrics.normalize("  ")).isEqualTo("unknown");
        assertThat(ShipmentIngestMetrics.normalize(" AE ")).isEqualTo("AE");
    }
}
