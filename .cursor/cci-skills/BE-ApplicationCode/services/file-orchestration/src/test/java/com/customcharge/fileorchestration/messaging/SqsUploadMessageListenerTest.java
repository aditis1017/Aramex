package com.customcharge.fileorchestration.messaging;

import com.customcharge.fileorchestration.pipeline.FileOrchestrationPipeline;
import com.customcharge.fileorchestration.pipeline.FileProcessingRequest;
import com.customcharge.fileorchestration.process.FileProcessRedisStore;
import com.customcharge.fileorchestration.service.FileTypeSupportService;
import com.customcharge.fileorchestration.support.FileOrchestrationTestDataFactory;
import com.customcharge.persistencelayer.entities.StagedFileUpload;
import com.customcharge.persistencelayer.enums.FileUploadStatus;
import com.customcharge.persistencelayer.repository.StagedFileUploadRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsUploadMessageListenerTest {

    @Mock
    private FileTypeSupportService fileTypeSupportService;
    @Mock
    private FileOrchestrationPipeline fileOrchestrationPipeline;
    @Mock
    private FileProcessRedisStore processRedisStore;
    @Mock
    private StagedFileUploadRepository stagedFileUploadRepository;

    private SqsUploadMessageListener listener;

    @BeforeEach
    void setUp() {
        listener = new SqsUploadMessageListener(
                new ObjectMapper(),
                fileTypeSupportService,
                fileOrchestrationPipeline,
                processRedisStore,
                stagedFileUploadRepository);
    }

    @Test
    void listen_shouldRunPipeline_whenS3EventValidAndLockAcquired() throws Exception {
        UUID fileId = UUID.fromString("c5f29284-237c-4079-bb37-0107d8aeac38");
        StagedFileUpload staged = stagedUpload(fileId, "Estimates-Upload-Template-RUH (1).xlsx");
        ReflectionTestUtils.setField(staged, "createdBy", "user@test.com");
        when(stagedFileUploadRepository.findByS3BucketAndS3Key(
                        "mvp-demo-bucket-v2",
                        "uploaded-files/c5f29284-237c-4079-bb37-0107d8aeac38/Estimates-Upload-Template-RUH (1).xlsx"))
                .thenReturn(Optional.of(staged));
        when(fileTypeSupportService.supports("XLSX")).thenReturn(true);
        when(processRedisStore.tryAcquire(any())).thenReturn(true);

        listener.listen(FileOrchestrationTestDataFactory.validS3UploadEvent());

        ArgumentCaptor<FileProcessingRequest> requestCaptor = ArgumentCaptor.forClass(FileProcessingRequest.class);
        verify(fileOrchestrationPipeline).process(requestCaptor.capture());
        FileProcessingRequest request = requestCaptor.getValue();
        assertThat(request.uploadedBy()).isEqualTo("user@test.com");
        assertThat(request.fileId()).isEqualTo(fileId.toString());
        assertThat(request.fileName()).isEqualTo("Estimates-Upload-Template-RUH (1).xlsx");
        assertThat(request.fileType()).isEqualTo("XLSX");
        assertThat(request.processFor()).isEqualTo("Estimates_Template");
        assertThat(request.documentPath())
                .isEqualTo("uploaded-files/c5f29284-237c-4079-bb37-0107d8aeac38/Estimates-Upload-Template-RUH (1).xlsx");
        assertThat(request.entityCode()).isEqualTo("RUH");
    }

    @Test
    void listen_shouldSkip_whenRedisLockNotAcquired() throws Exception {
        UUID fileId = UUID.fromString("c5f29284-237c-4079-bb37-0107d8aeac38");
        StagedFileUpload staged = stagedUpload(fileId, "Estimates-Upload-Template-RUH (1).xlsx");
        when(stagedFileUploadRepository.findByS3BucketAndS3Key(any(), any())).thenReturn(Optional.of(staged));
        when(fileTypeSupportService.supports("XLSX")).thenReturn(true);
        when(processRedisStore.tryAcquire(any())).thenReturn(false);

        listener.listen(FileOrchestrationTestDataFactory.validS3UploadEvent());

        verify(fileOrchestrationPipeline, never()).process(any());
    }

    @Test
    void listen_shouldRejectInvalidPayload() throws Exception {
        assertThatThrownBy(() -> listener.listen("{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing Records");
        verify(fileOrchestrationPipeline, never()).process(any());
    }

    @Test
    void listen_shouldRejectWhenStagedUploadMissing() throws Exception {
        when(stagedFileUploadRepository.findByS3BucketAndS3Key(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listener.listen(FileOrchestrationTestDataFactory.validS3UploadEvent()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No staged upload");
        verify(fileOrchestrationPipeline, never()).process(any());
    }

    @Test
    void listen_shouldRejectUnsupportedFileType() throws Exception {
        UUID fileId = UUID.randomUUID();
        StagedFileUpload staged = stagedUpload(fileId, "file.csv");
        when(stagedFileUploadRepository.findByS3BucketAndS3Key(any(), any())).thenReturn(Optional.of(staged));
        when(fileTypeSupportService.supports("CSV")).thenReturn(false);

        assertThatThrownBy(() -> listener.listen(FileOrchestrationTestDataFactory.validS3UploadEvent()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported fileType");
    }

    @Test
    void listen_shouldSkipAlreadyValidatedUpload() throws Exception {
        UUID fileId = UUID.randomUUID();
        StagedFileUpload staged = stagedUpload(fileId, "file.xlsx");
        staged.setStatus(FileUploadStatus.VALIDATED);
        when(stagedFileUploadRepository.findByS3BucketAndS3Key(any(), any())).thenReturn(Optional.of(staged));

        listener.listen(FileOrchestrationTestDataFactory.validS3UploadEvent());

        verify(fileOrchestrationPipeline, never()).process(any());
        verify(processRedisStore, never()).tryAcquire(any());
    }

    @Test
    void listen_shouldSkipAlreadyOrchestratedUpload() throws Exception {
        UUID fileId = UUID.randomUUID();
        StagedFileUpload staged = stagedUpload(fileId, "file.xlsx");
        staged.setExpectedDataChunks(10);
        when(stagedFileUploadRepository.findByS3BucketAndS3Key(any(), any())).thenReturn(Optional.of(staged));

        listener.listen(FileOrchestrationTestDataFactory.validS3UploadEvent());

        verify(fileOrchestrationPipeline, never()).process(any());
    }

    @Test
    void listen_shouldIgnoreS3TestEvent() throws Exception {
        listener.listen("{\"Event\":\"s3:TestEvent\"}");

        verify(fileOrchestrationPipeline, never()).process(any());
        verify(stagedFileUploadRepository, never()).findByS3BucketAndS3Key(any(), any());
    }

    @Test
    void listen_shouldRejectUnsupportedEventName() {
        String payload = """
                {
                  "Records" : [ {
                    "eventName" : "ObjectRemoved:Delete",
                    "s3" : {
                      "bucket" : { "name" : "mvp-demo-bucket-v2" },
                      "object" : { "key" : "uploaded-files/file.xlsx" }
                    }
                  } ]
                }
                """;

        assertThatThrownBy(() -> listener.listen(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported S3 event");
    }

    @Test
    void listen_shouldRejectWhenBucketOrKeyMissing() {
        String payload = """
                {
                  "Records" : [ {
                    "eventName" : "ObjectCreated:Put",
                    "s3" : { "bucket" : { "name" : "mvp-demo-bucket-v2" } }
                  } ]
                }
                """;

        assertThatThrownBy(() -> listener.listen(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing bucket or object key");
    }

    private static StagedFileUpload stagedUpload(UUID fileId, String fileName) {
        StagedFileUpload staged = new StagedFileUpload();
        staged.setFileId(fileId.toString());
        staged.setUploadProfile("Estimates_Template");
        staged.setOriginalFileName(fileName);
        staged.setS3Bucket("mvp-demo-bucket-v2");
        staged.setS3Key("uploaded-files/" + fileId + "/" + fileName);
        staged.setStatus(FileUploadStatus.UPLOADED);
        staged.setEntityCode("RUH");
        return staged;
    }
}
