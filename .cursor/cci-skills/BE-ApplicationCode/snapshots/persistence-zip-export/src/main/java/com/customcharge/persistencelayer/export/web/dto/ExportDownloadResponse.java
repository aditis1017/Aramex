package com.customcharge.persistencelayer.export.web.dto;

import java.net.URL;
import java.time.Instant;

public record ExportDownloadResponse(URL url, Instant expiresAt) {
}
