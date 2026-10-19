package com.customcharge.persistencelayer.service;

import com.customcharge.persistencelayer.repository.StagedFileUploadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StagedFileUploadCompletionServiceTest {

    @Mock
    private StagedFileUploadRepository stagedFileUploadRepository;

    @InjectMocks
    private StagedFileUploadCompletionService service;

    @Test
    void tryMarkFileProcessingComplete_shouldReturnTrue_whenRowUpdated() {
        UUID fileId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        when(stagedFileUploadRepository.markFileProcessingCompleteIfAbsent(eq(fileId.toString()), any()))
                .thenReturn(1);

        assertThat(service.tryMarkFileProcessingComplete(fileId)).isTrue();

        verify(stagedFileUploadRepository).markFileProcessingCompleteIfAbsent(eq(fileId.toString()), any());
    }

    @Test
    void tryMarkFileProcessingComplete_shouldReturnFalse_whenAlreadyMarked() {
        UUID fileId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        when(stagedFileUploadRepository.markFileProcessingCompleteIfAbsent(eq(fileId.toString()), any()))
                .thenReturn(0);

        assertThat(service.tryMarkFileProcessingComplete(fileId)).isFalse();
    }
}
