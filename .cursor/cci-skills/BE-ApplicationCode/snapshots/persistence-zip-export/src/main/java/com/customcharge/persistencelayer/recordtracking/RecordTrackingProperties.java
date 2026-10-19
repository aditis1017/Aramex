package com.customcharge.persistencelayer.recordtracking;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "customcharge.record-tracking")
public class RecordTrackingProperties {

    private boolean enabled = true;
    private boolean logRecordCompleted = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLogRecordCompleted() {
        return logRecordCompleted;
    }

    public void setLogRecordCompleted(boolean logRecordCompleted) {
        this.logRecordCompleted = logRecordCompleted;
    }
}
