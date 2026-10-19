package com.customcharge.fileorchestration.service;

import java.util.Map;

public interface FileRecordReaderFactory {
    FileRecordReader<Map<String, String>> getReader(String fileType);
}
