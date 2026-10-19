package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.constants.ShipmentAuditConstants;
import com.customcharge.dashboard.dto.ShipmentAuditHistoryCommitDto;
import com.customcharge.dashboard.dto.ShipmentAuditHistoryRequestDto;
import com.customcharge.dashboard.dto.AccountAuditStateChangeDto;
import com.customcharge.dashboard.service.ShipmentAuditService;
import com.customcharge.persistencelayer.audit.model.AuditCommitBlock;
import com.customcharge.persistencelayer.audit.model.AuditQueryRequest;
import com.customcharge.persistencelayer.audit.model.AuditStateChangeEntry;
import com.customcharge.persistencelayer.audit.service.PersistenceAuditQueryEngine;
import com.customcharge.persistencelayer.config.javers.AuditCommitProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShipmentAuditServiceImpl implements ShipmentAuditService {

    private final PersistenceAuditQueryEngine auditQueryEngine;

    @Override
    public Page<ShipmentAuditHistoryCommitDto> getAuditHistory(ShipmentAuditHistoryRequestDto request) {
        validateRequest(request);

        AuditQueryRequest queryRequest = new AuditQueryRequest();
        queryRequest.setGroupName(AuditCommitProperties.GROUP_SHIPMENT_CHARGE);
        queryRequest.setFromDate(request.getFromDate());
        queryRequest.setToDate(request.getToDate());
        queryRequest.setPage(request.getPage());
        queryRequest.setSize(request.getSize());
        queryRequest.getCommitPropertyFilters().put(
                AuditCommitProperties.HAWB_NUMBER,
                request.getHawbNumber().trim());

        if (request.getManagedType() != null && !request.getManagedType().isBlank()) {
            queryRequest.setManagedType(Optional.of(request.getManagedType().trim()));
        }

        Page<AuditCommitBlock> page = auditQueryEngine.getAuditHistory(queryRequest);
        List<ShipmentAuditHistoryCommitDto> content =
                page.getContent().stream().map(this::toAuditHistoryCommitDto).toList();
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    private static void validateRequest(ShipmentAuditHistoryRequestDto request) {
        if (request.getHawbNumber() == null || request.getHawbNumber().isBlank()) {
            throw new IllegalArgumentException(ShipmentAuditConstants.HAWB_NUMBER_REQUIRED);
        }
    }

    private ShipmentAuditHistoryCommitDto toAuditHistoryCommitDto(AuditCommitBlock block) {
        ShipmentAuditHistoryCommitDto dto = new ShipmentAuditHistoryCommitDto();
        dto.setCommitId(block.getCommitId());
        dto.setManagedType(block.getManagedType());
        dto.setAuthor(block.getAuthor());
        dto.setCommitDate(block.getCommitDate());
        List<AuditStateChangeEntry> entries = block.getStateChangeData();
        if (entries != null && !entries.isEmpty()) {
            dto.setStateChangeData(toAuditStateChangeDto(entries.getFirst()));
        }
        return dto;
    }

    private AccountAuditStateChangeDto toAuditStateChangeDto(AuditStateChangeEntry entry) {
        AccountAuditStateChangeDto dto = new AccountAuditStateChangeDto();
        dto.setPreState(stripAuditMetadata(entry.getPreState()));
        dto.setType(entry.getType());
        dto.setCurrentState(stripAuditMetadata(entry.getCurrentState()));
        dto.setChangeParameter(stripAuditMetadata(entry.getChangeParameter()));
        return dto;
    }

    private JsonNode stripAuditMetadata(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) {
            return node;
        }
        ObjectNode copy = (ObjectNode) node.deepCopy();
        copy.remove(ShipmentAuditConstants.AUDIT_METADATA_FIELDS_TO_STRIP);
        return copy;
    }
}
