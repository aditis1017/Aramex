package com.customcharge.dashboard.service;

import com.customcharge.dashboard.dto.*;
import com.customcharge.persistencelayer.dto.ActiveCurrencyDto;
import com.customcharge.persistencelayer.dto.CountryWithEntitiesDropdownDto;
import com.customcharge.persistencelayer.dto.response.MessageResponse;
import com.customcharge.persistencelayer.enums.AccountNature;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AccountService {
    AccountDropdownResponse getAccountDropdowns();

    /**
     * Countries with nested active entities; prefers master-data Redis keys when populated.
     */
    List<CountryWithEntitiesDropdownDto> getCountryWithEntitiesDropdown();

    /**
     * Active currencies from the {@code currency} table. When {@code country} is provided, returns the
     * active currency for that country; otherwise returns all active currencies.
     */
    List<ActiveCurrencyDto> getActiveCurrencies(String country);
    Page<AccountSearchResponseDTO> search(AccountSearchRequestDTO request);

    List<ApAccountSummaryDTO> getApAccountsByEntityCode(String entityCode);

    AccountDetailsResponseDTO getAccountDetails(
            String accountNumber, AccountNature accountNature, String entity, String chargeType);
    AccountDetailsResponseDTO updateAccount(AccountUpdateRequestDTO request);

    /** Creates an AP account and writes Redis cache when enabled (same shape as other account cache entries). */
    MessageResponse addAccount(AccountAddRequestDTO request);

    /** Inserts sample AP/AR/entity-settings accounts into the database and refreshes Redis cache when enabled. */
    void loadSampleAccountsToRedis();

    /**
     * Rebuilds Redis for one account when filters are provided; syncs all accounts when every parameter is omitted;
     * syncs all AR or AP masters when only {@code accountNature} is {@code AR} or {@code AP}.
     */
    MessageResponse syncAccountToRedis(
            String accountNumber, AccountNature accountNature, String entity, String chargeType);

    /** Active master-category codes for dropdowns (by category code). */
    List<String> masterCodes(String categoryCode);
}
