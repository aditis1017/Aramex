package com.customcharge.global_settings.export;

import com.customcharge.global_settings.rules.dto.GlobalSettingAuditHistoryCommitDto;
import com.customcharge.global_settings.rules.dto.GlobalSettingAuditStateChangeDto;
import com.customcharge.global_settings.rules.service.GlobalSettingService;
import com.customcharge.persistencelayer.audit.model.AuditQueryRequest;
import com.customcharge.persistencelayer.export.model.ExportContext;
import com.customcharge.persistencelayer.export.model.ExportRow;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.export.registry.ExportItemProcessor;
import com.customcharge.persistencelayer.export.registry.ExportItemReader;
import com.customcharge.persistencelayer.export.registry.ExportPipeline;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;

public class GlobalSettingExportPipeline implements ExportPipeline {
    public static final String EXPORT_TYPE = "GLOBAL_SETTING";
    private static final String SCREEN_NAME = "Global Setting";
    private static final DateTimeFormatter EXPORT_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
    private static final String DEFAULT_GROUP_NAME = "GLOBAL_SETTING";
    private static final List<String> EXPORT_COLUMN_NAMES = List.of(
            "Timestamp",
            "Section",
            "Target Value",
            "User",
            "Action Perform",
            "Rule Order",
            "Field Name",
            "Old Value",
            "New Value"
    );

    private final GlobalSettingService globalSettingService;
    private final ObjectMapper objectMapper;

    public GlobalSettingExportPipeline(
            GlobalSettingService globalSettingService,
            ObjectMapper objectMapper) {
        this.globalSettingService = globalSettingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String exportType() {
        return EXPORT_TYPE;
    }

    @Override
    public ExportItemReader createReader(ExportContext context) {
        return new StreamingAuditReader(buildAuditRequest(context));
    }

    @Override
    public OptionalLong resolveExportTotalRecords(ExportContext context) throws Exception {
        long total = countFlattenedRows(buildAuditRequest(context));
        return total > 0 ? OptionalLong.of(total) : OptionalLong.empty();
    }

    @Override
    public List<String> columnNames() {
        return EXPORT_COLUMN_NAMES;
    }

    @Override
    public String fileNamePrefix() {
        return "Global-Settings-History";
    }

    @Override
    public ExportItemProcessor createProcessor(ExportContext context) {
        return item -> {
            AuditExportItem e = (AuditExportItem) item;
            String ts = e.changedOn() == null ? "" : EXPORT_TIMESTAMP.format(e.changedOn());
            return new ExportRow(List.of(
                    ts, SCREEN_NAME, notNull(e.subSection()), notNull(e.changedBy()), notNull(e.operation()),
                    notNull(e.ruleSequence()), notNull(e.fieldName()), notNull(e.oldValue()), notNull(e.newValue())));
        };
    }

    private AuditQueryRequest buildAuditRequest(ExportContext context) {
        String filterSpec = context.filterSpec();
        if (filterSpec == null || filterSpec.isBlank()) {
            throw new BadRequestException(
                    "Export filterSpec is required and must include Start Date and End Date.");
        }
        try {
            JsonNode root = objectMapper.readTree(filterSpec);
            String fromDateRaw = requireText(root, "fromDate");
            String toDateRaw = requireText(root, "toDate");
            String groupName = optionalText(root, "groupName");

            AuditQueryRequest request = new AuditQueryRequest();
            request.setFromDate(Instant.parse(fromDateRaw.strip()));
            request.setToDate(Instant.parse(toDateRaw.strip()));
            request.setGroupName(groupName != null && !groupName.isBlank() ? groupName.toUpperCase(Locale.ROOT) : DEFAULT_GROUP_NAME);
            request.setPage(0);
            request.setSize(context.pageSize());
            return request;
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException(
                    "Invalid filterSpec JSON. Expected fromDate/toDate as UTC instants (e.g. 2026-01-01T00:00:00Z), and optional groupName.", ex);
        }
    }

    private static String requireText(JsonNode root, String key) {
        JsonNode node = root.get(key);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            throw new BadRequestException("Filter Spec." + key + " is required and must not be blank.");
        }
        return node.asText();
    }

