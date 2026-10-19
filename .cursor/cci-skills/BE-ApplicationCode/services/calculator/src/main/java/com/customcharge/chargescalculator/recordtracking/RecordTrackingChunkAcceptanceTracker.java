package com.customcharge.chargescalculator.recordtracking;

import com.customcharge.chargescalculator.dto.ProcessedChunkMessage;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingChunkSupport;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingGuard;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingMdc;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingTimeline;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "customcharge.record-tracking", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RecordTrackingChunkAcceptanceTracker {

    private final String serviceName;

    public RecordTrackingChunkAcceptanceTracker(@Value("${spring.application.name}") String serviceName) {
        this.serviceName = serviceName;
    }

    public void onChunkAccepted(ProcessedChunkMessage message) {
        if (!RecordTrackingChunkSupport.isTrackableDataChunk(
                message.getLastChunk(),
                message.getChunkIndex(),
                message.getRecordCount(),
                message.getRecords())) {
            return;
        }
        RecordTrackingGuard.runSafely(() -> {
            RecordTrackingMdc.putContext(
                    message.getTraceId(),
                    message.getFileId(),
                    message.getChunkIndex(),
                    null,
                    serviceName);
            RecordTrackingMdc.putFileName(message.getFileName());
            RecordTrackingMdc.putProcessFor(message.getProcessFor());
            RecordTrackingMdc.applyFileIdentity(message.getUploadedBy(), message.getEntityCode());
            RecordTrackingTimeline.markTraceStart();
        });
    }
}
