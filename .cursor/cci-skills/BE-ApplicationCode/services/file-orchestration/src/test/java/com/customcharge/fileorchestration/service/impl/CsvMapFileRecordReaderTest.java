package com.customcharge.fileorchestration.service.impl;

import com.customcharge.fileorchestration.support.FileOrchestrationTestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CsvMapFileRecordReaderTest {

    private final CsvMapFileRecordReader reader = new CsvMapFileRecordReader();

    @AfterEach
    void tearDown() throws IOException {
        reader.close();
    }

    @Test
    void supports_shouldAcceptCsvOnly() {
        assertThat(reader.supports("CSV")).isTrue();
        assertThat(reader.supports("XLSX")).isFalse();
    }

    @Test
    void read_shouldParseHeadersAndRows_andNormalizeBlankValuesToNull() throws IOException {
        reader.open(FileOrchestrationTestDataFactory.csvStream("""
                awb,saddad
                AWB-1,S1
                AWB-2,
                """));

        Map<String, String> first = reader.read();
        Map<String, String> second = reader.read();
        Map<String, String> eof = reader.read();

        assertThat(first).containsEntry("awb", "AWB-1").containsEntry("saddad", "S1");
        assertThat(second).containsEntry("awb", "AWB-2").containsEntry("saddad", null);
        assertThat(eof).isNull();
    }
}
