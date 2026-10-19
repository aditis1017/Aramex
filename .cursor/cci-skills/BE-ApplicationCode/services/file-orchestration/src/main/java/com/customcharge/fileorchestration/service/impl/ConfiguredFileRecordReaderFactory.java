package com.customcharge.fileorchestration.service.impl;

import com.customcharge.fileorchestration.service.FileRecordReader;
import com.customcharge.fileorchestration.service.FileRecordReaderFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Creates a <strong>new</strong> reader instance per call so concurrent files never share
 * mutable parser state.
 */
@Component
public class ConfiguredFileRecordReaderFactory implements FileRecordReaderFactory {

    @Override
    public FileRecordReader<Map<String, String>> getReader(String fileType) {
        if (fileType == null) {
            throw new IllegalArgumentException("No reader configured for fileType: null");
        }
        return switch (fileType.toUpperCase(Locale.ROOT)) {
            case "CSV" -> new CsvMapFileRecordReader();
            case "XLSX" -> new XlsxMapFileRecordReader();
            default -> throw new IllegalArgumentException("No reader configured for fileType: " + fileType);
        };
    }
}
