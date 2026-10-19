package com.customcharge.chargescalculator.service.batch;

import com.customcharge.chargescalculator.dto.ProcessedChunkMessage;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.entities.CustomChargeFileError;
import com.customcharge.persistencelayer.repository.CustomChargeFileErrorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChunkErrorPersistenceServiceTest {

    @Mock
    private CustomChargeFileErrorRepository customChargeFileErrorRepository;

    @InjectMocks
    private ChunkErrorPersistenceService service;

    @Test
    void persistErrors_shouldNoOp_whenMessageNull() {
        service.persistErrors(null, List.of(Map.of("awb", "1")));

        verifyNoInteractions(customChargeFileErrorRepository);
    }

    @Test
    void persistErrors_shouldNoOp_whenFailedRecordsEmpty() {
        service.persistErrors(CalculatorTestDataFactory.chunkMessage(), List.of());

        verifyNoInteractions(customChargeFileErrorRepository);
    }

    @Test
    void persistErrors_shouldSaveCopiedRows() {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();
        Map<String, String> failed = new HashMap<>();
        failed.put("awb", "AWB-1");
        failed.put("errorMessage", "failed");
        List<Map<String, String>> failedRecords = new ArrayList<>();
        failedRecords.add(failed);
        failedRecords.add(null);

        service.persistErrors(message, failedRecords);

        ArgumentCaptor<CustomChargeFileError> captor = ArgumentCaptor.forClass(CustomChargeFileError.class);
        verify(customChargeFileErrorRepository).save(captor.capture());
        CustomChargeFileError saved = captor.getValue();
        assertThat(saved.getFileId()).isEqualTo(CalculatorTestDataFactory.fileId());
        assertThat(saved.getFileName()).isEqualTo("upload.xlsx");
        assertThat(saved.getChunkNumber()).isZero();
        assertThat(saved.getErrorList()).hasSize(1);
        assertThat(saved.getErrorList().getFirst()).containsEntry("awb", "AWB-1");
        assertThat(saved.getErrorList().getFirst()).isNotSameAs(failed);
    }
}
