package com.customcharge.fileorchestration.service;

import java.io.IOException;
import java.io.InputStream;

public interface FileRecordReader<T> {
    boolean supports(String fileType);

    void open(InputStream inputStream) throws IOException;

    T read() throws IOException;

    void close() throws IOException;
}
