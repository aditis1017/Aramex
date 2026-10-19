package com.customcharge.chargescalculator.service.batch;

import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeMappingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecordProcessingLookupsTest {

    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private ChargeLineItemRepository chargeLineItemRepository;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private ServiceChargeMappingRepository serviceChargeMappingRepository;

    private RecordProcessingLookups lookups;

    @BeforeEach
    void setUp() {
        lookups = new RecordProcessingLookups(
                chargeRepository,
                chargeLineItemRepository,
                entityRepository,
                serviceChargeMappingRepository);
        UUID shipmentId = UUID.randomUUID();
        when(chargeRepository.findByShipment_IdWithEntity(shipmentId)).thenReturn(List.of());
        when(chargeLineItemRepository.findByCharge_IdIn(List.of())).thenReturn(List.of());
        lookups.openForShipment(shipmentId);
    }

    @AfterEach
    void tearDown() {
        lookups.closeScope();
    }

    @Test
    void dtaAccounts_secondCallWithinRow_shouldNotInvokeLoader() {
        Account account = CalculatorTestDataFactory.account(UUID.randomUUID(), "DTA");
        AtomicInteger loads = new AtomicInteger();
        List<Account> loaded = lookups.dtaAccounts("DXB", () -> {
            loads.incrementAndGet();
            return List.of(account);
        });

        assertThat(loaded).containsExactly(account);
        assertThat(lookups.dtaAccounts("DXB", () -> {
            loads.incrementAndGet();
            return List.of();
        })).containsExactly(account);
        assertThat(loads).hasValue(1);
    }

    @Test
    void dtcBillingAccount_secondCallWithinRow_shouldNotInvokeLoader() {
        Account account = CalculatorTestDataFactory.account(UUID.randomUUID(), "DTA");
        AtomicInteger loads = new AtomicInteger();
        Optional<Account> loaded = lookups.dtcBillingAccount("DXB", "12345", () -> {
            loads.incrementAndGet();
            return Optional.of(account);
        });

        assertThat(loaded).contains(account);
        assertThat(lookups.dtcBillingAccount("DXB", "12345", () -> {
            loads.incrementAndGet();
            return Optional.empty();
        })).contains(account);
        assertThat(loads).hasValue(1);
    }

    @Test
    void arMaster_withoutScope_shouldInvokeLoaderEachTime() {
        lookups.closeScope();
        ARAccount arAccount = new ARAccount();
        AtomicInteger loads = new AtomicInteger();

        lookups.arMaster("DXB", "AR-1", () -> {
            loads.incrementAndGet();
            return Optional.of(arAccount);
        });
        lookups.arMaster("DXB", "AR-1", () -> {
            loads.incrementAndGet();
            return Optional.empty();
        });

        assertThat(loads).hasValue(2);
    }

    @Test
    void arMaster_secondCallWithinRow_shouldNotInvokeLoader() {
        ARAccount arAccount = new ARAccount();
        AtomicInteger loads = new AtomicInteger();
        Optional<ARAccount> loaded = lookups.arMaster("DXB", "AR-1", () -> {
            loads.incrementAndGet();
            return Optional.of(arAccount);
        });

        assertThat(loaded).contains(arAccount);
        assertThat(lookups.arMaster("DXB", "AR-1", () -> {
            loads.incrementAndGet();
            return Optional.empty();
        })).contains(arAccount);
        assertThat(loads).hasValue(1);
    }
}
