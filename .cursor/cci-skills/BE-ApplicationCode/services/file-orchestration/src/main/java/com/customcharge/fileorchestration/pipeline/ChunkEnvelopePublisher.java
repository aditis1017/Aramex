package com.customcharge.fileorchestration.pipeline;

import com.customcharge.fileorchestration.config.BatchFlowProperties;
import com.customcharge.fileorchestration.dto.ProcessedChunkMessage;
import com.customcharge.persistencelayer.messaging.MessagePublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ChunkEnvelopePublisher {

    private static final Logger log = LoggerFactory.getLogger(ChunkEnvelopePublisher.class);
    private static final String CURRENCY_KEY = "currency";
    public static final int FILE_COMPLETION_CHUNK_INDEX = 0;

    private final BatchFlowProperties properties;
    private final MessagePublisher messagePublisher;
    private final ObjectMapper objectMapper;

    public ChunkEnvelopePublisher(
            BatchFlowProperties properties,
            @Qualifier("sqsPublisher") MessagePublisher messagePublisher,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.messagePublisher = messagePublisher;
        this.objectMapper = objectMapper;
    }

    public int publishDataChunk(
            FileProcessingContext context,
            List<? extends Map<String, String>> records) throws JsonProcessingException {
        if (records == null || records.isEmpty()) {
            return context.publishedDataChunkCount();
        }
        int chunkIndex = context.nextChunkIndex();
        FileProcessingRequest request = context.request();

        ProcessedChunkMessage payload = buildBasePayload(context);
        payload.setChunkIndex(chunkIndex);
        payload.setRecordCount(records.size());
        payload.setLastChunk(false);
        payload.setRecords(enrichRecords(records, context.defaultCurrencyCode()));

        sendWithRetry(objectMapper.writeValueAsString(payload));
        log.info("Published chunk fileId={} chunkIndex={} records={} traceId={}",
                request.fileId(), chunkIndex, records.size(), request.traceId());
        return chunkIndex;
    }

    public void publishFileCompletionEnvelope(FileProcessingContext context) throws JsonProcessingException {
        FileProcessingRequest request = context.request();
        int dataChunksPublished = context.publishedDataChunkCount();

        ProcessedChunkMessage payload = buildBasePayload(context);
        payload.setChunkIndex(FILE_COMPLETION_CHUNK_INDEX);
        payload.setRecordCount(0);
        payload.setTotalChunks(dataChunksPublished);
        payload.setLastChunk(true);
        payload.setRecords(List.of());

        sendWithRetry(objectMapper.writeValueAsString(payload));
        log.info("Published file completion envelope fileId={} dataChunksPublished={} traceId={}",
                request.fileId(), dataChunksPublished, request.traceId());
    }

    private void sendWithRetry(String body) {
        int maxAttempts = properties.getPublishMaxAttempts();
        long backoff = properties.getPublishBackoffMillis();
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                messagePublisher.send(properties.getOutputQueue(), body);
                return;
            } catch (RuntimeException ex) {
                last = ex;
                log.warn("Output SQS publish attempt {}/{} failed: {}", attempt, maxAttempts, ex.getMessage());
                if (attempt < maxAttempts) {
                    sleepQuietly(backoff * attempt);
                }
            }
        }
        throw new IllegalStateException("Failed to publish to output queue after " + maxAttempts + " attempts", last);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during publish backoff", ie);
        }
    }

    private static ProcessedChunkMessage buildBasePayload(FileProcessingContext context) {
        FileProcessingRequest request = context.request();
        ProcessedChunkMessage payload = new ProcessedChunkMessage();
        payload.setUploadedBy(request.uploadedBy());
        payload.setDocumentPath(request.documentPath());
        payload.setFileName(request.fileName());
        payload.setFileType(request.fileType());
        payload.setFileId(request.fileId());
        payload.setTraceId(request.traceId());
        payload.setProcessFor(request.processFor());
        payload.setEntityCode(request.entityCode());
        payload.setFileProcessingStartedAtEpochMs(context.processingStartedAtEpochMs());
        return payload;
    }

    static List<Map<String, String>> enrichRecords(
            List<? extends Map<String, String>> records,
            String defaultCurrencyCode) {
        List<Map<String, String>> enriched = new ArrayList<>(records.size());
        for (Map<String, String> record : records) {
            enriched.add(enrichRecord(record, defaultCurrencyCode));
        }
        return enriched;
    }

    static Map<String, String> enrichRecord(Map<String, String> record, String defaultCurrencyCode) {
        Map<String, String> copy = new LinkedHashMap<>(record);
        if (!StringUtils.hasText(defaultCurrencyCode) || hasCurrencyKey(copy)) {
            return copy;
        }
        copy.put(CURRENCY_KEY, defaultCurrencyCode.strip());
        return copy;
    }

    private static boolean hasCurrencyKey(Map<String, String> record) {
        for (String key : record.keySet()) {
            if (key != null && CURRENCY_KEY.equals(key.strip().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
