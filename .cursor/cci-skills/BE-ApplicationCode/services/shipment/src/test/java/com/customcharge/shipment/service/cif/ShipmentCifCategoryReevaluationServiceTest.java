package com.customcharge.shipment.service.cif;

import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.ShipmentRuleAssignmentRepository;
import com.customcharge.shipment.config.ShipmentProperties;
import com.customcharge.shipment.constants.ShipmentConstants;
import com.customcharge.shipment.service.mapper.ShipmentToDtoMapper;
import com.customcharge.shipment.service.rules.CategoryTypeRuleResolver;
import com.customcharge.shipment.service.rules.RuleResolverSupport;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentCifCategoryReevaluationServiceTest {

    @Mock
    private ShipmentToDtoMapper shipmentToDtoMapper;
    @Mock
    private CategoryTypeRuleResolver categoryTypeRuleResolver;
    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private ShipmentRuleAssignmentRepository shipmentRuleAssignmentRepository;
    @Mock
    private ObjectProvider<com.customcharge.persistencelayer.util.ExchangeRateService> exchangeRateService;
    @Mock
    private ShipmentProperties shipmentProperties;

    @InjectMocks
    private ShipmentCifCategoryReevaluationService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(shipmentProperties.getMonetaryScale()).thenReturn(4);
    }

    @Test
    void evaluate_shouldSkipAlreadyExemptedShipment() {
        Shipment shipment = shipment("Exempted", 100.0, 10.0);

        ShipmentCifCategoryReevaluationService.CategoryReevaluationResult result = service.evaluate(shipment);

        assertThat(result.mode()).isEqualTo(Sh267ChargeRecalculationMode.SKIP_ALREADY_EXEMPTED);
    }

    @Test
    void evaluate_shouldUseCostOfGoodsPlusCifForDrools() {
        Shipment shipment = shipment("LV", 100.0, 25.0);
        shipment.setCustomsCurrency("TND");
        shipment.setCifValueCurrency("TND");
        ShipmentDto dto = new ShipmentDto();
        when(shipmentToDtoMapper.mapForCategoryRules(shipment)).thenReturn(dto);
        when(categoryTypeRuleResolver.resolveCategoryType(dto)).thenReturn(Optional.empty());
        when(chargeRepository.findByShipment_IdWithEntity(shipment.getId())).thenReturn(List.of());

        service.evaluate(shipment);

        assertThat(dto.getCostOfGoods()).isEqualTo(125.0);
    }

    @Test
    void evaluate_shouldReturnLvToHvNormalWhenCategoryChangesOnEstimateShipment() {
        Shipment shipment = shipment("LV", 100.0, 25.0);
        ShipmentDto dto = new ShipmentDto();
        when(shipmentToDtoMapper.mapForCategoryRules(shipment)).thenReturn(dto);
        when(categoryTypeRuleResolver.resolveCategoryType(dto))
                .thenReturn(Optional.of(new RuleResolverSupport.ResolvedRule(
                        ShipmentConstants.CATEGORY_TYPE_HV, null)));
        when(chargeRepository.findByShipment_IdWithEntity(shipment.getId())).thenReturn(List.of());

        ShipmentCifCategoryReevaluationService.CategoryReevaluationResult result = service.evaluate(shipment);

        assertThat(result.mode()).isEqualTo(Sh267ChargeRecalculationMode.LV_TO_HV_NORMAL);
        assertThat(shipment.getCategoryType()).isEqualTo(ShipmentConstants.CATEGORY_TYPE_HV);
    }

    @Test
    void evaluate_shouldReturnActualizedWhenShipmentHasActualizedCharges() {
        Shipment shipment = shipment("LV", 100.0, 25.0);
        ShipmentDto dto = new ShipmentDto();
        Charge actualized = new Charge();
        actualized.setStatus("ACTUALIZED");
        when(shipmentToDtoMapper.mapForCategoryRules(shipment)).thenReturn(dto);
        when(categoryTypeRuleResolver.resolveCategoryType(dto)).thenReturn(Optional.empty());
        when(chargeRepository.findByShipment_IdWithEntity(shipment.getId())).thenReturn(List.of(actualized));

        ShipmentCifCategoryReevaluationService.CategoryReevaluationResult result = service.evaluate(shipment);

        assertThat(result.mode()).isEqualTo(Sh267ChargeRecalculationMode.ACTUALIZED_CIF_ONLY);
    }

    @Test
    void evaluate_shouldReturnActualizedLvToHvWhenCategoryChangesOnActualizedShipment() {
        Shipment shipment = shipment("LV", 100.0, 25.0);
        ShipmentDto dto = new ShipmentDto();
        Charge actualized = new Charge();
        actualized.setStatus("ACTUALIZED");
        when(shipmentToDtoMapper.mapForCategoryRules(shipment)).thenReturn(dto);
        when(categoryTypeRuleResolver.resolveCategoryType(dto))
                .thenReturn(Optional.of(new RuleResolverSupport.ResolvedRule(
                        ShipmentConstants.CATEGORY_TYPE_HV, null)));
        when(chargeRepository.findByShipment_IdWithEntity(shipment.getId())).thenReturn(List.of(actualized));

        ShipmentCifCategoryReevaluationService.CategoryReevaluationResult result = service.evaluate(shipment);

        assertThat(result.mode()).isEqualTo(Sh267ChargeRecalculationMode.ACTUALIZED_LV_TO_HV);
        assertThat(shipment.getCategoryType()).isEqualTo(ShipmentConstants.CATEGORY_TYPE_HV);
    }

    @Test
    void evaluate_shouldReturnNewlyExemptedWhenDroolsResolvesExempted() {
        Shipment shipment = shipment("LV", 100.0, 25.0);
        ShipmentDto dto = new ShipmentDto();
        when(shipmentToDtoMapper.mapForCategoryRules(shipment)).thenReturn(dto);
        when(categoryTypeRuleResolver.resolveCategoryType(dto))
                .thenReturn(Optional.of(new RuleResolverSupport.ResolvedRule(
                        ShipmentConstants.CATEGORY_TYPE_EXEMPTED, null)));

        ShipmentCifCategoryReevaluationService.CategoryReevaluationResult result = service.evaluate(shipment);

        assertThat(result.mode()).isEqualTo(Sh267ChargeRecalculationMode.NEWLY_EXEMPTED);
        assertThat(shipment.getCategoryType()).isEqualTo(ShipmentConstants.CATEGORY_TYPE_EXEMPTED);
    }

    @Test
    void evaluate_shouldReturnUnchanged_whenShipmentNull() {
        ShipmentCifCategoryReevaluationService.CategoryReevaluationResult result = service.evaluate(null);

        assertThat(result.mode()).isEqualTo(Sh267ChargeRecalculationMode.CATEGORY_UNCHANGED_NORMAL);
    }

    @Test
    void evaluate_shouldReturnUnchanged_whenCategoryUnchangedOnEstimate() {
        Shipment shipment = shipment("LV", 100.0, 0.0);
        ShipmentDto dto = new ShipmentDto();
        when(shipmentToDtoMapper.mapForCategoryRules(shipment)).thenReturn(dto);
        when(categoryTypeRuleResolver.resolveCategoryType(dto)).thenReturn(Optional.empty());
        when(chargeRepository.findByShipment_IdWithEntity(shipment.getId())).thenReturn(List.of());

        ShipmentCifCategoryReevaluationService.CategoryReevaluationResult result = service.evaluate(shipment);

        assertThat(result.mode()).isEqualTo(Sh267ChargeRecalculationMode.CATEGORY_UNCHANGED_NORMAL);
    }

    @Test
    void evaluate_shouldConvertCifToCustomsCurrency_whenCurrenciesDiffer() {
        Shipment shipment = shipment("LV", 100.0, 50.0);
        shipment.setCifValueCurrency("USD");
        shipment.setCustomsCurrency("TND");
        ShipmentDto dto = new ShipmentDto();
        ExchangeRateService exchangeRate = org.mockito.Mockito.mock(ExchangeRateService.class);
        when(shipmentToDtoMapper.mapForCategoryRules(shipment)).thenReturn(dto);
        when(categoryTypeRuleResolver.resolveCategoryType(dto)).thenReturn(Optional.empty());
        when(chargeRepository.findByShipment_IdWithEntity(shipment.getId())).thenReturn(List.of());
        when(exchangeRateService.getIfAvailable()).thenReturn(exchangeRate);
        when(exchangeRate.convertAmount(50.0, "USD", "TND"))
                .thenReturn(new CurrencyConversionResult(150.0, 3.0));

        service.evaluate(shipment);

        assertThat(dto.getCostOfGoods()).isEqualTo(250.0);
    }

    @Test
    void evaluate_shouldUseRawCif_whenExchangeServiceUnavailable() {
        Shipment shipment = shipment("LV", 100.0, 50.0);
        shipment.setCifValueCurrency("USD");
        shipment.setCustomsCurrency("TND");
        ShipmentDto dto = new ShipmentDto();
        when(shipmentToDtoMapper.mapForCategoryRules(shipment)).thenReturn(dto);
        when(categoryTypeRuleResolver.resolveCategoryType(dto)).thenReturn(Optional.empty());
        when(chargeRepository.findByShipment_IdWithEntity(shipment.getId())).thenReturn(List.of());
        when(exchangeRateService.getIfAvailable()).thenReturn(null);

        service.evaluate(shipment);

        assertThat(dto.getCostOfGoods()).isEqualTo(150.0);
    }

    @Test
    void evaluate_shouldTreatShipmentAsNotActualized_whenShipmentIdMissing() {
        Shipment shipment = shipment("LV", 100.0, 25.0);
        ReflectionTestUtils.setField(shipment, "id", null);
        ShipmentDto dto = new ShipmentDto();
        Charge actualized = new Charge();
        actualized.setStatus("ACTUALIZED");
        when(shipmentToDtoMapper.mapForCategoryRules(shipment)).thenReturn(dto);
        when(categoryTypeRuleResolver.resolveCategoryType(dto)).thenReturn(Optional.empty());

        ShipmentCifCategoryReevaluationService.CategoryReevaluationResult result = service.evaluate(shipment);

        assertThat(result.mode()).isEqualTo(Sh267ChargeRecalculationMode.CATEGORY_UNCHANGED_NORMAL);
    }

    private static Shipment shipment(String categoryType, double costOfGoods, double cifValue) {
        Shipment shipment = new Shipment();
        ReflectionTestUtils.setField(shipment, "id", UUID.randomUUID());
        shipment.setHawbNumber("HAWB-1");
        shipment.setCategoryType(categoryType);
        shipment.setCostOfGoods(costOfGoods);
        shipment.setCifValue(cifValue);
        return shipment;
    }
}
