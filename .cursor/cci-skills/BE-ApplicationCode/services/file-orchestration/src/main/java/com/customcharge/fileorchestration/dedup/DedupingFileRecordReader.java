package com.customcharge.fileorchestration.dedup;

import com.customcharge.fileorchestration.service.FileRecordReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DedupingFileRecordReader implements FileRecordReader<Map<String, String>> {

    private static final Logger log = LoggerFactory.getLogger(DedupingFileRecordReader.class);

    private final FileRecordReader<Map<String, String>> delegate;
    private final String processFor;
    private final boolean dedupEnabled;
    private final DedupeStatistics statistics;

    private Iterator<Map<String, String>> rowIterator;

    public DedupingFileRecordReader(
            FileRecordReader<Map<String, String>> delegate,
            String processFor,
            boolean dedupEnabled,
            DedupeStatistics statistics) {
        this.delegate = delegate;
        this.processFor = processFor;
        this.dedupEnabled = dedupEnabled;
        this.statistics = statistics;
    }

    @Override
    public boolean supports(String fileType) {
        return delegate.supports(fileType);
    }

    @Override
    public void open(InputStream inputStream) throws IOException {
        delegate.open(inputStream);
        LastRowWinsDedupeCollector collector = new LastRowWinsDedupeCollector(processFor, dedupEnabled);
        Map<String, String> row;
        while ((row = delegate.read()) != null) {
            collector.accept(row);
        }
        List<Map<String, String>> deduped = collector.toOrderedRows();
        statistics.record(
                collector.getRawRowCount(),
                collector.getDedupedRowCount(),
                collector.getDuplicateRows());
        rowIterator = deduped.iterator();
        if (dedupEnabled && FileRowDedupKeyResolver.dedupApplies(processFor)) {
            log.info(
                    "File row dedup complete processFor={} rawRows={} dedupedRows={} dropped={}",
                    processFor,
                    collector.getRawRowCount(),
                    collector.getDedupedRowCount(),
                    collector.getDroppedCount());
        }
    }

    @Override
    public Map<String, String> read() {
        if (rowIterator == null || !rowIterator.hasNext()) {
            return null;
        }
        return rowIterator.next();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
        rowIterator = null;
    }
}
