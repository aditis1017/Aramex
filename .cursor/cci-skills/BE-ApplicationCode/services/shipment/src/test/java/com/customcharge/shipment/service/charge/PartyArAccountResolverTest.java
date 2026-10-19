package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.shipment.observability.ShipmentProcessingPhaseTimer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyArAccountResolverTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ARAccountRepository arAccountRepository;
    @Mock
    private ServiceChargeAccountResolver serviceChargeAccountResolver;
    @Mock
    private ObjectProvider<ServiceChargeAccountResolver> serviceChargeAccountResolverProvider;
    @Mock
    private PartyArAccountChargeConfigService partyArAccountChargeConfigService;
    @Mock
    private ShipmentProcessingPhaseTimer phaseTimer;

    @InjectMocks
    private PartyArAccountResolver resolver;

    @BeforeEach
    void setUp() {
        lenient().when(serviceChargeAccountResolverProvider.getObject()).thenReturn(serviceChargeAccountResolver);
        lenient().doAnswer(invocation -> invocation.getArgument(2, java.util.function.Supplier.class).get())
                .when(phaseTimer).supply(anyString(), anyString(), any(java.util.function.Supplier.class));
    }

    @Test
    void resolveOrCreate_shouldReturnEmpty_whenShipmentMissing() {
        assertThat(resolver.resolveOrCreate(null, "JED", "DTS")).isEmpty();
        verifyNoInteractions(accountRepository, arAccountRepository);
    }

    @Test
    void resolveOrCreate_shouldReturnEmpty_whenChargeTypeUnsupported() {
        Shipment shipment = shipmentWithShipper("SHIP-1");

        assertThat(resolver.resolveOrCreate(shipment, "JED", "DTA")).isEmpty();
        verifyNoInteractions(accountRepository, arAccountRepository);
    }

    @Test
    void resolveOrCreate_shouldReturnEmpty_whenPartyNumberMissing() {
        Shipment shipment = new Shipment();
        shipment.setOriginEntity("JED");

        assertThat(resolver.resolveOrCreate(shipment, "JED", "DTS")).isEmpty();
        verifyNoInteractions(arAccountRepository);
    }

    @Test
    void resolveOrCreate_shouldReturnEmpty_whenEntitySettingsMissing() {
        Shipment shipment = shipmentWithShipper("SHIP-1");
        when(serviceChargeAccountResolver.resolveEntitySettings("JED", "DTS")).thenReturn(Optional.empty());

        assertThat(resolver.resolveOrCreate(shipment, "JED", "DTS")).isEmpty();
        verifyNoInteractions(arAccountRepository);
    }

    @Test
    void resolveOrCreate_shouldReturnEmpty_whenArMasterMissing() {
        Shipment shipment = shipmentWithShipper("SHIP-1");
        when(serviceChargeAccountResolver.resolveEntitySettings("JED", "DTS"))
                .thenReturn(Optional.of(entitySettings("JED", "DTS")));
        when(serviceChargeAccountResolver.resolveArChargeAccount("SHIP-1", "JED", "DTS"))
                .thenReturn(Optional.empty());
        when(arAccountRepository.findByAccountNumberAndEntityCode("SHIP-1", "JED"))
                .thenReturn(Optional.empty());

        assertThat(resolver.resolveOrCreate(shipment, "JED", "DTS")).isEmpty();
    }

    @Test
    void resolveOrCreate_shouldReturnExistingViaArChargeAccountFastPath() {
        Shipment shipment = shipmentWithShipper("SHIP-1");
        Account existing = new Account();
        when(serviceChargeAccountResolver.resolveEntitySettings("JED", "DTS"))
                .thenReturn(Optional.of(entitySettings("JED", "DTS")));
        when(serviceChargeAccountResolver.resolveArChargeAccount("SHIP-1", "JED", "DTS"))
                .thenReturn(Optional.of(existing));

        assertThat(resolver.resolveOrCreate(shipment, "JED", "DTS")).containsSame(existing);
        verifyNoInteractions(arAccountRepository);
        verifyNoInteractions(partyArAccountChargeConfigService);
    }

    @Test
    void resolveOrCreate_shouldReturnExistingDtsAccount_byArMasterId() {
        Shipment shipment = shipmentWithShipper("SHIP-1");
        ARAccount arMaster = arMaster("SHIP-1", "JED");
        Account existing = new Account();
        when(serviceChargeAccountResolver.resolveEntitySettings("JED", "DTS"))
                .thenReturn(Optional.of(entitySettings("JED", "DTS")));
        when(serviceChargeAccountResolver.resolveArChargeAccount("SHIP-1", "JED", "DTS"))
                .thenReturn(Optional.empty());
        when(arAccountRepository.findByAccountNumberAndEntityCode("SHIP-1", "JED"))
                .thenReturn(Optional.of(arMaster));
        when(accountRepository.findByArAccountIdAndChargeType(arMaster.getId(), "DTS"))
                .thenReturn(Optional.of(existing));

        assertThat(resolver.resolveOrCreate(shipment, "JED", "DTS")).containsSame(existing);
        verify(partyArAccountChargeConfigService, never()).createChargeConfigIfNeeded(any(), any(), anyString());
    }

    @Test
    void resolveOrCreate_shouldCreateMinimalDtsAccount_whenConfigMissing() {
        Shipment shipment = shipmentWithShipper("SHIP-1");
        ARAccount arMaster = arMaster("SHIP-1", "JED");
        Account entitySettings = entitySettings("JED", "DTS");
        Entity entity = entitySettings.getEntity();
        Account created = new Account();
        when(serviceChargeAccountResolver.resolveEntitySettings("JED", "DTS"))
                .thenReturn(Optional.of(entitySettings));
        when(serviceChargeAccountResolver.resolveArChargeAccount("SHIP-1", "JED", "DTS"))
                .thenReturn(Optional.empty());
        when(arAccountRepository.findByAccountNumberAndEntityCode("SHIP-1", "JED"))
                .thenReturn(Optional.of(arMaster));
        when(accountRepository.findByArAccountIdAndChargeType(arMaster.getId(), "DTS"))
                .thenReturn(Optional.empty());
        when(partyArAccountChargeConfigService.createChargeConfigIfNeeded(arMaster, entity, "DTS"))
                .thenReturn(created);

        assertThat(resolver.resolveOrCreate(shipment, "JED", "DTS")).containsSame(created);
    }

    @Test
    void resolveOrCreate_shouldCreateMinimalDtsAccount_whenEntitySettingsLoadedFromRedis() {
        Shipment shipment = shipmentWithShipper("SHIP-1");
        ARAccount arMaster = arMaster("SHIP-1", "JED");
        Account redisEntitySettings = redisEntitySettings("JED", "DTS");
        Entity entity = redisEntitySettings.getEntity();
        Account created = new Account();
        when(serviceChargeAccountResolver.resolveEntitySettings("JED", "DTS"))
                .thenReturn(Optional.of(redisEntitySettings));
        when(serviceChargeAccountResolver.resolveArChargeAccount("SHIP-1", "JED", "DTS"))
                .thenReturn(Optional.empty());
        when(arAccountRepository.findByAccountNumberAndEntityCode("SHIP-1", "JED"))
                .thenReturn(Optional.of(arMaster));
        when(accountRepository.findByArAccountIdAndChargeType(arMaster.getId(), "DTS"))
                .thenReturn(Optional.empty());
        when(partyArAccountChargeConfigService.createChargeConfigIfNeeded(arMaster, entity, "DTS"))
                .thenReturn(created);

        assertThat(resolver.resolveOrCreate(shipment, "JED", "DTS")).containsSame(created);
    }

    @Test
    void resolveOrCreate_shouldUseThirdPartyNumberForDt3() {
        Shipment shipment = new Shipment();
        shipment.setThirdPartyEntity("DXB");
        shipment.setThirdPartyNumber(" TP-1 ");
        ARAccount arMaster = arMaster("TP-1", "DXB");
        Account entitySettings = entitySettings("DXB", "DT3");
        Account created = new Account();
        when(serviceChargeAccountResolver.resolveEntitySettings("DXB", "DT3"))
                .thenReturn(Optional.of(entitySettings));
        when(serviceChargeAccountResolver.resolveArChargeAccount("TP-1", "DXB", "DT3"))
                .thenReturn(Optional.empty());
        when(arAccountRepository.findByAccountNumberAndEntityCode("TP-1", "DXB"))
                .thenReturn(Optional.of(arMaster));
        when(accountRepository.findByArAccountIdAndChargeType(arMaster.getId(), "DT3"))
                .thenReturn(Optional.empty());
        when(partyArAccountChargeConfigService.createChargeConfigIfNeeded(
                        eq(arMaster), eq(entitySettings.getEntity()), eq("DT3")))
                .thenReturn(created);

        assertThat(resolver.resolveOrCreate(shipment, "DXB", "DT3")).containsSame(created);
    }

    private static Shipment shipmentWithShipper(String shipperNumber) {
        Shipment shipment = new Shipment();
        shipment.setOriginEntity("JED");
        shipment.setShipperNumber(shipperNumber);
        return shipment;
    }

    private static Account redisEntitySettings(String entityCode, String chargeType) {
        Entity transientEntity = new Entity();
        transientEntity.setCode(entityCode);
        Account account = new Account();
        account.setEntity(transientEntity);
        account.setAccountNature(AccountNature.ENTITY_SETTINGS);
        account.setChargeType(chargeType);
        return account;
    }

    private static Account entitySettings(String entityCode, String chargeType) {
        Entity entity = new Entity();
        entity.setCode(entityCode);
        Account account = new Account();
        account.setEntity(entity);
        account.setAccountNature(AccountNature.ENTITY_SETTINGS);
        account.setChargeType(chargeType);
        return account;
    }

    private static ARAccount arMaster(String accountNumber, String entityCode) {
        ARAccount arAccount = new ARAccount();
        ReflectionTestUtils.setField(arAccount, "id", UUID.randomUUID());
        arAccount.setEntityCode(entityCode);
        arAccount.setAccountNumber(accountNumber);
        arAccount.setAccountName("Party AR");
        return arAccount;
    }
}
