package com.customcharge.fileorchestration.pipeline;

import com.customcharge.fileorchestration.dedup.DedupeStatistics;
import com.customcharge.fileorchestration.service.FileRecordReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-file mutable state. Must be closed to release streams and drop large collections.
 */
public final class FileProcessingContext implements AutoCloseable {

    private final FileProcessingRequest request;
    private final DedupeStatistics dedupeStatistics = new DedupeStatistics();
    private final AtomicInteger chunkCounter = new AtomicInteger(0);
    private final long processingStartedAtEpochMs = System.currentTimeMillis();
    private final String defaultCurrencyCode;

    private InputStream inputStream;
    private FileRecordReader<Map<String, String>> recordReader;
    private List<Map<String, String>> dedupedRows = List.of();

    public FileProcessingContext(FileProcessingRequest request, String defaultCurrencyCode) {
        this.request = request;
        this.defaultCurrencyCode = defaultCurrencyCode;
    }

    public FileProcessingRequest request() {
        return request;
    }

    public DedupeStatistics dedupeStatistics() {
        return dedupeStatistics;
    }

    public String defaultCurrencyCode() {
        return defaultCurrencyCode;
    }

    public void attachStream(InputStream inputStream, FileRecordReader<Map<String, String>> recordReader) {
        this.inputStream = inputStream;
        this.recordReader = recordReader;
    }

    public void setDedupedRows(List<Map<String, String>> rows) {
        this.dedupedRows = rows == null ? List.of() : rows;
    }

    public List<Map<String, String>> dedupedRows() {
        return dedupedRows;
    }

    public int nextChunkIndex() {
        return chunkCounter.incrementAndGet();
    }

    public int publishedDataChunkCount() {
        return chunkCounter.get();
    }

    public long processingStartedAtEpochMs() {
        return processingStartedAtEpochMs;
    }

    public void clearDedupedRows() {
        this.dedupedRows = List.of();
    }

    @Override
    public void close() {
        clearDedupedRows();
        dedupeStatistics.clearDuplicateRows();
        IOException readerEx = null;
        if (recordReader != null) {
            try {
                recordReader.close();
            } catch (IOException ex) {
                readerEx = ex;
            } finally {
                recordReader = null;
            }
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException ex) {
                if (readerEx == null) {
                    readerEx = ex;
                } else {
                    readerEx.addSuppressed(ex);
                }
            } finally {
                inputStream = null;
            }
        }
        if (readerEx != null) {
            throw new IllegalStateException("Failed to close file processing resources", readerEx);
        }
    }
}
