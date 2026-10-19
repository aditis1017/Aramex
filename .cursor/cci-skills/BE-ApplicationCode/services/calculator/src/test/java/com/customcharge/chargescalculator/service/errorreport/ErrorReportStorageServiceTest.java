package com.customcharge.chargescalculator.service.errorreport;

import com.customcharge.chargescalculator.config.CustomChargeProperties;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.config.aws.S3Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorReportStorageServiceTest {

    @Mock
    private S3Client s3Client;
    @Mock
    private S3Properties s3Properties;
    @Mock
    private S3Presigner s3Presigner;

    private CustomChargeProperties customChargeProperties;

    @InjectMocks
    private ErrorReportStorageService service;

    @BeforeEach
    void setUp() {
        customChargeProperties = new CustomChargeProperties();
        service = new ErrorReportStorageService(s3Client, s3Properties, s3Presigner, customChargeProperties);
    }

    @Test
    void uploadAndGenerateLink_shouldUploadAndPresign(@TempDir Path tempDir) throws Exception {
        Path report = tempDir.resolve("report.xlsx");
        Files.writeString(report, "data");
        when(s3Properties.getDefaultBucket()).thenReturn("test-bucket");

        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://example.com/report"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        var result = service.uploadAndGenerateLink(CalculatorTestDataFactory.fileId(), "upload.xlsx", report);

        ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putCaptor.capture(), any(RequestBody.class));
        assertThat(putCaptor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(putCaptor.getValue().key()).contains("error-reports/");
        assertThat(result.downloadUrl()).isEqualTo("https://example.com/report");
    }

    @Test
    void uploadAndGenerateLink_shouldUseConfiguredPrefixAndFallbackFileName(@TempDir Path tempDir) throws Exception {
        Path report = tempDir.resolve("report.xlsx");
        Files.writeString(report, "data");
        customChargeProperties.getErrorReport().setUploadPrefix("custom-prefix");
        when(s3Properties.getDefaultBucket()).thenReturn("test-bucket");

        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://example.com/report"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        var result = service.uploadAndGenerateLink(CalculatorTestDataFactory.fileId(), " ", report);

        ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putCaptor.capture(), any(RequestBody.class));
        assertThat(putCaptor.getValue().key()).startsWith("custom-prefix/");
        assertThat(putCaptor.getValue().key()).contains("error-report-");
        assertThat(result.key()).isEqualTo(putCaptor.getValue().key());
    }

    @Test
    void uploadAndGenerateLink_shouldThrow_whenBucketMissing(@TempDir Path tempDir) throws Exception {
        Path report = tempDir.resolve("report.xlsx");
        Files.writeString(report, "data");
        when(s3Properties.getDefaultBucket()).thenReturn(" ");

        java.util.UUID fileId = CalculatorTestDataFactory.fileId();
        String uploadFileName = "upload.xlsx";
        assertThatThrownBy(() -> service.uploadAndGenerateLink(fileId, uploadFileName, report))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("default-bucket");
    }
}
