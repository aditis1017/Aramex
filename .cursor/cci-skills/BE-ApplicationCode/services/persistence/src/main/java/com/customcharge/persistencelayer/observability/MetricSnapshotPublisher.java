package com.customcharge.persistencelayer.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Periodically exports Micrometer {@link FileProcessingMetrics} meters as structured
 * {@code customcharge_metric} logs for OpenSearch dashboards.
 */
public class MetricSnapshotPublisher {

    private static final Logger log = LoggerFactory.getLogger(MetricSnapshotPublisher.class);

    private final MeterRegistry registry;
    private final CustomChargeMetricsProperties properties;
    private final String serviceName;
    private final Map<String, Double> previousCounterValues = new ConcurrentHashMap<>();

    public MetricSnapshotPublisher(
            MeterRegistry registry,
            CustomChargeMetricsProperties properties,
            String serviceName) {
        this.registry = registry;
        this.properties = properties;
        this.serviceName = serviceName;
    }

    @Scheduled(fixedDelayString = "${customcharge.metrics.snapshot.interval-ms:60000}")
    public void publishSnapshots() {
        if (!properties.getSnapshot().isEnabled()) {
            return;
        }
        try {
            for (Meter meter : registry.getMeters()) {
                if (!isExportableMetric(meter.getId().getName())) {
                    continue;
                }
                publishMeter(meter);
            }
        } catch (Exception ex) {
            log.debug("Metric snapshot export failed: {}", ex.getMessage());
        }
    }

    private void publishMeter(Meter meter) {
        Meter.Id id = meter.getId();
        String metric = toDashboardMetricName(id.getName());
        Map<String, String> tags = tagMap(id.getTags());

        if (meter instanceof Counter counter) {
            double value = counter.count();
            double delta = delta(id, value);
            if (delta <= 0.0D) {
                return;
            }
            LoggingEventBuilder builder = baseBuilder(metric, "counter", tags)
                    .addKeyValue("delta", delta);
            builder.log("customcharge_metric");
            return;
        }

        if (meter instanceof Timer timer) {
            if (timer.count() == 0L) {
                return;
            }
            LoggingEventBuilder builder = baseBuilder(metric, "timer", tags)
                    .addKeyValue("count", timer.count())
                    .addKeyValue("meanMs", timer.mean(TimeUnit.MILLISECONDS))
                    .addKeyValue("p95Ms", percentile(timer, 0.95));
            builder.log("customcharge_metric");
            return;
        }

        if (meter instanceof DistributionSummary summary) {
            if (summary.count() == 0L) {
                return;
            }
            baseBuilder(metric, "distribution", tags)
                    .addKeyValue("count", summary.count())
                    .addKeyValue("mean", summary.mean())
                    .log("customcharge_metric");
        }
    }

    private LoggingEventBuilder baseBuilder(String metric, String metricType, Map<String, String> tags) {
        LoggingEventBuilder builder = log.atInfo()
                .addKeyValue("metric", metric)
                .addKeyValue("metricType", metricType)
                .addKeyValue("service", serviceName);
        applyTag(builder, tags, "process_for", "processFor");
        applyTag(builder, tags, "phase", "phase");
        applyTag(builder, tags, "entity_code", "entityCode");
        applyTag(builder, tags, "uploaded_by", "uploadedBy");
        applyTag(builder, tags, "outcome", "outcome");
        applyTag(builder, tags, "error_code", "errorCode");
        applyTag(builder, tags, "file_type", "fileType");
        applyTag(builder, tags, "destination_entity", "destinationEntity");
        applyTag(builder, tags, "reason", "reason");
        applyTag(builder, tags, "path", "path");
        applyTag(builder, tags, "channel", "channel");
        applyTag(builder, tags, "failure_type", "failureType");
        return builder;
    }

    private static boolean isExportableMetric(String meterName) {
        return meterName.startsWith(FileProcessingMetrics.PREFIX)
                || meterName.startsWith("customcharge.shipment.ingest");
    }

    private static void applyTag(
            LoggingEventBuilder builder,
            Map<String, String> tags,
            String tagKey,
            String fieldName) {
        String value = tags.get(tagKey);
        if (value != null && !value.isBlank() && !"unknown".equals(value)) {
            builder.addKeyValue(fieldName, value);
        }
    }

    private double delta(Meter.Id id, double currentValue) {
        String meterKey = meterKey(id);
        Double previous = previousCounterValues.put(meterKey, currentValue);
        if (previous == null) {
            return currentValue;
        }
        return Math.max(0.0D, currentValue - previous);
    }

    private static String meterKey(Meter.Id id) {
        if (id.getTags().isEmpty()) {
            return id.getName();
        }
        String tagSuffix = id.getTags().stream()
                .sorted(Comparator.comparing(Tag::getKey).thenComparing(Tag::getValue))
                .map(tag -> tag.getKey() + '=' + tag.getValue())
                .collect(Collectors.joining("|"));
        return id.getName() + '|' + tagSuffix;
    }

    private static String toDashboardMetricName(String meterName) {
        if (meterName.startsWith(FileProcessingMetrics.PREFIX + ".")) {
            return meterName.substring((FileProcessingMetrics.PREFIX + ".").length());
        }
        if (meterName.startsWith("customcharge.shipment.ingest.")) {
            return meterName.substring("customcharge.shipment.ingest.".length());
        }
        return meterName;
    }

    private static Map<String, String> tagMap(List<Tag> tags) {
        Map<String, String> map = new HashMap<>();
        for (Tag tag : tags) {
            map.put(tag.getKey(), tag.getValue());
        }
        return map;
    }

    private static double percentile(Timer timer, double quantile) {
        return timer.percentile(quantile, TimeUnit.MILLISECONDS);
    }
}
