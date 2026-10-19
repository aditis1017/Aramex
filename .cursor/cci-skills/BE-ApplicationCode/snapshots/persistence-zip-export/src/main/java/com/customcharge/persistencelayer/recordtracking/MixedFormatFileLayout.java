package com.customcharge.persistencelayer.recordtracking;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.encoder.Encoder;
import com.customcharge.persistencelayer.observability.MetricSnapshotPublisher;

import java.nio.charset.StandardCharsets;

/**
 * Writes plain-text lines for application logs and JSON lines for tracking/metrics loggers
 * into a single log file (Logback 1.5+ disallows two file appenders on the same path).
 */
public class MixedFormatFileLayout extends LayoutBase<ILoggingEvent> {

    private static final String TRACKING_LOGGER = RecordTrackingLogger.class.getName();
    private static final String METRIC_SNAPSHOT_LOGGER = MetricSnapshotPublisher.class.getName();

    private String plainPattern;
    private PatternLayout plainLayout;
    private Encoder<ILoggingEvent> jsonEncoder;

    public void setPlainPattern(String plainPattern) {
        this.plainPattern = plainPattern;
    }

    @Override
    public void start() {
        plainLayout = new PatternLayout();
        plainLayout.setContext(context);
        plainLayout.setPattern(plainPattern != null && !plainPattern.isBlank()
                ? plainPattern
                : "%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %5p ${PID:- } --- [%15.15t] %-40.40logger{39} : %m%n");
        plainLayout.start();

        ObservabilityLogEncoder encoder = new ObservabilityLogEncoder();
        encoder.setContext(context);
        encoder.start();
        jsonEncoder = encoder;

        super.start();
    }

    @Override
    public void stop() {
        if (plainLayout != null) {
            plainLayout.stop();
        }
        if (jsonEncoder != null) {
            jsonEncoder.stop();
        }
        super.stop();
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        if (TRACKING_LOGGER.equals(event.getLoggerName()) || METRIC_SNAPSHOT_LOGGER.equals(event.getLoggerName())) {
            try {
                return new String(jsonEncoder.encode(event), StandardCharsets.UTF_8) + CoreConstants.LINE_SEPARATOR;
            } catch (Exception ignored) {
                return plainLayout.doLayout(event);
            }
        }
        return plainLayout.doLayout(event);
    }
}
