package com.customcharge.fileorchestration.service.impl;

import com.customcharge.fileorchestration.service.FileRecordReader;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfiguredFileRecordReaderFactoryTest {

    private final ConfiguredFileRecordReaderFactory factory = new ConfiguredFileRecordReaderFactory();

    @Test
    void getReader_shouldReturnNewCsvInstanceEachCall() {
        FileRecordReader<Map<String, String>> first = factory.getReader("CSV");
        FileRecordReader<Map<String, String>> second = factory.getReader("CSV");

        assertThat(first).isInstanceOf(CsvMapFileRecordReader.class);
        assertThat(second).isInstanceOf(CsvMapFileRecordReader.class);
        assertThat(first).isNotSameAs(second);
    }

    @Test
    void getReader_shouldReturnNewXlsxInstanceEachCall() {
        FileRecordReader<Map<String, String>> first = factory.getReader("XLSX");
        FileRecordReader<Map<String, String>> second = factory.getReader("xlsx");

        assertThat(first).isInstanceOf(XlsxMapFileRecordReader.class);
        assertThat(second).isInstanceOf(XlsxMapFileRecordReader.class);
        assertThat(first).isNotSameAs(second);
    }

    @Test
    void getReader_shouldThrow_whenNoReaderConfigured() {
        assertThatThrownBy(() -> factory.getReader("PDF"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No reader configured");
    }
}
