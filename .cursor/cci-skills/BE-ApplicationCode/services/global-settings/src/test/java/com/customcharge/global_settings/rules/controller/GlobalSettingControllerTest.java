package com.customcharge.global_settings.rules.controller;

import com.customcharge.global_settings.rules.dto.MessageResponse;
import com.customcharge.global_settings.rules.dto.UpdateGlobalSettingSequenceRequest;
import com.customcharge.global_settings.rules.dto.UpdateStatusRequest;
import com.customcharge.global_settings.rules.service.GlobalSettingService;
import com.customcharge.global_settings.rules.service.imp.GlobalSettingSyncService;
import com.customcharge.persistencelayer.audit.model.AuditQueryRequest;
import com.customcharge.persistencelayer.dto.GlobalSettingDto;
import com.customcharge.persistencelayer.search.SearchCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSettingControllerTest {

    @Mock
    private GlobalSettingService globalSettingService;

    @Mock
    private GlobalSettingSyncService globalSettingSyncService;

    @InjectMocks
    private GlobalSettingController controller;

    @Test
    void updateStatus_shouldDelegateToService() {
        UUID id = UUID.randomUUID();
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(true);
        when(globalSettingService.updateStatus(id, true)).thenReturn(new MessageResponse("Status Updated"));

        MessageResponse response = controller.updateStatus(id.toString(), request);

        assertThat(response.getMessage()).isEqualTo("Status Updated");
    }

    @Test
    void sync_shouldDelegateToSyncService() {
        LocalDate date = LocalDate.of(2026, 5, 24);
        when(globalSettingSyncService.syncGlobalSettings(date, true))
                .thenReturn(new MessageResponse("Sync Completed"));

        MessageResponse response = controller.sync(date);

        assertThat(response.getMessage()).isEqualTo("Sync Completed");
        verify(globalSettingSyncService).syncGlobalSettings(date, true);
    }

    @Test
    void getDataByCriteria_shouldDelegateToService() {
        Page<GlobalSettingDto> page = new PageImpl<>(List.of());
        when(globalSettingService.getDataByCriteria(List.of(), 0, 10)).thenReturn(page);

        Page<GlobalSettingDto> result = controller.getDataByCriteria(List.of(), 0, 10);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void updateSequence_shouldDelegateToService() {
        UpdateGlobalSettingSequenceRequest request = new UpdateGlobalSettingSequenceRequest("DDU", List.of(UUID.randomUUID().toString()));
        when(globalSettingService.updateSequence(request)).thenReturn(new MessageResponse("Sequence updated successfully"));

        MessageResponse response = controller.updateSequence(request);

        assertThat(response.getMessage()).contains("Sequence updated");
    }

    @Test
    void getAuditHistory_shouldDelegateToService() {
        AuditQueryRequest request = new AuditQueryRequest();
        when(globalSettingService.getAuditHistory(request)).thenReturn(Page.empty());

        assertThat(controller.getAuditHistory(request).getContent()).isEmpty();
    }
}
