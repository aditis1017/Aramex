package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.config.DashboardEntityChargeProperties;
import com.customcharge.dashboard.constants.EntityChargeFilterConstants;
import com.customcharge.dashboard.constants.EntityChargeLogMessages;
import com.customcharge.dashboard.constants.EntityChargeMessageConstants;
import com.customcharge.dashboard.dto.EntityChargeGridRowDTO;
import com.customcharge.dashboard.mapper.EntityChargeDashboardViewMapper;
import com.customcharge.dashboard.search.EntityChargeChargeSpecificationFactory;
import com.customcharge.dashboard.search.EntityChargeCriteriaNormalizer;
import com.customcharge.dashboard.search.EntityChargeEntityFilterResolver;
import com.customcharge.dashboard.search.EntityChargeNormalizedFilters;
import com.customcharge.dashboard.search.EntityChargeChargeFieldPaths;
import com.customcharge.dashboard.search.EntityChargeSearchQuerySupport;
import com.customcharge.dashboard.search.EntityChargeViewSearchSpecificationFactory;
import com.customcharge.dashboard.service.EntityChargeSearchService;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.EntityChargeDashboardView;
import com.customcharge.persistencelayer.exception.AbstractApiException;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.InternalServerException;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.EntityChargeDashboardViewRepository;
import com.customcharge.persistencelayer.search.SearchCriteria;
import com.customcharge.persistencelayer.search.SpecBuilderUtil;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EntityChargeSearchServiceImpl implements EntityChargeSearchService {

    private static final Sort VIEW_DEFAULT_SORT = Sort.by(
            Sort.Direction.DESC, EntityChargeFilterConstants.VIEW_SORT_FIELD_CREATED_TIME);

    private static final Sort CHARGE_DEFAULT_SORT = Sort.by(
            Sort.Direction.DESC, "createdTime");

    private final EntityChargeDashboardViewRepository entityChargeDashboardViewRepository;
    private final ChargeRepository chargeRepository;
    private final EntityChargeCriteriaNormalizer criteriaNormalizer;
    private final EntityChargeDashboardViewMapper dashboardViewMapper;
    private final EntityChargeEntityFilterResolver entityFilterResolver;
    private final EntityManager entityManager;
    private final DashboardEntityChargeProperties dashboardEntityChargeProperties;

    @Override
    public Page<EntityChargeGridRowDTO> search(List<SearchCriteria> filters, Integer page, Integer size) {
        int resolvedPage = validatePagination(page, size);
        try {
            EntityChargeNormalizedFilters normalized = criteriaNormalizer.normalizeForView(filters);

            if (size == null) {
                Specification<EntityChargeDashboardView> exportSpec = buildViewFilterSpec(normalized);
                List<EntityChargeDashboardView> rows =
                        entityChargeDashboardViewRepository.findAll(exportSpec, VIEW_DEFAULT_SORT);
                return new PageImpl<>(rows.stream().map(dashboardViewMapper::toGridRow).toList());
            }

            Pageable pageable = PageRequest.of(resolvedPage, size, VIEW_DEFAULT_SORT);
            EntityChargeEntityFilterResolver.EntityFilterResolution chargeScope =
                    entityFilterResolver.resolveScope(normalized.criteria());
            if (canUseChargeFirstPagination(normalized, chargeScope)) {
                if (chargeScope.entityFilterApplied() && chargeScope.entityIds().isEmpty()) {
                    return new PageImpl<>(List.of(), pageable, 0);
                }
                return searchViaChargeFirstPagination(chargeScope, pageable);
            }

            Specification<EntityChargeDashboardView> filterSpec = buildViewFilterSpec(normalized);
            Page<EntityChargeDashboardView> viewPage = findViewPage(filterSpec, pageable);
            return viewPage.map(dashboardViewMapper::toGridRow);
        } catch (AbstractApiException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            log.warn(EntityChargeLogMessages.INVALID_SEARCH_REQUEST, ex.getMessage());
            throw new BadRequestException(
                    ex.getMessage() != null
                            ? ex.getMessage()
                            : EntityChargeMessageConstants.INVALID_SEARCH_CRITERIA_OR_PAGINATION);
        } catch (RuntimeException ex) {
            log.error(EntityChargeLogMessages.SEARCH_FAILED, ex);
            throw new InternalServerException(EntityChargeMessageConstants.UNABLE_TO_LOAD_ENTITY_CHARGES);
        }
    }

    private Page<EntityChargeDashboardView> findViewPage(
            Specification<EntityChargeDashboardView> filterSpec, Pageable pageable) {
        int queryTimeoutMs = dashboardEntityChargeProperties.getQueryTimeoutMs();
        if (queryTimeoutMs > 0) {
            return EntityChargeSearchQuerySupport.findAll(
                    entityManager,
                    EntityChargeDashboardView.class,
                    filterSpec,
                    pageable,
                    queryTimeoutMs);
        }
        return entityChargeDashboardViewRepository.findAll(filterSpec, pageable);
    }

    private Page<EntityChargeGridRowDTO> searchViaChargeFirstPagination(
            EntityChargeEntityFilterResolver.EntityFilterResolution entityFilter,
            Pageable pageable) {
        Specification<Charge> chargeSpec = EntityChargeChargeSpecificationFactory.build(
                entityFilter.entityIds(),
                entityFilter.entityFilterApplied(),
                entityFilter.criteriaWithoutEntity(),
                chargeRepository);

        PageRequest chargePageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                CHARGE_DEFAULT_SORT);
        Page<Charge> chargePage = chargeRepository.findAll(chargeSpec, chargePageable);
        List<UUID> chargeIds = chargePage.getContent().stream()
                .map(Charge::getId)
                .toList();

        long total = chargePage.getTotalElements();
        if (chargeIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        List<EntityChargeDashboardView> hydratedRows =
                entityChargeDashboardViewRepository.findAllById(chargeIds);
        Map<UUID, EntityChargeDashboardView> rowsByChargeId = hydratedRows.stream()
                .collect(Collectors.toMap(
                        EntityChargeDashboardView::getChargeId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));

        List<EntityChargeGridRowDTO> content = new ArrayList<>(chargeIds.size());
        for (UUID chargeId : chargeIds) {
            EntityChargeDashboardView row = rowsByChargeId.get(chargeId);
            if (row != null) {
                content.add(dashboardViewMapper.toGridRow(row));
            }
        }
        return new PageImpl<>(content, pageable, total);
    }

    private static boolean canUseChargeFirstPagination(
            EntityChargeNormalizedFilters normalized,
            EntityChargeEntityFilterResolver.EntityFilterResolution chargeScope) {
        if (!normalized.accountNumbers().isEmpty()
                || !normalized.accountTypes().isEmpty()
                || !normalized.boeNumbers().isEmpty()) {
            return false;
        }
        if (chargeScope.entityFilterApplied()) {
            return true;
        }
        return EntityChargeChargeFieldPaths.allNonEntityCriteriaSupportedOnCharge(normalized.criteria());
    }

    @SuppressWarnings("unchecked")
    private Specification<EntityChargeDashboardView> buildViewFilterSpec(EntityChargeNormalizedFilters normalized) {
        SpecBuilderUtil specBuilder = SpecBuilderUtil.getInstance()
                .addCriteria(normalized.criteria())
                .from(entityChargeDashboardViewRepository);

        Specification<EntityChargeDashboardView> filterSpec =
                (Specification<EntityChargeDashboardView>) (Object) specBuilder.getSpecification();
        if (!normalized.accountNumbers().isEmpty()) {
            filterSpec = filterSpec.and(
                    EntityChargeViewSearchSpecificationFactory.accountNumberMatchesAnyParty(
                            normalized.accountNumbers()));
        }
        if (!normalized.accountTypes().isEmpty()) {
            filterSpec = filterSpec.and(
                    EntityChargeViewSearchSpecificationFactory.accountTypeIn(normalized.accountTypes()));
        }
        if (!normalized.boeNumbers().isEmpty()) {
            filterSpec = filterSpec.and(
                    EntityChargeViewSearchSpecificationFactory.boeNumberMatchesAnyLineItem(
                            normalized.boeNumbers()));
        }

        return filterSpec;
    }

    private static int validatePagination(Integer page, Integer size) {
        if (size == null) {
            return 0;
        }
        if (size < 1) {
            throw new BadRequestException(EntityChargeMessageConstants.SIZE_MUST_BE_AT_LEAST_ONE);
        }
        int resolvedPage = page == null ? 0 : page;
        if (resolvedPage < 0) {
            throw new BadRequestException(EntityChargeMessageConstants.PAGE_MUST_BE_ZERO_OR_GREATER);
        }
        return resolvedPage;
    }
}
