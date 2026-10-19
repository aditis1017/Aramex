package com.customcharge.chargescalculator.recordtracking;

import com.customcharge.chargescalculator.dto.ProcessedChunkMessage;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.customcharge.persistencelayer.recordtracking.RecordContext;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingEvent;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingLogger;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingProperties;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingMdc;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordTrackingAspectTest {

    @Mock
    private RecordTrackingLogger recordTrackingLogger;
    @Mock
    private ProceedingJoinPoint joinPoint;

    private final RecordTrackingProperties recordTrackingProperties = new RecordTrackingProperties();

    private RecordTrackingAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RecordTrackingAspect(
                recordTrackingLogger,
                recordTrackingProperties,
                "chargesCalculator");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void aroundSqsListener_shouldRestoreMdc() throws Throwable {
        MDC.put(RecordTrackingMdc.TRACE_ID, "existing");
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.aroundSqsListener(joinPoint);

        assertThat(result).isEqualTo("ok");
        assertThat(MDC.get(RecordTrackingMdc.TRACE_ID)).isEqualTo("existing");
    }

    @Test
    void aroundChunkProcessing_shouldRestoreMdc() throws Throwable {
        MDC.put(RecordTrackingMdc.FILE_ID, "file-1");
        when(joinPoint.proceed()).thenReturn(null);

        aspect.aroundChunkProcessing(joinPoint);

        assertThat(MDC.get(RecordTrackingMdc.FILE_ID)).isEqualTo("file-1");
    }

    @Test
    void aroundChunkRows_shouldSkipNonTrackableChunk() throws Throwable {
        ProcessedChunkMessage message = new ProcessedChunkMessage();
        message.setLastChunk(true);
        message.setChunkIndex(0);
        message.setRecordCount(0);
        message.setRecords(List.of());
        when(joinPoint.getArgs()).thenReturn(new Object[] {message});
        when(joinPoint.proceed()).thenReturn("done");

        assertThat(aspect.aroundChunkRows(joinPoint)).isEqualTo("done");
        verify(recordTrackingLogger, org.mockito.Mockito.never())
                .logEvent(any(RecordTrackingEvent.class), any(RecordContext.class));
    }

    @Test
    void aroundChunkRows_shouldApplyMdcWithoutLoggingEvents() throws Throwable {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();
        message.setChunkIndex(2);
        message.setUploadedBy("uploader@aramex.com");
        message.setEntityCode("DXB");
        when(joinPoint.getArgs()).thenReturn(new Object[] {message});
        when(joinPoint.proceed()).thenReturn("done");

        assertThat(aspect.aroundChunkRows(joinPoint)).isEqualTo("done");
        verify(recordTrackingLogger, org.mockito.Mockito.never())
                .logEvent(any(RecordTrackingEvent.class), any(RecordContext.class));
    }

    @Test
    void aroundChunkRows_shouldNotLogWhenProceedThrows() throws Throwable {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();
        message.setChunkIndex(2);
        when(joinPoint.getArgs()).thenReturn(new Object[] {message});
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> aspect.aroundChunkRows(joinPoint))
                .isInstanceOf(IllegalStateException.class);
        verify(recordTrackingLogger, org.mockito.Mockito.never())
                .logFailure(any(RecordTrackingEvent.class), any(), any(Throwable.class), any(Long.class));
    }

    @Test
    void aroundRecordProcessing_shouldTrackSuccess() throws Throwable {
        recordTrackingProperties.setLogRecordCompleted(false);
        Map<String, String> record = CalculatorTestDataFactory.recordMap();
        Object[] args = new Object[] {record, "ESTIMATE", CalculatorTestDataFactory.fileId().toString(), 0};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.aroundRecordProcessing(joinPoint)).isEqualTo("ok");
        verify(recordTrackingLogger, org.mockito.Mockito.never())
                .logEvent(eq(RecordTrackingEvent.RECORD_COMPLETED), any(), any(Long.class));
        verify(recordTrackingLogger, org.mockito.Mockito.never())
                .logEvent(eq(RecordTrackingEvent.RECORD_PROCESSING_STARTED), any(RecordContext.class));
    }

    @Test
    void aroundRecordProcessing_shouldLogSuccessWhenConfigured() throws Throwable {
        recordTrackingProperties.setLogRecordCompleted(true);
        Map<String, String> record = CalculatorTestDataFactory.recordMap();
        Object[] args = new Object[] {record, "ESTIMATE", CalculatorTestDataFactory.fileId().toString(), 0};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("ok");

        assertThat(aspect.aroundRecordProcessing(joinPoint)).isEqualTo("ok");
        verify(recordTrackingLogger).logEvent(eq(RecordTrackingEvent.RECORD_COMPLETED), any(), any(Long.class));
    }

    @Test
    void aroundRecordProcessing_shouldTrackApiFailure() throws Throwable {
        Map<String, String> record = CalculatorTestDataFactory.recordMap();
        Object[] args = new Object[] {record, "ESTIMATE", CalculatorTestDataFactory.fileId().toString(), 0};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenThrow(new ValidationException("bad"));

        assertThatThrownBy(() -> aspect.aroundRecordProcessing(joinPoint))
                .isInstanceOf(ValidationException.class);
        verify(recordTrackingLogger).logFailure(
                eq(RecordTrackingEvent.RECORD_FAILED), any(), any(ValidationException.class), any(Long.class));
    }

    @Test
    void aroundRecordProcessing_shouldTrackGenericFailure() throws Throwable {
        Map<String, String> record = new HashMap<>(CalculatorTestDataFactory.recordMap());
        record.put("payment reference", "PR-1");
        record.put("row", "7");
        Object[] args = new Object[] {record, "ESTIMATE", CalculatorTestDataFactory.fileId().toString(), 2};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> aspect.aroundRecordProcessing(joinPoint))
                .isInstanceOf(RuntimeException.class);
        verify(recordTrackingLogger).logFailure(
                eq(RecordTrackingEvent.RECORD_FAILED), any(), any(RuntimeException.class), any(Long.class));
    }
}
