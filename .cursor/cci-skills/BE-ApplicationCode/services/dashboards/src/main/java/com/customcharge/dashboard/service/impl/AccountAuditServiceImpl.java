package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.cache.DashboardCacheNames;
import com.customcharge.dashboard.cache.DashboardLocalCacheConfiguration;
import com.customcharge.dashboard.constants.AccountAuditMessageConstants;
import com.customcharge.dashboard.dto.AccountAuditHistoryCommitDto;
import com.customcharge.dashboard.dto.AccountAuditHistoryRequestDto;
import com.customcharge.dashboard.dto.AccountAuditStateChangeDto;
import com.customcharge.dashboard.service.AccountAuditService;
import com.customcharge.persistencelayer.audit.model.AuditCommitBlock;
import com.customcharge.persistencelayer.audit.model.AuditQueryRequest;
import com.customcharge.persistencelayer.audit.model.AuditStateChangeEntry;
import com.customcharge.persistencelayer.audit.service.PersistenceAuditQueryEngine;
import com.customcharge.persistencelayer.config.javers.AuditCommitProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountAuditServiceImpl implements AccountAuditService {

    private final PersistenceAuditQueryEngine auditQueryEngine;
    private final AccountAuditSnapshotEnricher snapshotEnricher;
    private final ObjectProvider<AccountAuditServiceImpl> selfProvider;

    @Override
    public Page<AccountAuditHistoryCommitDto> getAuditHistory(AccountAuditHistoryRequestDto request) {
        return selfProvider.getObject().getAuditHistoryCached(request);
    }

    /**
     * Local in-memory cache only — avoids Redis {@code JsonNode} / polymorphic deserialization issues.
     * Repeated pagination for the same account/entity/date window hits this on the same pod.
     */
    @Cacheable(
            cacheManager = DashboardLocalCacheConfiguration.LOCAL_CACHE_MANAGER,
            cacheNames = DashboardCacheNames.ACCOUNT_AUDIT_HISTORY,
            key = "{#request.accountId, #request.entityCode?.trim()?.toUpperCase(), "
                    + "#request.fromDate, #request.toDate, #request.managedType?.trim(), "
                    + "#request.page, #request.size}")
    public Page<AccountAuditHistoryCommitDto> getAuditHistoryCached(AccountAuditHistoryRequestDto request) {
        return loadAuditHistory(request);
    }

    private Page<AccountAuditHistoryCommitDto> loadAuditHistory(AccountAuditHistoryRequestDto request) {
        validateRequest(request);

        AuditQueryRequest queryRequest = new AuditQueryRequest();
        queryRequest.setGroupName(AuditCommitProperties.GROUP_ACCOUNT_AR_AP);
        queryRequest.setFromDate(request.getFromDate());
        queryRequest.setToDate(request.getToDate());
        queryRequest.setPage(request.getPage());
        queryRequest.setSize(request.getSize());
        if (request.getAccountId() != null) {
            queryRequest.getCommitPropertyFilters().put(
                    AuditCommitProperties.ACCOUNT_ID,
                    request.getAccountId().toString());
        }

        if (request.getEntityCode() != null && !request.getEntityCode().isBlank()) {
            queryRequest.getCommitPropertyFilters().put(
                    AuditCommitProperties.ENTITY_CODE,
                    request.getEntityCode().trim().toUpperCase(Locale.ROOT));
        }

        if (request.getManagedType() != null && !request.getManagedType().isBlank()) {
            queryRequest.setManagedType(Optional.of(request.getManagedType().trim()));
        }

        Page<AuditCommitBlock> page = auditQueryEngine.getAuditHistory(queryRequest);
        List<JsonNode> snapshotNodes = collectSnapshotNodes(page.getContent());
        snapshotEnricher.prepareBatch(snapshotNodes);
        try {
            List<AccountAuditHistoryCommitDto> content = page.getContent().stream()
                    .flatMap(block -> toAuditHistoryCommitDtos(block).stream())
                    .toList();
            return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
        } finally {
            snapshotEnricher.clearBatch();
        }
    }

    private void validateRequest(AccountAuditHistoryRequestDto request) {
        boolean hasAccountId = request.getAccountId() != null;
        boolean hasEntityCode = request.getEntityCode() != null && !request.getEntityCode().isBlank();
        if (!hasAccountId && !hasEntityCode) {
            throw new IllegalArgumentException(AccountAuditMessageConstants.ACCOUNT_ID_OR_ENTITY_CODE_REQUIRED);
        }
    }

    private List<JsonNode> collectSnapshotNodes(List<AuditCommitBlock> blocks) {
        List<JsonNode> nodes = new ArrayList<>();
        for (AuditCommitBlock block : blocks) {
            List<AuditStateChangeEntry> entries = block.getStateChangeData();
            if (entries == null) {
                continue;
            }
            for (AuditStateChangeEntry entry : entries) {
                addIfPresent(nodes, entry.getPreState());
                addIfPresent(nodes, entry.getCurrentState());
                addIfPresent(nodes, entry.getChangeParameter());
            }
        }
        return nodes;
    }

    private static void addIfPresent(List<JsonNode> nodes, JsonNode node) {
        if (node != null && !node.isNull()) {
            nodes.add(node);
        }
    }

    private List<AccountAuditHistoryCommitDto> toAuditHistoryCommitDtos(AuditCommitBlock block) {
        List<AuditStateChangeEntry> entries = block.getStateChangeData();
        if (entries == null || entries.isEmpty()) {
            return List.of(toAuditHistoryCommitDto(block, null));
        }
        return entries.stream()
                .map(entry -> toAuditHistoryCommitDto(block, entry))
                .toList();
    }

    private AccountAuditHistoryCommitDto toAuditHistoryCommitDto(
            AuditCommitBlock block, AuditStateChangeEntry entry) {
        AccountAuditHistoryCommitDto dto = new AccountAuditHistoryCommitDto();
        dto.setCommitId(block.getCommitId());
        dto.setManagedType(block.getManagedType());
        dto.setAuthor(block.getAuthor());
        dto.setCommitDate(block.getCommitDate());
        if (entry != null) {
            dto.setStateChangeData(toAuditStateChangeDto(entry));
        }
        return dto;
    }

    private AccountAuditStateChangeDto toAuditStateChangeDto(AuditStateChangeEntry entry) {
        AccountAuditStateChangeDto dto = new AccountAuditStateChangeDto();
        dto.setPreState(sanitizeSnapshot(entry.getPreState()));
        dto.setType(entry.getType());
        dto.setCurrentState(sanitizeSnapshot(entry.getCurrentState()));
        dto.setChangeParameter(sanitizeSnapshot(entry.getChangeParameter()));
        return dto;
    }

    private JsonNode sanitizeSnapshot(JsonNode snapshot) {
        return AccountAuditSnapshotSanitizer.sanitizeForApi(snapshotEnricher.enrich(snapshot));
    }
}

