package com.customcharge.persistencelayer.export.service;

import com.customcharge.persistencelayer.export.config.ExportEngineProperties;
import com.customcharge.persistencelayer.export.entity.ExportJob;
import com.customcharge.persistencelayer.export.event.ExportProgressEvent;
import com.customcharge.persistencelayer.export.model.ExportContext;
import com.customcharge.persistencelayer.export.model.ExportFormat;
import com.customcharge.persistencelayer.export.model.ExportJobStatus;
import com.customcharge.persistencelayer.export.registry.ExportPipeline;
import com.customcharge.persistencelayer.export.registry.ExportPipelineRegistry;
import com.customcharge.persistencelayer.export.repository.ExportJobRepository;
import com.customcharge.persistencelayer.export.web.dto.CreateExportRequest;
import com.customcharge.persistencelayer.export.web.dto.ExportDownloadResponse;
import com.customcharge.persistencelayer.export.web.dto.ExportJobStatusResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mvp.persistence.export", name = "enabled", havingValue = "true")
public class ExportEngineService {

    private final ExportJobRepository exportJobRepository;
    private final ExportPipelineRegistry exportPipelineRegistry;
    private final ExportEngineProperties exportEngineProperties;
    private final ObjectMapper objectMapper;
    private final ExportJobRunner exportJobRunner;
    private final TaskExecutor exportJobLauncherExecutor;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ExportJobProgressService exportJobProgressService;

    public ExportEngineService(
            ExportJobRepository exportJobRepository,
            ExportPipelineRegistry exportPipelineRegistry,
            ExportEngineProperties exportEngineProperties,
            ObjectMapper objectMapper,
            ExportJobRunner exportJobRunner,
            @Qualifier("exportJobLauncherExecutor") TaskExecutor exportJobLauncherExecutor,
            ApplicationEventPublisher applicationEventPublisher,
            ExportJobProgressService exportJobProgressService) {
        this.exportJobRepository = exportJobRepository;
        this.exportPipelineRegistry = exportPipelineRegistry;
        this.exportEngineProperties = exportEngineProperties;
        this.objectMapper = objectMapper;
        this.exportJobRunner = exportJobRunner;
        this.exportJobLauncherExecutor = exportJobLauncherExecutor;
        this.applicationEventPublisher = applicationEventPublisher;
        this.exportJobProgressService = exportJobProgressService;
    }

    @Transactional
    public UUID startExport(CreateExportRequest request, String requestedBy) throws Exception {
        ExportPipeline pipeline = requirePipeline(request.exportType());
        List<String> columnNames = !pipeline.columnNames().isEmpty() ? pipeline.columnNames() : request.columnNames();

        UUID jobId = UUID.randomUUID();
        String suffix = request.format() == ExportFormat.XLSX ? ".xlsx" : ".csv";
        String exportFileName = buildExportFileName(pipeline.fileNamePrefix(), suffix);
        Path tempFile = Files.createTempFile("export-" + jobId + "-", suffix);

        ExportJob job = new ExportJob();
        job.setId(jobId);
        job.setExportType(request.exportType().toUpperCase());
        job.setFormat(request.format());
        job.setFilterSpec(request.filterSpec() != null && !request.filterSpec().isBlank() ? request.filterSpec() : null);
        job.setColumnNamesJson(columnNames == null || columnNames.isEmpty()
                ? null
                : objectMapper.writeValueAsString(columnNames));
        job.setStatus(ExportJobStatus.INITIATED);
        job.setCorrelationId(Optional.ofNullable(MDC.get("traceId")).orElse(null));
        job.setRequestedBy(requestedBy);
        job.setLocalTempPath(tempFile.toAbsolutePath().toString());
        exportJobRepository.save(job);

        applicationEventPublisher.publishEvent(new ExportProgressEvent(jobId, exportJobProgressService.toPayload(job)));

        ExportContext context = new ExportContext(
                jobId,
                job.getExportType(),
                job.getFormat(),
                tempFile.toAbsolutePath(),
                exportEngineProperties.getChunkSize(),
                job.getFilterSpec() != null ? job.getFilterSpec() : "",
                job.getColumnNamesJson() != null ? job.getColumnNamesJson() : "",
                exportFileName);

        scheduleExportAfterCommit(jobId, context);

        return jobId;
    }

    private void scheduleExportAfterCommit(UUID jobId, ExportContext context) {
        Runnable launch = () -> exportJobLauncherExecutor.execute(() -> runExport(jobId, context));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    launch.run();
                }
            });
        } else {
            launch.run();
        }
    }

    private void runExport(UUID jobId, ExportContext context) {
        try {
            exportJobRunner.run(context);
        } catch (Exception e) {
            exportJobProgressService.markFailed(jobId, "JOB_LAUNCH_FAILED", e.getMessage());
            applicationEventPublisher.publishEvent(new ExportProgressEvent(
                    jobId, exportJobProgressService.payloadFor(jobId)));
        }
    }

    @Transactional(readOnly = true)
    public ExportJob requireJob(UUID jobId) {
        return exportJobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown export job"));
    }

    @Transactional(readOnly = true)
    public ExportJobStatusResponse buildStatusResponse(UUID jobId) {
        ExportJob job = requireJob(jobId);
        boolean downloadAvailable = job.getStatus() == ExportJobStatus.COMPLETED
                && job.getS3Key() != null
                && !job.getS3Key().isBlank();
        return new ExportJobStatusResponse(
                job.getStatus(),
                job.getTotalRecords(),
                job.getProcessedRecords(),
                job.getPercentComplete(),
                job.getErrorCode(),
                job.getErrorMessage(),
                downloadAvailable);
    }

    @Transactional(readOnly = true)
    public ExportDownloadResponse createDownloadResponse(
            UUID jobId, Optional<ExportDownloadLinkService> exportDownloadLinkService) {
        ExportDownloadLinkService linkService = exportDownloadLinkService.orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "Pre-signed downloads are not configured"));
        ExportJob job = requireJob(jobId);
        if (job.getStatus() != ExportJobStatus.COMPLETED
                || job.getS3Bucket() == null
                || job.getS3Key() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Export not ready for download");
        }
        return linkService.presignDownload(job.getS3Bucket(), job.getS3Key(), job.getContentType());
    }

    private ExportPipeline requirePipeline(String exportType) {
        try {
            return exportPipelineRegistry.require(exportType);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported exportType: " + exportType);
        }
    }

    private static String buildExportFileName(String prefix, String suffix) {
        String name = (prefix == null || prefix.isBlank()) ? "Export" : prefix.strip();
        String timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return name + "_" + timestamp + suffix;
    }
}
