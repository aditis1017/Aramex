package com.customcharge.dashboard.fileupload.dto;

import java.util.Map;
import java.util.UUID;

public record PresignUploadResponse(String url, String key, String method, UUID fileId, Map<String, String> headers) {
}
