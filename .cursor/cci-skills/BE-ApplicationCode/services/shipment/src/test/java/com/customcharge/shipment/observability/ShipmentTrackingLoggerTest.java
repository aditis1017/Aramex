package com.customcharge.shipment.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentTrackingLoggerTest {

    private ShipmentTrackingLogger trackingLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        trackingLogger = new ShipmentTrackingLogger();
        Logger logger = (Logger) LoggerFactory.getLogger(ShipmentTrackingLogger.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @Test
    void logEntered_shouldEmitShipmentTrackingWithIngestChannel() {
        trackingLogger.logEntered("HAWB-1", "RUH", ShipmentTrackingLogger.CHANNEL_RABBIT);

        assertThat(appender.list)
                .extracting(ILoggingEvent::getMessage)
                .contains("shipment_tracking");
    }

    @Test
    void logFilterPassed_shouldEmitShipmentTrackingMessage() {
        trackingLogger.logFilterPassed("HAWB-1", "RUH");

        assertThat(appender.list)
                .extracting(ILoggingEvent::getMessage)
                .contains("shipment_tracking");
    }

    @Test
    void logDuplicateSkipped_shouldEmitShipmentTrackingMessage() {
        trackingLogger.logDuplicateSkipped("HAWB-1", "RUH");

        assertThat(appender.list)
                .extracting(ILoggingEvent::getMessage)
                .contains("shipment_tracking");
    }

    @Test
    void logCreated_shouldEmitShipmentTrackingMessage() {
        trackingLogger.logCreated("HAWB-1", "RUH");

        assertThat(appender.list)
                .extracting(ILoggingEvent::getMessage)
                .contains("shipment_tracking");
    }

    @Test
    void logFailedAwb_shouldEmitShipmentTrackingMessage() {
        trackingLogger.logFailedAwb(
                "HAWB-1", "incomplete shipment", "AE", "incomplete", ShipmentTrackingLogger.CHANNEL_SQS);

        assertThat(appender.list)
                .extracting(ILoggingEvent::getMessage)
                .contains("shipment_tracking");
    }
}
