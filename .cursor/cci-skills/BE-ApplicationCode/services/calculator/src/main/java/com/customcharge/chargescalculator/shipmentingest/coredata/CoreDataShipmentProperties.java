package com.customcharge.chargescalculator.shipmentingest.coredata;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "custom-charge.shipment-details-api")
public class CoreDataShipmentProperties {

    private boolean enabled = true;

    private String url =
            "http://devbe01/corp.shippingapi.coredata.v1/api/DataUtilities/GetShipmentsDetails";

    private Duration connectTimeout = Duration.ofSeconds(5);

    private Duration readTimeout = Duration.ofSeconds(30);
}
