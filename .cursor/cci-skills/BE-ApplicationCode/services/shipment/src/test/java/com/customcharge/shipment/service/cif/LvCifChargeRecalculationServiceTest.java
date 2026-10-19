package com.customcharge.shipment.service.cif;

import com.customcharge.persistencelayer.accountRedis.AccountRedisChargeDataService;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.RateSheetItemRate;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.enums.PercentageOf;
import com.customcharge.persistencelayer.enums.RateType;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.shipment.service.charge.ChargeLineItemTaxSupport;
import com.customcharge.shipment.service.charge.ServiceCodeEligibilityService;
import com.customcharge.shipment.service.charge.ShipmentBillingEntitySupport;
import com.customcharge.shipment.service.charge.ShipmentChargeAmountSupport;
import com.customcharge.shipment.service.charge.processing.calculation.RateAmountCalculator;
import com.customcharge.shipment.service.charge.processing.calculation.RateAmountCalculator.CalculatedRateAmount;
import com.customcharge.shipment.service.charge.processing.model.ResolvedRateSheet;
import com.customcharge.shipment.service.charge.processing.ratesheet.RateSheetLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LvCifChargeRecalculationServiceTest {

    @Mock
    private RateSheetLookupService rateSheetLookupService;
    @Mock
    private ChargeLineItemRepository chargeLineItemRepository;
    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private RateAmountCalculator rateAmountCalculator;
    @Mock
    private ShipmentChargeAmountSupport chargeAmountSupport;
    @Mock
    private ShipmentBillingEntitySupport shipmentBillingEntitySupport;
    @Mock
    private ServiceCodeEligibilityService serviceCodeEligibilityService;
    @Mock
    private AccountRedisChargeDataService accountRedisChargeDataService;
    @Mock
    private ChargeLineItemTaxSupport chargeLineItemTaxSupport;

    @InjectMocks
    private LvCifChargeRecalculationService service;

    @org.junit.jupiter.api.BeforeEach
    void stubApplyToLineItem() {
        lenient().doAnswer(invocation -> {
            ChargeLineItem item = invocation.getArgument(0);
            ChargeLineItemTaxSupport.TaxedLineAmount taxed = invocation.getArgument(1);
            item.setAmount(taxed.preTaxableAmount());
            item.setPreTaxableAmount(taxed.amount());
            item.setExchangeRate(taxed.exchangeRate());
            return null;
        }).when(chargeLineItemTaxSupport).applyToLineItem(any(), any());
    }

    @Test
    void recalculate_shouldSkipChargeWhenRateSheetHasNoCif() {
        Shipment shipment = shipment();
        Charge ecd = charge("ECD");
        when(rateSheetLookupService.resolveForShipmentCharge(shipment, "ECD"))
                .thenReturn(Optional.of(resolvedRateSheetWithoutCif()));

        service.recalculate(shipment, List.of(ecd));

        verify(chargeLineItemRepository, never()).saveAll(any());
    }

    @Test
    void recalculate_shouldUpdateCifLineInPlaceWithoutDeleting() {
        Shipment shipment = shipment();
        Charge ecd = charge("ECD");
        ChargeLineItem dutyLine = lineItem("DUTY", "100");

        RateSheetItem cifItem = cifRateSheetItem("DUTY");
        ResolvedRateSheet resolved = resolvedRateSheet(cifItem);

        when(rateSheetLookupService.resolveForShipmentCharge(shipment, "ECD"))
                .thenReturn(Optional.of(resolved));
        when(chargeLineItemRepository.findByCharge_Id(any())).thenReturn(List.of(dutyLine));
        when(serviceCodeEligibilityService.isRateSheetItemEligible(any(), any(), eq("ECD"), eq(cifItem)))
                .thenReturn(true);
        when(rateAmountCalculator.calculateWithBasisRate(cifItem, shipment, "ECD"))
                .thenReturn(new CalculatedRateAmount(BigDecimal.valueOf(150), 4.7));
        when(accountRedisChargeDataService.findServiceCodeMaster(any(), eq("ECD"), eq("DUTY")))
                .thenReturn(Optional.empty());
        when(chargeLineItemTaxSupport.computeFromRawRate(
                eq(BigDecimal.valueOf(150)), eq(shipment), eq(ecd), any(), any(), eq("DUTY"), eq(null),
                eq(4.7)))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        BigDecimal.valueOf(150), BigDecimal.valueOf(172.5), null));

        lenient().when(chargeAmountSupport.formatAmount(any(BigDecimal.class), eq(shipment), eq(ecd)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> {
            Charge charge = invocation.getArgument(1);
            charge.setEstimateAmount(BigDecimal.valueOf(150));
            return null;
        }).when(chargeAmountSupport).applyChargeHeaderAmounts(eq(shipment), eq(ecd), any());

        service.recalculate(shipment, List.of(ecd));

        verify(chargeLineItemRepository).saveAll(List.of(dutyLine));
        verify(chargeRepository).saveAll(List.of(ecd));
        assertThat(ecd.getStatus()).isEqualTo("DUE");
        assertThat(dutyLine.getAmount()).isEqualByComparingTo("150");
        assertThat(dutyLine.getPreTaxableAmount()).isEqualByComparingTo("172.5");
    }

    @Test
    void recalculate_shouldRecalculateRevenueIndependentlyFromCost() {
        Shipment shipment = shipment();
        Charge ecd = charge("ECD");
        Charge dtc = charge("DTC");

        ChargeLineItem costDuty = lineItem("DUTY", "150");
        ChargeLineItem revenueDuty = lineItem("DUTY", "100");

        RateSheetItem costCifItem = cifRateSheetItem("DUTY");
        RateSheetItem revenueCifItem = cifRateSheetItem("DUTY");
        ResolvedRateSheet costResolved = resolvedRateSheet(costCifItem);
        ResolvedRateSheet revenueResolved = resolvedRateSheet(revenueCifItem);

        when(rateSheetLookupService.resolveForShipmentCharge(shipment, "ECD"))
                .thenReturn(Optional.of(costResolved));
        when(rateSheetLookupService.resolveForShipmentCharge(shipment, "DTC"))
                .thenReturn(Optional.of(revenueResolved));
        when(chargeLineItemRepository.findByCharge_Id(any()))
                .thenReturn(List.of(costDuty))
                .thenReturn(List.of(costDuty))
                .thenReturn(List.of(revenueDuty))
                .thenReturn(List.of(revenueDuty));
        when(serviceCodeEligibilityService.isRateSheetItemEligible(any(), any(), eq("ECD"), eq(costCifItem)))
                .thenReturn(true);
        when(serviceCodeEligibilityService.isRateSheetItemEligible(any(), any(), eq("DTC"), eq(revenueCifItem)))
                .thenReturn(true);
        when(rateAmountCalculator.calculateWithBasisRate(costCifItem, shipment, "ECD"))
                .thenReturn(new CalculatedRateAmount(BigDecimal.valueOf(150), 4.7));
        when(rateAmountCalculator.calculateWithBasisRate(revenueCifItem, shipment, "DTC"))
                .thenReturn(new CalculatedRateAmount(BigDecimal.valueOf(80), 4.7));
        when(accountRedisChargeDataService.findServiceCodeMaster(any(), any(), eq("DUTY")))
                .thenReturn(Optional.empty());
        when(chargeLineItemTaxSupport.computeFromRawRate(
                eq(BigDecimal.valueOf(150)), eq(shipment), eq(ecd), any(), any(), eq("DUTY"), eq(null),
                eq(4.7)))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        BigDecimal.valueOf(150), BigDecimal.valueOf(150), 4.7));
        when(chargeLineItemTaxSupport.computeFromRawRate(
                eq(BigDecimal.valueOf(80)), eq(shipment), eq(dtc), any(), any(), eq("DUTY"), eq(null),
                eq(4.7)))
                .thenReturn(new ChargeLineItemTaxSupport.TaxedLineAmount(
                        BigDecimal.valueOf(80), BigDecimal.valueOf(80), null));
        lenient().when(chargeAmountSupport.formatAmount(any(BigDecimal.class), eq(shipment), any(Charge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.recalculate(shipment, List.of(ecd, dtc));

        assertThat(costDuty.getAmount()).isEqualByComparingTo("150");
        assertThat(revenueDuty.getAmount()).isEqualByComparingTo("80");
    }

    private static Shipment shipment() {
        Shipment shipment = new Shipment();
        shipment.setHawbNumber("HAWB-1");
        shipment.setCategoryType("LV");
        shipment.setShipmentType("DDU");
        return shipment;
    }

    private static Charge charge(String code) {
        Charge charge = new Charge();
        charge.setChargeCode(code);
        return charge;
    }

    private static ChargeLineItem lineItem(String serviceCode, String amount) {
        ChargeLineItem line = new ChargeLineItem();
        line.setServiceCode(serviceCode);
        line.setAmount(new BigDecimal(amount));
        return line;
    }

    private static RateSheetItem cifRateSheetItem(String serviceCode) {
        RateSheetItemRate rate = new RateSheetItemRate();
        rate.setRateType(RateType.PERCENTAGE);
        rate.setValue(BigDecimal.TEN);
        rate.setPercentageOf(PercentageOf.CIF_VALUE);
        RateSheetItem item = new RateSheetItem();
        item.setServiceCode(serviceCode);
        item.setRates(List.of(rate));
        return item;
    }

    private static ResolvedRateSheet resolvedRateSheet(RateSheetItem item) {
        Account account = new Account();
        return new ResolvedRateSheet(account, new RateSheet(), List.of(item));
    }

    private static ResolvedRateSheet resolvedRateSheetWithoutCif() {
        RateSheetItemRate rate = new RateSheetItemRate();
        rate.setRateType(RateType.FIXED);
        rate.setValue(BigDecimal.ONE);
        RateSheetItem item = new RateSheetItem();
        item.setServiceCode("OF");
        item.setRates(List.of(rate));
        return resolvedRateSheet(item);
    }
}
