package com.customcharge.fileorchestration.pipeline;

import com.customcharge.fileorchestration.config.BatchFlowProperties;
import com.customcharge.fileorchestration.dedup.DedupeStatistics;
import com.customcharge.fileorchestration.dedup.DedupingFileRecordReader;
import com.customcharge.fileorchestration.dedup.FileOrchestrationDedupConstants;
import com.customcharge.fileorchestration.process.FileProcessRedisStore;
import com.customcharge.fileorchestration.recordtracking.FileOrchestrationPipelineTracker;
import com.customcharge.fileorchestration.service.EntityCurrencyLookupService;
import com.customcharge.fileorchestration.service.FileFetchService;
import com.customcharge.fileorchestration.service.FileRecordReader;
import com.customcharge.fileorchestration.service.FileRecordReaderFactory;
import com.customcharge.persistencelayer.RecordPayloadMetaKeys;
import com.customcharge.persistencelayer.entities.CustomChargeFileError;
import com.customcharge.persistencelayer.repository.CustomChargeFileErrorRepository;
import com.customcharge.persistencelayer.service.StagedFileUploadCompletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FileOrchestrationPipeline {

    private static final Logger log = LoggerFactory.getLogger(FileOrchestrationPipeline.class);

    private final BatchFlowProperties batchFlowProperties;
    private final FileFetchService fileFetchService;
    private final FileRecordReaderFactory fileRecordReaderFactory;
    private final EntityCurrencyLookupService entityCurrencyLookupService;
    private final ChunkEnvelopePublisher chunkEnvelopePublisher;
    private final FileProcessRedisStore processRedisStore;
    private final StagedFileUploadCompletionService stagedFileUploadCompletionService;
    private final CustomChargeFileErrorRepository customChargeFileErrorRepository;
    private final TransactionTemplate requiresNewTx;
    private final ObjectProvider<FileOrchestrationPipelineTracker> pipelineTracker;

    public FileOrchestrationPipeline(
            BatchFlowProperties batchFlowProperties,
            FileFetchService fileFetchService,
            FileRecordReaderFactory fileRecordReaderFactory,
            EntityCurrencyLookupService entityCurrencyLookupService,
            ChunkEnvelopePublisher chunkEnvelopePublisher,
            FileProcessRedisStore processRedisStore,
            StagedFileUploadCompletionService stagedFileUploadCompletionService,
            CustomChargeFileErrorRepository customChargeFileErrorRepository,
            PlatformTransactionManager transactionManager,
            ObjectProvider<FileOrchestrationPipelineTracker> pipelineTracker) {
        this.batchFlowProperties = batchFlowProperties;
        this.fileFetchService = fileFetchService;
        this.fileRecordReaderFactory = fileRecordReaderFactory;
        this.entityCurrencyLookupService = entityCurrencyLookupService;
        this.chunkEnvelopePublisher = chunkEnvelopePublisher;
        this.processRedisStore = processRedisStore;
        this.stagedFileUploadCompletionService = stagedFileUploadCompletionService;
        this.customChargeFileErrorRepository = customChargeFileErrorRepository;
        this.pipelineTracker = pipelineTracker;
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.requiresNewTx = new TransactionTemplate(transactionManager, definition);
    }

    public void process(FileProcessingRequest request) throws Exception {
        long started = System.currentTimeMillis();
        FileOrchestrationPipelineTracker tracker = pipelineTracker.getIfAvailable();
        if (tracker != null) {
            tracker.onFileReceived(request);
        }
        String defaultCurrency = entityCurrencyLookupService
                .findCurrencyCodeByEntityCode(request.entityCode())
                .orElse(null);

        try (FileProcessingContext context = new FileProcessingContext(request, defaultCurrency)) {
            processRedisStore.markStatus(request.fileId(), FileProcessRedisStore.STATUS_FETCHING);
            openAndDedup(context);

            processRedisStore.markDedupComplete(request.fileId(), context.dedupeStatistics().getDedupedRowCount());
            publishChunks(context);

            processRedisStore.markStatus(request.fileId(), FileProcessRedisStore.STATUS_FINALIZING);
            finalizeSuccessfulFile(context);

            processRedisStore.markStatus(request.fileId(), FileProcessRedisStore.STATUS_COMPLETED);
            processRedisStore.release(request.fileId());

            long durationMs = System.currentTimeMillis() - started;
            if (tracker != null) {
                tracker.onOrchestrationFinished(
                        request,
                        durationMs,
                        context.dedupeStatistics().getDedupedRowCount(),
                        context.dedupeStatistics().getDroppedCount(),
                        context.publishedDataChunkCount());
            }

            log.info(
                    "File orchestration complete fileId={} totalRows={} chunks={} dropped={} durationMs={} traceId={}",
                    request.fileId(),
                    context.dedupeStatistics().getDedupedRowCount(),
                    context.publishedDataChunkCount(),
                    context.dedupeStatistics().getDroppedCount(),
                    durationMs,
                    request.traceId());
        } catch (Exception ex) {
            if (tracker != null) {
                tracker.onFileFailed(request, ex, System.currentTimeMillis() - started);
            }
            processRedisStore.markFailed(request.fileId(), ex.getMessage());
            throw ex;
        }
    }

    private void openAndDedup(FileProcessingContext context) throws Exception {
        FileProcessingRequest request = context.request();
        processRedisStore.markStatus(request.fileId(), FileProcessRedisStore.STATUS_DEDUPING);

        InputStream inputStream = fileFetchService.fetch(request.documentPath());
        FileRecordReader<Map<String, String>> delegate = fileRecordReaderFactory.getReader(request.fileType());
        DedupingFileRecordReader dedupingReader = new DedupingFileRecordReader(
                delegate,
                request.processFor(),
                batchFlowProperties.isDedupEnabled(),
                context.dedupeStatistics());

        context.attachStream(inputStream, dedupingReader);
        dedupingReader.open(inputStream);

        List<Map<String, String>> rows = new ArrayList<>();
        long nextIndex = 1L;
        Map<String, String> row;
        while ((row = dedupingReader.read()) != null) {
            Map<String, String> indexed = new HashMap<>(row);
            indexed.put(RecordPayloadMetaKeys.META_FILE_ROW_INDEX, Long.toString(nextIndex++));
            rows.add(indexed);
        }
        context.setDedupedRows(rows);
    }

    private void publishChunks(FileProcessingContext context) throws Exception {
        processRedisStore.markStatus(context.request().fileId(), FileProcessRedisStore.STATUS_PUBLISHING);
        int chunkSize = batchFlowProperties.getChunkSize();
        List<Map<String, String>> rows = context.dedupedRows();
        List<Map<String, String>> buffer = new ArrayList<>(chunkSize);

        for (Map<String, String> row : rows) {
            buffer.add(row);
            if (buffer.size() >= chunkSize) {
                publishOneChunk(context, buffer);
                buffer.clear();
            }
        }
        if (!buffer.isEmpty()) {
            publishOneChunk(context, buffer);
            buffer.clear();
        }
        context.clearDedupedRows();
    }

    private void publishOneChunk(FileProcessingContext context, List<Map<String, String>> buffer) throws Exception {
        int chunkIndex = chunkEnvelopePublisher.publishDataChunk(context, buffer);
        processRedisStore.onChunkPublished(
                context.request().fileId(),
                chunkIndex,
                buffer.size(),
                context.publishedDataChunkCount());
    }

    void finalizeSuccessfulFile(FileProcessingContext context) throws Exception {
        FileProcessingRequest request = context.request();
        int dataChunksPublished = context.publishedDataChunkCount();
        UUID parsedFileId = UUID.fromString(request.fileId());

        requiresNewTx.executeWithoutResult(status ->
                stagedFileUploadCompletionService.saveExpectedDataChunksIfAbsent(
                        parsedFileId, dataChunksPublished));

        persistDedupErrors(parsedFileId, request, context.dedupeStatistics());
        chunkEnvelopePublisher.publishFileCompletionEnvelope(context);
    }

    private void persistDedupErrors(
            UUID parsedFileId,
            FileProcessingRequest request,
            DedupeStatistics dedupeStatistics) {
        if (dedupeStatistics.getDuplicateRows().isEmpty()) {
            return;
        }
        requiresNewTx.executeWithoutResult(status -> {
            boolean alreadySaved = customChargeFileErrorRepository.getAllByFileId(parsedFileId).stream()
                    .anyMatch(error ->
                            FileOrchestrationDedupConstants.DEDUP_ERROR_CHUNK_NUMBER == error.getChunkNumber());
            if (alreadySaved) {
                return;
            }
            CustomChargeFileError fileError = new CustomChargeFileError();
            fileError.setFileId(parsedFileId);
            fileError.setFileName(request.fileName());
            fileError.setFileType(request.fileType());
            fileError.setChunkNumber(FileOrchestrationDedupConstants.DEDUP_ERROR_CHUNK_NUMBER);
            fileError.setErrorList(new ArrayList<>());
            dedupeStatistics.getDuplicateRows().forEach(row ->
                    fileError.getErrorList().add(new HashMap<>(row)));
            customChargeFileErrorRepository.save(fileError);
            log.info("Saved dedup error rows fileId={} errorRows={}",
                    request.fileId(), fileError.getErrorList().size());
        });
        dedupeStatistics.clearDuplicateRows();
    }
}
