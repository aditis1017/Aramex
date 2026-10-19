package com.customcharge.global_settings.rules.service;

import com.customcharge.persistencelayer.dto.GlobalSettingDto;
import com.customcharge.persistencelayer.audit.model.AuditQueryRequest;
import com.customcharge.persistencelayer.search.SearchCriteria;
import com.customcharge.persistencelayer.service.AbstractService;
import com.customcharge.global_settings.rules.dto.GlobalSettingAuditHistoryCommitDto;
import com.customcharge.global_settings.rules.dto.MessageResponse;
import com.customcharge.global_settings.rules.dto.UpdateGlobalSettingSequenceRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface GlobalSettingService extends AbstractService<GlobalSettingDto> {
    Page<GlobalSettingAuditHistoryCommitDto> getAuditHistory(AuditQueryRequest request);
    long countAuditExportRows(AuditQueryRequest request);

    MessageResponse updateStatus(UUID id, boolean status);

    List<GlobalSettingDto> findAll(Integer page, Integer size);

    Page<GlobalSettingDto> getDataByCriteria(List<SearchCriteria> criteriaList, Integer page, Integer size);

    MessageResponse updateSequence(UpdateGlobalSettingSequenceRequest request);
}
