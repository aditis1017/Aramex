package com.customcharge.fileorchestration.dedup;

import com.customcharge.fileorchestration.service.FileRecordReader;
import com.customcharge.fileorchestration.support.FileOrchestrationTestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DedupingFileRecordReaderTest {

    @Test
    void open_shouldDedupeRows_andPopulateStatistics() throws IOException {
        List<Map<String, String>> sourceRows = List.of(
                FileOrchestrationTestDataFactory.row("AWB-1", "S1"),
                FileOrchestrationTestDataFactory.row("AWB-1", "S2"),
                FileOrchestrationTestDataFactory.row("AWB-2", "S3"));
        StubFileRecordReader delegate = new StubFileRecordReader(sourceRows);
        DedupeStatistics statistics = new DedupeStatistics();
        DedupingFileRecordReader reader = new DedupingFileRecordReader(
                delegate, "Estimates_Template", true, statistics);

        reader.open(new ByteArrayInputStream(new byte[0]));

        assertThat(reader.read().get("saddad")).isEqualTo("S2");
        assertThat(reader.read().get("awb")).isEqualTo("AWB-2");
        assertThat(reader.read()).isNull();
        assertThat(statistics.getRawRowCount()).isEqualTo(3);
        assertThat(statistics.getDedupedRowCount()).isEqualTo(2);

        reader.close();
        verify(delegate.mock).close();
    }

    @Test
    void supports_shouldDelegateToUnderlyingReader() {
        FileRecordReader<Map<String, String>> delegate = mock(FileRecordReader.class);
        when(delegate.supports("CSV")).thenReturn(true);
        DedupingFileRecordReader reader = new DedupingFileRecordReader(
                delegate, "Estimates_Template", true, new DedupeStatistics());

        org.assertj.core.api.Assertions.assertThat(reader.supports("CSV")).isTrue();

        verify(delegate).supports("CSV");
    }

    private static final class StubFileRecordReader implements FileRecordReader<Map<String, String>> {
        private final FileRecordReader<Map<String, String>> mock = mock(FileRecordReader.class);
        private final List<Map<String, String>> rows;
        private int index;

        private StubFileRecordReader(List<Map<String, String>> rows) {
            this.rows = new ArrayList<>(rows);
        }

        @Override
        public boolean supports(String fileType) {
            return mock.supports(fileType);
        }

        @Override
        public void open(java.io.InputStream inputStream) {
            index = 0;
        }

        @Override
        public Map<String, String> read() {
            if (index >= rows.size()) {
                return null;
            }
            return rows.get(index++);
        }

        @Override
        public void close() throws IOException {
            mock.close();
        }
    }
}
