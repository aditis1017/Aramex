package com.customcharge.chargescalculator.recordtracking;

import com.customcharge.chargescalculator.dto.ProcessedChunkMessage;
import com.customcharge.persistencelayer.exception.AbstractApiException;
import com.customcharge.persistencelayer.recordtracking.RecordContext;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingChunkSupport;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingEvent;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingGuard;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingLogger;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingMdc;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingProperties;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingTimeline;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Aspect
@Component
@ConditionalOnProperty(prefix = "customcharge.record-tracking", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RecordTrackingAspect {

    private final RecordTrackingLogger recordTrackingLogger;
    private final RecordTrackingProperties recordTrackingProperties;
    private final String serviceName;

    public RecordTrackingAspect(
            RecordTrackingLogger recordTrackingLogger,
            RecordTrackingProperties recordTrackingProperties,
            @Value("${spring.application.name}") String serviceName) {
        this.recordTrackingLogger = recordTrackingLogger;
        this.recordTrackingProperties = recordTrackingProperties;
        this.serviceName = serviceName;
    }

    @Around("execution(* com.customcharge.chargescalculator.listener.BatchOutputQueueListener.listen(String))")
    public Object aroundSqsListener(ProceedingJoinPoint joinPoint) throws Throwable {
        Map<String, String> previous = RecordTrackingMdc.snapshot();
        try {
            return joinPoint.proceed();
        } finally {
            RecordTrackingGuard.runSafely(() -> RecordTrackingMdc.restore(previous));
        }
    }

    @Around("execution(* com.customcharge.chargescalculator.service.batch.ChunkProcessingCoordinator.process(..))")
    public Object aroundChunkProcessing(ProceedingJoinPoint joinPoint) throws Throwable {
        Map<String, String> previous = RecordTrackingMdc.snapshot();
        try {
            return joinPoint.proceed();
        } finally {
            RecordTrackingGuard.runSafely(() -> RecordTrackingMdc.restore(previous));
        }
    }

    @Around("execution(* com.customcharge.chargescalculator.service.chunk.ProcessedChunkProcessingService.processChunk(..))")
    public Object aroundChunkRows(ProceedingJoinPoint joinPoint) throws Throwable {
        ProcessedChunkMessage message = (ProcessedChunkMessage) joinPoint.getArgs()[0];
        if (!RecordTrackingChunkSupport.isTrackableDataChunk(
                message.getLastChunk(),
                message.getChunkIndex(),
                message.getRecordCount(),
                message.getRecords())) {
            return joinPoint.proceed();
        }
        Map<String, String> previous = RecordTrackingMdc.snapshot();
        RecordTrackingGuard.runSafely(() -> {
            applyChunkMdc(message);
            RecordTrackingTimeline.markChunkStart();
        });
        try {
            return joinPoint.proceed();
        } finally {
            RecordTrackingGuard.runSafely(RecordTrackingTimeline::clearChunkScope);
            RecordTrackingGuard.runSafely(RecordTrackingMdc::clearFileIdentity);
            RecordTrackingGuard.runSafely(() -> RecordTrackingMdc.restore(previous));
        }
    }

    @Around("execution(* com.customcharge.chargescalculator.service.batch.SingleRecordProcessorService.processRecordOrThrow(..))")
    public Object aroundRecordProcessing(ProceedingJoinPoint joinPoint) throws Throwable {
        Map<String, String> previous = RecordTrackingMdc.snapshot();
        long startedAt = System.currentTimeMillis();
        RecordTrackingGuard.runSafely(() -> prepareRecordTracking(joinPoint.getArgs()));
        try {
            Object result = joinPoint.proceed();
            RecordTrackingGuard.runSafely(() -> trackRecordSuccess(startedAt, joinPoint.getArgs()));
            return result;
        } catch (AbstractApiException ex) {
            RecordTrackingGuard.runSafely(() -> trackRecordFailure(startedAt, joinPoint.getArgs(), ex));
            throw ex;
        } catch (Throwable ex) {
            RecordTrackingGuard.runSafely(() -> trackRecordFailure(startedAt, joinPoint.getArgs(), ex));
            throw ex;
        } finally {
            RecordTrackingGuard.runSafely(RecordTrackingTimeline::clearRecordScope);
            RecordTrackingGuard.runSafely(() -> RecordTrackingMdc.restore(previous));
        }
    }

    private void applyChunkMdc(ProcessedChunkMessage message) {
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
    }

    private void prepareRecordTracking(Object[] args) {
        @SuppressWarnings("unchecked")
        Map<String, String> recordData = (Map<String, String>) args[0];
        String processFor = (String) args[1];
        String fileId = (String) args[2];
        int chunkIndex = (Integer) args[3];
        RecordContext recordContext = RecordContext.fromRecord(recordData, processFor);
        RecordTrackingMdc.putRecordId(RecordContext.resolveRecordId(recordData, fileId, chunkIndex));
        RecordTrackingMdc.putAwb(recordContext.awb());
        RecordTrackingMdc.putProcessFor(processFor);
        if (recordContext.paymentReference() != null) {
            RecordTrackingMdc.putPaymentReference(recordContext.paymentReference());
        }
        if (recordContext.fileRowIndex() != null) {
            RecordTrackingMdc.putFileRowIndex(recordContext.fileRowIndex());
        }
        RecordTrackingTimeline.markRecordStart();
    }

    private void trackRecordSuccess(long startedAt, Object[] args) {
        if (!recordTrackingProperties.isLogRecordCompleted()) {
            return;
        }
        RecordContext context = recordContext(args);
        long durationMs = RecordTrackingTimeline.durationSince(startedAt);
        recordTrackingLogger.logEvent(RecordTrackingEvent.RECORD_COMPLETED, context, durationMs);
    }

    private void trackRecordFailure(long startedAt, Object[] args, Throwable ex) {
        RecordContext context = recordContext(args);
        long durationMs = RecordTrackingTimeline.durationSince(startedAt);
        if (ex instanceof AbstractApiException apiException) {
            recordTrackingLogger.logFailure(RecordTrackingEvent.RECORD_FAILED, context, apiException, durationMs);
        } else {
            recordTrackingLogger.logFailure(RecordTrackingEvent.RECORD_FAILED, context, ex, durationMs);
        }
    }

    private static RecordContext recordContext(Object[] args) {
        @SuppressWarnings("unchecked")
        Map<String, String> recordData = (Map<String, String>) args[0];
        String processFor = (String) args[1];
        return RecordContext.fromRecord(recordData, processFor);
    }
}
