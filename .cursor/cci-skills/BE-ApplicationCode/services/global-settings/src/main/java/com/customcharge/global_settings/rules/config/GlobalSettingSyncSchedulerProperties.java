package com.customcharge.global_settings.rules.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "global-settings.sync")
public class GlobalSettingSyncSchedulerProperties {

    private boolean onStartupEnabled = true;
    private boolean scheduledEnabled = true;
    private String cron = "1 0 0 * * ?";
    private String zone = "UTC";
}
