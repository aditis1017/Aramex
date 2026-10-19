package com.customcharge.persistencelayer.recordtracking;

import com.customcharge.persistencelayer.messaging.MessagePublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

/**
 * Applies record-tracking MDC from chunk payloads before SQS publish without changing callers.
 * Skips file-completion envelopes ({@code lastChunk=true}).
 */
public class RecordTrackingMessagePublisherDecorator implements MessagePublisher {

    private final MessagePublisher delegate;
    private final ObjectMapper objectMapper;
    private final String serviceName;

    public RecordTrackingMessagePublisherDecorator(
            MessagePublisher delegate,
            ObjectMapper objectMapper,
            String serviceName) {
        this.delegate = delegate;
        this.objectMapper = objectMapper;
        this.serviceName = serviceName;
    }

    @Override
    public void send(String destination, Object payload) {
        if (!isTrackableDataPayload(payload)) {
            delegate.send(destination, payload);
            return;
        }
        RecordTrackingGuard.runSafely(() -> {
            applyContextFromPayload(payload);
            RecordTrackingTimeline.markChunkStart();
        });
        delegate.send(destination, payload);
    }

    private boolean isTrackableDataPayload(Object payload) {
        if (!(payload instanceof String json) || json.isBlank()) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            Boolean lastChunk = readBoolean(root, "lastChunk");
            int chunkIndex = root.hasNonNull("chunkIndex") ? root.get("chunkIndex").asInt() : -1;
            int recordCount = root.hasNonNull("recordCount") ? root.get("recordCount").asInt() : 0;
            List<?> records = readRecords(root);
            return RecordTrackingChunkSupport.isTrackableDataChunk(lastChunk, chunkIndex, recordCount, records);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void applyContextFromPayload(Object payload) {
        if (!(payload instanceof String json) || json.isBlank()) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            String traceId = textOrNull(root, "traceId");
            String fileId = textOrNull(root, "fileId");
            String fileName = textOrNull(root, "fileName");
            String processFor = textOrNull(root, "processFor");
            String uploadedBy = textOrNull(root, "uploadedBy");
            String entityCode = textOrNull(root, "entityCode");
            JsonNode chunkIndex = root.get("chunkIndex");
            RecordTrackingMdc.putContext(
                    traceId,
                    fileId,
                    chunkIndex != null && !chunkIndex.isNull() ? chunkIndex.asInt() : null,
                    null,
                    serviceName);
            RecordTrackingMdc.putFileName(fileName);
            RecordTrackingMdc.putProcessFor(processFor);
            RecordTrackingMdc.applyFileIdentity(uploadedBy, entityCode);
            RecordTrackingTimeline.markTraceStart();
        } catch (Exception ignored) {
            // MDC population is best-effort
        }
    }

    private static List<?> readRecords(JsonNode root) {
        JsonNode recordsNode = root.get("records");
        if (recordsNode == null || recordsNode.isNull() || !recordsNode.isArray() || recordsNode.isEmpty()) {
            return Collections.emptyList();
        }
        return List.of(recordsNode.size());
    }

    private static Boolean readBoolean(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asBoolean();
    }

    private static String textOrNull(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value.isBlank() ? null : value;
    }
}
