package com.customcharge.dashboard.controller;

import com.customcharge.dashboard.dto.AccountAuditHistoryCommitDto;
import com.customcharge.dashboard.dto.AccountAuditHistoryRequestDto;
import com.customcharge.dashboard.service.AccountAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountAuditController {

    private final AccountAuditService accountAuditService;

    @PostMapping("/audit/history")
    @PreAuthorize("hasAnyAuthority('AR_MANAGEMENT_VIEW', 'AP_MANAGEMENT_VIEW', 'ENTITY_SETTINGS_VIEW')")
    public Page<AccountAuditHistoryCommitDto> getAuditHistory(
            @RequestBody AccountAuditHistoryRequestDto request) {
        return accountAuditService.getAuditHistory(request);
    }
}
