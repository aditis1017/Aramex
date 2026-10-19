package com.customcharge.dashboard.importfile.format;

import com.customcharge.dashboard.constants.ImportConstants;
import com.customcharge.dashboard.importfile.exception.ImportInternalException;
import com.customcharge.dashboard.importfile.support.CsvLineTokenizer;
import com.customcharge.dashboard.importfile.support.ImportColumnUtils;
import com.customcharge.persistencelayer.exception.AbstractApiException;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class CsvSpreadsheetMultipartReader implements SpreadsheetMultipartReader {
    private final ObjectMapper objectMapper;

    public CsvSpreadsheetMultipartReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    @Override
    public boolean supports(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        String name = file.getOriginalFilename();
        if (name == null) {
            return false;
        }
        return name.toLowerCase(Locale.ROOT).endsWith(ImportConstants.EXTENSION_CSV);
    }

    @Override
    public SpreadsheetReadOutcome read(MultipartFile file, List<String> requiredColumns, boolean trimCellValues) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String first = reader.readLine();
            if (first == null || first.isBlank()) {
                throw new ValidationException(ImportConstants.MESSAGE_CSV_HEADER_ROW_REQUIRED);
            }
            if (first.startsWith("\uFEFF")) {
                first = first.substring(1);
            }
            String headerLine = first.strip();
            if (headerLine.endsWith("\r")) {
                headerLine = headerLine.substring(0, headerLine.length() - 1).strip();
            }
            List<String> headers = CsvLineTokenizer.splitLine(headerLine, true);
            Optional<String> missing = ImportColumnUtils.missingRequiredColumns(requiredColumns, headers);
            if (missing.isPresent()) {
                throw new ValidationException(missing.get());
            }

            int dataRows = 0;
            String line;
            List<JsonNode> rows = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String bodyLine = trimCellValues ? line.strip() : line;
                if (bodyLine.endsWith("\r")) {
                    bodyLine = trimCellValues
                            ? bodyLine.substring(0, bodyLine.length() - 1).strip()
                            : bodyLine.substring(0, bodyLine.length() - 1);
                }
                List<String> values = CsvLineTokenizer.splitLine(bodyLine, trimCellValues);
                rows.add(rowObject(headers, values));
                dataRows++;
            }
            return new SpreadsheetReadOutcome(dataRows, rows);
        } catch (AbstractApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ImportInternalException(ImportConstants.MESSAGE_CSV_READ_FAILED, e);
        }
    }

    private JsonNode rowObject(List<String> headers, List<String> values) {
        ObjectNode node = objectMapper.createObjectNode();
        for (int i = 0; i < headers.size(); i++) {
            String key = headers.get(i);
            String value = i < values.size() ? values.get(i) : "";
            node.put(key, value);
        }
        return node;
    }
}
