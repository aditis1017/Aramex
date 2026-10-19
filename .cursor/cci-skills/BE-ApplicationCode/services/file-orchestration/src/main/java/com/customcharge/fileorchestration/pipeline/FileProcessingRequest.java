package com.customcharge.fileorchestration.pipeline;

/**
 * Immutable metadata for one file orchestration run.
 */
public record FileProcessingRequest(
        String fileId,
        String uploadedBy,
        String documentPath,
        String fileName,
        String fileType,
        String processFor,
        String entityCode,
        String traceId) {
}
