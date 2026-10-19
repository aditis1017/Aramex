package com.customcharge.dashboard.controller;

import com.customcharge.dashboard.dto.*;
import com.customcharge.dashboard.dto.AccountRedisSyncJobStatusResponse;
import com.customcharge.dashboard.service.impl.AccountRedisChargeDataAsyncSyncService;
import com.customcharge.dashboard.service.impl.AccountRedisChargeDataSyncRunner;
import com.customcharge.dashboard.service.AccountService;
import com.customcharge.persistencelayer.dto.CountryWithEntitiesDropdownDto;
import com.customcharge.persistencelayer.dto.ActiveCurrencyDto;
import com.customcharge.persistencelayer.dto.response.MessageResponse;
import com.customcharge.persistencelayer.enums.AccountNature;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountRedisChargeDataAsyncSyncService accountRedisChargeDataAsyncSyncService;


    @GetMapping("/account")
    @PreAuthorize("hasAnyAuthority('AR_MANAGEMENT_VIEW', 'AP_MANAGEMENT_VIEW')")
    public AccountDropdownResponse getAccountDropdowns() {
        return accountService.getAccountDropdowns();
    }

    /**
     * Active countries with nested active entities for filters and forms. Data is read from the same Redis
     * master keys as category refresh when available; otherwise from the database.
     */
    @GetMapping("/countries-with-entities")
    @PreAuthorize("hasAnyAuthority('AR_MANAGEMENT_VIEW', 'AP_MANAGEMENT_VIEW')")
    public List<CountryWithEntitiesDropdownDto> getCountryWithEntitiesDropdown() {
        return accountService.getCountryWithEntitiesDropdown();
    }

    /**
     * Active currencies from the {@code currency} table. Pass {@code country} to get the active currency
     * for that country; omit it to list all active currencies.
     */
    @GetMapping("/currencies")
    @PreAuthorize("hasAnyAuthority('AR_MANAGEMENT_VIEW', 'AP_MANAGEMENT_VIEW')")
    public List<ActiveCurrencyDto> getActiveCurrencies(
            @RequestParam(required = false) String country) {
        return accountService.getActiveCurrencies(country);
    }

    @GetMapping("/defaultAccountNumber")
    @PreAuthorize("hasAnyAuthority('AR_MANAGEMENT_VIEW', 'AP_MANAGEMENT_VIEW')")
    public List<ApAccountSummaryDTO> getApAccountsByEntity(@RequestParam String entityCode) {
        return accountService.getApAccountsByEntityCode(entityCode);
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyAuthority('AR_MANAGEMENT_VIEW', 'AP_MANAGEMENT_VIEW', 'CUSTOMS_VIEW', "
            + "'DOCUMENTS_LIST_VIEW', 'REPORTS_VIEW', 'GLOBAL_SETTINGS_VIEW')")
    public Page<AccountSearchResponseDTO> search(
            @RequestBody AccountSearchRequestDTO request) {
        return accountService.search(request);
    }

    /**
     * Inserts sample accounts into the database and writes their details to Redis when enabled.
     */
    @PostMapping("/redis/load-sample")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_EDIT')")
    public ResponseEntity<MessageResponse> loadRedisSample() {
        accountService.loadSampleAccountsToRedis();
        return ResponseEntity.ok(new MessageResponse(
                "Sample accounts loaded (DB + Redis cache when enabled) with master service codes per entity and charge type"));
    }

    /**
     * Rebuilds account Redis cache. Bulk sync (no query parameters) runs asynchronously to avoid gateway timeouts.
     * Poll {@code GET /redis/sync-charge-data/{jobId}} for completion status.
     */
    @PostMapping("/redis/sync")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_EDIT')")
    public ResponseEntity<?> syncAccountToRedis(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) AccountNature accountNature,
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) String chargeType) {
        if (AccountRedisChargeDataSyncRunner.isBulkSyncRequest(
                accountNumber, accountNature, entity, chargeType)) {
            return ResponseEntity.accepted()
                    .body(accountRedisChargeDataAsyncSyncService.startSync(
                            accountNature, entity, accountNumber, chargeType));
        }
        return ResponseEntity.ok(
                accountService.syncAccountToRedis(accountNumber, accountNature, entity, chargeType));
    }

    /**
     * Rebuilds Redis with account, service codes, and rate sheet data. Always runs asynchronously.
     * Poll {@code GET /redis/sync-charge-data/{jobId}} for completion status.
     */
    @PostMapping("/redis/sync-charge-data")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_EDIT')")
    public ResponseEntity<AccountRedisSyncJobStatusResponse> syncChargeDataToRedis(
            @RequestParam(required = false) AccountNature accountNature,
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String chargeType) {
        return ResponseEntity.accepted()
                .body(accountRedisChargeDataAsyncSyncService.startSync(
                        accountNature, entity, accountNumber, chargeType));
    }

    @GetMapping("/redis/sync-charge-data/{jobId}")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_EDIT')")
    public ResponseEntity<AccountRedisSyncJobStatusResponse> getChargeDataSyncStatus(@PathVariable String jobId) {
        AccountRedisSyncJobStatusResponse status = accountRedisChargeDataAsyncSyncService.getStatus(jobId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('AR_MANAGEMENT_VIEW', 'AP_MANAGEMENT_VIEW', 'CUSTOMS_VIEW', "
            + "'DOCUMENTS_LIST_VIEW', 'REPORTS_VIEW', 'GLOBAL_SETTINGS_VIEW')")
    public AccountDetailsResponseDTO getAccount(
            @RequestParam(required = false) String accountNumber,
            @RequestParam AccountNature accountNature,
            @RequestParam String entity,
            @RequestParam(required = false) String chargeType) {

        return accountService.getAccountDetails(accountNumber, accountNature, entity, chargeType);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_EDIT')")
    public AccountDetailsResponseDTO updateAccount(@RequestBody AccountUpdateRequestDTO request) {

        return accountService.updateAccount(request);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_CREATE')")
    public MessageResponse addAccount(@RequestBody AccountAddRequestDTO request) {
        return accountService.addAccount(request);
    }
}
