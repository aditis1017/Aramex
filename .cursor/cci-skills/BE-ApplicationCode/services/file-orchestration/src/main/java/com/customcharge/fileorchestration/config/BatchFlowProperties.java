package com.customcharge.fileorchestration.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "file-orchestration.batch")
@Getter
@Setter
public class BatchFlowProperties {
    @NotBlank
    private String inputQueue;
    @NotBlank
    private String outputQueue;
    @Min(1)
    private int chunkSize = 200;
    @Min(1)
    private int listenerConcurrency = 1;
    @NotEmpty
    private List<String> supportedFileTypes = List.of("CSV", "XLSX");

    private boolean dedupEnabled = true;

    /** Redis lock/progress TTL in seconds (crash safety net). */
    @Min(60)
    private long redisTtlSeconds = 1800;

    /** Refresh Redis TTL every N published chunks during long runs. */
    @Min(1)
    private int heartbeatEveryChunks = 25;

    /** Retries for transient output SQS publish failures. */
    @Min(1)
    private int publishMaxAttempts = 3;

    /** Base backoff millis between publish retries (exponential). */
    @Min(1)
    private long publishBackoffMillis = 200;
}
