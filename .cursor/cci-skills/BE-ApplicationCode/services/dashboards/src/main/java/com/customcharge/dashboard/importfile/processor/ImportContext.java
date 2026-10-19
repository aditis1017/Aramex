package com.customcharge.dashboard.importfile.processor;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public record ImportContext(
        MultipartFile file,
        String screenName,
        List<String> requiredColumns,
        Map<String, Object> params) {
}