    private static String optionalText(JsonNode root, String key) {
        JsonNode node = root.get(key);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    private static String notNull(String v) {
        return v != null ? v : "";
    }

    private static String subSectionFromCurrentState(JsonNode currentState) {
        String targetValue = valueForField(currentState, "targetValue").trim();
        if (targetValue.isEmpty()) {
            return "Category";
        }
        if ("DDU".equalsIgnoreCase(targetValue) || "DDP".equalsIgnoreCase(targetValue)) {
            return targetValue.toUpperCase(Locale.ROOT);
        }
        return "Category";
    }

    private static String mapOperationType(String type) {
        if (type == null || type.isBlank()) {
            return "";
        }
        if ("INITIAL".equalsIgnoreCase(type)) {
            return "Create";
        }
        return type;
    }

    private static String ruleSequenceFromCurrentState(JsonNode currentState) {
        if (currentState == null || currentState.isNull() || !currentState.isObject()) {
            return "";
        }
        JsonNode seq = currentState.get("sequenceNumber");
        if (seq == null || seq.isNull()) {
            return "";
        }
        if (seq.isIntegralNumber()) {
            return String.valueOf(seq.longValue());
        }
        return seq.asText();
    }

    private static String toCellValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isValueNode()) {
            return node.asText();
        }
        return node.toString();
    }

    private static String valueForField(JsonNode state, String fieldName) {
        if (state == null || state.isNull() || !state.isObject()) {
            return "";
        }
        return toCellValue(state.get(fieldName));
    }

    private long countFlattenedRows(AuditQueryRequest request) {
        AuditQueryRequest paged = copyRequest(request);
        long count = 0L;
        paged.setPage(0);
        while (true) {
            Page<GlobalSettingAuditHistoryCommitDto> page = globalSettingService.getAuditHistory(paged);
            if (page.getContent().isEmpty()) {
                break;
            }
            for (GlobalSettingAuditHistoryCommitDto block : page.getContent()) {
                count += countCommitRows(block);
            }
            if (page.isLast()) {
                break;
            }
            paged.setPage(paged.getPage() + 1);
        }
        return count;
    }

    private static int countCommitRows(GlobalSettingAuditHistoryCommitDto block) {
        if (block == null) {
            return 0;
        }
        GlobalSettingAuditStateChangeDto entry = block.getStateChangeData();
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

    private List<AuditExportItem> flattenCommitBlock(GlobalSettingAuditHistoryCommitDto block) {
        List<AuditExportItem> rows = new ArrayList<>();
        if (block == null) {
            return rows;
        }
        GlobalSettingAuditStateChangeDto entry = block.getStateChangeData();
        JsonNode changeParameter = entry != null ? entry.getChangeParameter() : null;
        if (!isObjectNode(changeParameter)) {
            return rows;
        }
        JsonNode currentState = entry != null ? entry.getCurrentState() : null;
        String subSection = subSectionFromCurrentState(currentState);
        String operation = mapOperationType(entry != null ? entry.getType() : null);
        String ruleSequence = ruleSequenceFromCurrentState(currentState);
        String author = (block.getAuthor() == null || block.getAuthor().isBlank() || block.getAuthor().equals("unknown"))
                ? "System"
                : block.getAuthor();
        for (Iterator<String> it = changeParameter.fieldNames(); it.hasNext(); ) {
            String fieldName = it.next();
            rows.add(new AuditExportItem(
                    block.getCommitDate(),
                    subSection,
                    author,
                    operation,
                    ruleSequence,
                    fieldName,
                    valueForField(entry.getPreState(), fieldName),
                    valueForField(entry.getCurrentState(), fieldName)));
        }
        return rows;
    }

    private static AuditQueryRequest copyRequest(AuditQueryRequest source) {
        AuditQueryRequest copy = new AuditQueryRequest();
        copy.setFromDate(source.getFromDate());
        copy.setToDate(source.getToDate());
        copy.setGroupName(source.getGroupName());
        copy.setPage(source.getPage());
        copy.setSize(source.getSize());
        return copy;
    }

    private static boolean isObjectNode(JsonNode node) {
        return node != null && !node.isNull() && node.isObject();
    }

    private record AuditExportItem(
            Instant changedOn,
            String subSection,
            String changedBy,
            String operation,
            String ruleSequence,
            String fieldName,
            String oldValue,
            String newValue
    ) {
    }

    private final class StreamingAuditReader implements ExportItemReader {
        private final AuditQueryRequest request;
        private Iterator<AuditExportItem> buffer = List.<AuditExportItem>of().iterator();
        private boolean noMorePages;

        private StreamingAuditReader(AuditQueryRequest request) {
            this.request = copyRequest(request);
            this.request.setPage(0);
        }

        @Override
        public Object read() {
            while (!buffer.hasNext() && !noMorePages) {
                loadNextPage();
            }
            return buffer.hasNext() ? buffer.next() : null;
        }

        private void loadNextPage() {
            Page<GlobalSettingAuditHistoryCommitDto> page = globalSettingService.getAuditHistory(request);
            if (page.getContent().isEmpty()) {
                noMorePages = true;
                return;
            }
            List<AuditExportItem> pageRows = new ArrayList<>();
            for (GlobalSettingAuditHistoryCommitDto block : page.getContent()) {
                pageRows.addAll(flattenCommitBlock(block));
            }
            buffer = pageRows.iterator();
            if (page.isLast()) {
                noMorePages = true;
            } else {
                request.setPage(request.getPage() + 1);
            }
        }
    }
}
