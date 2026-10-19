package com.customcharge.shipment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ShipmentProperties.class)
public class ShipmentPropertiesConfiguration {
}
