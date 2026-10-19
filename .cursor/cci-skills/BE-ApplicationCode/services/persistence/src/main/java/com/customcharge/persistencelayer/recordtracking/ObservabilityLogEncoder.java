package com.customcharge.persistencelayer.recordtracking;

import net.logstash.logback.encoder.LogstashEncoder;
import net.logstash.logback.fieldnames.LogstashFieldNames;

/**
 * Slim JSON encoder for {@code record_tracking} and {@code customcharge_metric} logs.
 * Emits only structured arguments (explicit {@code addKeyValue} fields), not the full MDC map.
 */
public class ObservabilityLogEncoder extends LogstashEncoder {

    public ObservabilityLogEncoder() {
        setIncludeMdc(false);
        setIncludeStructuredArguments(true);
        setIncludeCallerData(false);
        setIncludeContext(false);

        LogstashFieldNames fieldNames = new LogstashFieldNames();
        fieldNames.setVersion("[ignore]");
        fieldNames.setThread("[ignore]");
        fieldNames.setLogger("[ignore]");
        fieldNames.setLevelValue("[ignore]");
        setFieldNames(fieldNames);
    }
}
