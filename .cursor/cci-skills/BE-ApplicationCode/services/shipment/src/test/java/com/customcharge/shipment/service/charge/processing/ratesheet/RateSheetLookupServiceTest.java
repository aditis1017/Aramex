package com.customcharge.shipment.service.charge.processing.ratesheet;

import com.customcharge.persistencelayer.accountRedis.AccountRedisChargeDataService;
import com.customcharge.persistencelayer.accountRedis.AccountRedisRateSheetSnapshot;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.shipment.service.charge.PartyArAccountResolver;
import com.customcharge.shipment.service.charge.ServiceChargeAccountResolver;
import com.customcharge.shipment.service.charge.processing.model.ResolvedRateSheet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.customcharge.persistencelayer.entities.RateSheetItemRate;
import com.customcharge.persistencelayer.enums.RateType;

import static com.customcharge.shipment.constants.ShipmentConstants.CATEGORY_TYPE_HV;
import static com.customcharge.shipment.constants.ShipmentConstants.CATEGORY_TYPE_LV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateSheetLookupServiceTest {

    @Mock
    private AccountRedisChargeDataService accountRedisChargeDataService;
    @Mock
    private ServiceChargeAccountResolver serviceChargeAccountResolver;
    @Mock
    private PartyArAccountResolver partyArAccountResolver;

    @InjectMocks
    private RateSheetLookupService service;

    @Test
    void resolveForEntitySettings_shouldReturnEmpty_whenBillingEntityMissing() {
        Shipment shipment = new Shipment();

        assertThat(service.resolveForEntitySettings(shipment, "DTC")).isEmpty();
    }

    @Test
    void resolveForEntitySettings_shouldReturnEmpty_whenInputsBlank() {
        assertThat(service.resolveForEntitySettings(" ", "DTC")).isEmpty();
        assertThat(service.resolveForEntitySettings("RUH", " ")).isEmpty();
    }

    @Test
    void resolveForEntitySettings_shouldReturnEmpty_whenAccountMissing() {
        when(serviceChargeAccountResolver.resolveEntitySettings("RUH", "DTC"))
                .thenReturn(Optional.empty());

        assertThat(service.resolveForEntitySettings("RUH", "DTC")).isEmpty();
    }

    @Test
    void resolveForEntitySettings_shouldReturnResolvedSheet_whenActiveItemsExist() {
        Account account = entitySettingsAccount(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem item = billableItem("DDU");

        when(serviceChargeAccountResolver.resolveEntitySettings("RUH", "DTC"))
                .thenReturn(Optional.of(account));
        stubRateSheetSnapshot(account, rateSheet, List.of(item));

        Shipment shipment = shipment("DDU", "RUH");
        assertThat(service.resolveForEntitySettings(shipment, "DTC")).isPresent();
    }

    @Test
    void resolveForArAccountNumber_shouldPreferArAccount() {
        Shipment shipment = shipment("DDU", "RUH");
        shipment.setConsigneeNumber("AR-1");
        Account account = account(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem item = billableItem("DDU");

        when(serviceChargeAccountResolver.resolveArChargeAccount("AR-1", "RUH", "DTC"))
                .thenReturn(Optional.of(account));
        stubRateSheetSnapshot(account, rateSheet, List.of(item));

        assertThat(service.resolveForArAccountNumber("AR-1", shipment, "DTC")).isPresent();
    }

    @Test
    void resolveForShipmentCharge_shouldUseShipperNumberForDts() {
        Shipment shipment = shipment("DDP", "RUH");
        shipment.setOriginEntity("JED");
        shipment.setConsigneeNumber("CONS-1");
        shipment.setShipperNumber("SHIP-1");
        Account account = account(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem item = billableItem("DDP");

        when(partyArAccountResolver.resolveOrCreate(shipment, "JED", "DTS")).thenReturn(Optional.of(account));
        stubRateSheetSnapshot(account, rateSheet, List.of(item));

        assertThat(service.resolveForShipmentCharge(shipment, "DTS")).isPresent();
        verify(partyArAccountResolver).resolveOrCreate(shipment, "JED", "DTS");
    }

    @Test
    void resolveForShipmentCharge_shouldUseUnifiedResolverForDta_whenConfigured() {
        Shipment shipment = shipment("DDP", "RUH");
        Account arDtaAccount = account(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem item = billableItem("DDP");

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "DTA")).thenReturn(Optional.of(arDtaAccount));
        stubRateSheetSnapshot(arDtaAccount, rateSheet, List.of(item));

        assertThat(service.resolveForShipmentCharge(shipment, "DTA")).isPresent()
                .get()
                .extracting(ResolvedRateSheet::account)
                .isSameAs(arDtaAccount);
        verify(serviceChargeAccountResolver).resolve(shipment, "RUH", "DTA");
    }

    @Test
    void resolveForShipmentCharge_shouldFallbackToEntitySettingsForDta_whenDefaultArHasNoRateSheet() {
        Shipment shipment = shipment("DDP", "RUH");
        Account arDtaAccount = account(UUID.randomUUID());
        Account entitySettingsAccount = entitySettingsAccount(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem item = billableItem("DDP");

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "DTA")).thenReturn(Optional.of(arDtaAccount));
        stubEmptyRateSheetSnapshot(arDtaAccount);
        when(serviceChargeAccountResolver.resolveEntitySettings("RUH", "DTA"))
                .thenReturn(Optional.of(entitySettingsAccount));
        stubRateSheetSnapshot(entitySettingsAccount, rateSheet, List.of(item));

        assertThat(service.resolveForShipmentCharge(shipment, "DTA")).isPresent()
                .get()
                .extracting(ResolvedRateSheet::account)
                .isSameAs(entitySettingsAccount);
    }

    @Test
    void resolveForShipmentCharge_shouldUseUnifiedResolverForDta_whenDefaultArMissing() {
        Shipment shipment = shipment("DDP", "RUH");
        shipment.setConsigneeNumber("CONSIGNEE-IGNORED");
        Account account = account(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem item = billableItem("DDP");

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "DTA")).thenReturn(Optional.of(account));
        stubRateSheetSnapshot(account, rateSheet, List.of(item));

        assertThat(service.resolveForShipmentCharge(shipment, "DTA")).isPresent()
                .get()
                .extracting(ResolvedRateSheet::account)
                .isSameAs(account);
    }

    @Test
    void resolveForShipmentCharge_shouldReturnEmptyForDta_whenUnifiedResolverReturnsEmpty() {
        Shipment shipment = shipment("DDP", "RUH");
        shipment.setConsigneeNumber("CONS-1");
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "DTA")).thenReturn(Optional.empty());

        assertThat(service.resolveForShipmentCharge(shipment, "DTA")).isEmpty();
    }

    @Test
    void resolveForShipmentCharge_shouldFallbackToEntitySettingsForDtc_whenResolvedArHasNoRateSheet() {
        Shipment shipment = shipment("DDP", "RUH");
        shipment.setConsigneeNumber("CONS-1");
        Account arDtcAccount = account(UUID.randomUUID());
        Account entitySettingsAccount = entitySettingsAccount(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem item = billableItem("DDP");

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "DTC")).thenReturn(Optional.of(arDtcAccount));
        stubEmptyRateSheetSnapshot(arDtcAccount);
        when(serviceChargeAccountResolver.resolveEntitySettings("RUH", "DTC"))
                .thenReturn(Optional.of(entitySettingsAccount));
        stubRateSheetSnapshot(entitySettingsAccount, rateSheet, List.of(item));

        assertThat(service.resolveForShipmentCharge(shipment, "DTC")).isPresent()
                .get()
                .extracting(ResolvedRateSheet::account)
                .isSameAs(entitySettingsAccount);
    }

    @Test
    void resolveForShipmentCharge_shouldFallbackToEntitySettingsForDts_whenPartyArHasNoRateSheet() {
        Shipment shipment = shipment("DDP", "RUH");
        shipment.setOriginEntity("JED");
        shipment.setShipperNumber("SHIP-1");
        Account arDtsAccount = account(UUID.randomUUID());
        Account entitySettingsAccount = entitySettingsAccount(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem item = billableItem("DDP");

        when(partyArAccountResolver.resolveOrCreate(shipment, "JED", "DTS")).thenReturn(Optional.of(arDtsAccount));
        stubEmptyRateSheetSnapshot(arDtsAccount);
        when(serviceChargeAccountResolver.resolveEntitySettings("JED", "DTS"))
                .thenReturn(Optional.of(entitySettingsAccount));
        stubRateSheetSnapshot(entitySettingsAccount, rateSheet, List.of(item));

        assertThat(service.resolveForShipmentCharge(shipment, "DTS")).isPresent()
                .get()
                .extracting(ResolvedRateSheet::account)
                .isSameAs(entitySettingsAccount);
    }

    @Test
    void resolveForShipmentCharge_shouldUseUnifiedResolverForDtc() {
        Shipment shipment = shipment("DDP", "RUH");
        shipment.setConsigneeNumber("CONS-1");
        Account dtcAccount = account(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem item = billableItem("DDP");

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "DTC")).thenReturn(Optional.of(dtcAccount));
        stubRateSheetSnapshot(dtcAccount, rateSheet, List.of(item));

        assertThat(service.resolveForShipmentCharge(shipment, "DTC")).isPresent()
                .get()
                .extracting(ResolvedRateSheet::account)
                .isSameAs(dtcAccount);
        verify(serviceChargeAccountResolver).resolve(shipment, "RUH", "DTC");
    }

    @Test
    void resolveForShipmentCharge_shouldUseThirdPartyEntityForDt3() {
        Shipment shipment = shipment("DDP", "RUH");
        shipment.setThirdPartyEntity("JED");
        shipment.setConsigneeNumber("CONS-1");
        shipment.setThirdPartyNumber("TP-1");
        Account account = account(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem item = billableItem("DDP");

        when(partyArAccountResolver.resolveOrCreate(shipment, "JED", "DT3")).thenReturn(Optional.of(account));
        stubRateSheetSnapshot(account, rateSheet, List.of(item));

        assertThat(service.resolveForShipmentCharge(shipment, "DT3")).isPresent();
        verify(partyArAccountResolver).resolveOrCreate(shipment, "JED", "DT3");
    }

    @Test
    void resolveForShipmentCharge_shouldFallbackToEntitySettingsForDts_whenPartyArMissing() {
        Shipment shipment = shipment("DDP", "RUH");
        shipment.setOriginEntity("JED");
        shipment.setShipperNumber("SHIP-1");
        Account entitySettingsAccount = entitySettingsAccount(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem item = billableItem("DDP");

        when(partyArAccountResolver.resolveOrCreate(shipment, "JED", "DTS")).thenReturn(Optional.empty());
        when(serviceChargeAccountResolver.resolveEntitySettings("JED", "DTS"))
                .thenReturn(Optional.of(entitySettingsAccount));
        stubRateSheetSnapshot(entitySettingsAccount, rateSheet, List.of(item));

        assertThat(service.resolveForShipmentCharge(shipment, "DTS")).isPresent()
                .get()
                .extracting(ResolvedRateSheet::account)
                .isSameAs(entitySettingsAccount);
    }

    @Test
    void resolveForAccount_shouldReturnEmpty_whenRateSheetInactive() {
        Account account = account(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), false);
        stubEmptyRateSheetSnapshot(account);

        assertThat(service.resolveForAccount(account)).isEmpty();
    }

    @Test
    void resolveForAccount_shouldFilterItemsByShipmentType() {
        Account account = account(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem matching = billableItem("DDU");
        RateSheetItem other = billableItem("DDP");

        stubRateSheetSnapshot(account, rateSheet, List.of(matching, other));

        Shipment shipment = shipment("DDU", "RUH");
        assertThat(service.resolveForAccount(account, shipment)).isPresent()
                .get()
                .satisfies(resolved -> assertThat(resolved.items()).containsExactly(matching));
    }

    @Test
    void resolveForAccount_shouldExcludeItem_whenBusinessTypeDoesNotMatch() {
        Account account = account(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem matching = billableItem("DDU");
        matching.setBusinessType("B2B");
        matching.setCategoryType("LV");
        RateSheetItem other = billableItem("DDU");
        other.setBusinessType("B2C");
        other.setCategoryType("LV");

        stubRateSheetSnapshot(account, rateSheet, List.of(matching, other));

        Shipment shipment = shipment("DDU", "RUH");
        shipment.setShipmentBusinessType("B2B");
        shipment.setCategoryType("LV");
        assertThat(service.resolveForAccount(account, shipment)).isPresent()
                .get()
                .satisfies(resolved -> assertThat(resolved.items()).containsExactly(matching));
    }

    @Test
    void resolveForAccount_shouldMatchCatchAllRowForAnyShipmentCombination() {
        Account account = account(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem catchAll = billableItem("ALL");
        catchAll.setBusinessType("ALL");
        catchAll.setCategoryType("ALL");

        stubRateSheetSnapshot(account, rateSheet, List.of(catchAll));

        Shipment dduShipment = shipment("DDU", "RUH");
        dduShipment.setShipmentBusinessType("B2B");
        dduShipment.setCategoryType("LV");
        Shipment ddoShipment = shipment("DDO", "RUH");
        ddoShipment.setShipmentBusinessType("B2C");
        ddoShipment.setCategoryType("HV");

        assertThat(service.resolveForAccount(account, dduShipment)).isPresent()
                .get()
                .satisfies(resolved -> assertThat(resolved.items()).containsExactly(catchAll));
        assertThat(service.resolveForAccount(account, ddoShipment)).isPresent()
                .get()
                .satisfies(resolved -> assertThat(resolved.items()).containsExactly(catchAll));
    }

    @Test
    void resolveForAccount_shouldPreferSpecificRowOverCatchAllForSameServiceCode() {
        Account account = account(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem catchAll = billableItem("ALL");
        catchAll.setBusinessType("ALL");
        catchAll.setCategoryType("ALL");
        catchAll.setServiceCode("DUTY");
        RateSheetItem exact = billableItem("DDP");
        exact.setBusinessType("B2C");
        exact.setCategoryType("HV");
        exact.setServiceCode("DUTY");

        stubRateSheetSnapshot(account, rateSheet, List.of(catchAll, exact));

        Shipment shipment = shipment("DDP", "RUH");
        shipment.setShipmentBusinessType("B2C");
        shipment.setCategoryType("HV");

        assertThat(service.resolveForAccount(account, shipment)).isPresent()
                .get()
                .satisfies(resolved -> assertThat(resolved.items()).containsExactly(exact));
    }

    @Test
    void resolveForEntitySettings_shouldDelegateWithShipmentOnly() {
        Account account = entitySettingsAccount(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem item = billableItem("DDU");

        when(serviceChargeAccountResolver.resolveEntitySettings("RUH", "DTC"))
                .thenReturn(Optional.of(account));
        stubRateSheetSnapshot(account, rateSheet, List.of(item));

        Shipment shipment = shipment("DDU", "RUH");
        assertThat(service.resolveForEntitySettings("RUH", "DTC", shipment)).isPresent();
    }

    @Test
    void resolveForAccount_shouldReturnEmptyWhenAccountMissing() {
        assertThat(service.resolveForAccount(null)).isEmpty();
        assertThat(service.resolveForAccount(new Account())).isEmpty();
    }

    @Test
    void resolveForArAccountNumber_shouldReturnEmptyWhenBlankOrEntityMissing() {
        Shipment shipment = shipment("DDP", "RUH");
        assertThat(service.resolveForArAccountNumber(" ", shipment, "DTC")).isEmpty();
        assertThat(service.resolveForArAccountNumber("AR-1", new Shipment(), "DTC")).isEmpty();
    }

    @Test
    void resolveForShipmentCharge_shouldReturnEmptyWhenInputsMissing() {
        assertThat(service.resolveForShipmentCharge(null, "DTC")).isEmpty();
        assertThat(service.resolveForShipmentCharge(shipment("DDP", "RUH"), " ")).isEmpty();
    }

    @Test
    void resolveForShipmentCharge_shouldUseEntitySettingsForEcd() {
        Account account = entitySettingsAccount(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem item = billableItem("DDU");

        when(serviceChargeAccountResolver.resolveEntitySettings("RUH", "ECD"))
                .thenReturn(Optional.of(account));
        stubRateSheetSnapshot(account, rateSheet, List.of(item));

        Shipment shipment = shipment("DDU", "RUH");
        assertThat(service.resolveForShipmentCharge(shipment, "ECD")).isPresent();
    }

    @Test
    void resolveForShipmentCharge_shouldFallbackToEntitySettingsForUnknownChargeType() {
        Account account = entitySettingsAccount(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem item = billableItem("DDU");

        when(serviceChargeAccountResolver.resolveEntitySettings("RUH", "ETS"))
                .thenReturn(Optional.of(account));
        stubRateSheetSnapshot(account, rateSheet, List.of(item));

        Shipment shipment = shipment("DDU", "RUH");
        assertThat(service.resolveForShipmentCharge(shipment, "ETS")).isPresent();
    }

    @Test
    void resolveForAccount_shouldReturnEmptyWhenNoBillableItems() {
        Account account = account(UUID.randomUUID());
        RateSheet rateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem nonMatching = billableItem("DDP");

        stubRateSheetSnapshot(account, rateSheet, List.of(nonMatching));

        Shipment shipment = shipment("DDU", "RUH");
        assertThat(service.resolveForAccount(account, shipment)).isEmpty();
    }

    @Test
    void resolveForShipmentCharge_shouldFallbackToEntity_whenArRateSheetHasNoRows() {
        Shipment shipment = shipment("DDP", "RUH");
        shipment.setCategoryType(CATEGORY_TYPE_LV);
        Account arDtcAccount = arAccount(UUID.randomUUID());
        Account entitySettingsAccount = entitySettingsAccount(UUID.randomUUID());
        RateSheet emptyArRateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheet entityRateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem lvItem = billableItem("DDP");
        lvItem.setCategoryType(CATEGORY_TYPE_LV);

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "DTC")).thenReturn(Optional.of(arDtcAccount));
        stubRateSheetSnapshot(arDtcAccount, emptyArRateSheet, List.of());
        when(serviceChargeAccountResolver.resolveEntitySettings("RUH", "DTC"))
                .thenReturn(Optional.of(entitySettingsAccount));
        stubRateSheetSnapshot(entitySettingsAccount, entityRateSheet, List.of(lvItem));

        assertThat(service.resolveForShipmentCharge(shipment, "DTC", CATEGORY_TYPE_LV)).isPresent()
                .get()
                .extracting(ResolvedRateSheet::account)
                .isSameAs(entitySettingsAccount);
    }

    @Test
    void resolveForShipmentCharge_shouldFallbackToEntity_whenArRateSheetHasNoMatchingCategory() {
        Shipment shipment = shipment("DDP", "RUH");
        shipment.setCategoryType(CATEGORY_TYPE_LV);
        Account arDtcAccount = arAccount(UUID.randomUUID());
        Account entitySettingsAccount = entitySettingsAccount(UUID.randomUUID());
        RateSheet arRateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheet entityRateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem hvOnlyItem = billableItem("DDP");
        hvOnlyItem.setCategoryType(CATEGORY_TYPE_HV);
        RateSheetItem lvItem = billableItem("DDP");
        lvItem.setCategoryType(CATEGORY_TYPE_LV);

        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "DTC")).thenReturn(Optional.of(arDtcAccount));
        stubRateSheetSnapshot(arDtcAccount, arRateSheet, List.of(hvOnlyItem));
        when(serviceChargeAccountResolver.resolveEntitySettings("RUH", "DTC"))
                .thenReturn(Optional.of(entitySettingsAccount));
        stubRateSheetSnapshot(entitySettingsAccount, entityRateSheet, List.of(lvItem));

        assertThat(service.resolveForShipmentCharge(shipment, "DTC", CATEGORY_TYPE_LV)).isPresent()
                .get()
                .extracting(ResolvedRateSheet::account)
                .isSameAs(entitySettingsAccount);
    }

    @Test
    void resolveForShipmentCharge_shouldFallbackToEntityForDts_whenArRateSheetHasNoMatchingCategory() {
        Shipment shipment = shipment("DDP", "RUH");
        shipment.setOriginEntity("JED");
        shipment.setShipperNumber("SHIP-1");
        shipment.setCategoryType(CATEGORY_TYPE_HV);
        Account arDtsAccount = arAccount(UUID.randomUUID());
        Account entitySettingsAccount = entitySettingsAccount(UUID.randomUUID());
        RateSheet arRateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheet entityRateSheet = rateSheet(UUID.randomUUID(), true);
        RateSheetItem lvOnlyItem = billableItem("DDP");
        lvOnlyItem.setCategoryType(CATEGORY_TYPE_LV);
        RateSheetItem hvItem = billableItem("DDP");
        hvItem.setCategoryType(CATEGORY_TYPE_HV);

        when(partyArAccountResolver.resolveOrCreate(shipment, "JED", "DTS")).thenReturn(Optional.of(arDtsAccount));
        stubRateSheetSnapshot(arDtsAccount, arRateSheet, List.of(lvOnlyItem));
        when(serviceChargeAccountResolver.resolveEntitySettings("JED", "DTS"))
                .thenReturn(Optional.of(entitySettingsAccount));
        stubRateSheetSnapshot(entitySettingsAccount, entityRateSheet, List.of(hvItem));

        assertThat(service.resolveForShipmentCharge(shipment, "DTS", CATEGORY_TYPE_HV)).isPresent()
                .get()
                .extracting(ResolvedRateSheet::account)
                .isSameAs(entitySettingsAccount);
    }

    @Test
    void resolveForUnifiedRevenueCharge_shouldReturnEmptyWhenNoAccountsResolved() {
        Shipment shipment = shipment("DDP", "RUH");
        shipment.setConsigneeNumber("CONS-1");
        when(serviceChargeAccountResolver.resolve(shipment, "RUH", "DTC")).thenReturn(Optional.empty());
        when(serviceChargeAccountResolver.resolveEntitySettings("RUH", "DTC"))
                .thenReturn(Optional.empty());

        assertThat(service.resolveForShipmentCharge(shipment, "DTC")).isEmpty();
    }

    private void stubRateSheetSnapshot(Account account, RateSheet rateSheet, List<RateSheetItem> items) {
        when(accountRedisChargeDataService.getRateSheetSnapshot(
                        eq(account), any(), any()))
                .thenReturn(Optional.of(new AccountRedisRateSheetSnapshot(rateSheet, items)));
    }

    private void stubEmptyRateSheetSnapshot(Account account) {
        when(accountRedisChargeDataService.getRateSheetSnapshot(
                        eq(account), any(), any()))
                .thenReturn(Optional.empty());
    }

    private static Shipment shipment(String shipmentType, String destinationEntity) {
        Shipment shipment = new Shipment();
        shipment.setShipmentType(shipmentType);
        shipment.setHAWBDestinationEntity(destinationEntity);
        return shipment;
    }

    private static Account arAccount(UUID id) {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", id);
        account.setAccountNature(AccountNature.AR);
        return account;
    }

    private static Account entitySettingsAccount(UUID id) {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", id);
        account.setAccountNature(AccountNature.ENTITY_SETTINGS);
        return account;
    }

    private static Account account(UUID id) {
        return arAccount(id);
    }

    private static RateSheet rateSheet(UUID id, boolean active) {
        RateSheet rateSheet = new RateSheet();
        ReflectionTestUtils.setField(rateSheet, "id", id);
        rateSheet.setActive(active);
        return rateSheet;
    }

    private static RateSheetItem billableItem(String shipmentType) {
        RateSheetItem item = new RateSheetItem();
        item.setShipmentType(shipmentType);
        RateSheetItemRate rate = new RateSheetItemRate();
        rate.setRateType(RateType.FIXED);
        rate.setValue(BigDecimal.ONE);
        item.setRates(List.of(rate));
        item.setServiceCode("SVC");
        return item;
    }
}
