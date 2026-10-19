package com.customcharge.shipment.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(MeterRegistry.class)
public class ShipmentMetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ShipmentIngestMetrics shipmentIngestMetrics(MeterRegistry registry) {
        return new ShipmentIngestMetrics(registry);
    }
}
