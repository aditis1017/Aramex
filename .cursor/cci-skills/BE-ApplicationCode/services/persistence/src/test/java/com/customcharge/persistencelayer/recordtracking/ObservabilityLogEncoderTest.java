package com.customcharge.persistencelayer.recordtracking;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import net.logstash.logback.argument.StructuredArguments;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityLogEncoderTest {

    @Test
    void encode_shouldEmitStructuredArgumentsOnly() {
        ObservabilityLogEncoder encoder = new ObservabilityLogEncoder();
        encoder.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        encoder.start();

        MDC.put("traceId", "trace-1");
        MDC.put("chunkId", "99");
        try {
            LoggingEvent event = new LoggingEvent();
            event.setLoggerName(RecordTrackingLogger.class.getName());
            event.setLevel(Level.INFO);
            event.setMessage("record_tracking");
            event.setTimeStamp(System.currentTimeMillis());
            event.setArgumentArray(new Object[]{
                    StructuredArguments.kv("event", "RECORD_COMPLETED"),
                    StructuredArguments.kv("fileId", "file-1"),
                    StructuredArguments.kv("recordElapsedMs", 42L)
            });

            String json = new String(encoder.encode(event), StandardCharsets.UTF_8);

            assertThat(json).contains("\"event\":\"RECORD_COMPLETED\"");
            assertThat(json).contains("\"fileId\":\"file-1\"");
            assertThat(json).contains("\"recordElapsedMs\":42");
            assertThat(json).doesNotContain("traceId");
            assertThat(json).doesNotContain("chunkId");
            assertThat(json).doesNotContain("logger_name");
            assertThat(json).doesNotContain("thread_name");
        } finally {
            MDC.clear();
            encoder.stop();
        }
    }
}
