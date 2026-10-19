package com.customcharge.dashboard.service;

import com.customcharge.dashboard.dto.AccountAuditHistoryCommitDto;
import com.customcharge.dashboard.dto.AccountAuditHistoryRequestDto;
import org.springframework.data.domain.Page;

public interface AccountAuditService {
    Page<AccountAuditHistoryCommitDto> getAuditHistory(AccountAuditHistoryRequestDto request);
}
