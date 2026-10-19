package com.customcharge.dashboard.controller;

import com.customcharge.dashboard.dto.AccountAuditHistoryCommitDto;
import com.customcharge.dashboard.dto.AccountAuditHistoryRequestDto;
import com.customcharge.dashboard.service.AccountAuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountAuditControllerTest {

    @Mock
    private AccountAuditService accountAuditService;

    @InjectMocks
    private AccountAuditController controller;

    @Test
    void getAuditHistory_shouldDelegateToService() {
        AccountAuditHistoryRequestDto request = new AccountAuditHistoryRequestDto();
        PageImpl<AccountAuditHistoryCommitDto> page = new PageImpl<>(List.of(new AccountAuditHistoryCommitDto()));
        when(accountAuditService.getAuditHistory(request)).thenReturn(page);

        assertThat(controller.getAuditHistory(request).getTotalElements()).isEqualTo(1);
    }
}
