package com.customcharge.fileorchestration.pipeline;

import com.customcharge.fileorchestration.dedup.DedupeStatistics;
import com.customcharge.fileorchestration.service.FileRecordReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileProcessingContextTest {

    @Mock
    private FileRecordReader<Map<String, String>> recordReader;

    @Test
    void shouldTrackChunkCounterAndDedupeStatistics() {
        FileProcessingContext context = new FileProcessingContext(request(), "SAR");

        assertThat(context.defaultCurrencyCode()).isEqualTo("SAR");
        assertThat(context.nextChunkIndex()).isEqualTo(1);
        assertThat(context.nextChunkIndex()).isEqualTo(2);
        assertThat(context.publishedDataChunkCount()).isEqualTo(2);
        assertThat(context.dedupeStatistics()).isInstanceOf(DedupeStatistics.class);

        context.setDedupedRows(List.of(Map.of("awb", "A1")));
        assertThat(context.dedupedRows()).containsExactly(Map.of("awb", "A1"));

        context.clearDedupedRows();
        assertThat(context.dedupedRows()).isEmpty();
    }

    @Test
    void setDedupedRows_shouldTreatNullAsEmpty() {
        FileProcessingContext context = new FileProcessingContext(request(), null);
        context.setDedupedRows(null);
        assertThat(context.dedupedRows()).isEmpty();
    }

    @Test
    void close_shouldCloseAttachedResources() throws Exception {
        FileProcessingContext context = new FileProcessingContext(request(), null);
        InputStream inputStream = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));
        context.attachStream(inputStream, recordReader);

        context.close();

        verify(recordReader).close();
        assertThat(context.dedupedRows()).isEmpty();
    }

    @Test
    void close_shouldAggregateReaderAndStreamFailures() throws Exception {
        FileProcessingContext context = new FileProcessingContext(request(), null);
        InputStream inputStream = new InputStream() {
            @Override
            public int read() {
                return -1;
            }

            @Override
            public void close() throws IOException {
                throw new IOException("stream close failed");
            }
        };
        context.attachStream(inputStream, recordReader);
        doThrow(new IOException("reader close failed")).when(recordReader).close();

        assertThatThrownBy(context::close)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to close file processing resources")
                .hasCauseInstanceOf(IOException.class);
    }

    private static FileProcessingRequest request() {
        return new FileProcessingRequest(
                "file-1", "user", "path", "file.csv", "CSV", "Estimates_Template", "RUH", "trace");
    }
}
