package com.customcharge.persistencelayer.service;

import com.customcharge.persistencelayer.entity.AbstractEntity;
import com.customcharge.persistencelayer.entities.ProcessedChunk;
import com.customcharge.persistencelayer.repository.ProcessedChunkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessedChunkServiceTest {

    @Mock
    private ProcessedChunkRepository processedChunkRepository;

    private ProcessedChunkService processedChunkService;

    private final UUID fileId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        processedChunkService = new ProcessedChunkService(processedChunkRepository);
    }

    @Test
    void tryBeginChunk_shouldAccept_whenNoExistingClaim() {
        when(processedChunkRepository.findByFileIdAndChunkIndex(fileId, 0)).thenReturn(Optional.empty());
        when(processedChunkRepository.saveAndFlush(any(ProcessedChunk.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChunkBeginResult result = processedChunkService.tryBeginChunk(fileId, 0);

        assertThat(result).isEqualTo(ChunkBeginResult.ACCEPTED);
        verify(processedChunkRepository).saveAndFlush(any(ProcessedChunk.class));
    }

    @Test
    void tryBeginChunk_shouldReturnAlreadyCompleted_whenCompletedClaimExists() {
        ProcessedChunk completed = claimRow(0);
        completed.setCompletedAt(Instant.now());
        when(processedChunkRepository.findByFileIdAndChunkIndex(fileId, 0)).thenReturn(Optional.of(completed));

        ChunkBeginResult result = processedChunkService.tryBeginChunk(fileId, 0);

        assertThat(result).isEqualTo(ChunkBeginResult.ALREADY_COMPLETED);
        verify(processedChunkRepository, never()).saveAndFlush(any());
        verify(processedChunkRepository, never()).delete(any());
    }

    @Test
    void tryBeginChunk_shouldReturnInProgress_whenActiveClaimExists() {
        ProcessedChunk inFlight = claimRowWithCreatedTime(0, Instant.now());
        when(processedChunkRepository.findByFileIdAndChunkIndex(fileId, 0)).thenReturn(Optional.of(inFlight));

        ChunkBeginResult result = processedChunkService.tryBeginChunk(fileId, 0);

        assertThat(result).isEqualTo(ChunkBeginResult.IN_PROGRESS);
        verify(processedChunkRepository, never()).saveAndFlush(any());
        verify(processedChunkRepository, never()).delete(any());
    }

    @Test
    void tryBeginChunk_shouldReclaimStaleClaim_andAccept() {
        ProcessedChunk stale = claimRowWithCreatedTime(
                0, Instant.now().minus(ProcessedChunkService.STALE_INCOMPLETE_CLAIM.plusMinutes(1)));
        when(processedChunkRepository.findByFileIdAndChunkIndex(fileId, 0)).thenReturn(Optional.of(stale));
        when(processedChunkRepository.saveAndFlush(any(ProcessedChunk.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChunkBeginResult result = processedChunkService.tryBeginChunk(fileId, 0);

        assertThat(result).isEqualTo(ChunkBeginResult.ACCEPTED);
        verify(processedChunkRepository).delete(stale);
        verify(processedChunkRepository).flush();
        verify(processedChunkRepository).saveAndFlush(any(ProcessedChunk.class));
    }

    @Test
    void tryBeginChunk_shouldRetryAfterConcurrentInsert() {
        ProcessedChunk inFlight = claimRowWithCreatedTime(0, Instant.now());
        when(processedChunkRepository.findByFileIdAndChunkIndex(fileId, 0))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(inFlight));
        when(processedChunkRepository.saveAndFlush(any(ProcessedChunk.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChunkBeginResult result = processedChunkService.tryBeginChunk(fileId, 0);

        assertThat(result).isEqualTo(ChunkBeginResult.IN_PROGRESS);
        verify(processedChunkRepository, times(2)).findByFileIdAndChunkIndex(fileId, 0);
    }

    @Test
    void tryBeginChunk_shouldReturnInProgress_afterExhaustingInsertAttempts() {
        ProcessedChunk inFlight = claimRowWithCreatedTime(0, Instant.now());
        when(processedChunkRepository.findByFileIdAndChunkIndex(fileId, 0))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(inFlight));
        when(processedChunkRepository.saveAndFlush(any(ProcessedChunk.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        ChunkBeginResult result = processedChunkService.tryBeginChunk(fileId, 0);

        assertThat(result).isEqualTo(ChunkBeginResult.IN_PROGRESS);
    }

    @Test
    void markChunkCompleted_shouldSetCompletedAt() {
        ProcessedChunk row = claimRow(1);
        when(processedChunkRepository.findByFileIdAndChunkIndex(fileId, 1)).thenReturn(Optional.of(row));

        processedChunkService.markChunkCompleted(fileId, 1);

        ArgumentCaptor<ProcessedChunk> captor = ArgumentCaptor.forClass(ProcessedChunk.class);
        verify(processedChunkRepository).save(captor.capture());
        assertThat(captor.getValue().getCompletedAt()).isNotNull();
    }

    private ProcessedChunk claimRow(int chunkIndex) {
        ProcessedChunk row = new ProcessedChunk();
        row.setFileId(fileId);
        row.setChunkIndex(chunkIndex);
        return row;
    }

    private ProcessedChunk claimRowWithCreatedTime(int chunkIndex, Instant createdTime) {
        ProcessedChunk row = claimRow(chunkIndex);
        setCreatedTime(row, createdTime);
        return row;
    }

    private static void setCreatedTime(ProcessedChunk row, Instant createdTime) {
        try {
            var field = AbstractEntity.class.getDeclaredField("createdTime");
            field.setAccessible(true);
            field.set(row, createdTime);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
