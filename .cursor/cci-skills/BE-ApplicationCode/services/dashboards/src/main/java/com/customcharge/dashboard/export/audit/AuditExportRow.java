package com.customcharge.dashboard.export.audit;

import java.time.Instant;

record AuditExportRow(
        String trackingNumber,
        Instant changedOn,
        String section,
        String changedBy,
        String operation,
        String fieldName,
        String oldValue,
        String newValue) {
}
