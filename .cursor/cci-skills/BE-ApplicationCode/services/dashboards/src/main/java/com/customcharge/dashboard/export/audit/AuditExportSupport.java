package com.customcharge.dashboard.export.audit;

import com.customcharge.dashboard.constants.AuditExportConstants;
import com.customcharge.dashboard.dto.AccountAuditHistoryCommitDto;
import com.customcharge.dashboard.dto.AccountAuditStateChangeDto;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.export.registry.ExportItemReader;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

final class AuditExportSupport {

    static final List<String> COLUMN_NAMES = AuditExportConstants.COLUMN_NAMES;

    static final List<String> SHIPMENT_COLUMN_NAMES = AuditExportConstants.SHIPMENT_COLUMN_NAMES;

    private AuditExportSupport() {}

    static ExportItemReader streamingReader(
            Function<PageRequest, Page<AccountAuditHistoryCommitDto>> pageLoader,
            String trackingNumber) {
        return new PagedAuditExportReader(pageLoader, trackingNumber);
    }

    static long countFlattenedRows(
            Function<PageRequest, Page<AccountAuditHistoryCommitDto>> pageLoader) {
        long count = 0L;
        int page = 0;
        while (true) {
            Page<AccountAuditHistoryCommitDto> result = pageLoader.apply(new PageRequest(page));
            if (result.getContent().isEmpty()) {
                break;
            }
            for (AccountAuditHistoryCommitDto block : result.getContent()) {
                count += countCommitRows(block);
            }
            if (result.isLast()) {
                break;
            }
            page++;
        }
        return count;
    }

    static Instant requireInstant(JsonNode root, String key) {
        JsonNode node = root.get(key);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            throw new BadRequestException(
                    AuditExportConstants.FILTER_SPEC_FIELD_REQUIRED_PREFIX + key + " is required");
        }
        return Instant.parse(node.asText().strip());
    }

    static String optionalText(JsonNode root, String key) {
        JsonNode node = root.get(key);
        return node == null || node.isNull() ? null : node.asText().strip();
    }

    static UUID optionalUuid(JsonNode root, String key) {
        String value = optionalText(root, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }

    private static List<AuditExportRow> flattenCommitBlock(
            AccountAuditHistoryCommitDto block,
            String trackingNumber) {
        List<AuditExportRow> rows = new ArrayList<>();
        appendCommitRows(rows, block, trackingNumber);
        return rows;
    }

    private static void appendCommitRows(
            List<AuditExportRow> rows,
            AccountAuditHistoryCommitDto block,
            String trackingNumber) {
        if (block == null) {
            return;
        }
        AccountAuditStateChangeDto entry = block.getStateChangeData();
        JsonNode changeParameter = entry != null ? entry.getChangeParameter() : null;
        if (!isObjectNode(changeParameter)) {
            return;
        }
        String author = formatAuthor(block.getAuthor());
        String operation = mapOperation(entry != null ? entry.getType() : null);
        String managedType = block.getManagedType() != null ? block.getManagedType() : "";
        for (Iterator<String> it = changeParameter.fieldNames(); it.hasNext(); ) {
            String fieldName = it.next();
            rows.add(new AuditExportRow(
                    trackingNumber,
                    block.getCommitDate(),
                    managedType,
                    author,
                    operation,
                    fieldName,
                    valueForField(entry != null ? entry.getPreState() : null, fieldName),
                    valueForField(entry != null ? entry.getCurrentState() : null, fieldName)));
        }
    }

    private static int countCommitRows(AccountAuditHistoryCommitDto block) {
        if (block == null) {
            return 0;
        }
        AccountAuditStateChangeDto entry = block.getStateChangeData();
        JsonNode changeParameter = entry != null ? entry.getChangeParameter() : null;
        if (!isObjectNode(changeParameter)) {
            return 0;
        }
        int count = 0;
        for (Iterator<String> it = changeParameter.fieldNames(); it.hasNext(); ) {
            it.next();
            count++;
        }
        return count;
    }

    private static String formatAuthor(String author) {
        if (author == null || author.isBlank()
                || AuditExportConstants.AUTHOR_UNKNOWN.equalsIgnoreCase(author)) {
            return AuditExportConstants.AUTHOR_SYSTEM;
        }
        return author;
    }

    private static String mapOperation(String type) {
        if (type == null || type.isBlank()) {
            return "";
        }
        return AuditExportConstants.OPERATION_INITIAL.equalsIgnoreCase(type)
                ? AuditExportConstants.OPERATION_CREATE
                : type;
    }

    private static boolean isObjectNode(JsonNode node) {
        return node != null && !node.isNull() && node.isObject();
    }

    private static String valueForField(JsonNode state, String fieldName) {
        if (!isObjectNode(state)) {
            return "";
        }
        JsonNode node = state.get(fieldName);
        if (node == null || node.isNull()) {
            return "";
        }
        return node.isValueNode() ? node.asText() : node.toString();
    }

    record PageRequest(int page) {}

    private static final class PagedAuditExportReader implements ExportItemReader {
        private final Function<PageRequest, Page<AccountAuditHistoryCommitDto>> pageLoader;
        private final String trackingNumber;

        private int page;
        private boolean noMorePages;
        private Iterator<AuditExportRow> buffer = List.<AuditExportRow>of().iterator();

        private PagedAuditExportReader(
                Function<PageRequest, Page<AccountAuditHistoryCommitDto>> pageLoader,
                String trackingNumber) {
            this.pageLoader = pageLoader;
            this.trackingNumber = trackingNumber;
        }

        @Override
        public Object read() {
            while (!buffer.hasNext() && !noMorePages) {
                loadNextPage();
            }
            return buffer.hasNext() ? buffer.next() : null;
        }

        private void loadNextPage() {
            Page<AccountAuditHistoryCommitDto> result = pageLoader.apply(new PageRequest(page));
            if (result.getContent().isEmpty()) {
                noMorePages = true;
                return;
            }
            List<AuditExportRow> pageRows = new ArrayList<>();
            for (AccountAuditHistoryCommitDto block : result.getContent()) {
                pageRows.addAll(flattenCommitBlock(block, trackingNumber));
            }
            buffer = pageRows.iterator();
            if (result.isLast()) {
                noMorePages = true;
            } else {
                page++;
            }
        }
    }
}
