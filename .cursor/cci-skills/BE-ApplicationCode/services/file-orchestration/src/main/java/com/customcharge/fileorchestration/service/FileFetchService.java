package com.customcharge.fileorchestration.service;

import java.io.InputStream;

public interface FileFetchService {
    InputStream fetch(String documentPath);
}
