package com.customcharge.fileorchestration.service.impl;

import com.customcharge.fileorchestration.service.FileRecordReader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvMapFileRecordReader implements FileRecordReader<Map<String, String>> {

    private Reader reader;
    private CSVParser parser;
    private Iterator<CSVRecord> iterator;
    private List<Map.Entry<String, Integer>> headerByIndex;

    @Override
    public boolean supports(String fileType) {
        return "CSV".equalsIgnoreCase(fileType);
    }

    @Override
    public void open(InputStream inputStream) throws IOException {
        this.reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        this.parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .build()
                .parse(reader);
        this.iterator = parser.iterator();
        this.headerByIndex = new ArrayList<>(parser.getHeaderMap().entrySet());
        this.headerByIndex.sort(Comparator.comparing(Map.Entry::getValue));
    }

    @Override
    public Map<String, String> read() {
        if (iterator == null || !iterator.hasNext()) {
            return null;
        }

        CSVRecord record = iterator.next();
        Map<String, String> row = new LinkedHashMap<>(headerByIndex.size());
        for (Map.Entry<String, Integer> e : headerByIndex) {
            String headerName = e.getKey();
            int col = e.getValue();
            String raw = col < record.size() ? record.get(col) : null;
            row.put(headerName, toNullIfBlank(raw));
        }
        return row;
    }

    private static String toNullIfBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public void close() throws IOException {
        if (parser != null) {
            parser.close();
        }
        if (reader != null) {
            reader.close();
        }
    }
}
