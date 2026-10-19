package com.customcharge.shipment.service.charge;

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
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChargeLineItemTaxSupportTest {

    @Mock
    private ServiceChargeAccountResolver serviceChargeAccountResolver;
    @Mock
    private ServiceChargeRepository serviceChargeRepository;
    @Mock
    private ServiceCodesRepository serviceCodesRepository;
    @Mock
    private AccountRedisChargeDataService accountRedisChargeDataService;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private ShipmentChargeAmountSupport chargeAmountSupport;

    @InjectMocks
    private ChargeLineItemTaxSupport chargeLineItemTaxSupport;

    @AfterEach
    void clearProcessingContext() {
        ShipmentProcessingContext.clear();
    }

    @Test
    void computeFromRawRate_shouldStoreBasisExchangeRate_forEcd() {
        Shipment shipment = new Shipment();
        shipment.setConsigneeNumber("AR-1");
        Charge charge = new Charge();
        charge.setChargeCode("ECD");
        Account rateSheetAccount = accountWithEntity("RUH");

        ServiceCodes master = new ServiceCodes();
        UUID masterId = UUID.randomUUID();
        ReflectionTestUtils.setField(master, "id", masterId);
        master.setFetchTax(false);

        ServiceCharge accountCharge = new ServiceCharge();
        accountCharge.setTaxValue(15.0);

        when(chargeAmountSupport.convertAndFormatWithRate(eq(BigDecimal.valueOf(100)), any(), any()))
                .thenReturn(new ConvertedChargeAmount(BigDecimal.valueOf(100), 1.0));
        when(accountRedisChargeDataService.findServiceCharge(
                eq(rateSheetAccount), eq("RUH"), any(), eq(master))).thenReturn(Optional.of(accountCharge));
        when(entityRepository.findByCodeIgnoreCase("RUH")).thenReturn(Optional.of(entityWithTax(10.0)));
        when(chargeAmountSupport.formatAmount(any(BigDecimal.class), any(Shipment.class), any(Charge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChargeLineItemTaxSupport.TaxedLineAmount result = chargeLineItemTaxSupport.computeFromRawRate(
                BigDecimal.valueOf(100), shipment, charge, rateSheetAccount, "RUH", "DUTY", master, 4.7);

        assertThat(result.preTaxableAmount()).isEqualByComparingTo("100");
        assertThat(result.amount()).isEqualByComparingTo("115");
        assertThat(result.exchangeRate()).isEqualTo(4.7);
    }

    @Test
    void computeFromRawRate_shouldApplyTaxFromAccountCharge() {
        Shipment shipment = new Shipment();
        shipment.setConsigneeNumber("AR-1");
        Charge charge = new Charge();
        charge.setChargeCode("ECD");
        Account rateSheetAccount = accountWithEntity("RUH");

        ServiceCodes master = new ServiceCodes();
        UUID masterId = UUID.randomUUID();
        ReflectionTestUtils.setField(master, "id", masterId);
        master.setFetchTax(false);

        ServiceCharge accountCharge = new ServiceCharge();
        accountCharge.setTaxValue(15.0);

        when(chargeAmountSupport.convertAndFormatWithRate(eq(BigDecimal.valueOf(100)), any(), any()))
                .thenReturn(new ConvertedChargeAmount(BigDecimal.valueOf(100), null));
        when(accountRedisChargeDataService.findServiceCharge(
                eq(rateSheetAccount), eq("RUH"), any(), eq(master))).thenReturn(Optional.of(accountCharge));
        when(entityRepository.findByCodeIgnoreCase("RUH")).thenReturn(Optional.of(entityWithTax(10.0)));
        when(chargeAmountSupport.formatAmount(any(BigDecimal.class), any(Shipment.class), any(Charge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChargeLineItemTaxSupport.TaxedLineAmount result = chargeLineItemTaxSupport.computeFromRawRate(
                BigDecimal.valueOf(100), shipment, charge, rateSheetAccount, "RUH", "DUTY", master);

        assertThat(result.preTaxableAmount()).isEqualByComparingTo("100");
        assertThat(result.amount()).isEqualByComparingTo("115");
    }

    @Test
    void computeFromRawRate_shouldUseArAccountOnlyForDtcTax() {
        Shipment shipment = new Shipment();
        shipment.setConsigneeNumber("AR-1");
        Charge charge = new Charge();
        charge.setChargeCode("DTC");
        Account rateSheetAccount = accountWithEntity("RUH");
        Account arAccount = accountWithEntity("RUH");
        arAccount.setChargeType("DTC");

        ServiceCodes master = new ServiceCodes();
        UUID masterId = UUID.randomUUID();
        ReflectionTestUtils.setField(master, "id", masterId);
        master.setFetchTax(true);

        ServiceCharge arCharge = new ServiceCharge();
        arCharge.setTaxValue(10.0);

        when(chargeAmountSupport.convertAndFormatWithRate(any(), any(), any()))
                .thenReturn(new ConvertedChargeAmount(BigDecimal.valueOf(100), null));
        when(serviceChargeAccountResolver.resolveArBillingAccount(shipment, "RUH", "DTC"))
                .thenReturn(Optional.of(arAccount));
        when(accountRedisChargeDataService.findServiceCharge(
                eq(arAccount), eq("RUH"), eq("DTC"), eq(master))).thenReturn(Optional.of(arCharge));
        when(entityRepository.findByCodeIgnoreCase("RUH")).thenReturn(Optional.of(entityWithTax(15.0)));
        when(chargeAmountSupport.formatAmount(any(BigDecimal.class), any(Shipment.class), any(Charge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChargeLineItemTaxSupport.TaxedLineAmount result = chargeLineItemTaxSupport.computeFromRawRate(
                BigDecimal.valueOf(100), shipment, charge, rateSheetAccount, "RUH", "DUTY", master);

        assertThat(result.preTaxableAmount()).isEqualByComparingTo("100");
        assertThat(result.amount()).isEqualByComparingTo("110");
    }

    @Test
    void computeContractedRate_shouldNotApplyTax() {
        Shipment shipment = new Shipment();
        Charge charge = new Charge();
        charge.setChargeCode("DTC");

        when(chargeAmountSupport.convertAndFormatWithRate(eq(BigDecimal.valueOf(100)), any(), any()))
                .thenReturn(new ConvertedChargeAmount(BigDecimal.valueOf(100), null));

        BigDecimal contractedRate = chargeLineItemTaxSupport.computeContractedRate(
                BigDecimal.valueOf(100), shipment, charge);

        assertThat(contractedRate).isEqualByComparingTo("100");
    }

    @Test
    void computeFromRawRate_shouldReturnZeroWhenForced() {
        ChargeLineItemTaxSupport.TaxedLineAmount result = chargeLineItemTaxSupport.computeFromRawRate(
                BigDecimal.valueOf(100),
                new Shipment(),
                new Charge(),
                new Account(),
                "RUH",
                "DUTY",
                null,
                true);

        assertThat(result.preTaxableAmount()).isEqualByComparingTo("0");
        assertThat(result.amount()).isEqualByComparingTo("0");
    }

    @Test
    void applyToLineItem_shouldCopyTaxedValues() {
        ChargeLineItem lineItem = new ChargeLineItem();
        ChargeLineItemTaxSupport.TaxedLineAmount taxed =
                new ChargeLineItemTaxSupport.TaxedLineAmount(
                        BigDecimal.valueOf(80), BigDecimal.valueOf(92), 1.5);

        chargeLineItemTaxSupport.applyToLineItem(lineItem, taxed);

        assertThat(lineItem.getAmount()).isEqualByComparingTo("80");
        assertThat(lineItem.getPreTaxableAmount()).isEqualByComparingTo("92");
        assertThat(lineItem.getExchangeRate()).isEqualTo(1.5);
    }

    @Test
    void formatWithTax_shouldApplyPercentAndFormat() {
        Shipment shipment = new Shipment();
        Charge charge = new Charge();
        charge.setChargeCode("ECD");
        when(chargeAmountSupport.formatAmount(any(BigDecimal.class), any(Shipment.class), any(Charge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal result = chargeLineItemTaxSupport.formatWithTax(
                BigDecimal.valueOf(100), 10.0, shipment, charge);

        assertThat(result).isEqualByComparingTo("110");
    }

    @Test
    void resolveTaxPercent_shouldLoadMasterFromRepositoryWhenMissing() {
        Shipment shipment = new Shipment();
        Charge charge = new Charge();
        charge.setChargeCode("ECD");
        Account account = accountWithEntity("RUH");
        ServiceCodes master = new ServiceCodes();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());
        master.setFetchTax(false);
        when(accountRedisChargeDataService.findServiceCodeMaster("RUH", "ECD", "DUTY"))
                .thenReturn(Optional.of(master));
        when(accountRedisChargeDataService.findServiceCharge(
                eq(account), eq("RUH"), any(), eq(master))).thenReturn(Optional.empty());
        when(entityRepository.findByCodeIgnoreCase("RUH")).thenReturn(Optional.of(entityWithTax(5.0)));

        Double tax = chargeLineItemTaxSupport.resolveTaxPercent(
                shipment, charge, account, "RUH", "DUTY", null);

        assertThat(tax).isNotNull();
    }

    @Test
    void resolveTaxPercent_shouldLoadMasterFromDbWhenRedisMissAndNoContext() {
        Shipment shipment = new Shipment();
        Charge charge = new Charge();
        charge.setChargeCode("ECD");
        Account account = accountWithEntity("RUH");
        ServiceCodes master = new ServiceCodes();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());
        master.setFetchTax(false);

        when(accountRedisChargeDataService.findServiceCodeMaster("RUH", "ECD", "DUTY"))
                .thenReturn(Optional.empty());
        when(serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                "RUH", "DUTY", "ECD")).thenReturn(Optional.of(master));
        when(accountRedisChargeDataService.findServiceCharge(
                eq(account), eq("RUH"), any(), eq(master))).thenReturn(Optional.empty());
        when(entityRepository.findByCodeIgnoreCase("RUH")).thenReturn(Optional.of(entityWithTax(5.0)));

        Double tax = chargeLineItemTaxSupport.resolveTaxPercent(
                shipment, charge, account, "RUH", "DUTY", null);

        assertThat(tax).isNotNull();
        verify(serviceCodesRepository).findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                "RUH", "DUTY", "ECD");
    }

    private static Entity entityWithTax(double tax) {
        Entity entity = new Entity();
        entity.setTaxValue(tax);
        return entity;
    }

    private static Account accountWithEntity(String code) {
        Account account = new Account();
        Entity entity = new Entity();
        entity.setCode(code);
        account.setEntity(entity);
        return account;
    }
}
