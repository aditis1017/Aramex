package com.customcharge.dashboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "dashboard.entity-charges")
public class DashboardEntityChargeProperties {

    /**
     * JPA query timeout in milliseconds for view-based entity-charge search (0 = no hint).
     */
    private int queryTimeoutMs;
}
