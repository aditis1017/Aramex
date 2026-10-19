package com.customcharge.chargescalculator.listener;

import com.customcharge.chargescalculator.dto.ProcessedChunkMessage;
import com.customcharge.chargescalculator.service.batch.ChunkProcessingCoordinator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "custom-charge.sqs", name = "listener-enabled", havingValue = "true")
public class BatchOutputQueueListener {

    private static final Logger log = LoggerFactory.getLogger(BatchOutputQueueListener.class);

    private final ObjectMapper objectMapper;
    private final ChunkProcessingCoordinator chunkProcessingCoordinator;
    public BatchOutputQueueListener(
            ObjectMapper objectMapper,
            ChunkProcessingCoordinator chunkProcessingCoordinator) {
        this.objectMapper = objectMapper;
        this.chunkProcessingCoordinator = chunkProcessingCoordinator;
    }

    @SqsListener(
            value = "${custom-charge.sqs.batch-output-queue}",
            maxConcurrentMessages = "${custom-charge.sqs.listener-concurrency}",
            maxMessagesPerPoll = "${custom-charge.sqs.listener-messages-per-poll}")
    public void listen(String payload) {
        if (payload == null || payload.isBlank()) {
            log.warn("Received empty message from batch output queue — acknowledging poison message");
            return;
        }
        try {
            ProcessedChunkMessage message = objectMapper.readValue(payload, ProcessedChunkMessage.class);
            if (log.isDebugEnabled()) {
                log.debug(
                        "Received chunk fileId={} chunkIndex={} recordCount={} processFor={} traceId={}",
                        message.getFileId(),
                        message.getChunkIndex(),
                        message.getRecordCount(),
                        message.getProcessFor(),
                        message.getTraceId());
            }
            chunkProcessingCoordinator.process(message);
            if (log.isDebugEnabled()) {
                log.debug(
                        "Calculator chunk handling finished fileId={} chunkIndex={}",
                        message.getFileId(),
                        message.getChunkIndex());
            }
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            log.error("Poison message on batch output queue — acknowledging without retry", ex);
        } catch (Exception ex) {
            throw new RuntimeException("Chunk processing failed", ex);
        }
    }
}
