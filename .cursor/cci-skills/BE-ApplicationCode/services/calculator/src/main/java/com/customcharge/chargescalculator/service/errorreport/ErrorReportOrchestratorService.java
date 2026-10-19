package com.customcharge.chargescalculator.service.errorreport;

import com.customcharge.chargescalculator.config.CustomChargeProperties;
import com.customcharge.chargescalculator.model.ErrorReportStorageResult;
import com.customcharge.persistencelayer.entities.CustomChargeFileError;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ErrorReportOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(ErrorReportOrchestratorService.class);

    private final EntityManager entityManager;
    private final CustomChargeProperties customChargeProperties;
    private final ErrorExcelBuilder errorExcelBuilder;
    private final ErrorReportStorageService errorReportStorageService;
    private final ErrorReportMailService errorReportMailService;

    public ErrorReportOrchestratorService(
            EntityManager entityManager,
            CustomChargeProperties customChargeProperties,
            ErrorExcelBuilder errorExcelBuilder,
            ErrorReportStorageService errorReportStorageService,
            ErrorReportMailService errorReportMailService) {
        this.entityManager = entityManager;
        this.customChargeProperties = customChargeProperties;
        this.errorExcelBuilder = errorExcelBuilder;
        this.errorReportStorageService = errorReportStorageService;
        this.errorReportMailService = errorReportMailService;
    }

    @Async("errorReportTaskExecutor")
    public void generateAndSendReportAsync(UUID fileId) {
        log.info("Starting error report generation for fileId={}", fileId);
        int pageSize = Math.max(50, customChargeProperties.getErrorReport().getPageSize());
        List<CustomChargeFileError> firstPage = fetchPage(fileId, 0, pageSize);
        if (firstPage.isEmpty()) {
            log.warn("No error records found for fileId={}", fileId);
            return;
        }

        String fileName = firstPage.getFirst().getFileName();
        List<String> headers = collectHeaders(fileId, pageSize);
        if (headers.isEmpty()) {
            log.warn("No error rows found in errorList for fileId={}", fileId);
            return;
        }

        Path tempFile = null;
        try (ErrorExcelBuilder.StreamingWorkbook workbook = errorExcelBuilder.createStreamingWorkbook(
                headers,
                customChargeProperties.getErrorReport().getExcelRowAccessWindowSize())) {
            appendAllRows(fileId, pageSize, workbook);
            tempFile = Files.createTempFile("error-report-" + fileId + "-", ".xlsx");
            workbook.writeTo(tempFile);
            ErrorReportStorageResult storageResult =
                    errorReportStorageService.uploadAndGenerateLink(fileId, fileName, tempFile);
            errorReportMailService.sendReportMail(fileId, fileName, storageResult.downloadUrl());
            log.info("Error report generated and mailed for fileId={}, s3Key={}", fileId, storageResult.key());
        } catch (Exception e) {
            log.error("Failed to generate error report for fileId={}", fileId, e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ex) {
                    log.warn("Failed to delete temporary report file {}", tempFile, ex);
                }
            }
        }
    }

    private List<String> collectHeaders(UUID fileId, int pageSize) {
        Set<String> headerSet = new LinkedHashSet<>();
        int pageNumber = 0;
        while (true) {
            List<CustomChargeFileError> page = fetchPage(fileId, pageNumber, pageSize);
            if (page.isEmpty()) {
                break;
            }
            for (CustomChargeFileError recordData : page) {
                headerSet = errorExcelBuilder.collectHeaders(recordData.getErrorList(), headerSet);
            }
            if (page.size() < pageSize) {
                break;
            }
            pageNumber++;
        }
        return errorExcelBuilder.finalizeHeaderOrder(headerSet);
    }

    private void appendAllRows(UUID fileId, int pageSize, ErrorExcelBuilder.StreamingWorkbook workbook) {
        int pageNumber = 0;
        while (true) {
            List<CustomChargeFileError> page = fetchPage(fileId, pageNumber, pageSize);
            if (page.isEmpty()) {
                break;
            }
            for (CustomChargeFileError recordData : page) {
                workbook.appendRows(recordData.getErrorList());
            }
            if (page.size() < pageSize) {
                break;
            }
            pageNumber++;
        }
    }

    private List<CustomChargeFileError> fetchPage(UUID fileId, int pageNumber, int pageSize) {
        return entityManager.createQuery(
                        "select c from CustomChargeFileError c where c.fileId = :fileId order by c.chunkNumber",
                        CustomChargeFileError.class)
                .setParameter("fileId", fileId)
                .setFirstResult(pageNumber * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }
}
