package com.customcharge.persistencelayer.export.event;

import com.customcharge.persistencelayer.export.dto.ExportProgressPayload;

import java.util.UUID;

public record ExportProgressEvent(UUID jobId, ExportProgressPayload payload) {
}
