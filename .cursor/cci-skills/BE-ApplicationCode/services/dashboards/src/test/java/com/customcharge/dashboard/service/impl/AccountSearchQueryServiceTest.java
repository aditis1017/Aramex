package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.dto.AccountSearchPageDTO;
import com.customcharge.dashboard.dto.AccountSearchRequestDTO;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountSearchQueryServiceTest {

    @Mock
    private APAccountRepository apAccountRepository;
    @Mock
    private ARAccountRepository arAccountRepository;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private CountryEntityLookupService countryEntityLookupService;

    @InjectMocks
    private AccountSearchQueryService accountSearchQueryService;

    @Test
    void search_resolvesEntityIdFromMasterLookupOncePerRequest() {
        ARAccount master = new ARAccount();
        master.setAccountNumber("8001");
        master.setAccountName("AR Account");
        master.setEntityCode("RUH");
        master.setStatus("ACTIVE");
        master.setCreatedBy("tester");
        master.setCreatedDate(Instant.parse("2026-01-01T00:00:00Z"));

        when(countryEntityLookupService.listCountriesWithActiveEntities()).thenReturn(List.of(
                new CountryWithEntitiesDropdownDto(
                        1L, "SA", "Saudi Arabia", List.of(new EntityDropdownOption(12L, "RUH", "Riyadh")))));
        when(arAccountRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(master)));

        AccountSearchRequestDTO request = new AccountSearchRequestDTO();
        request.setAccountNature(AccountNature.AR);
        request.setPage(0);
        request.setPageSize(10);

        AccountSearchPageDTO result = accountSearchQueryService.search(request);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getEntityId()).isEqualTo(12L);
        verify(countryEntityLookupService).listCountriesWithActiveEntities();
    }

    @Test
    void search_apAccounts_returnsMappedResultsWithEntityId() {
        APAccount master = new APAccount();
        master.setAccountNumber("AP01");
        master.setAccountName("AP Account");
        master.setEntityCode("RUH");
        master.setStatus("ACTIVE");
        master.setCreatedBy("tester");
        master.setCreatedTime(Instant.parse("2026-01-01T00:00:00Z"));
        master.setUpdatedTime(Instant.parse("2026-01-02T00:00:00Z"));

        when(countryEntityLookupService.listCountriesWithActiveEntities()).thenReturn(List.of(
                new CountryWithEntitiesDropdownDto(
                        1L, "SA", "Saudi Arabia", List.of(new EntityDropdownOption(12L, "RUH", "Riyadh")))));
        when(apAccountRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(master)));

        AccountSearchRequestDTO request = new AccountSearchRequestDTO();
        request.setAccountNature(AccountNature.AP);
        request.setPage(0);
        request.setPageSize(10);

        AccountSearchPageDTO result = accountSearchQueryService.search(request);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getEntityId()).isEqualTo(12L);
        assertThat(result.getContent().getFirst().getUpdatedDate()).isNotNull();
        verify(apAccountRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void search_nullAccountNature_throwsBadRequest() {
        AccountSearchRequestDTO request = new AccountSearchRequestDTO();

        assertThatThrownBy(() -> accountSearchQueryService.search(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void search_withEntityIdFilter_resolvesEntityCode() {
        Entity entity = new Entity();
        entity.setCode("DXB");
        when(entityRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(countryEntityLookupService.listCountriesWithActiveEntities()).thenReturn(List.of());
        when(arAccountRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        AccountSearchRequestDTO request = new AccountSearchRequestDTO();
        request.setAccountNature(AccountNature.AR);
        request.setEntityId(5L);
        request.setPage(0);
        request.setPageSize(10);

        accountSearchQueryService.search(request);

        verify(entityRepository).findById(5L);
        verify(arAccountRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void search_skipsInvalidEntityLookupEntries() {
        ARAccount master = new ARAccount();
        master.setAccountNumber("8001");
        master.setAccountName("AR Account");
        master.setEntityCode("RUH");
        master.setStatus("ACTIVE");
        master.setCreatedBy("tester");
        master.setCreatedDate(null);

        when(countryEntityLookupService.listCountriesWithActiveEntities()).thenReturn(List.of(
                new CountryWithEntitiesDropdownDto(1L, "SA", "Saudi Arabia", null),
                new CountryWithEntitiesDropdownDto(
                        2L, "AE", "UAE",
                        List.of(
                                new EntityDropdownOption(null, "DXB", "Dubai"),
                                new EntityDropdownOption(12L, " ", "Blank"),
                                new EntityDropdownOption(13L, "RUH", "Riyadh")))));
        when(arAccountRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(master)));

        AccountSearchRequestDTO request = new AccountSearchRequestDTO();
        request.setAccountNature(AccountNature.AR);
        request.setPage(0);
        request.setPageSize(10);

        AccountSearchPageDTO result = accountSearchQueryService.search(request);

        assertThat(result.getContent().getFirst().getEntityId()).isEqualTo(13L);
        assertThat(result.getContent().getFirst().getCreatedDate()).isNull();
    }
}
