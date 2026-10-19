package com.customcharge.fileorchestration.pipeline;

import com.customcharge.fileorchestration.config.BatchFlowProperties;
import com.customcharge.fileorchestration.process.FileProcessRedisStore;
import com.customcharge.fileorchestration.service.EntityCurrencyLookupService;
import com.customcharge.fileorchestration.service.FileFetchService;
import com.customcharge.fileorchestration.service.FileRecordReaderFactory;
import com.customcharge.fileorchestration.service.impl.CsvMapFileRecordReader;
import com.customcharge.fileorchestration.support.FileOrchestrationTestDataFactory;
import com.customcharge.fileorchestration.recordtracking.FileOrchestrationPipelineTracker;
import com.customcharge.persistencelayer.entities.CustomChargeFileError;
import com.customcharge.persistencelayer.repository.CustomChargeFileErrorRepository;
import com.customcharge.persistencelayer.service.StagedFileUploadCompletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileOrchestrationPipelineTest {

    @Mock
    private FileFetchService fileFetchService;
    @Mock
    private FileRecordReaderFactory fileRecordReaderFactory;
    @Mock
    private EntityCurrencyLookupService entityCurrencyLookupService;
    @Mock
    private ChunkEnvelopePublisher chunkEnvelopePublisher;
    @Mock
    private FileProcessRedisStore processRedisStore;
    @Mock
    private StagedFileUploadCompletionService stagedFileUploadCompletionService;
    @Mock
    private CustomChargeFileErrorRepository customChargeFileErrorRepository;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private ObjectProvider<FileOrchestrationPipelineTracker> pipelineTracker;
    @Mock
    private FileOrchestrationPipelineTracker tracker;

    private FileOrchestrationPipeline pipeline;

    @BeforeEach
    void setUp() {
        BatchFlowProperties properties = FileOrchestrationTestDataFactory.batchFlowProperties();
        properties.setChunkSize(2);
        TransactionStatus status = mock(TransactionStatus.class);
        lenient().when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(status);
        lenient().doAnswer(invocation -> null).when(transactionManager).commit(any());
        lenient().when(pipelineTracker.getIfAvailable()).thenReturn(null);

        pipeline = new FileOrchestrationPipeline(
                properties,
                fileFetchService,
                fileRecordReaderFactory,
                entityCurrencyLookupService,
                chunkEnvelopePublisher,
                processRedisStore,
                stagedFileUploadCompletionService,
                customChargeFileErrorRepository,
                transactionManager,
                pipelineTracker);
    }

    @Test
    void process_shouldPublishChunksFinalizeAndReleaseRedis() throws Exception {
        String fileId = "11111111-1111-1111-1111-111111111111";
        FileProcessingRequest request = new FileProcessingRequest(
                fileId, "user", "path.csv", "file.csv", "CSV", "Estimates_Template", "RUH", "trace");

        when(entityCurrencyLookupService.findCurrencyCodeByEntityCode("RUH")).thenReturn(Optional.of("SAR"));
        when(fileFetchService.fetch("path.csv"))
                .thenReturn(FileOrchestrationTestDataFactory.csvStream("awb,saddad\nA1,S1\nA2,S2\nA3,S3\n"));
        when(fileRecordReaderFactory.getReader("CSV")).thenReturn(new CsvMapFileRecordReader());
        when(chunkEnvelopePublisher.publishDataChunk(any(), anyList())).thenAnswer(inv -> {
            FileProcessingContext ctx = inv.getArgument(0);
            return ctx.nextChunkIndex();
        });

        pipeline.process(request);

        verify(chunkEnvelopePublisher, times(2)).publishDataChunk(any(), anyList());
        verify(chunkEnvelopePublisher).publishFileCompletionEnvelope(any());
        verify(processRedisStore).release(fileId);
        verify(processRedisStore).markDedupComplete(eq(fileId), anyInt());
        verify(stagedFileUploadCompletionService)
                .saveExpectedDataChunksIfAbsent(eq(UUID.fromString(fileId)), anyInt());
    }

    @Test
    void finalizeSuccessfulFile_shouldPersistExpectedChunksBeforeCompletionEnvelope() throws Exception {
        UUID fileId = UUID.randomUUID();
        FileProcessingContext context = new FileProcessingContext(
                new FileProcessingRequest(
                        fileId.toString(), "user", "path", "file.csv", "CSV",
                        "Estimates_Template", "RUH", "trace"),
                null);
        context.nextChunkIndex();
        context.nextChunkIndex();

        pipeline.finalizeSuccessfulFile(context);

        InOrder inOrder = inOrder(stagedFileUploadCompletionService, chunkEnvelopePublisher);
        inOrder.verify(stagedFileUploadCompletionService)
                .saveExpectedDataChunksIfAbsent(eq(fileId), eq(2));
        inOrder.verify(chunkEnvelopePublisher).publishFileCompletionEnvelope(context);
    }

    @Test
    void finalizeSuccessfulFile_shouldPersistDedupErrors() throws Exception {
        UUID fileId = UUID.randomUUID();
        FileProcessingContext context = new FileProcessingContext(
                new FileProcessingRequest(
                        fileId.toString(), "user", "path", "file.csv", "CSV",
                        "Estimates_Template", "RUH", "trace"),
                null);
        context.nextChunkIndex();
        context.dedupeStatistics().record(2, 1, List.of(Map.of("awb", "A1", "errorCode", "E")));
        when(customChargeFileErrorRepository.getAllByFileId(fileId)).thenReturn(List.of());

        pipeline.finalizeSuccessfulFile(context);

        verify(customChargeFileErrorRepository).save(any(CustomChargeFileError.class));
    }

    @Test
    void process_shouldInvokeTrackerCallbacks_onSuccess() throws Exception {
        String fileId = "11111111-1111-1111-1111-111111111111";
        FileProcessingRequest request = new FileProcessingRequest(
                fileId, "user", "path.csv", "file.csv", "CSV", "Estimates_Template", "RUH", "trace");
        when(pipelineTracker.getIfAvailable()).thenReturn(tracker);
        when(entityCurrencyLookupService.findCurrencyCodeByEntityCode("RUH")).thenReturn(Optional.empty());
        when(fileFetchService.fetch("path.csv"))
                .thenReturn(FileOrchestrationTestDataFactory.csvStream("awb,saddad\nA1,S1\n"));
        when(fileRecordReaderFactory.getReader("CSV")).thenReturn(new CsvMapFileRecordReader());
        when(chunkEnvelopePublisher.publishDataChunk(any(), anyList())).thenAnswer(inv -> {
            FileProcessingContext ctx = inv.getArgument(0);
            return ctx.nextChunkIndex();
        });

        pipeline.process(request);

        verify(tracker).onFileReceived(request);
        verify(tracker).onOrchestrationFinished(eq(request), anyLong(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void process_shouldMarkFailedAndNotifyTracker_onError() throws Exception {
        String fileId = "11111111-1111-1111-1111-111111111111";
        FileProcessingRequest request = new FileProcessingRequest(
                fileId, "user", "path.csv", "file.csv", "CSV", "Estimates_Template", "RUH", "trace");
        when(pipelineTracker.getIfAvailable()).thenReturn(tracker);
        lenient().when(entityCurrencyLookupService.findCurrencyCodeByEntityCode("RUH")).thenReturn(Optional.empty());
        when(fileFetchService.fetch("path.csv")).thenThrow(new RuntimeException("s3 unavailable"));

        assertThatThrownBy(() -> pipeline.process(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("s3 unavailable");

        verify(tracker).onFileReceived(request);
        verify(tracker).onFileFailed(eq(request), org.mockito.ArgumentMatchers.any(RuntimeException.class), org.mockito.ArgumentMatchers.anyLong());
        verify(processRedisStore).markFailed(eq(fileId), org.mockito.ArgumentMatchers.eq("s3 unavailable"));
    }

    @Test
    void finalizeSuccessfulFile_shouldSkipDedupErrors_whenAlreadyPersisted() throws Exception {
        UUID fileId = UUID.randomUUID();
        FileProcessingContext context = new FileProcessingContext(
                new FileProcessingRequest(
                        fileId.toString(), "user", "path", "file.csv", "CSV",
                        "Estimates_Template", "RUH", "trace"),
                null);
        context.dedupeStatistics().record(2, 1, List.of(Map.of("awb", "A1")));
        CustomChargeFileError existing = new CustomChargeFileError();
        existing.setChunkNumber(com.customcharge.fileorchestration.dedup.FileOrchestrationDedupConstants.DEDUP_ERROR_CHUNK_NUMBER);
        when(customChargeFileErrorRepository.getAllByFileId(fileId)).thenReturn(List.of(existing));

        pipeline.finalizeSuccessfulFile(context);

        verify(customChargeFileErrorRepository, never()).save(any(CustomChargeFileError.class));
        verify(chunkEnvelopePublisher).publishFileCompletionEnvelope(context);
    }
}
