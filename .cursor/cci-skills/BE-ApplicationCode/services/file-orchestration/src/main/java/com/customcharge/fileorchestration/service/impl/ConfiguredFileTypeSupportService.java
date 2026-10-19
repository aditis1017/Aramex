package com.customcharge.fileorchestration.service.impl;

import com.customcharge.fileorchestration.config.BatchFlowProperties;
import com.customcharge.fileorchestration.service.FileTypeSupportService;
import org.springframework.stereotype.Service;

@Service
public class ConfiguredFileTypeSupportService implements FileTypeSupportService {

    private final BatchFlowProperties batchFlowProperties;

    public ConfiguredFileTypeSupportService(BatchFlowProperties batchFlowProperties) {
        this.batchFlowProperties = batchFlowProperties;
    }

    @Override
    public boolean supports(String fileType) {
        if (fileType == null) {
            return false;
        }
        return batchFlowProperties.getSupportedFileTypes().stream()
                .anyMatch(supported -> supported.equalsIgnoreCase(fileType));
    }
}
