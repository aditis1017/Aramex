package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.Specification.ApAccountSpecification;
import com.customcharge.dashboard.Specification.ArAccountSpecification;
import com.customcharge.dashboard.constants.AccountConstants;
import com.customcharge.dashboard.constants.AccountMessageConstants;
import com.customcharge.dashboard.dto.AccountSearchPageDTO;
import com.customcharge.dashboard.dto.AccountSearchRequestDTO;
import com.customcharge.dashboard.dto.AccountSearchResponseDTO;
import com.customcharge.dashboard.search.AccountSearchPagination;
import com.customcharge.persistencelayer.cache.CountryEntityLookupService;
import com.customcharge.persistencelayer.dto.CountryWithEntitiesDropdownDto;
import com.customcharge.persistencelayer.dto.EntityDropdownOption;
import com.customcharge.persistencelayer.entities.APAccount;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.repository.APAccountRepository;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountSearchQueryService {

    private final APAccountRepository apAccountRepository;
    private final ARAccountRepository arAccountRepository;
    private final EntityRepository entityRepository;
    private final CountryEntityLookupService countryEntityLookupService;

    public AccountSearchPageDTO search(AccountSearchRequestDTO request) {
        if (request.getAccountNature() == null) {
            throw new BadRequestException(AccountMessageConstants.ACCOUNT_NATURE_REQUIRED);
        }
        String resolvedEntityCode = resolveEntityCodeForFilter(request.getEntityId());
        int page = AccountSearchPagination.resolvePage(request.getPage());
        int pageSize = AccountSearchPagination.resolvePageSize(request.getPageSize());
        Map<String, Long> entityIdByCode = activeEntityIdByCode();

        Page<AccountSearchResponseDTO> result;
        if (AccountNature.AP.equals(request.getAccountNature())) {
            PageRequest pageable = PageRequest.of(
                    page,
                    pageSize,
                    Sort.by(Sort.Direction.DESC, "updatedTime")
                            .and(Sort.by(Sort.Direction.DESC, "createdTime")));
            result = apAccountRepository
                    .findAll(ApAccountSpecification.search(request, resolvedEntityCode), pageable)
                    .map(master -> mapApToSearchResponse(master, entityIdByCode));
        } else if (AccountNature.AR.equals(request.getAccountNature())) {
            PageRequest pageable = PageRequest.of(
                    page,
                    pageSize,
                    Sort.by(Sort.Direction.DESC, "createdDate"));
            result = arAccountRepository
                    .findAll(ArAccountSpecification.search(request, resolvedEntityCode), pageable)
                    .map(master -> mapArToSearchResponse(master, entityIdByCode));
        } else {
            throw new BadRequestException(AccountMessageConstants.ACCOUNT_SEARCH_NATURE_MUST_BE_AP_OR_AR);
        }
        return AccountSearchPageDTO.from(result);
    }

    private String resolveEntityCodeForFilter(Long entityId) {
        if (Objects.isNull(entityId)) {
            return null;
        }
        return entityRepository.findById(entityId)
                .map(Entity::getCode)
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .orElse(null);
    }

    /**
     * One lookup per search request from master country/entity data (Redis-backed when available).
     */
    private Map<String, Long> activeEntityIdByCode() {
        Map<String, Long> byCode = new LinkedHashMap<>();
        for (CountryWithEntitiesDropdownDto country : countryEntityLookupService.listCountriesWithActiveEntities()) {
            if (country.entities() == null) {
                continue;
            }
            for (EntityDropdownOption entity : country.entities()) {
                if (entity.code() == null || entity.code().isBlank() || entity.id() == null) {
                    continue;
                }
                byCode.put(entity.code().trim().toLowerCase(Locale.ROOT), entity.id());
            }
        }
        return byCode;
    }

    private static Long resolveEntityId(Map<String, Long> entityIdByCode, String entityCode) {
        if (entityCode == null || entityCode.isBlank()) {
            return null;
        }
        return entityIdByCode.get(entityCode.trim().toLowerCase(Locale.ROOT));
    }

    private AccountSearchResponseDTO mapApToSearchResponse(APAccount master, Map<String, Long> entityIdByCode) {
        Long entityId = resolveEntityId(entityIdByCode, master.getEntityCode());
        DateTimeFormatter formatter = AccountConstants.SEARCH_RESULT_DATE_FORMAT
                .withZone(ZoneId.systemDefault());
        String createdDate =
                master.getCreatedTime() == null ? null : formatter.format(master.getCreatedTime());
        String updatedDate =
                master.getUpdatedTime() == null ? null : formatter.format(master.getUpdatedTime());
        return new AccountSearchResponseDTO(
                master.getAccountNumber(),
                master.getAccountName(),
                master.getEntityCode(),
                master.getCreatedBy(),
                entityId,
                master.getStatus(),
                createdDate,
                updatedDate);
    }

    private AccountSearchResponseDTO mapArToSearchResponse(ARAccount master, Map<String, Long> entityIdByCode) {
        Long entityId = resolveEntityId(entityIdByCode, master.getEntityCode());
        DateTimeFormatter formatter = AccountConstants.SEARCH_RESULT_DATE_FORMAT
                .withZone(ZoneId.systemDefault());
        String createdDate =
                master.getCreatedDate() == null ? null : formatter.format(master.getCreatedDate());
        return new AccountSearchResponseDTO(
                master.getAccountNumber(),
                master.getAccountName(),
                master.getEntityCode(),
                master.getCreatedBy(),
                entityId,
                master.getStatus(),
                createdDate,
                null);
    }
}
