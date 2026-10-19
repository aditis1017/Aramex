package com.customcharge.chargescalculator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "custom-charge")
public class CustomChargeProperties {

    private Sqs sqs = new Sqs();
    private Chunk chunk = new Chunk();
    private ErrorReport errorReport = new ErrorReport();

    @Getter
    @Setter
    public static class Sqs {
        private boolean listenerEnabled;
        private String batchOutputQueue;
        private int listenerConcurrency = 10;
    }

    @Getter
    @Setter
    public static class Chunk {
        /**
         * When true, rows within a chunk are processed concurrently (one transaction per row).
         * Total concurrent rows per pod is capped by {@link #maxRowConcurrency}.
         */
        private boolean parallelRowsEnabled = true;
        /**
         * Max concurrent rows per chunk per pod (virtual-thread permit count).
         * Keep below datasource maximum-pool-size (reserve connections for chunk dedup and error writes).
         */
        private int maxRowConcurrency = 40;
        /**
         * When false, per-row start/complete logs are emitted at DEBUG instead of INFO (recommended for bulk files).
         */
        private boolean rowLoggingEnabled = false;
    }

    @Getter
    @Setter
    public static class ErrorReport {
        private Integer presignedUrlExpiryMinutes = 1440;
        private String uploadPrefix = "error-reports";
        private Integer pageSize = 50;
        private Integer excelRowAccessWindowSize = 10;
        private Integer asyncCorePoolSize = 2;
        private Integer asyncMaxPoolSize = 4;
        private Integer asyncQueueCapacity = 20;
    }
}
