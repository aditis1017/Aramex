package com.customcharge.shipment.config;

import com.customcharge.shipment.constants.ShipmentConstants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "shipment")
public class ShipmentProperties {

    private int monetaryScale = ShipmentConstants.DEFAULT_MONETARY_SCALE;
    private Listener listener = new Listener();
    private Observability observability = new Observability();

    @Getter
    @Setter
    public static class Observability {
        private boolean phaseTimingEnabled = false;
    }

    @Getter
    @Setter
    public static class Listener {
        private int minConcurrency = 5;
        private int maxConcurrency = 12;
        private int prefetch = 5;
    }
}
