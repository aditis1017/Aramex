package com.customcharge.chargescalculator.service.charge;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.service.batch.RecordProcessingLookups;
import com.customcharge.chargescalculator.service.calculation.ShipmentCostBasisCalculator;
import com.customcharge.chargescalculator.service.ratesheet.PercentageRateSpec;
import com.customcharge.chargescalculator.service.ratesheet.RateSheetAmountBreakdown;
import com.customcharge.chargescalculator.constants.ChargeCreationConstants;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Country;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.enums.PercentageOf;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.constant.DomEntityConstants;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.repository.CountryRepository;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import com.customcharge.persistencelayer.util.DomEntityConfigResolver;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import com.customcharge.persistencelayer.util.FixedDecimalUtil;
import com.customcharge.persistencelayer.constant.DomEntityConstants;
import com.customcharge.persistencelayer.util.DomEntityConfigResolver;
import com.customcharge.persistencelayer.util.RevenueBillingAccountResolver;
import com.customcharge.persistencelayer.util.ServiceChargeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChargeLineSupportTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ARAccountRepository arAccountRepository;
    @Mock
    private ServiceChargeResolver serviceChargeResolver;
    @Mock
    private FixedDecimalUtil fixedDecimalUtil;
    @Mock
    private ExchangeRateService exchangeRateService;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private ObjectProvider<CacheService> cacheServiceProvider;
    @Mock
    private ShipmentCostBasisCalculator shipmentCostBasisCalculator;
    @Mock
    private RevenueBillingAccountResolver revenueBillingAccountResolver;
    @Mock
    private DomEntityConfigResolver domEntityConfigResolver;
    @Mock
    private RecordProcessingLookups recordProcessingLookups;

    private ChargeLineSupport chargeLineSupport;

    private Shipment shipment;
    private Account account;

    @BeforeEach
    void setUp() {
        shipment = CalculatorTestDataFactory.shipment();
        account = CalculatorTestDataFactory.account(UUID.randomUUID(), "DTC");
        chargeLineSupport = new ChargeLineSupport(
                accountRepository,
                arAccountRepository,
                serviceChargeResolver,
                fixedDecimalUtil,
                exchangeRateService,
                currencyRepository,
                countryRepository,
                cacheServiceProvider,
                shipmentCostBasisCalculator,
                revenueBillingAccountResolver,
                domEntityConfigResolver,
                recordProcessingLookups);
        lenient().when(domEntityConfigResolver.resolveDomEntityCode(any()))
                .thenReturn(DomEntityConstants.DEFAULT_DOM_ENTITY_CODE);
        lenient().when(recordProcessingLookups.entitySettingsAccounts(any(), any(), any()))
                .thenAnswer(invocation -> invocation.<java.util.function.Supplier<List<Account>>>getArgument(2).get());
        lenient().when(recordProcessingLookups.dtaAccounts(any(), any()))
                .thenAnswer(invocation -> invocation.<java.util.function.Supplier<List<Account>>>getArgument(1).get());
        lenient().when(recordProcessingLookups.arMaster(any(), any(), any()))
                .thenAnswer(invocation -> invocation.<java.util.function.Supplier<Optional<ARAccount>>>getArgument(2).get());
        lenient().when(recordProcessingLookups.dtcBillingAccount(any(), any(), any()))
                .thenAnswer(invocation -> invocation.<java.util.function.Supplier<Optional<Account>>>getArgument(2).get());
        lenient().when(recordProcessingLookups.partyArAccount(any(), any(), any(), any()))
                .thenAnswer(invocation -> invocation.<java.util.function.Supplier<java.util.Optional<Account>>>getArgument(3).get());
        lenient().when(recordProcessingLookups.serviceCharges(any(), any(), any(), any()))
                .thenAnswer(invocation -> invocation.<java.util.function.Supplier<List<ServiceChargeResponse>>>getArgument(3).get());
        lenient().when(fixedDecimalUtil.convertToFixedDecimal(anyDouble(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(exchangeRateService.convertAmount(anyDouble(), anyString(), anyString(), any(), any()))
                .thenAnswer(invocation -> new CurrencyConversionResult(invocation.getArgument(0), 1.0));
    }

    void convertEstimateLineAmount_shouldThrow_whenChargeMissing() {
        assertThatThrownBy(() -> chargeLineSupport.convertEstimateLineAmount(
                "DTC",
                BigDecimal.TEN,
                null,
                Map.of(),
                shipment,
                CalculatorTestDataFactory.serviceCharge("SC1", "rev", "C", null, true),
                null,
                false,
                BigDecimal.ZERO,
                null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(ChargeCreationConstants.MISSING_CHARGE_FOR_ESTIMATE_CONVERSION);
    }

    @Test
    void usesEntitySettingsAccounts_shouldBeTrue_forEcdEcsEc3() {
        assertThat(ChargeLineSupport.usesEntitySettingsAccounts("ecd")).isTrue();
        assertThat(ChargeLineSupport.usesEntitySettingsAccounts(BatchProcessingConstants.CHARGE_CODE_ECS)).isTrue();
        assertThat(ChargeLineSupport.usesEntitySettingsAccounts(BatchProcessingConstants.CHARGE_CODE_EC3)).isTrue();
        assertThat(ChargeLineSupport.usesEntitySettingsAccounts("DTC")).isFalse();
    }

    @Test
    void matchesPaymentType_shouldCompareCaseInsensitively() {
        assertThat(chargeLineSupport.matchesPaymentType(shipment, BatchProcessingConstants.PAYMENT_TYPE_PREPAID))
                .isTrue();
        assertThat(chargeLineSupport.matchesPaymentType(shipment, "3")).isFalse();
        assertThat(chargeLineSupport.matchesPaymentType(null, "P")).isFalse();
    }

    @Test
    void resolveEntityCodes_shouldTrimBlankToEmpty() {
        shipment.setHAWBDestinationEntity("  DXB  ");
        shipment.setOriginEntity(null);
        shipment.setThirdPartyEntity("");

        assertThat(chargeLineSupport.resolveDestinationEntityCode(shipment)).isEqualTo("DXB");
        assertThat(chargeLineSupport.resolveOriginEntityCode(shipment)).isEmpty();
        assertThat(chargeLineSupport.resolveThirdPartyEntityCode(shipment)).isEmpty();
        assertThat(chargeLineSupport.resolveEntityCode(shipment)).isEqualTo("DXB");
    }

    @Test
    void parseOptionalFileReference_shouldReturnNull_whenBlank() {
        assertThat(ChargeLineSupport.parseOptionalFileReference(null)).isNull();
        assertThat(ChargeLineSupport.parseOptionalFileReference(" ")).isNull();
    }

    @Test
    void parseOptionalFileReference_shouldThrow_whenInvalidUuid() {
        String invalidFileReference = "bad-id";
        assertThatThrownBy(() -> ChargeLineSupport.parseOptionalFileReference(invalidFileReference))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(ChargeCreationConstants.INVALID_FILE_REFERENCE_ID);
    }

    @Test
    void sourceValue_shouldReadLowercaseKeys() {
        Map<String, String> source = Map.of("saddad", " S1 ");

        assertThat(ChargeLineSupport.sourceValue(source, BatchProcessingConstants.PAYMENT_REFERENCE)).isEqualTo("S1");
        assertThat(ChargeLineSupport.sourceValue(null, "saddad")).isEmpty();
    }

    @Test
    void resolveLineAmount_shouldReturnZero_forDtsRevenueLinesWithoutContext() {
        ServiceChargeResponse sc = CalculatorTestDataFactory.serviceCharge(
                "SC-R", "Revenue Fee", "C", new BigDecimal("12.00"), true);
        Map<String, String> source = Map.of("revenue fee", "99.00");
        Map<String, BigDecimal> dtaRevenue = Map.of("revenue fee", new BigDecimal("5.00"));

        assertThat(ChargeLineSupport.resolveLineAmount(
                BatchProcessingConstants.CHARGE_CODE_DTS, source, sc, true, dtaRevenue))
                .isEqualByComparingTo("5.00");
    }

    @Test
    void resolveLineAmount_shouldUseSourceThenDtaRevenue_forDtsNonRevenueLines() {
        ServiceChargeResponse sc = CalculatorTestDataFactory.serviceCharge("SC-C", "Customs", "X", null, true);
        Map<String, BigDecimal> dtaRevenue = Map.of("customs", new BigDecimal("4.00"));

        assertThat(ChargeLineSupport.resolveLineAmount(
                BatchProcessingConstants.CHARGE_CODE_DTS, Map.of(), sc, true, dtaRevenue))
                .isEqualByComparingTo("4.00");
    }

    @Test
    void resolveLineAmount_shouldUseDtaContextOnly_forDtsNonRevenueLines() {
        ServiceChargeResponse sc = CalculatorTestDataFactory.serviceCharge("SC-C", "Customs", "X", null, true);
        Map<String, String> source = Map.of("customs", "7.50");
        Map<String, BigDecimal> dtaRevenue = Map.of("customs", new BigDecimal("4.00"));

        assertThat(ChargeLineSupport.resolveLineAmount(
                BatchProcessingConstants.CHARGE_CODE_DTS, source, sc, true, dtaRevenue))
                .isEqualByComparingTo("4.00");
    }

    @Test
    void resolveDtaMappingChargeType_shouldMapDownstreamCharges() {
        assertThat(ChargeLineSupport.resolveDtaMappingChargeType(BatchProcessingConstants.CHARGE_CODE_ECS))
                .isEqualTo(BatchProcessingConstants.CHARGE_CODE_ECS);
        assertThat(ChargeLineSupport.resolveDtaMappingChargeType(BatchProcessingConstants.CHARGE_CODE_DTS))
                .isEqualTo(BatchProcessingConstants.CHARGE_CODE_DTS);
        assertThat(ChargeLineSupport.resolveDtaMappingChargeType(BatchProcessingConstants.CHARGE_CODE_EC3))
                .isEqualTo(BatchProcessingConstants.CHARGE_CODE_EC3);
        assertThat(ChargeLineSupport.resolveDtaMappingChargeType(BatchProcessingConstants.CHARGE_CODE_DT3))
                .isEqualTo(BatchProcessingConstants.CHARGE_CODE_DT3);
        assertThat(ChargeLineSupport.resolveDtaMappingChargeType(BatchProcessingConstants.CHARGE_CODE_DTA))
                .isEmpty();
    }

    @Test
    void resolveLineAmount_shouldUseDtaContextOnly_forEcsLines() {
        ServiceChargeResponse sc = CalculatorTestDataFactory.serviceCharge("SC-C", "Customs", "X", null, true);
        Map<String, BigDecimal> dtaContext = Map.of("customs", new BigDecimal("6.25"));

        assertThat(ChargeLineSupport.resolveLineAmount(
                BatchProcessingConstants.CHARGE_CODE_ECS, Map.of("customs", "99"), sc, true, dtaContext))
                .isEqualByComparingTo("6.25");
    }

    @Test
    void resolveLineAmount_shouldUseDtaContext_forDt3RevenueLines() {
        ServiceChargeResponse sc = CalculatorTestDataFactory.serviceCharge(
                "SC-R", "Revenue Fee", "C", new BigDecimal("9.00"), true);
        Map<String, String> source = Map.of("revenue fee", "8.00");
        Map<String, BigDecimal> dtaRevenue = Map.of("revenue fee", new BigDecimal("5.00"));

        assertThat(ChargeLineSupport.resolveLineAmount(
                BatchProcessingConstants.CHARGE_CODE_DT3, source, sc, true, dtaRevenue))
                .isEqualByComparingTo("5.00");
    }

    @Test
    void resolveLineAmount_shouldUseSourceThenDtaRevenue_forDt3NonRevenueLines() {
        ServiceChargeResponse sc = CalculatorTestDataFactory.serviceCharge("SC-C", "Customs", "X", null, true);
        Map<String, BigDecimal> dtaRevenue = Map.of("customs", new BigDecimal("3.50"));

        assertThat(ChargeLineSupport.resolveLineAmount(
                BatchProcessingConstants.CHARGE_CODE_DT3, Map.of(), sc, true, dtaRevenue))
                .isEqualByComparingTo("3.50");
    }

    @Test
    void resolveLineAmount_shouldUseSourceMap_forCostLines() {
        ServiceChargeResponse sc = CalculatorTestDataFactory.serviceCharge("SC1", "Customs", "C", BigDecimal.ONE, true);
        Map<String, String> source = Map.of("customs", "12.50");

        assertThat(ChargeLineSupport.resolveLineAmount("DTC", source, sc, false, Map.of()))
                .isEqualByComparingTo("12.50");
    }

    @ParameterizedTest
    @MethodSource("invalidResolveLineAmountSourceValues")
    void resolveLineAmount_shouldThrow_whenSourceValueInvalid(String rawValue, String expectedMessageFragment) {
        ServiceChargeResponse sc = CalculatorTestDataFactory.serviceCharge("SC1", "Customs", "C", BigDecimal.ONE, true);
        Map<String, String> source = Map.of("customs", rawValue);
        Map<String, BigDecimal> emptyDta = Map.of();

        assertThatThrownBy(() -> ChargeLineSupport.resolveLineAmount("DTC", source, sc, false, emptyDta))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(expectedMessageFragment);
    }

    private static Stream<Arguments> invalidResolveLineAmountSourceValues() {
        return Stream.of(
                Arguments.of("abc", "Invalid numeric amount for mapped column 'customs'"),
                Arguments.of("-10.50", "Negative amount not allowed for mapped column 'customs'"),
                Arguments.of("x".repeat(100), "..."));
    }

    @Test
    void resolveServiceCharges_shouldIncludeNonBillableLines() {
        ServiceChargeResponse billable = CalculatorTestDataFactory.serviceCharge("SC1", "rev", "C", BigDecimal.TEN, true);
        ServiceChargeResponse nonBillable = CalculatorTestDataFactory.serviceCharge("SC2", "rev", "C", BigDecimal.TEN, false);
        when(serviceChargeResolver.resolve(any())).thenReturn(List.of(billable, nonBillable));

        List<ServiceChargeResponse> resolved =
                chargeLineSupport.resolveServiceCharges("DXB", account, "DTC");

        assertThat(resolved).containsExactly(billable, nonBillable);
    }

    @ParameterizedTest
    @MethodSource("arAccountPreferenceCases")
    void resolveAccounts_shouldPreferArAccount(
            String chargeCode,
            String entityCode,
            Consumer<Shipment> shipmentSetup,
            String accountNumber) {
        shipmentSetup.accept(shipment);
        Account arAccount = CalculatorTestDataFactory.account(UUID.randomUUID(), chargeCode);
        when(accountRepository.findByAccountNumberAndEntityCodeAndAccountNatureAndChargeType(
                accountNumber, entityCode, AccountNature.AR, chargeCode))
                .thenReturn(Optional.of(arAccount));

        assertThat(chargeLineSupport.resolveAccounts(shipment, entityCode, chargeCode)).containsExactly(arAccount);
    }

    private static Stream<Arguments> arAccountPreferenceCases() {
        return Stream.of(
                Arguments.of("DTC", "DXB", (Consumer<Shipment>) shipment -> {}, "ACC-001"),
                Arguments.of(
                        BatchProcessingConstants.CHARGE_CODE_DTS,
                        "RUH",
                        (Consumer<Shipment>) shipment -> shipment.setShipperNumber("SHIP-001"),
                        "SHIP-001"),
                Arguments.of(
                        BatchProcessingConstants.CHARGE_CODE_DT3,
                        "JED",
                        (Consumer<Shipment>) shipment -> shipment.setThirdPartyNumber("TP-001"),
                        "TP-001"));
    }

    @Test
    void resolveAccounts_shouldUseConsigneeNotShipper_forDtc() {
        shipment.setShipperNumber("SHIP-001");
        Account arAccount = CalculatorTestDataFactory.account(UUID.randomUUID(), "DTC");
        when(accountRepository.findByAccountNumberAndEntityCodeAndAccountNatureAndChargeType(
                "ACC-001", "DXB", AccountNature.AR, "DTC")).thenReturn(Optional.of(arAccount));

        assertThat(chargeLineSupport.resolveAccounts(shipment, "DXB", "DTC")).containsExactly(arAccount);
    }

    @Test
    void resolveDtcEntityCode_shouldReturnRuh_forDomProductGroup() {
        shipment.setProductGroup(BatchProcessingConstants.PRODUCT_GROUP_DOM);
        shipment.setHAWBDestinationEntity("DXB");

        assertThat(chargeLineSupport.resolveDtcEntityCode(shipment))
                .isEqualTo(DomEntityConstants.DEFAULT_DOM_ENTITY_CODE);
    }

    @Test
    void resolveDtcEntityCode_shouldReturnDestination_forNonDomProductGroup() {
        shipment.setProductGroup("EXP");
        shipment.setHAWBDestinationEntity("DXB");

        assertThat(chargeLineSupport.resolveDtcEntityCode(shipment)).isEqualTo("DXB");
    }

    @Test
    void resolveDomDestinationChargeEntityCode_shouldReturnRuh_forDomEcd() {
        shipment.setProductGroup(BatchProcessingConstants.PRODUCT_GROUP_DOM);
        shipment.setHAWBDestinationEntity("DXB");

        assertThat(chargeLineSupport.resolveDomDestinationChargeEntityCode(
                shipment, BatchProcessingConstants.CHARGE_CODE_ECD))
                .isEqualTo(DomEntityConstants.DEFAULT_DOM_ENTITY_CODE);
    }

    @Test
    void resolveDomDestinationChargeEntityCode_shouldReturnDestination_forNonDomEcd() {
        shipment.setProductGroup("EXP");
        shipment.setHAWBDestinationEntity("DXB");

        assertThat(chargeLineSupport.resolveDomDestinationChargeEntityCode(
                shipment, BatchProcessingConstants.CHARGE_CODE_ECD))
                .isEqualTo("DXB");
    }

    @Test
    void resolveAccounts_shouldUseShipperForDomDtc() {
        shipment.setProductGroup(BatchProcessingConstants.PRODUCT_GROUP_DOM);
        shipment.setShipperNumber("SHIP-DOM-001");
        shipment.setConsigneeNumber("ACC-001");
        Account arAccount = CalculatorTestDataFactory.account(UUID.randomUUID(), "DTC");
        when(accountRepository.findByAccountNumberAndEntityCodeAndAccountNatureAndChargeType(
                "SHIP-DOM-001", "RUH", AccountNature.AR, "DTC")).thenReturn(Optional.of(arAccount));

        assertThat(chargeLineSupport.resolveAccounts(shipment, "RUH", "DTC")).containsExactly(arAccount);
    }

    @Test
    void resolveDtcBillingAccount_shouldUseRuhEntity_forDomWithBlankShipperNumber() {
        shipment.setProductGroup(BatchProcessingConstants.PRODUCT_GROUP_DOM);
        shipment.setShipperNumber(" ");
        Account entitySettings = CalculatorTestDataFactory.account(UUID.randomUUID(), BatchProcessingConstants.CHARGE_CODE_DTC);
        when(revenueBillingAccountResolver.resolveBillingAccount(
                DomEntityConstants.DEFAULT_DOM_ENTITY_CODE,
                RevenueBillingAccountResolver.CHARGE_TYPE_DTC,
                ""))
                .thenReturn(Optional.of(entitySettings));

        assertThat(chargeLineSupport.resolveDtcBillingAccount(shipment, DomEntityConstants.DEFAULT_DOM_ENTITY_CODE))
                .contains(entitySettings);
    }

    @Test
    void resolveDtcBillingAccount_shouldDelegateToRevenueBillingAccountResolver() {
        Account resolved = CalculatorTestDataFactory.account(UUID.randomUUID(), BatchProcessingConstants.CHARGE_CODE_DTC);
        when(revenueBillingAccountResolver.resolveBillingAccount(
                "DXB",
                RevenueBillingAccountResolver.CHARGE_TYPE_DTC,
                "ACC-001"))
                .thenReturn(Optional.of(resolved));

        assertThat(chargeLineSupport.resolveDtcBillingAccount(shipment, "DXB")).contains(resolved);
    }

    @Test
    void resolveAccountsForNewDtcLine_shouldFallbackToResolveAccounts_whenResolverEmpty() {
        when(revenueBillingAccountResolver.resolveBillingAccount(any(), any(), any())).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumberAndEntityCodeAndAccountNatureAndChargeType(
                "ACC-001", "DXB", AccountNature.AR, "DTC")).thenReturn(Optional.of(account));

        assertThat(chargeLineSupport.resolveAccountsForNewDtcLine(shipment, "DXB")).containsExactly(account);
    }

    @Test
    void resolveAccountsForDtcProcessing_shouldKeepExistingLineAccounts_onUpdate() {
        Account existingAccount = CalculatorTestDataFactory.account(UUID.randomUUID(), BatchProcessingConstants.CHARGE_CODE_DTC);
        ChargeLineItem existingLine = CalculatorTestDataFactory.chargeLineItem(
                CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, BatchProcessingConstants.CHARGE_CODE_DTC),
                existingAccount,
                "SC1",
                BigDecimal.ONE);
        Account resolverAccount = CalculatorTestDataFactory.account(UUID.randomUUID(), BatchProcessingConstants.CHARGE_CODE_DTC);
        when(revenueBillingAccountResolver.resolveBillingAccount(any(), any(), any()))
                .thenReturn(Optional.of(resolverAccount));

        assertThat(chargeLineSupport.resolveAccountsForDtcProcessing(shipment, "DXB", List.of(existingLine)))
                .containsExactlyInAnyOrder(existingAccount, resolverAccount);
    }

    @Test
    void resolveAccounts_shouldUseEntitySettings_forEcsChargeCode() {
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeTypeOrderByChargeTypeAsc(
                "DXB", AccountNature.ENTITY_SETTINGS, BatchProcessingConstants.CHARGE_CODE_ECS))
                .thenReturn(List.of(account));

        assertThat(chargeLineSupport.resolveAccounts(shipment, "DXB", BatchProcessingConstants.CHARGE_CODE_ECS))
                .containsExactly(account);
    }

    @Test
    void isExemptedCategoryType_shouldBeTrue_whenCategoryTypeExempted() {
        shipment.setCategoryType(BatchProcessingConstants.CATEGORY_TYPE_EXEMPTED);
        assertThat(ChargeLineSupport.isExemptedCategoryType(shipment)).isTrue();
        assertThat(ChargeLineSupport.requiresZeroAmounts(shipment)).isTrue();
        assertThat(ChargeLineSupport.resolveChargeStatusForEstimate(shipment))
                .isEqualTo(BatchProcessingConstants.STATUS_EXEMPTED);
    }

    @Test
    void requiresZeroAmounts_shouldBeTrue_forDdoShipmentType() {
        shipment.setShipmentType(BatchProcessingConstants.SHIPMENT_TYPE_DDO);
        assertThat(ChargeLineSupport.requiresZeroAmounts(shipment)).isTrue();
        assertThat(ChargeLineSupport.resolveChargeStatusForEstimate(shipment))
                .isEqualTo(BatchProcessingConstants.STATUS_EXEMPTED);
    }

    @Test
    void buildLineItemsForCharge_shouldUseZeroAmounts_whenCategoryExempted() {
        shipment.setCategoryType(BatchProcessingConstants.CATEGORY_TYPE_EXEMPTED);
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTC");
        charge.setEntity(CalculatorTestDataFactory.billingEntity("DXB"));
        ServiceChargeResponse sc = CalculatorTestDataFactory.serviceCharge("SC1", "customs", "C", BigDecimal.ONE, true);
        when(serviceChargeResolver.resolve(any())).thenReturn(List.of(sc));

        var lines = chargeLineSupport.buildLineItemsForCharge(
                charge,
                List.of(account),
                Map.of("customs", "5.00"),
                "DTC",
                "DXB",
                CalculatorTestDataFactory.fileId(),
                "PAY-1");

        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst().getAmount()).isEqualByComparingTo("0");
    }

    @Test
    void buildLineItemsForCharge_shouldCreateLinesFromServiceCharges() {
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTC");
        charge.setEntity(CalculatorTestDataFactory.billingEntity("DXB"));
        ServiceChargeResponse sc = CalculatorTestDataFactory.serviceCharge("SC1", "customs", "C", null, true);
        sc.setDefaultAccountNumber("AP-DEFAULT-1");
        when(serviceChargeResolver.resolve(any())).thenReturn(List.of(sc));

        var lines = chargeLineSupport.buildLineItemsForCharge(
                charge,
                List.of(account),
                Map.of("customs", "5.00"),
                "DTC",
                "DXB",
                CalculatorTestDataFactory.fileId(),
                "PAY-1");

        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst().getPreTaxableAmount()).isEqualByComparingTo("5.00");
        assertThat(lines.getFirst().getAmount()).isEqualByComparingTo("5.00");
        assertThat(lines.getFirst().getServiceCode()).isEqualTo("SC1");
        assertThat(lines.getFirst().getApAccountNumber()).isEqualTo("AP-DEFAULT-1");
    }

    @Test
    void buildLineItemsForCharge_shouldSetPreConvertedAmount_onActual_withoutChangingAmountPath() {
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTC");
        charge.setEntity(CalculatorTestDataFactory.billingEntity("DXB"));
        ServiceChargeResponse sc = CalculatorTestDataFactory.serviceCharge(
                "SC1", "customs", "C", BigDecimal.valueOf(10), true);
        when(serviceChargeResolver.resolve(any())).thenReturn(List.of(sc));

        var lines = chargeLineSupport.buildLineItemsForCharge(
                charge,
                List.of(account),
                Map.of("customs", "5.00"),
                "DTC",
                "DXB",
                CalculatorTestDataFactory.fileId(),
                "PAY-1",
                null,
                true);

        assertThat(lines).hasSize(1);
        // Actual file upload: file values are final (FX only, no tax).
        assertThat(lines.getFirst().getPreConvertedAmount()).isEqualByComparingTo("5.00");
        assertThat(lines.getFirst().getPreTaxableAmount()).isEqualByComparingTo("5.00");
        assertThat(lines.getFirst().getAmount()).isEqualByComparingTo("5.00");
    }

    @Test
    void applyApAccountNumber_shouldIgnoreBlankDefault() {
        ChargeLineItem line = new ChargeLineItem();
        ServiceChargeResponse sc = CalculatorTestDataFactory.serviceCharge("SC1", "customs", "C", null, true);
        sc.setDefaultAccountNumber("  ");

        ChargeLineSupport.applyApAccountNumber(line, sc);

        assertThat(line.getApAccountNumber()).isNull();
    }

    @Test
    void buildLineItemsForCharge_shouldSetApAccountNumberFromFile_onActual() {
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTC");
        charge.setEntity(CalculatorTestDataFactory.billingEntity("DXB"));
        ServiceChargeResponse sc = CalculatorTestDataFactory.serviceCharge("SC1", "customs", "C", null, true);
        sc.setDefaultAccountNumber("AP-FROM-SC");
        when(serviceChargeResolver.resolve(any())).thenReturn(List.of(sc));

        var lines = chargeLineSupport.buildLineItemsForCharge(
                charge,
                List.of(account),
                Map.of(
                        "customs", "5.00",
                        BatchProcessingConstants.SOURCE_KEY_AP_ACCOUNT, "AP-FROM-FILE"),
                "DTC",
                "DXB",
                CalculatorTestDataFactory.fileId(),
                "PAY-1",
                null,
                true);

        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst().getApAccountNumber()).isEqualTo("AP-FROM-FILE");
    }

    @Test
    void applyExchangeToLineAmount_shouldReturnZero_withoutCallingExchange_whenAmountZero() {
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTC");

        assertThat(chargeLineSupport.applyExchangeToLineAmount(BigDecimal.ZERO, Map.of(), charge))
                .isEqualByComparingTo("0");

        verify(exchangeRateService, never()).convertAmount(anyDouble(), anyString(), anyString(), any(), any());
    }

    @Test
    void applyExchangeToLineAmount_shouldUseSarDefaults_whenCurrenciesMissing() {
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTC");
        when(exchangeRateService.convertAmount(
                10.0, BatchProcessingConstants.DEFAULT_CURRENCY, BatchProcessingConstants.DEFAULT_CURRENCY, null, null))
                .thenReturn(new CurrencyConversionResult(10.0, 1.0));

        assertThat(chargeLineSupport.applyExchangeToLineAmount(BigDecimal.TEN, Map.of(), charge))
                .isEqualByComparingTo("10");
    }

    @Test
    void applyExchangeToLineAmount_shouldUseSourceAndChargeCurrencies() {
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTC");
        charge.setCurrency("AED");
        Map<String, String> source = Map.of("currency", "USD");
        when(exchangeRateService.convertAmount(100.0, "USD", "AED", null, null))
                .thenReturn(new CurrencyConversionResult(367.0, 3.67));

        assertThat(chargeLineSupport.applyExchangeToLineAmount(BigDecimal.valueOf(100), source, charge))
                .isEqualByComparingTo("367");
    }

    @Test
    void buildLineItemsForCharge_shouldCaptureExchangeRate_whenCurrencyConverted() {
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTC");
        charge.setEntity(CalculatorTestDataFactory.billingEntity("DXB"));
        charge.setCurrency("AED");
        ServiceChargeResponse sc = CalculatorTestDataFactory.serviceCharge("SC1", "customs", "C", null, true);
        Map<String, String> source = Map.of("customs", "100.00", "currency", "USD");
        when(serviceChargeResolver.resolve(any())).thenReturn(List.of(sc));
        when(exchangeRateService.convertAmount(100.0, "USD", "AED", null, null))
                .thenReturn(new CurrencyConversionResult(367.0, 3.67));

        var lines = chargeLineSupport.buildLineItemsForCharge(
                charge,
                List.of(account),
                source,
                "DTC",
                "DXB",
                CalculatorTestDataFactory.fileId(),
                "PAY-1");

        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst().getAmount()).isEqualByComparingTo("367");
        assertThat(lines.getFirst().getExchangeRate()).isEqualTo(3.67);
    }

    @Test
    void exchangeRateKeyPrefix_shouldBuildFromContext() {
        var ctx = CalculatorTestDataFactory.chargeContext();
        assertThat(ChargeLineSupport.exchangeRateKeyPrefix(ctx))
                .isEqualTo("ExchangeRate:" + CalculatorTestDataFactory.fileId());
    }

    @Test
    void applyExchangeToLineAmount_shouldPassKeyPrefixToExchangeService() {
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTC");
        Map<String, String> source = Map.of("currency", "USD");
        String keyPrefix = "ExchangeRate:file-1";
        when(exchangeRateService.convertAmount(100.0, "USD", BatchProcessingConstants.DEFAULT_CURRENCY, null, keyPrefix))
                .thenReturn(new CurrencyConversionResult(375.0, 3.75));

        assertThat(chargeLineSupport.applyExchangeToLineAmount(BigDecimal.valueOf(100), source, charge, keyPrefix))
                .isEqualByComparingTo("375");
    }

    @Test
    void convertEstimateLineAmount_shouldNotFxRateSheetFixedPart() {
        shipment.setCustomsCurrency("USD");
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTC");
        charge.setCurrency("AED");

        CurrencyConversionResult result = chargeLineSupport.convertEstimateLineAmount(
                "DTC",
                BigDecimal.ZERO,
                new RateSheetAmountBreakdown(BigDecimal.valueOf(50), List.of()),
                Map.of(),
                shipment,
                CalculatorTestDataFactory.serviceCharge("SC1", "rev", "C", null, true),
                charge,
                false,
                BigDecimal.ZERO,
                null);

        assertThat(result.convertedAmount()).isEqualTo(50.0);
        assertThat(result.exchangeRate()).isEqualTo(1.0);
        verify(exchangeRateService, never()).convertAmount(anyDouble(), anyString(), anyString(), any(), any());
    }

    @Test
    void convertEstimateLineAmount_shouldFxRateSheetPercentageOnly() {
        shipment.setCustomsCurrency("USD");
        shipment.setCostOfGoods(1000.0);
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTC");
        charge.setCurrency("AED");
        com.customcharge.persistencelayer.entities.Currency destinationCurrency =
                new com.customcharge.persistencelayer.entities.Currency();
        destinationCurrency.setCurrencyCode("AED");
        when(currencyRepository.findActiveByEntityCode("DXB")).thenReturn(Optional.of(destinationCurrency));
        when(shipmentCostBasisCalculator.resolveBasis(shipment, PercentageOf.COST_OF_GOODS))
                .thenReturn(BigDecimal.valueOf(1000));
        when(exchangeRateService.convertAmount(1000.0, "USD", "AED", null, null))
                .thenReturn(new CurrencyConversionResult(3670.0, 3.67));

        CurrencyConversionResult result = chargeLineSupport.convertEstimateLineAmount(
                "DTC",
                BigDecimal.ZERO,
                new RateSheetAmountBreakdown(
                        BigDecimal.valueOf(50),
                        List.of(new PercentageRateSpec(BigDecimal.valueOf(10), PercentageOf.COST_OF_GOODS))),
                Map.of(),
                shipment,
                CalculatorTestDataFactory.serviceCharge("SC1", "rev", "C", null, true),
                charge,
                false,
                BigDecimal.ZERO,
                null);

        assertThat(result.convertedAmount()).isEqualTo(417.0);
        assertThat(result.exchangeRate()).isEqualTo(3.67);
    }

    @Test
    void convertEstimateLineAmount_shouldFxDtaRateSheetFixedFromDestinationToUsd() {
        shipment.setCustomsCurrency("USD");
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, BatchProcessingConstants.CHARGE_CODE_DTA);
        charge.setCurrency("USD");
        com.customcharge.persistencelayer.entities.Currency destinationCurrency =
                new com.customcharge.persistencelayer.entities.Currency();
        destinationCurrency.setCurrencyCode("AED");
        when(currencyRepository.findActiveByEntityCode("DXB")).thenReturn(Optional.of(destinationCurrency));
        when(exchangeRateService.convertAmount(100.0, "AED", "USD", null, null))
                .thenReturn(new CurrencyConversionResult(27.0, 0.27));

        CurrencyConversionResult result = chargeLineSupport.convertEstimateLineAmount(
                BatchProcessingConstants.CHARGE_CODE_DTA,
                BigDecimal.ZERO,
                new RateSheetAmountBreakdown(BigDecimal.valueOf(100), List.of()),
                Map.of(),
                shipment,
                CalculatorTestDataFactory.serviceCharge("SC1", "rev", "C", null, true),
                charge,
                false,
                BigDecimal.ZERO,
                null);

        assertThat(result.convertedAmount()).isEqualTo(27.0);
        assertThat(result.exchangeRate()).isEqualTo(0.27);
        verify(exchangeRateService).convertAmount(100.0, "AED", "USD", null, null);
    }

    @Test
    void convertEstimateLineAmount_shouldFxDtaRateSheetFixedAndPercentageSeparately() {
        shipment.setCustomsCurrency("USD");
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, BatchProcessingConstants.CHARGE_CODE_DTA);
        charge.setCurrency("USD");
        com.customcharge.persistencelayer.entities.Currency destinationCurrency =
                new com.customcharge.persistencelayer.entities.Currency();
        destinationCurrency.setCurrencyCode("AED");
        when(currencyRepository.findActiveByEntityCode("DXB")).thenReturn(Optional.of(destinationCurrency));
        when(shipmentCostBasisCalculator.resolveBasis(shipment, PercentageOf.COST_OF_GOODS))
                .thenReturn(BigDecimal.valueOf(50));
        when(exchangeRateService.convertAmount(100.0, "AED", "USD", null, null))
                .thenReturn(new CurrencyConversionResult(27.0, 0.27));
        when(exchangeRateService.convertAmount(50.0, "USD", "AED", null, null))
                .thenReturn(new CurrencyConversionResult(13.624, 0.27248));
        when(exchangeRateService.convertAmount(13.6240, "AED", "USD", null, null))
                .thenReturn(new CurrencyConversionResult(3.678, 0.27));

        CurrencyConversionResult result = chargeLineSupport.convertEstimateLineAmount(
                BatchProcessingConstants.CHARGE_CODE_DTA,
                BigDecimal.ZERO,
                new RateSheetAmountBreakdown(
                        BigDecimal.valueOf(100),
                        List.of(new PercentageRateSpec(BigDecimal.valueOf(100), PercentageOf.COST_OF_GOODS))),
                Map.of(),
                shipment,
                CalculatorTestDataFactory.serviceCharge("SC1", "rev", "C", null, true),
                charge,
                false,
                BigDecimal.ZERO,
                null);

        assertThat(result.convertedAmount()).isEqualTo(30.678);
        verify(exchangeRateService).convertAmount(100.0, "AED", "USD", null, null);
        verify(exchangeRateService).convertAmount(50.0, "USD", "AED", null, null);
        verify(exchangeRateService).convertAmount(13.6240, "AED", "USD", null, null);
    }

    @Test
    void matchesPaymentType_shouldReturnFalse_whenShipmentPaymentTypeBlank() {
        shipment.setPaymentType("  ");
        assertThat(chargeLineSupport.matchesPaymentType(shipment, BatchProcessingConstants.PAYMENT_TYPE_PREPAID))
                .isFalse();
    }

    @Test
    void resolveAccounts_shouldUseFirstAccount_whenEntitySettingsReturnsMultiple() {
        Account first = CalculatorTestDataFactory.account(UUID.randomUUID(), "DTC");
        Account second = CalculatorTestDataFactory.account(UUID.randomUUID(), "DTC");
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeTypeOrderByChargeTypeAsc(
                "DXB", AccountNature.ENTITY_SETTINGS, "DTC")).thenReturn(List.of(first, second));

        assertThat(chargeLineSupport.resolveAccounts(shipment, "DXB", "DTC")).containsExactly(first);
    }

    @Test
    void resolveAccounts_shouldFallbackToEntitySettings_whenArAccountMissing() {
        when(accountRepository.findByAccountNumberAndEntityCodeAndAccountNatureAndChargeType(
                "ACC-001", "DXB", AccountNature.AR, "DTC")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeTypeOrderByChargeTypeAsc(
                "DXB", AccountNature.ENTITY_SETTINGS, "DTC")).thenReturn(List.of(account));

        assertThat(chargeLineSupport.resolveAccounts(shipment, "DXB", "DTC")).containsExactly(account);
    }

    @Test
    void resolveAccounts_shouldResolveDtaThroughDefaultArAccountNumber() {
        String entityCode = "DXB";
        Account entitySettings = CalculatorTestDataFactory.account(UUID.randomUUID(), BatchProcessingConstants.CHARGE_CODE_DTA);
        entitySettings.setDefaultArAccountNumber("AR-DEFAULT-001");
        Account dtaArAccount = CalculatorTestDataFactory.account(UUID.randomUUID(), BatchProcessingConstants.CHARGE_CODE_DTA);
        ARAccount arMaster = new ARAccount();
        arMaster.setId(UUID.randomUUID());
        arMaster.setArAccountNumber("AR-DEFAULT-001");
        arMaster.setEntityCode(entityCode);

        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                entityCode, AccountNature.ENTITY_SETTINGS, BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(Optional.of(entitySettings));
        when(arAccountRepository.findByArAccountNumberAndEntityCode("AR-DEFAULT-001", entityCode))
                .thenReturn(Optional.of(arMaster));
        when(accountRepository.findByArAccountIdAndChargeType(arMaster.getId(), BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(Optional.of(dtaArAccount));

        assertThat(chargeLineSupport.resolveAccounts(shipment, entityCode, BatchProcessingConstants.CHARGE_CODE_DTA))
                .containsExactly(dtaArAccount);
        verify(accountRepository, never()).findByAccountNumberAndEntityCodeAndAccountNatureAndChargeType(
                anyString(), anyString(), any(), anyString());
    }

    @Test
    void resolveAccounts_shouldFallbackToEntitySettings_forDta_whenDefaultArAccountNumberMissing() {
        String entityCode = "DXB";
        Account entitySettings = CalculatorTestDataFactory.account(UUID.randomUUID(), BatchProcessingConstants.CHARGE_CODE_DTA);
        entitySettings.setDefaultArAccountNumber(" ");

        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                entityCode, AccountNature.ENTITY_SETTINGS, BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(Optional.of(entitySettings));
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeTypeOrderByChargeTypeAsc(
                entityCode, AccountNature.ENTITY_SETTINGS, BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(List.of(entitySettings));

        assertThat(chargeLineSupport.resolveAccounts(shipment, entityCode, BatchProcessingConstants.CHARGE_CODE_DTA))
                .containsExactly(entitySettings);
        verify(arAccountRepository, never()).findByArAccountNumberAndEntityCode(anyString(), anyString());
    }

    @Test
    void resolveAccounts_shouldFallbackToEntitySettings_forDta_whenArMasterMissing() {
        String entityCode = "DXB";
        Account entitySettings = CalculatorTestDataFactory.account(UUID.randomUUID(), BatchProcessingConstants.CHARGE_CODE_DTA);
        entitySettings.setDefaultArAccountNumber("AR-DEFAULT-001");

        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                entityCode, AccountNature.ENTITY_SETTINGS, BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(Optional.of(entitySettings));
        when(arAccountRepository.findByArAccountNumberAndEntityCode("AR-DEFAULT-001", entityCode))
                .thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeTypeOrderByChargeTypeAsc(
                entityCode, AccountNature.ENTITY_SETTINGS, BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(List.of(entitySettings));

        assertThat(chargeLineSupport.resolveAccounts(shipment, entityCode, BatchProcessingConstants.CHARGE_CODE_DTA))
                .containsExactly(entitySettings);
    }

    @Test
    void resolveAccounts_shouldFallbackToEntitySettings_forDta_whenDtaChargeConfigMissing() {
        String entityCode = "DXB";
        Account entitySettings = CalculatorTestDataFactory.account(UUID.randomUUID(), BatchProcessingConstants.CHARGE_CODE_DTA);
        entitySettings.setDefaultArAccountNumber("AR-DEFAULT-001");
        ARAccount arMaster = new ARAccount();
        arMaster.setId(UUID.randomUUID());

        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                entityCode, AccountNature.ENTITY_SETTINGS, BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(Optional.of(entitySettings));
        when(arAccountRepository.findByArAccountNumberAndEntityCode("AR-DEFAULT-001", entityCode))
                .thenReturn(Optional.of(arMaster));
        when(accountRepository.findByArAccountIdAndChargeType(arMaster.getId(), BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeTypeOrderByChargeTypeAsc(
                entityCode, AccountNature.ENTITY_SETTINGS, BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(List.of(entitySettings));

        assertThat(chargeLineSupport.resolveAccounts(shipment, entityCode, BatchProcessingConstants.CHARGE_CODE_DTA))
                .containsExactly(entitySettings);
    }

    @Test
    void resolveAccounts_shouldNotUseConsignee_forDta() {
        String entityCode = "DXB";
        Account entitySettings = CalculatorTestDataFactory.account(UUID.randomUUID(), BatchProcessingConstants.CHARGE_CODE_DTA);
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                entityCode, AccountNature.ENTITY_SETTINGS, BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeTypeOrderByChargeTypeAsc(
                entityCode, AccountNature.ENTITY_SETTINGS, BatchProcessingConstants.CHARGE_CODE_DTA))
                .thenReturn(List.of(entitySettings));

        assertThat(chargeLineSupport.resolveAccounts(shipment, entityCode, BatchProcessingConstants.CHARGE_CODE_DTA))
                .containsExactly(entitySettings);
        verify(accountRepository, never()).findByAccountNumberAndEntityCodeAndAccountNatureAndChargeType(
                anyString(), anyString(), any(), anyString());
    }

    @Test
    void formatAmount_shouldReturnZero_whenAmountNull() {
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTC");
        assertThat(chargeLineSupport.formatAmount(null, charge)).isEqualByComparingTo("0");
    }

    @Test
    void formatAmount_shouldReturnZero_whenFixedDecimalReturnsNull() {
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTC");
        charge.setEntity(CalculatorTestDataFactory.billingEntity("DXB"));
        when(fixedDecimalUtil.convertToFixedDecimal(anyDouble(), anyString())).thenReturn(null);

        assertThat(chargeLineSupport.formatAmount(BigDecimal.TEN, charge)).isEqualByComparingTo("0");
    }

    @Test
    void resolveDtaMappingChargeType_shouldReturnEmpty_whenChargeCodeNull() {
        assertThat(ChargeLineSupport.resolveDtaMappingChargeType(null)).isEmpty();
    }

    @Test
    void resolveLineAmount_shouldReturnZero_whenDtaDescriptionMissing() {
        ServiceChargeResponse sc = CalculatorTestDataFactory.serviceCharge("SC-C", " ", "X", null, true);
        assertThat(ChargeLineSupport.resolveLineAmount(
                BatchProcessingConstants.CHARGE_CODE_DTS, Map.of(), sc, true, Map.of()))
                .isEqualByComparingTo("0");
    }

    @Test
    void resolveLineAmount_shouldReturnZero_whenSourceValueBlank() {
        ServiceChargeResponse sc = CalculatorTestDataFactory.serviceCharge("SC1", "Customs", "C", BigDecimal.ONE, true);
        assertThat(ChargeLineSupport.resolveLineAmount("DTC", Map.of("customs", " "), sc, false, Map.of()))
                .isEqualByComparingTo("0");
    }

    @Test
    void buildLineItemsForCharge_shouldUseDtaAmountsAndMergeCapture_forActualDts() {
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, BatchProcessingConstants.CHARGE_CODE_DTS);
        charge.setEntity(CalculatorTestDataFactory.billingEntity("RUH"));
        ServiceChargeResponse sc = CalculatorTestDataFactory.serviceCharge("SC1", "Customs", "X", null, true);
        Map<String, BigDecimal> capture = new java.util.LinkedHashMap<>();
        DtaContextSupport.DtaLineContext dta = new DtaContextSupport.DtaLineContext(
                Map.of("customs", new BigDecimal("7.50")), true, false, capture);
        when(serviceChargeResolver.resolve(any())).thenReturn(List.of(sc));

        List<ChargeLineItem> lines = chargeLineSupport.buildLineItemsForCharge(
                charge,
                List.of(account),
                Map.of("customs", "99.00"),
                BatchProcessingConstants.CHARGE_CODE_DTS,
                "RUH",
                CalculatorTestDataFactory.fileId(),
                "PAY-1",
                dta,
                true);

        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst().getPreTaxableAmount()).isEqualByComparingTo("7.50");
        assertThat(lines.getFirst().getAmount()).isEqualByComparingTo("7.50");
        assertThat(capture.get("customs")).isEqualByComparingTo("7.50");
    }

    @Test
    void convertEstimateLineAmount_shouldFxDtsRevenueFromUsd_whenSiblingContextOnEstimate() {
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, BatchProcessingConstants.CHARGE_CODE_DTS);
        charge.setEntity(CalculatorTestDataFactory.billingEntity("RUH"));
        charge.setCurrency("AED");
        ServiceChargeResponse revenueLine = CalculatorTestDataFactory.serviceCharge(
                "SC-R", "Revenue Fee", BatchProcessingConstants.REVENUE_TYPE_CODE, null, true);
        when(exchangeRateService.convertAmount(110.0, "USD", "AED", null, "ExchangeRate:file-1"))
                .thenReturn(new CurrencyConversionResult(403.7, 3.67));

        CurrencyConversionResult result = chargeLineSupport.convertEstimateLineAmount(
                BatchProcessingConstants.CHARGE_CODE_DTS,
                new BigDecimal("110"),
                null,
                Map.of(),
                shipment,
                revenueLine,
                charge,
                true,
                true,
                BigDecimal.ZERO,
                "ExchangeRate:file-1");

        assertThat(result.convertedAmount()).isEqualTo(403.7);
        assertThat(result.exchangeRate()).isEqualTo(3.67);
        verify(exchangeRateService).convertAmount(110.0, "USD", "AED", null, "ExchangeRate:file-1");
    }

    @Test
    void convertEstimateLineAmount_shouldKeepEcsRevenueAmount_withoutUsdFx_whenFromDtaContext() {
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, BatchProcessingConstants.CHARGE_CODE_ECS);
        charge.setEntity(CalculatorTestDataFactory.billingEntity("RUH"));
        charge.setCurrency("USD");
        ServiceChargeResponse revenueLine = CalculatorTestDataFactory.serviceCharge(
                "SC-R", "Revenue Fee", BatchProcessingConstants.REVENUE_TYPE_CODE, null, true);

        CurrencyConversionResult result = chargeLineSupport.convertEstimateLineAmount(
                BatchProcessingConstants.CHARGE_CODE_ECS,
                new BigDecimal("110"),
                null,
                Map.of(),
                shipment,
                revenueLine,
                charge,
                true,
                false,
                BigDecimal.ZERO,
                null);

        assertThat(result.convertedAmount()).isEqualTo(110.0);
        assertThat(result.exchangeRate()).isEqualTo(1.0);
        verify(exchangeRateService, never()).convertAmount(anyDouble(), any(), any(), any(), any());
    }

    @Test
    void convertEstimateLineAmount_shouldFxDtsRevenueFromUsd_whenFromDtaContext() {
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, BatchProcessingConstants.CHARGE_CODE_DTS);
        charge.setCurrency("AED");
        ServiceChargeResponse revenueLine = CalculatorTestDataFactory.serviceCharge(
                "SC-R", "Revenue Fee", BatchProcessingConstants.REVENUE_TYPE_CODE, null, true);
        Map<String, String> source = Map.of("currency", BatchProcessingConstants.DEFAULT_CURRENCY);
        when(exchangeRateService.convertAmount(100.0, "USD", "AED", null, null))
                .thenReturn(new CurrencyConversionResult(367.0, 3.67));

        CurrencyConversionResult result = chargeLineSupport.convertEstimateLineAmount(
                BatchProcessingConstants.CHARGE_CODE_DTS,
                new BigDecimal("100"),
                null,
                source,
                shipment,
                revenueLine,
                charge,
                true,
                BigDecimal.ZERO,
                null);

        assertThat(result.convertedAmount()).isEqualTo(367.0);
        verify(exchangeRateService).convertAmount(100.0, "USD", "AED", null, null);
    }

    @Test
    void buildLineItemsForCharge_shouldFxActualDtsRevenueFromUsd_whenFromDtaContext() {
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, BatchProcessingConstants.CHARGE_CODE_DTS);
        charge.setEntity(CalculatorTestDataFactory.billingEntity("RUH"));
        charge.setCurrency("AED");
        ServiceChargeResponse revenueLine = CalculatorTestDataFactory.serviceCharge(
                "SC-R", "Revenue Fee", BatchProcessingConstants.REVENUE_TYPE_CODE, null, true);
        Map<String, BigDecimal> capture = new java.util.LinkedHashMap<>();
        DtaContextSupport.DtaLineContext dta = new DtaContextSupport.DtaLineContext(
                Map.of("revenue fee", new BigDecimal("100")), true, false, capture);
        when(serviceChargeResolver.resolve(any())).thenReturn(List.of(revenueLine));
        when(exchangeRateService.convertAmount(100.0, "USD", "AED", null, null))
                .thenReturn(new CurrencyConversionResult(367.0, 3.67));

        List<ChargeLineItem> lines = chargeLineSupport.buildLineItemsForCharge(
                charge,
                List.of(account),
                Map.of("currency", BatchProcessingConstants.DEFAULT_CURRENCY, "revenue fee", "500.00"),
                BatchProcessingConstants.CHARGE_CODE_DTS,
                "RUH",
                CalculatorTestDataFactory.fileId(),
                "PAY-1",
                dta,
                true);

        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst().getPreTaxableAmount()).isEqualByComparingTo("367");
        assertThat(lines.getFirst().getAmount()).isEqualByComparingTo("367");
        verify(exchangeRateService).convertAmount(100.0, "USD", "AED", null, null);
    }

    @Test
    void applyChargeCurrency_shouldSetUsdForEcsAndEc3CostCharges() {
        Charge ecsCharge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, BatchProcessingConstants.CHARGE_CODE_ECS);
        Charge ec3Charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, BatchProcessingConstants.CHARGE_CODE_EC3);

        chargeLineSupport.applyChargeCurrency(ecsCharge, BatchProcessingConstants.CHARGE_CODE_ECS, "RUH", shipment, true);
        chargeLineSupport.applyChargeCurrency(ec3Charge, BatchProcessingConstants.CHARGE_CODE_EC3, "DXB", shipment, true);

        assertThat(ecsCharge.getCurrency()).isEqualTo("USD");
        assertThat(ec3Charge.getCurrency()).isEqualTo("USD");
    }

    @Test
    void applyChargeCurrency_shouldUseShipperCountryCurrencyForDtsAndThirdPartyCountryForDt3() {
        shipment.setShipperCountryCode("US");
        shipment.setThirdPartyCountry("AE");

        com.customcharge.persistencelayer.entities.Country usCountry = new com.customcharge.persistencelayer.entities.Country();
        usCountry.setCode("US");
        com.customcharge.persistencelayer.entities.Country aeCountry = new com.customcharge.persistencelayer.entities.Country();
        aeCountry.setCode("AE");
        com.customcharge.persistencelayer.entities.Currency usd = new com.customcharge.persistencelayer.entities.Currency();
        usd.setCurrencyCode("USD");
        com.customcharge.persistencelayer.entities.Currency aed = new com.customcharge.persistencelayer.entities.Currency();
        aed.setCurrencyCode("AED");

        when(countryRepository.findByCodeIgnoreCase("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCodeIgnoreCase("AE")).thenReturn(Optional.of(aeCountry));
        when(currencyRepository.findByCountry_CodeIgnoreCaseAndActiveTrue("US")).thenReturn(Optional.of(usd));
        when(currencyRepository.findByCountry_CodeIgnoreCaseAndActiveTrue("AE")).thenReturn(Optional.of(aed));

        Charge dtsCharge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, BatchProcessingConstants.CHARGE_CODE_DTS);
        Charge dt3Charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, BatchProcessingConstants.CHARGE_CODE_DT3);

        chargeLineSupport.applyChargeCurrency(dtsCharge, BatchProcessingConstants.CHARGE_CODE_DTS, "RUH", shipment, true);
        chargeLineSupport.applyChargeCurrency(dt3Charge, BatchProcessingConstants.CHARGE_CODE_DT3, "JED", shipment, true);

        assertThat(dtsCharge.getCurrency()).isEqualTo("USD");
        assertThat(dt3Charge.getCurrency()).isEqualTo("AED");
    }

    @Test
    void resolveTaxPercentForServiceCode_shouldReturnTaxValue_whenServiceMatches() {
        ServiceChargeResponse match = CalculatorTestDataFactory.serviceCharge(
                "SC-TAX", "Tax line", "C", BigDecimal.valueOf(5), true);
        when(serviceChargeResolver.resolve(any())).thenReturn(List.of(match));

        BigDecimal tax = chargeLineSupport.resolveTaxPercentForServiceCode(
                "DXB", account, BatchProcessingConstants.CHARGE_CODE_DTC, "SC-TAX");

        assertThat(tax).isEqualByComparingTo("5");
    }

    @Test
    void resolveTaxPercentForServiceCode_shouldReturnZero_whenAccountMissing() {
        assertThat(chargeLineSupport.resolveTaxPercentForServiceCode("DXB", null, "DTC", "SC1"))
                .isEqualByComparingTo("0");
    }

    @Test
    void convertAmountBetweenCurrencies_shouldReturnSameAmount_whenCurrenciesMatch() {
        BigDecimal result = chargeLineSupport.convertAmountBetweenCurrencies(
                BigDecimal.TEN, "USD", "usd", "FX");

        assertThat(result).isEqualByComparingTo("10");
        verify(exchangeRateService, never()).convertAmount(anyDouble(), anyString(), anyString(), any(), any());
    }

    @Test
    void convertAmountBetweenCurrencies_shouldConvert_whenCurrenciesDiffer() {
        when(exchangeRateService.convertAmount(10.0, "USD", "AED", null, "FX"))
                .thenReturn(new CurrencyConversionResult(36.7, 3.67));

        BigDecimal result = chargeLineSupport.convertAmountBetweenCurrencies(
                BigDecimal.TEN, "USD", "AED", "FX");

        assertThat(result).isEqualByComparingTo("36.7");
    }

    @Test
    void applyChargeHeaderAmounts_shouldDelegateToChargeHeaderSupport() {
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTC");
        ChargeLineItem line = CalculatorTestDataFactory.chargeLineItem(charge, account, "SC1", BigDecimal.valueOf(12));

        chargeLineSupport.applyChargeHeaderAmounts(charge, List.of(line));

        assertThat(charge.getEstimateAmount()).isEqualByComparingTo("12");
    }
}
