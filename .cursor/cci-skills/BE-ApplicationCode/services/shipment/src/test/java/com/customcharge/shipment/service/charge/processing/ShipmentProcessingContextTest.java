package com.customcharge.shipment.service.charge.processing;

import com.customcharge.persistencelayer.accountRedis.AccountRedisChargeDataService;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import com.customcharge.shipment.service.charge.ShipmentCurrencyContext;
import com.customcharge.shipment.service.charge.processing.model.ResolvedRateSheet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentProcessingContextTest {

    @Mock
    private EntityRepository entityRepository;
    @Mock
    private ServiceCodesRepository serviceCodesRepository;
    @Mock
    private ServiceChargeRepository serviceChargeRepository;
    @Mock
    private AccountRedisChargeDataService accountRedisChargeDataService;

    private Shipment shipment;
    private ShipmentProcessingContext context;

    @BeforeEach
    void setUp() {
        shipment = new Shipment();
        shipment.setHAWBDestinationEntity("RUH");
        shipment.setOriginEntity("JED");
        context = ShipmentProcessingContext.create(
                shipment,
                new ShipmentCurrencyContext(shipment, null),
                entityRepository,
                serviceCodesRepository,
                serviceChargeRepository,
                accountRedisChargeDataService);
        ShipmentProcessingContext.bind(context);
    }

    @AfterEach
    void tearDown() {
        ShipmentProcessingContext.clear();
    }

    @Test
    void cachedAccount_shouldUseLoader_whenContextMissing() {
        ShipmentProcessingContext.clear();

        Optional<Account> result = ShipmentProcessingContext.cachedAccount("key", () -> Optional.of(new Account()));

        assertThat(result).isPresent();
    }

    @Test
    void findEntity_shouldCacheLoadedEntity() {
        Entity entity = entity("RUH");
        when(entityRepository.findByCodeIgnoreCase("RUH")).thenReturn(Optional.of(entity));

        assertThat(context.findEntity("RUH")).containsSame(entity);
        assertThat(context.findEntity("ruh")).containsSame(entity);
        verify(entityRepository).findByCodeIgnoreCase("RUH");
    }

    @Test
    void registerAndFindCharge_shouldRoundTrip() {
        Charge charge = new Charge();
        charge.setChargeCode("ECD");
        context.registerCharge("ECD", charge);

        assertThat(context.findChargeByCode("ecd")).containsSame(charge);
    }

    @Test
    void registerAndFindLineItems_shouldRoundTrip() {
        ChargeLineItem line = new ChargeLineItem();
        line.setServiceCode("DUTY");
        context.registerLineItems("ECD", List.of(line));

        assertThat(context.findLineItemsByChargeCode("ECD")).contains(List.of(line));
    }

    @Test
    void findServiceCode_shouldLoadFromRepository() {
        ServiceCodes master = serviceCode("DUTY", "ECD");
        when(serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                "RUH", "DUTY", "ECD")).thenReturn(Optional.of(master));

        assertThat(context.findServiceCode("RUH", "ECD", "DUTY")).containsSame(master);
    }

    @Test
    void findServiceCharge_shouldLoadFromRepository() {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        ServiceCodes master = serviceCode("DUTY", "ECD");
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());
        ServiceCharge charge = new ServiceCharge();
        when(serviceChargeRepository.findByAccountAndServiceCodes_IdWithActiveServiceCodes(account, master.getId()))
                .thenReturn(Optional.of(charge));

        assertThat(context.findServiceCharge(account, master)).containsSame(charge);
    }

    @Test
    void ensureServiceChargesIndexed_shouldLoadMastersAndCharges() {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        ServiceCodes master = serviceCode("DUTY", "ECD");
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());
        Entity entity = entity("RUH");
        master.setEntity(entity);
        context.registerCharge("ECD", new Charge());
        when(serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(eq("RUH"), any()))
                .thenReturn(List.of(master));
        when(serviceChargeRepository.findByAccountAndServiceCodesIn(eq(account), any()))
                .thenReturn(List.of());

        context.ensureServiceChargesIndexed(account, "RUH", "ECD");

        verify(serviceCodesRepository).findActiveMastersByEntityCodeAndChargeTypes(eq("RUH"), any());
    }

    @Test
    void preloadForChargeCodes_shouldPrimeEntityAndServiceCodes() {
        Entity entity = entity("RUH");
        when(entityRepository.findByCodeIgnoreCase("RUH")).thenReturn(Optional.of(entity));
        when(entityRepository.findByCodeIgnoreCase("JED")).thenReturn(Optional.of(entity("JED")));
        when(serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(any(), any()))
                .thenReturn(List.of());

        context.preloadForChargeCodes(List.of("ECD", "ECS"));

        verify(entityRepository).findByCodeIgnoreCase("RUH");
        verify(entityRepository).findByCodeIgnoreCase("JED");
    }

    @Test
    void cachedRateSheet_shouldCacheByAccountId() {
        UUID accountId = UUID.randomUUID();
        ResolvedRateSheet sheet = new ResolvedRateSheet(
                new Account(), new com.customcharge.persistencelayer.entities.RateSheet(), List.of());

        Optional<ResolvedRateSheet> first = ShipmentProcessingContext.cachedRateSheet(
                accountId, () -> Optional.of(sheet));
        Optional<ResolvedRateSheet> second = ShipmentProcessingContext.cachedRateSheet(
                accountId, () -> Optional.empty());

        assertThat(first).containsSame(sheet);
        assertThat(second).containsSame(sheet);
    }

    @Test
    void cachedDtaArAccount_shouldCacheByEntityCode() {
        Account account = new Account();

        Optional<Account> first = ShipmentProcessingContext.cachedDtaArAccount(
                "RUH", () -> Optional.of(account));
        Optional<Account> second = ShipmentProcessingContext.cachedDtaArAccount(
                "ruh", () -> Optional.empty());

        assertThat(first).containsSame(account);
        assertThat(second).containsSame(account);
    }

    @Test
    void findServiceCharge_shouldUseRepositoryWhenContextBound() {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        ServiceCodes master = serviceCode("DUTY", "ECD");
        UUID masterId = UUID.randomUUID();
        ReflectionTestUtils.setField(master, "id", masterId);
        ServiceCharge charge = new ServiceCharge();
        when(serviceChargeRepository.findByAccountAndServiceCodes_IdWithActiveServiceCodes(account, masterId))
                .thenReturn(Optional.of(charge));

        assertThat(context.findServiceCharge(account, master)).containsSame(charge);
        assertThat(context.findServiceCharge(account, master)).containsSame(charge);
    }

    @Test
    void cachedAccount_shouldCacheWhenContextBound() {
        Account account = new Account();

        Optional<Account> first = ShipmentProcessingContext.cachedAccount("CACHE-KEY", () -> Optional.of(account));
        Optional<Account> second = ShipmentProcessingContext.cachedAccount("CACHE-KEY", () -> Optional.empty());

        assertThat(first).containsSame(account);
        assertThat(second).containsSame(account);
    }

    @Test
    void cachedAccount_shouldAllowNestedLoadsOfDifferentKeys() {
        Account partyAccount = new Account();
        Account entitySettings = new Account();
        Account arAccount = new Account();

        Optional<Account> result = ShipmentProcessingContext.cachedAccount("PARTY_AR:75815981:DXB:DTS", () -> {
            Optional<Account> settings = ShipmentProcessingContext.cachedAccount(
                    "ENTITY_SETTINGS:DXB:DTS", () -> Optional.of(entitySettings));
            Optional<Account> existingAr = ShipmentProcessingContext.cachedAccount(
                    "AR:75815981:DXB:DTS", () -> Optional.of(arAccount));
            assertThat(settings).containsSame(entitySettings);
            assertThat(existingAr).containsSame(arAccount);
            return Optional.of(partyAccount);
        });

        assertThat(result).containsSame(partyAccount);
        assertThat(ShipmentProcessingContext.cachedAccount(
                "ENTITY_SETTINGS:DXB:DTS", () -> Optional.empty())).containsSame(entitySettings);
        assertThat(ShipmentProcessingContext.cachedAccount(
                "AR:75815981:DXB:DTS", () -> Optional.empty())).containsSame(arAccount);
        assertThat(ShipmentProcessingContext.cachedAccount(
                "PARTY_AR:75815981:DXB:DTS", () -> Optional.empty())).containsSame(partyAccount);
    }

    @Test
    void preloadForChargeCodes_shouldNoOpWhenNullOrEmpty() {
        context.preloadForChargeCodes(null);
        context.preloadForChargeCodes(List.of());
    }

    @Test
    void findServiceCode_shouldReturnCachedMaster() {
        ServiceCodes master = serviceCode("DUTY", "ECD");
        when(serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                "RUH", "DUTY", "ECD")).thenReturn(Optional.of(master));

        assertThat(context.findServiceCode("RUH", "ECD", "DUTY")).containsSame(master);
        assertThat(context.findServiceCode("RUH", "ECD", "DUTY")).containsSame(master);
        verify(serviceCodesRepository).findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                "RUH", "DUTY", "ECD");
    }

    @Test
    void findServiceCode_shouldReturnEmptyWhenInputsBlank() {
        assertThat(context.findServiceCode("", "ECD", "DUTY")).isEmpty();
        assertThat(context.findServiceCode("RUH", "ECD", "")).isEmpty();
    }

    @Test
    void findServiceCharge_shouldReturnEmptyWhenInputsNull() {
        assertThat(context.findServiceCharge(null, null)).isEmpty();
    }

    @Test
    void ensureServiceChargesIndexed_shouldLoadServiceChargesFromRepository() {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        ServiceCodes master = serviceCode("DUTY", "ECD");
        UUID masterId = UUID.randomUUID();
        ReflectionTestUtils.setField(master, "id", masterId);
        Entity entity = entity("RUH");
        master.setEntity(entity);
        ServiceCharge serviceCharge = new ServiceCharge();
        serviceCharge.setServiceCodes(master);

        when(serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(eq("RUH"), any()))
                .thenReturn(List.of(master));
        when(serviceChargeRepository.findByAccountAndServiceCodesIn(eq(account), any()))
                .thenReturn(List.of(serviceCharge));

        context.ensureServiceChargesIndexed(account, "RUH", "ECD");

        assertThat(context.findServiceCharge(account, master)).containsSame(serviceCharge);
    }

    @Test
    void ensureServiceChargesIndexed_shouldNoOpWhenAlreadyIndexed() {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());

        context.ensureServiceChargesIndexed(account, "RUH", "ECD");
        context.ensureServiceChargesIndexed(account, "RUH", "ECD");

        verify(serviceCodesRepository, times(1)).findActiveMastersByEntityCodeAndChargeTypes(any(), any());
    }

    @Test
    void registerCharge_shouldNoOpWhenInvalid() {
        context.registerCharge("", null);
        assertThat(context.findChargeByCode("ECD")).isEmpty();
    }

    @Test
    void registerLineItems_shouldNoOpWhenInvalid() {
        context.registerLineItems("", null);
        context.registerLineItems("ECD", List.of());
        assertThat(context.findLineItemsByChargeCode("ECD")).isEmpty();
    }

    @Test
    void findLineItemsByChargeCode_shouldReturnEmptyWhenBlank() {
        assertThat(context.findLineItemsByChargeCode("")).isEmpty();
    }

    @Test
    void preloadForChargeCodes_shouldPrimeThirdPartyEntity() {
        shipment.setThirdPartyEntity("DXB");
        when(entityRepository.findByCodeIgnoreCase("DXB")).thenReturn(Optional.of(entity("DXB")));
        when(entityRepository.findByCodeIgnoreCase("RUH")).thenReturn(Optional.of(entity("RUH")));
        when(entityRepository.findByCodeIgnoreCase("JED")).thenReturn(Optional.of(entity("JED")));
        when(serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(any(), any()))
                .thenReturn(List.of());

        context.preloadForChargeCodes(List.of("ECD"));

        verify(entityRepository).findByCodeIgnoreCase("DXB");
    }

    private static Entity entity(String code) {
        Entity entity = new Entity();
        entity.setCode(code);
        return entity;
    }

    private static ServiceCodes serviceCode(String serviceCode, String chargeType) {
        ServiceCodes master = new ServiceCodes();
        master.setServiceCode(serviceCode);
        master.setChargeType(chargeType);
        return master;
    }
}
