package com.customcharge.persistencelayer.export.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "mvp.persistence.export")
public class ExportEngineProperties {

    private boolean enabled = false;
    private int chunkSize = 2000;
    private int launcherCorePoolSize = 2;
    private int launcherMaxPoolSize = 8;
    private int launcherQueueCapacity = 100;
    private int sseHeartbeatSeconds = 25;
    private int sseDbPollIntervalMs = 1500;
    private int presignedUrlDurationMinutes = 60;
    private int progressUpdateInterval = 10_000;
}
