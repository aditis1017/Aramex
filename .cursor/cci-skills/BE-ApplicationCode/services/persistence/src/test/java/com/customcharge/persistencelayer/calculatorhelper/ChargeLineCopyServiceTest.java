package com.customcharge.persistencelayer.calculatorhelper;

import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import com.customcharge.persistencelayer.dto.ServiceChargeDescriptionMappingDto;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeMappingRepository;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import com.customcharge.persistencelayer.util.FixedDecimalUtil;
import com.customcharge.persistencelayer.util.ServiceChargeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChargeLineCopyServiceTest {

    @Mock
    private ServiceChargeResolver serviceChargeResolver;
    @Mock
    private ServiceChargeMappingRepository serviceChargeMappingRepository;
    @Mock
    private ExchangeRateService exchangeRateService;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private FixedDecimalUtil fixedDecimalUtil;

    private ChargeLineCopyService service;

    @BeforeEach
    void setUp() {
        service = new ChargeLineCopyService(
                serviceChargeResolver,
                serviceChargeMappingRepository,
                exchangeRateService,
                currencyRepository,
                fixedDecimalUtil);
        lenient().when(fixedDecimalUtil.convertToFixedDecimal(any(Double.class), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void normalizeDescription_shouldLowercaseAndTrim() {
        assertThat(ChargeLineCopyService.normalizeDescription("  Customs Fee  "))
                .isEqualTo("customs fee");
    }

    @Test
    void sumLineTotals_shouldUseEffectiveAmountPerLine() {
        ChargeLineItem line = new ChargeLineItem();
        line.setAmount(new BigDecimal("10"));
        line.setContractedRate(new BigDecimal("2.5"));

        assertThat(ChargeLineCopyService.sumLineTotals(List.of(line)))
                .isEqualByComparingTo("2.5");
    }

    @Test
    void sumLineTotals_shouldUseAmountWhenContractedRateZero() {
        ChargeLineItem line = new ChargeLineItem();
        line.setAmount(new BigDecimal("10"));
        line.setContractedRate(BigDecimal.ZERO);

        assertThat(ChargeLineCopyService.sumLineTotals(List.of(line)))
                .isEqualByComparingTo("10");
    }


    private static ChargeLineCopyService.ChargeLineCopyRequest request(
            String sourceType,
            String targetType,
            Charge sourceCharge,
            Charge targetCharge,
            List<ChargeLineItem> sourceLines,
            ChargeLineCopyService.CopyMatchMode matchMode,
            ChargeLineCopyService.CopyTaxMode taxMode) {
        Account account = org.mockito.Mockito.mock(Account.class);
        lenient().when(account.getId()).thenReturn(UUID.randomUUID());
        return new ChargeLineCopyService.ChargeLineCopyRequest(
                sourceType,
                targetType,
                new Shipment(),
                "DXB",
                "RUH",
                sourceCharge,
                targetCharge,
                List.of(account),
                sourceLines,
                null,
                true,
                matchMode,
                null,
                null,
                taxMode);
    }

    private static ChargeLineItem line(String description, String amount, String contractedRate) {
        ChargeLineItem line = new ChargeLineItem();
        line.setServiceCodeDescription(description);
        line.setAmount(new BigDecimal(amount));
        line.setContractedRate(new BigDecimal(contractedRate));
        return line;
    }

    private static Charge charge(String code, String currency) {
        Charge charge = new Charge();
        charge.setChargeCode(code);
        charge.setCurrency(currency);
        return charge;
    }

    private static ServiceChargeResponse serviceCharge(String code, String description, double tax) {
        return serviceCharge(code, description, tax, null);
    }

    private static ServiceChargeResponse serviceCharge(
            String code, String description, double tax, String defaultAccountNumber) {
        ServiceChargeResponse response = org.mockito.Mockito.mock(ServiceChargeResponse.class);
        lenient().when(response.getServiceCode()).thenReturn(code);
        lenient().when(response.getDescription()).thenReturn(description);
        lenient().when(response.getTaxValue()).thenReturn(tax);
        lenient().when(response.getDefaultAccountNumber()).thenReturn(defaultAccountNumber);
        return response;
    }

    @Test
    void effectiveAmount_shouldUseContractedRateWhenPresent() {
        ChargeLineItem line = new ChargeLineItem();
        line.setAmount(new BigDecimal("2"));
        line.setContractedRate(new BigDecimal("5"));

        assertThat(ChargeLineCopyService.effectiveAmount(line)).isEqualByComparingTo("5");
    }

    @Test
    void effectiveAmount_shouldUseAmountWhenContractedRateMissingOrZero() {
        ChargeLineItem line = new ChargeLineItem();
        line.setAmount(new BigDecimal("8"));

        assertThat(ChargeLineCopyService.effectiveAmount(line)).isEqualByComparingTo("8");
        line.setContractedRate(BigDecimal.ZERO);
        assertThat(ChargeLineCopyService.effectiveAmount(line)).isEqualByComparingTo("8");
    }

    @Test
    void copy_ecs_shouldUseServiceChargeDefaultAp_notSourceLineAp() {
        Charge sourceCharge = charge("DTA", "USD");
        Charge targetCharge = charge("ECS", "USD");
        ChargeLineItem sourceLine = line("Customs", "100", "0");
        sourceLine.setServiceCode("SC1");
        sourceLine.setApAccountNumber("DTA-AP");

        ServiceChargeResponse ecsServiceCharge = serviceCharge("SC1", "ECS Customs", 0, "ECS-AP");
        when(serviceChargeResolver.resolve(any())).thenReturn(List.of(ecsServiceCharge));
        when(serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                eq("DXB"), eq("RUH"), eq("ECS")))
                .thenReturn(List.of(mapping("Customs", "SC1", "ECS Customs", "SC1")));

        List<ChargeLineItem> copied = service.copy(request(
                "DTA",
                "ECS",
                sourceCharge,
                targetCharge,
                List.of(sourceLine),
                ChargeLineCopyService.CopyMatchMode.SERVICE_CHARGE_MAPPING,
                ChargeLineCopyService.CopyTaxMode.EFFECTIVE_X));

        assertThat(copied).hasSize(1);
        assertThat(copied.get(0).getApAccountNumber()).isEqualTo("ECS-AP");
    }

    @Test
    void copy_ec3_shouldUseServiceChargeDefaultAp_notSourceLineAp() {
        Charge sourceCharge = charge("DTA", "USD");
        Charge targetCharge = charge("EC3", "USD");
        ChargeLineItem sourceLine = line("Customs", "100", "0");
        sourceLine.setServiceCode("SC1");
        sourceLine.setApAccountNumber("DTA-AP");

        ServiceChargeResponse ec3ServiceCharge = serviceCharge("SC1", "EC3 Customs", 0, "EC3-AP");
        when(serviceChargeResolver.resolve(any())).thenReturn(List.of(ec3ServiceCharge));
        when(serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                eq("DXB"), eq("RUH"), eq("EC3")))
                .thenReturn(List.of(mapping("Customs", "SC1", "EC3 Customs", "SC1")));

        List<ChargeLineItem> copied = service.copy(request(
                "DTA",
                "EC3",
                sourceCharge,
                targetCharge,
                List.of(sourceLine),
                ChargeLineCopyService.CopyMatchMode.SERVICE_CHARGE_MAPPING,
                ChargeLineCopyService.CopyTaxMode.EFFECTIVE_X));

        assertThat(copied).hasSize(1);
        assertThat(copied.get(0).getApAccountNumber()).isEqualTo("EC3-AP");
    }

    @Test
    void copy_dtaToEcs_shouldSumManyToOneMappings_andZeroUnmappedTargets() {
        Charge sourceCharge = charge("DTA", "USD");
        Charge targetCharge = charge("ECS", "USD");

        ChargeLineItem s1 = line("desc s1", "100", "0");
        s1.setServiceCode("S1");
        ChargeLineItem s3 = line("desc s3", "50", "0");
        s3.setServiceCode("S3");
        ChargeLineItem s4 = line("desc s4", "20", "0");
        s4.setServiceCode("S4");

        List<ServiceChargeResponse> ecsServiceCharges = List.of(
                serviceCharge("S1", "ecs s1", 0),
                serviceCharge("S2", "ecs s2", 0),
                serviceCharge("S3", "ecs s3", 0),
                serviceCharge("S4", "ecs s4", 0));
        when(serviceChargeResolver.resolve(any())).thenReturn(ecsServiceCharges);
        when(serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                eq("DXB"), eq("RUH"), eq("ECS")))
                .thenReturn(List.of(
                        mapping("desc s1", "S1", "ecs s2", "S2"),
                        mapping("desc s3", "S3", "ecs s2", "S2")));

        List<ChargeLineItem> copied = service.copy(request(
                "DTA",
                "ECS",
                sourceCharge,
                targetCharge,
                List.of(s1, s3, s4),
                ChargeLineCopyService.CopyMatchMode.SERVICE_CHARGE_MAPPING,
                ChargeLineCopyService.CopyTaxMode.EFFECTIVE_X));

        assertThat(copied).hasSize(4);
        assertThat(amountForServiceCode(copied, "S1")).isEqualByComparingTo("0");
        assertThat(amountForServiceCode(copied, "S2")).isEqualByComparingTo("150");
        assertThat(amountForServiceCode(copied, "S3")).isEqualByComparingTo("0");
        assertThat(amountForServiceCode(copied, "S4")).isEqualByComparingTo("0");
    }

    @Test
    void copy_dtaToEcs_shouldUseEffectiveAmountWhenContractedRatePresent() {
        Charge sourceCharge = charge("DTA", "USD");
        Charge targetCharge = charge("ECS", "USD");

        ChargeLineItem s1 = line("desc s1", "0.53", "10");
        s1.setServiceCode("S1");

        ServiceChargeResponse ecsS2 = serviceCharge("S2", "ecs s2", 0);
        when(serviceChargeResolver.resolve(any())).thenReturn(List.of(ecsS2));
        when(serviceChargeMappingRepository.findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
                eq("DXB"), eq("RUH"), eq("ECS")))
                .thenReturn(List.of(mapping("desc s1", "S1", "ecs s2", "S2")));

        List<ChargeLineItem> copied = service.copy(request(
                "DTA",
                "ECS",
                sourceCharge,
                targetCharge,
                List.of(s1),
                ChargeLineCopyService.CopyMatchMode.SERVICE_CHARGE_MAPPING,
                ChargeLineCopyService.CopyTaxMode.EFFECTIVE_X));

        assertThat(amountForServiceCode(copied, "S2")).isEqualByComparingTo("10");
    }

    @Test
    void copy_shouldReturnEmpty_whenRequestInvalid() {
        assertThat(service.copy(null)).isEmpty();
        assertThat(service.copy(request(
                "DTA", "ECS", charge("DTA", "USD"), null, List.of(), null, null))).isEmpty();
    }

    @Test
    void copy_descriptionMode_shouldCopyMatchingLineAmount() {
        Charge sourceCharge = charge("DTA", "USD");
        Charge targetCharge = charge("DTC", "USD");
        ChargeLineItem sourceLine = line("Customs Fee", "100", "0");
        sourceLine.setServiceCode("SC1");

        ServiceChargeResponse targetServiceCharge = serviceCharge("SC1", "Customs Fee", 5);
        when(serviceChargeResolver.resolve(any())).thenReturn(List.of(targetServiceCharge));

        List<ChargeLineItem> copied = service.copy(request(
                "DTA",
                "DTC",
                sourceCharge,
                targetCharge,
                List.of(sourceLine),
                ChargeLineCopyService.CopyMatchMode.DESCRIPTION,
                ChargeLineCopyService.CopyTaxMode.AMOUNT_ONLY));

        assertThat(copied).hasSize(1);
        assertThat(copied.get(0).getAmount()).isEqualByComparingTo("100");
        assertThat(copied.get(0).getPreTaxableAmount()).isEqualByComparingTo("105");
    }

    @Test
    void convertCopyAmount_shouldConvertBetweenDifferentCurrencies() {
        Charge sourceCharge = charge("DTA", "USD");
        Charge targetCharge = charge("DTC", "SAR");
        when(exchangeRateService.convertAmount(100.0, "USD", "SAR", null, "fx"))
                .thenReturn(new CurrencyConversionResult(375.0, 3.75));

        ChargeLineCopyService.CopyAmountFxResult result = service.convertCopyAmount(
                new BigDecimal("100"),
                new Shipment(),
                sourceCharge,
                targetCharge,
                "DTA",
                "DTC",
                "DXB",
                "RUH",
                "fx",
                false);

        assertThat(result.convertedAmount()).isEqualByComparingTo("375");
        assertThat(result.exchangeRate()).isEqualTo(3.75);
    }

    @Test
    void convertCopyAmount_shouldDefaultCurrencyWhenChargeCurrencyMissing() {
        Charge sourceCharge = charge("ECD", "");
        Charge targetCharge = charge("DTC", "");
        when(currencyRepository.findActiveByEntityCode("RUH"))
                .thenReturn(java.util.Optional.of(currency("SAR")));

        ChargeLineCopyService.CopyAmountFxResult result = service.convertCopyAmount(
                new BigDecimal("50"),
                new Shipment(),
                sourceCharge,
                targetCharge,
                "ECD",
                "DTC",
                "DXB",
                "RUH",
                null,
                false);

        assertThat(result.convertedAmount()).isEqualByComparingTo("50");
        assertThat(result.exchangeRate()).isEqualTo(1.0);
    }

    @Test
    void sumLineTotals_shouldIgnoreNullLines() {
        assertThat(ChargeLineCopyService.sumLineTotals(null)).isEqualByComparingTo("0");
        ChargeLineItem line = line("desc", "10", "0");
        java.util.ArrayList<ChargeLineItem> lines = new java.util.ArrayList<>();
        lines.add(null);
        lines.add(line);
        assertThat(ChargeLineCopyService.sumLineTotals(lines)).isEqualByComparingTo("10");
    }

    private static com.customcharge.persistencelayer.entities.Currency currency(String code) {
        com.customcharge.persistencelayer.entities.Currency currency =
                new com.customcharge.persistencelayer.entities.Currency();
        currency.setCurrencyCode(code);
        return currency;
    }

    private static BigDecimal amountForServiceCode(List<ChargeLineItem> lines, String serviceCode) {
        return lines.stream()
                .filter(line -> serviceCode.equals(line.getServiceCode()))
                .map(ChargeLineItem::getAmount)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private static ServiceChargeDescriptionMappingDto mapping(String from, String fromCode, String to, String toCode) {
        return new ServiceChargeDescriptionMappingDto(from, fromCode, to, toCode);
    }
}
