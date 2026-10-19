package com.customcharge.persistencelayer.export.model;

public record ExportContext(
        java.util.UUID jobId,
        String exportType,
        ExportFormat format,
        java.nio.file.Path localFilePath,
        long chunkSize,
        String filterSpec,
        String columnNamesJson,
        String exportFileName) {

    public int pageSize() {
        return chunkSize > 0 ? (int) Math.min(chunkSize, Integer.MAX_VALUE) : 2000;
    }
}
