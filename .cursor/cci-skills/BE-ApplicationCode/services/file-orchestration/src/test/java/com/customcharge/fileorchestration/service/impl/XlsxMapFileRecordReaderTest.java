package com.customcharge.fileorchestration.service.impl;

import com.customcharge.fileorchestration.support.FileOrchestrationTestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XlsxMapFileRecordReaderTest {

    private final XlsxMapFileRecordReader reader = new XlsxMapFileRecordReader();

    @AfterEach
    void tearDown() {
        reader.close();
    }

    @Test
    void supports_shouldAcceptXlsxOnly() {
        assertThat(reader.supports("XLSX")).isTrue();
        assertThat(reader.supports("CSV")).isFalse();
    }

    @Test
    void read_shouldParseFirstSheetRows_andSkipBlankDataRows() throws IOException {
        reader.open(FileOrchestrationTestDataFactory.xlsxStream(new String[][] {
                {"awb", "saddad"},
                {"AWB-1", "S1"},
                {"", ""},
                {"AWB-2", "S2"}
        }));

        Map<String, String> first = reader.read();
        Map<String, String> second = reader.read();
        Map<String, String> eof = reader.read();

        assertThat(first).containsEntry("awb", "AWB-1").containsEntry("saddad", "S1");
        assertThat(second).containsEntry("awb", "AWB-2");
        assertThat(eof).isNull();
    }

    @Test
    void open_shouldRejectDuplicateHeaders() throws IOException {
        byte[] bytes = FileOrchestrationTestDataFactory.xlsxBytes(new String[][] {
                {"awb", "AWB"},
                {"AWB-1", "S1"}
        });

        assertThatThrownBy(() -> reader.open(new java.io.ByteArrayInputStream(bytes)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to open XLSX reader");
    }
}
