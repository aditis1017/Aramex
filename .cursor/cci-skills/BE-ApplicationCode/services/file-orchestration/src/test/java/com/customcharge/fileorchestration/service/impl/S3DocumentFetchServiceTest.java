package com.customcharge.fileorchestration.service.impl;

import com.customcharge.persistencelayer.util.aws.S3Helper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3DocumentFetchServiceTest {

    @Mock
    private S3Helper s3Helper;

    @InjectMocks
    private S3DocumentFetchService service;

    @Test
    void fetch_shouldRejectBlankPath() {
        assertThatThrownBy(() -> service.fetch(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("documentPath");
    }

    @Test
    void fetch_shouldParseS3Uri_andReturnStream() {
        InputStream stream = new ByteArrayInputStream(new byte[] {1});
        when(s3Helper.getAsStream("bucket", "folder/file.csv")).thenReturn(Optional.of(stream));

        assertThat(service.fetch("s3://bucket/folder/file.csv")).isSameAs(stream);
    }

    @Test
    void fetch_shouldRejectInvalidS3Uri() {
        assertThatThrownBy(() -> service.fetch("s3://bucketonly"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid S3 path format");
    }

    @Test
    void fetch_shouldTreatPlainValueAsObjectKey() {
        InputStream stream = new ByteArrayInputStream(new byte[] {2});
        when(s3Helper.getAsStream("folder/file.csv")).thenReturn(Optional.of(stream));

        assertThat(service.fetch("folder/file.csv")).isSameAs(stream);
    }

    @Test
    void fetch_shouldThrow_whenObjectMissing() {
        when(s3Helper.getAsStream("missing.csv")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.fetch("missing.csv"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("S3 object not found");
    }
}
