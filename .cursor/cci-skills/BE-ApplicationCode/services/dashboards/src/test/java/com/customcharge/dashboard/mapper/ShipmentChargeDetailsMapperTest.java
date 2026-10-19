package com.customcharge.dashboard.mapper;

import com.customcharge.dashboard.dto.charge.ChargeLineItemRowDTO;
import com.customcharge.dashboard.dto.charge.ChargeTypeDetailsDTO;
import com.customcharge.dashboard.dto.charge.ShipmentChargeDetailsResponse;
import com.customcharge.dashboard.dto.charge.ShipmentSellerInfoDTO;
import com.customcharge.persistencelayer.entities.APAccount;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.enums.AccountNature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentChargeDetailsMapperTest {

    private ShipmentChargeDetailsMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ShipmentChargeDetailsMapper();
    }

    @Test
    void toResponse_shouldBuildBoeListFromPaymentReferenceLinesOrderedByCreatedTime() {
        Shipment shipment = shipment("AWB-BOE", "DDP");
        Charge ecsCharge = charge("ECS", "DXB", shipment);

        ChargeLineItem first = lineItem(ecsCharge, "SC01", "PAY-1", BigDecimal.TEN, null);
        first.setDeclarationNumber("BOE-001");
        ReflectionTestUtils.setField(first, "createdTime", Instant.parse("2024-01-01T10:00:00Z"));

        ChargeLineItem duplicate = lineItem(ecsCharge, "SC02", "PAY-1", BigDecimal.ONE, null);
        duplicate.setDeclarationNumber("BOE-001");
        ReflectionTestUtils.setField(duplicate, "createdTime", Instant.parse("2024-01-01T10:01:00Z"));

        ChargeLineItem nullBoe = lineItem(ecsCharge, "SC03", "PAY-1", BigDecimal.ONE, null);
        nullBoe.setDeclarationNumber(null);
        ReflectionTestUtils.setField(nullBoe, "createdTime", Instant.parse("2024-01-01T10:02:00Z"));

        ChargeLineItem second = lineItem(ecsCharge, "SC04", "PAY-1", BigDecimal.ONE, null);
        second.setDeclarationNumber("BOE-002");
        ReflectionTestUtils.setField(second, "createdTime", Instant.parse("2024-01-01T10:03:00Z"));

        ChargeLineItem withoutPaymentRef = lineItem(ecsCharge, "SC05", null, BigDecimal.ONE, null);
        withoutPaymentRef.setDeclarationNumber("BOE-999");
        ReflectionTestUtils.setField(withoutPaymentRef, "createdTime", Instant.parse("2024-01-01T10:04:00Z"));

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "ECS",
                Set.of("ECS"),
                List.of(ecsCharge),
                lineItemsByChargeId(
                        ecsCharge,
                        List.of(first, duplicate, nullBoe, second, withoutPaymentRef)),
                Map.of(),
                Map.of(),
                Map.of());

        ChargeTypeDetailsDTO ecs = chargeDetail(response, "ECS");
        assertThat(ecs.getBoeList()).containsExactly("BOE-001", null, "BOE-002");
        assertThat(ecs.getPaymentReferenceList()).containsExactly("PAY-1", "PAY-1", "PAY-1");
    }

    @Test
    void toResponse_shouldBuildBoeListUsingPaymentReferenceNotDeclarationNumberForUniqueness() {
        Shipment shipment = shipment("AWB-BOE-PR", "DDU");
        Charge ecdCharge = charge("ECD", "DXB", shipment);

        ChargeLineItem first = lineItem(ecdCharge, "SC01", "1234", BigDecimal.TEN, null);
        first.setDeclarationNumber("2345");
        ReflectionTestUtils.setField(first, "createdTime", Instant.parse("2024-01-01T10:00:00Z"));

        ChargeLineItem second = lineItem(ecdCharge, "SC02", "12345", BigDecimal.ONE, null);
        second.setDeclarationNumber("2345");
        ReflectionTestUtils.setField(second, "createdTime", Instant.parse("2024-01-01T10:01:00Z"));

        ChargeLineItem third = lineItem(ecdCharge, "SC03", "12345", BigDecimal.ONE, null);
        third.setDeclarationNumber(null);
        ReflectionTestUtils.setField(third, "createdTime", Instant.parse("2024-01-01T10:02:00Z"));

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "ECD",
                Set.of("ECD"),
                List.of(ecdCharge),
                lineItemsByChargeId(ecdCharge, List.of(first, second, third)),
                Map.of(),
                Map.of(),
                Map.of());

        ChargeTypeDetailsDTO ecd = chargeDetail(response, "ECD");
        assertThat(ecd.getBoeList()).containsExactly("2345", "2345", null);
        assertThat(ecd.getPaymentReferenceList()).containsExactly("1234", "12345", "12345");
    }

    @Test
    void toResponse_shouldMirrorDtcActualsToEcdWithoutFx() {
        Shipment shipment = shipment("AWB-1", "DDU");
        Charge ecdCharge = charge("ECD", "DXB", shipment);
        Charge dtcCharge = charge("DTC", "DXB", shipment);

        ChargeLineItem ecdLine = lineItem(ecdCharge, "SC01", null, BigDecimal.TEN, null);
        ChargeLineItem dtcLine = lineItem(dtcCharge, "SC01", "PAY-1", BigDecimal.valueOf(50), null);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "ECD",
                Set.of("ECD", "DTC"),
                List.of(ecdCharge, dtcCharge),
                lineItemsByChargeId(ecdCharge, List.of(ecdLine), dtcCharge, List.of(dtcLine)),
                Map.of(),
                Map.of(),
                Map.of());

        ChargeTypeDetailsDTO ecd = chargeDetail(response, "ECD");
        ChargeTypeDetailsDTO dtc = chargeDetail(response, "DTC");

        assertThat(ecd.getLineItems().get(0).getActualOne()).isEqualByComparingTo("50");
        assertThat(dtc.getLineItems().get(0).getActualOne()).isEqualByComparingTo("50");
    }

    @Test
    void toResponse_shouldUseContractedRateForRevenueOutstandingWhenPresent() {
        Shipment shipment = shipment("AWB-CR-1", "DDU");
        Charge dtcCharge = charge("DTC", "DXB", shipment);
        ChargeLineItem line = lineItem(dtcCharge, "SC01", null, BigDecimal.valueOf(100), null);
        line.setContractedRate(BigDecimal.valueOf(50));

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTC",
                Set.of("DTC"),
                List.of(dtcCharge),
                lineItemsByChargeId(dtcCharge, List.of(line)),
                Map.of(),
                Map.of(),
                Map.of());

        ChargeLineItemRowDTO row = chargeDetail(response, "DTC").getLineItems().get(0);
        assertThat(row.getContractedRate()).isEqualByComparingTo("50");
        assertThat(row.getEstimateAmount()).isEqualByComparingTo("100");
        assertThat(row.getEligibleCost()).isEqualByComparingTo("150");
        assertThat(row.getOutstanding()).isEqualByComparingTo("50");
        assertThat(chargeDetail(response, "DTC").getTotals().getContractRateTotal())
                .isEqualByComparingTo("50");
    }

    @Test
    void toResponse_shouldUseEstimateAmountForRevenueOutstandingWhenContractedRateMissing() {
        Shipment shipment = shipment("AWB-CR-3", "DDU");
        Charge dtcCharge = charge("DTC", "DXB", shipment);
        ChargeLineItem line = lineItem(dtcCharge, "SC01", null, BigDecimal.valueOf(100), null);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTC",
                Set.of("DTC"),
                List.of(dtcCharge),
                lineItemsByChargeId(dtcCharge, List.of(line)),
                Map.of(),
                Map.of(),
                Map.of());

        ChargeLineItemRowDTO row = chargeDetail(response, "DTC").getLineItems().get(0);
        assertThat(row.getEstimateAmount()).isEqualByComparingTo("100");
        assertThat(row.getOutstanding()).isEqualByComparingTo("100");
    }

    @Test
    void toResponse_shouldUseContractedRateForRevenueOutstandingOnActualLine() {
        Shipment shipment = shipment("AWB-CR-2", "DDU");
        Charge dtcCharge = charge("DTC", "DXB", shipment);
        ChargeLineItem line = lineItem(dtcCharge, "SC01", "PAY-1", BigDecimal.valueOf(80), null);
        line.setContractedRate(BigDecimal.valueOf(20));

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTC",
                Set.of("DTC"),
                List.of(dtcCharge),
                lineItemsByChargeId(dtcCharge, List.of(line)),
                Map.of(),
                Map.of(),
                Map.of());

        ChargeLineItemRowDTO row = chargeDetail(response, "DTC").getLineItems().get(0);
        assertThat(row.getContractedRate()).isEqualByComparingTo("20");
        assertThat(row.getEstimateAmount()).isEqualByComparingTo("80");
        assertThat(row.getActualOne()).isEqualByComparingTo("100");
        assertThat(row.getEligibleCost()).isEqualByComparingTo("100");
        assertThat(row.getOutstanding()).isEqualByComparingTo("20");
        assertThat(chargeDetail(response, "DTC").getTotals().getContractRateTotal())
                .isEqualByComparingTo("20");
    }

    @Test
    void toResponse_shouldUsePreConvertedAmountWhenMirroringDtaToEcd() {
        Shipment shipment = shipment("AWB-2", "DDP");
        Charge ecdCharge = charge("ECD", "DXB", shipment);
        Charge dtaCharge = charge("DTA", "DXB", shipment);

        ChargeLineItem ecdLine = lineItem(ecdCharge, "SC01", null, BigDecimal.TEN, null);
        ChargeLineItem dtaLine = lineItem(
                dtaCharge, "SC01", "PAY-1", BigDecimal.valueOf(375), BigDecimal.valueOf(100));

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "ECD",
                Set.of("ECD", "DTA"),
                List.of(ecdCharge, dtaCharge),
                lineItemsByChargeId(ecdCharge, List.of(ecdLine), dtaCharge, List.of(dtaLine)),
                Map.of(),
                Map.of(),
                Map.of());

        ChargeTypeDetailsDTO ecd = chargeDetail(response, "ECD");
        ChargeTypeDetailsDTO dta = chargeDetail(response, "DTA");

        assertThat(ecd.getLineItems().get(0).getActualOne()).isEqualByComparingTo("100");
        assertThat(dta.getLineItems().get(0).getActualOne()).isEqualByComparingTo("375");
    }

    @Test
    void toResponse_shouldUsePreConvertedAmountWhenMirroringDtsToEcs() {
        Shipment shipment = shipment("AWB-3", "DDP");
        Charge ecsCharge = charge("ECS", "RUH", shipment);
        Charge dtsCharge = charge("DTS", "RUH", shipment);

        ChargeLineItem ecsLine = lineItem(ecsCharge, "DUTY", null, BigDecimal.ONE, null);
        ChargeLineItem dtsLine = lineItem(
                dtsCharge, "DUTY", "PAY-1", BigDecimal.valueOf(200), BigDecimal.valueOf(50));

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "ECS",
                Set.of("ECS", "DTS"),
                List.of(ecsCharge, dtsCharge),
                lineItemsByChargeId(ecsCharge, List.of(ecsLine), dtsCharge, List.of(dtsLine)),
                Map.of(),
                Map.of(),
                Map.of());

        assertThat(chargeDetail(response, "ECS").getLineItems().get(0).getActualOne())
                .isEqualByComparingTo("50");
        assertThat(chargeDetail(response, "DTS").getLineItems().get(0).getActualOne())
                .isEqualByComparingTo("200");
    }

    @Test
    void toResponse_shouldUsePreConvertedAmountWhenMirroringDt3ToEc3() {
        Shipment shipment = shipment("AWB-4", "DDP");
        Charge ec3Charge = charge("EC3", "JED", shipment);
        Charge dt3Charge = charge("DT3", "JED", shipment);

        ChargeLineItem ec3Line = lineItem(ec3Charge, "TAX", null, BigDecimal.ONE, null);
        ChargeLineItem dt3Line = lineItem(
                dt3Charge, "TAX", "PAY-1", BigDecimal.valueOf(90), BigDecimal.valueOf(30));

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "EC3",
                Set.of("EC3", "DT3"),
                List.of(ec3Charge, dt3Charge),
                lineItemsByChargeId(ec3Charge, List.of(ec3Line), dt3Charge, List.of(dt3Line)),
                Map.of(),
                Map.of(),
                Map.of());

        assertThat(chargeDetail(response, "EC3").getLineItems().get(0).getActualOne())
                .isEqualByComparingTo("30");
        assertThat(chargeDetail(response, "DT3").getLineItems().get(0).getActualOne())
                .isEqualByComparingTo("90");
    }

    @Test
    void toResponse_shouldFallbackToRevenueAmountWhenMissingPreConvertedAmount() {
        Shipment shipment = shipment("AWB-5", "DDP");
        Charge ecdCharge = charge("ECD", "DXB", shipment);
        Charge dtaCharge = charge("DTA", "DXB", shipment);

        ChargeLineItem ecdLine = lineItem(ecdCharge, "SC01", null, BigDecimal.TEN, null);
        ChargeLineItem dtaLine = lineItem(dtaCharge, "SC01", "PAY-1", BigDecimal.valueOf(375), null);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "ECD",
                Set.of("ECD", "DTA"),
                List.of(ecdCharge, dtaCharge),
                lineItemsByChargeId(ecdCharge, List.of(ecdLine), dtaCharge, List.of(dtaLine)),
                Map.of(),
                Map.of(),
                Map.of());

        assertThat(chargeDetail(response, "ECD").getLineItems().get(0).getActualOne())
                .isEqualByComparingTo("375");
        assertThat(chargeDetail(response, "DTA").getLineItems().get(0).getActualOne())
                .isEqualByComparingTo("375");
    }

    @Test
    void toResponse_shouldKeepZeroEcdEligibleCostWhenCostEstimateIsZeroAndRevenueHasNoActuals() {
        Shipment shipment = shipment("AWB-6", "DDU");
        Charge ecdCharge = charge("ECD", "RUH", shipment);
        Charge dtcCharge = charge("DTC", "RUH", shipment);

        ChargeLineItem ecdClearance = lineItem(ecdCharge, "Clearance Fee", null, BigDecimal.ZERO, null);
        ChargeLineItem ecdVat = lineItem(ecdCharge, "Import VAT", null, BigDecimal.valueOf(275), null);
        ChargeLineItem dtcClearance = lineItem(dtcCharge, "Clearance Fee", null, BigDecimal.valueOf(105), null);
        ChargeLineItem dtcVat = lineItem(dtcCharge, "Import VAT", null, BigDecimal.valueOf(275), null);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTC",
                Set.of("ECD", "DTC"),
                List.of(ecdCharge, dtcCharge),
                lineItemsByChargeId(
                        ecdCharge, List.of(ecdClearance, ecdVat),
                        dtcCharge, List.of(dtcClearance, dtcVat)),
                Map.of(),
                Map.of(),
                Map.of());

        ChargeTypeDetailsDTO ecd = chargeDetail(response, "ECD");

        assertThat(findLine(ecd, "Clearance Fee").getEstimateAmount()).isEqualByComparingTo("0");
        assertThat(findLine(ecd, "Clearance Fee").getEligibleCost()).isEqualByComparingTo("0");
        assertThat(findLine(ecd, "Clearance Fee").getActualOne()).isNull();
        assertThat(findLine(ecd, "Import VAT").getEligibleCost()).isEqualByComparingTo("275");
        assertThat(ecd.getTotals().getEstimateAmount()).isEqualByComparingTo("275");
        assertThat(ecd.getTotals().getEligibleCost()).isEqualByComparingTo("275");
    }

    @Test
    void toResponse_shouldUseCollectAmountAndCurrencyForGoodsSummary() {
        Shipment shipment = shipment("AWB-9", "DDP");
        shipment.setCollectAmount(1700.0);
        shipment.setCollectCurrency("AED");
        shipment.setCostOfGoods(999.0);
        shipment.setInsuranceValue(1.0);
        shipment.setCustomsCurrency("SAR");

        Charge ecdCharge = charge("ECD", "JED", shipment);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "ECD",
                Set.of("ECD"),
                List.of(ecdCharge),
                lineItemsByChargeId(ecdCharge, List.of()),
                Map.of(),
                Map.of(),
                Map.of());

        assertThat(chargeDetail(response, "ECD")
                .getShipment()
                .getAdditionalDetails()
                .getGoodsSummary()
                .getCollectedValue())
                .isEqualByComparingTo("1700");
        assertThat(chargeDetail(response, "ECD")
                .getShipment()
                .getAdditionalDetails()
                .getGoodsSummary()
                .getCollectedValueCurrency())
                .isEqualTo("AED");
    }

    @Test
    void toResponse_shouldUseShipperFieldsForSellerInformationAcrossChargeTypes() {
        Shipment shipment = shipmentWithShipper("AWB-8", "DDU");
        Charge ecdCharge = charge("ECD", "JED", shipment);
        Charge dtcCharge = charge("DTC", "JED", shipment);

        Account entitySettingsAccount = new Account();
        entitySettingsAccount.setChargeType("ECD");

        Map<String, Account> entitySettingsAccounts = Map.of(
                ShipmentChargeDetailsMapper.accountKey("JED", "ECD"),
                entitySettingsAccount);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTC",
                Set.of("ECD", "DTC"),
                List.of(ecdCharge, dtcCharge),
                lineItemsByChargeId(ecdCharge, List.of(), dtcCharge, List.of()),
                Map.of(),
                entitySettingsAccounts,
                Map.of());

        ShipmentSellerInfoDTO expectedSeller = ShipmentSellerInfoDTO.builder()
                .country("EG")
                .entity("RUH")
                .accountNumber("9003")
                .accountName("SPX7")
                .collectionReference("C109845")
                .build();

        assertThat(chargeDetail(response, "ECD").getShipment().getAdditionalDetails().getSellerInformation())
                .isEqualTo(expectedSeller);
        assertThat(chargeDetail(response, "DTC").getShipment().getAdditionalDetails().getSellerInformation())
                .isEqualTo(expectedSeller);
    }

    @Test
    void toResponse_shouldUseArAccountNameForSellerInformationWhenArAccountFound() {
        Shipment shipment = shipmentWithShipper("AWB-SELLER-AR", "DDU");
        Charge ecdCharge = charge("ECD", "JED", shipment);

        ARAccount sellerArAccount = new ARAccount();
        sellerArAccount.setAccountNumber("9003");
        sellerArAccount.setEntityCode("RUH");
        sellerArAccount.setAccountName("AR Seller Name");

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "ECD",
                Set.of("ECD"),
                List.of(ecdCharge),
                lineItemsByChargeId(ecdCharge, List.of()),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                sellerArAccount);

        assertThat(chargeDetail(response, "ECD").getShipment().getAdditionalDetails().getSellerInformation())
                .isEqualTo(ShipmentSellerInfoDTO.builder()
                        .country("EG")
                        .entity("RUH")
                        .accountNumber("9003")
                        .accountName("AR Seller Name")
                        .collectionReference("C109845")
                        .build());
    }

    @Test
    void toResponse_shouldNotAddRevenueOnlyServiceCodesToEcd() {
        Shipment shipment = shipment("AWB-REV-ONLY", "DDU");
        Charge ecdCharge = charge("ECD", "DXB", shipment);
        Charge dtcCharge = charge("DTC", "DXB", shipment);

        ChargeLineItem ecdClearance = lineItem(ecdCharge, "Clearance Fee", null, BigDecimal.TEN, null);
        ChargeLineItem ecdVat = lineItem(ecdCharge, "Import VAT", null, BigDecimal.valueOf(20), null);
        ChargeLineItem dtcClearance = lineItem(dtcCharge, "Clearance Fee", "PAY-1", BigDecimal.valueOf(50), null);
        ChargeLineItem dtcVat = lineItem(dtcCharge, "Import VAT", "PAY-1", BigDecimal.valueOf(30), null);
        ChargeLineItem dtcExtraOne = lineItem(dtcCharge, "Extra Fee 1", "PAY-1", BigDecimal.valueOf(10), null);
        ChargeLineItem dtcExtraTwo = lineItem(dtcCharge, "Extra Fee 2", "PAY-1", BigDecimal.valueOf(15), null);
        ChargeLineItem dtcExtraThree = lineItem(dtcCharge, "Extra Fee 3", "PAY-1", BigDecimal.valueOf(5), null);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "ECD",
                Set.of("ECD", "DTC"),
                List.of(ecdCharge, dtcCharge),
                lineItemsByChargeId(
                        ecdCharge, List.of(ecdClearance, ecdVat),
                        dtcCharge, List.of(dtcClearance, dtcVat, dtcExtraOne, dtcExtraTwo, dtcExtraThree)),
                Map.of(),
                Map.of(),
                Map.of());

        ChargeTypeDetailsDTO ecd = chargeDetail(response, "ECD");
        ChargeTypeDetailsDTO dtc = chargeDetail(response, "DTC");

        assertThat(ecd.getLineItems()).hasSize(2);
        assertThat(ecd.getLineItems())
                .extracting(ChargeLineItemRowDTO::getServiceCode)
                .containsExactly("Clearance Fee", "Import VAT");
        assertThat(findLine(ecd, "Clearance Fee").getActualOne()).isEqualByComparingTo("50");
        assertThat(findLine(ecd, "Import VAT").getActualOne()).isEqualByComparingTo("30");
        assertThat(dtc.getLineItems()).hasSize(5);
    }

    @Test
    void toResponse_shouldNotOverwriteExistingCostActualsWhenMirroringFromRevenue() {
        Shipment shipment = shipment("AWB-KEEP-ACTUALS", "DDU");
        Charge ecdCharge = charge("ECD", "DXB", shipment);
        Charge dtcCharge = charge("DTC", "DXB", shipment);

        ChargeLineItem ecdLine = lineItem(ecdCharge, "Clearance Fee", "PAY-ECD", BigDecimal.TEN, null);
        ChargeLineItem dtcLine = lineItem(dtcCharge, "Clearance Fee", "PAY-DTC", BigDecimal.valueOf(50), null);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "ECD",
                Set.of("ECD", "DTC"),
                List.of(ecdCharge, dtcCharge),
                lineItemsByChargeId(ecdCharge, List.of(ecdLine), dtcCharge, List.of(dtcLine)),
                Map.of(),
                Map.of(),
                Map.of());

        assertThat(chargeDetail(response, "ECD").getLineItems().get(0).getActualOne())
                .isEqualByComparingTo("10");
    }

    @Test
    void toResponse_shouldNotAddRevenueOnlyServiceCodesToEcs() {
        Shipment shipment = shipment("AWB-ECS-REV-ONLY", "DDP");
        Charge ecsCharge = charge("ECS", "RUH", shipment);
        Charge dtsCharge = charge("DTS", "RUH", shipment);

        ChargeLineItem ecsDuty = lineItem(ecsCharge, "DUTY", null, BigDecimal.ONE, null);
        ChargeLineItem dtsDuty = lineItem(
                dtsCharge, "DUTY", "PAY-1", BigDecimal.valueOf(200), BigDecimal.valueOf(50));
        ChargeLineItem dtsExtra = lineItem(
                dtsCharge, "Extra Fee", "PAY-1", BigDecimal.valueOf(40), BigDecimal.valueOf(10));

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "ECS",
                Set.of("ECS", "DTS"),
                List.of(ecsCharge, dtsCharge),
                lineItemsByChargeId(ecsCharge, List.of(ecsDuty), dtsCharge, List.of(dtsDuty, dtsExtra)),
                Map.of(),
                Map.of(),
                Map.of());

        ChargeTypeDetailsDTO ecs = chargeDetail(response, "ECS");
        ChargeTypeDetailsDTO dts = chargeDetail(response, "DTS");

        assertThat(ecs.getLineItems()).hasSize(1);
        assertThat(ecs.getLineItems().get(0).getServiceCode()).isEqualTo("DUTY");
        assertThat(ecs.getLineItems().get(0).getActualOne()).isEqualByComparingTo("50");
        assertThat(dts.getLineItems()).hasSize(2);
    }

    @Test
    void toResponse_shouldUseMirroredActualsForEligibleCostButNotRevenueEstimateWhenCostIsZero() {
        Shipment shipment = shipment("AWB-7", "DDU");
        Charge ecdCharge = charge("ECD", "DXB", shipment);
        Charge dtcCharge = charge("DTC", "DXB", shipment);

        ChargeLineItem ecdLine = lineItem(ecdCharge, "Clearance Fee", null, BigDecimal.ZERO, null);
        ChargeLineItem dtcLine = lineItem(dtcCharge, "Clearance Fee", "PAY-1", BigDecimal.valueOf(50), null);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "ECD",
                Set.of("ECD", "DTC"),
                List.of(ecdCharge, dtcCharge),
                lineItemsByChargeId(ecdCharge, List.of(ecdLine), dtcCharge, List.of(dtcLine)),
                Map.of(),
                Map.of(),
                Map.of());

        ChargeTypeDetailsDTO ecd = chargeDetail(response, "ECD");

        assertThat(findLine(ecd, "Clearance Fee").getEstimateAmount()).isEqualByComparingTo("0");
        assertThat(findLine(ecd, "Clearance Fee").getActualOne()).isEqualByComparingTo("50");
        assertThat(findLine(ecd, "Clearance Fee").getEligibleCost()).isEqualByComparingTo("50");
    }

    private static ChargeLineItemRowDTO findLine(
            ChargeTypeDetailsDTO charge, String serviceCode) {
        return charge.getLineItems().stream()
                .filter(row -> serviceCode.equals(row.getServiceCode()))
                .findFirst()
                .orElseThrow();
    }

    private static Shipment shipment(String hawb, String shipmentType) {
        Shipment shipment = new Shipment();
        ReflectionTestUtils.setField(shipment, "id", UUID.randomUUID());
        shipment.setHawbNumber(hawb);
        shipment.setShipmentType(shipmentType);
        return shipment;
    }

    private static Shipment shipmentWithShipper(String hawb, String shipmentType) {
        Shipment shipment = shipment(hawb, shipmentType);
        shipment.setShipperCountryCode("EG");
        shipment.setOriginEntity("RUH");
        shipment.setShipperNumber("9003");
        shipment.setShipperName("SPX7");
        shipment.setCollectionReference("C109845");
        return shipment;
    }

    private static Charge charge(String chargeCode, String entityCode, Shipment shipment) {
        Entity entity = new Entity();
        entity.setCode(entityCode);

        Charge charge = new Charge();
        ReflectionTestUtils.setField(charge, "id", UUID.randomUUID());
        charge.setChargeCode(chargeCode);
        charge.setEntity(entity);
        charge.setShipment(shipment);
        charge.setCurrency("USD");
        return charge;
    }

    private static ChargeLineItem lineItem(
            Charge charge,
            String serviceCode,
            String paymentReference,
            BigDecimal amount,
            BigDecimal preConvertedAmount) {

        ChargeLineItem item = new ChargeLineItem();
        ReflectionTestUtils.setField(item, "id", UUID.randomUUID());
        item.setCharge(charge);
        item.setServiceCode(serviceCode);
        item.setPaymentReference(paymentReference);
        item.setAmount(amount);
        item.setPreConvertedAmount(preConvertedAmount);
        return item;
    }

    @Test
    void toResponse_shouldZeroEligibleCostOnEstimateOnlyLineWhenOtherLinesHaveActuals() {
        Shipment shipment = shipment("SHIPPER22", "DDP");
        Charge ecsCharge = charge("ECS", "DXB", shipment);

        List<ChargeLineItem> ecsLines = List.of(
                lineItem(ecsCharge, "Cost", "PAY-1", BigDecimal.valueOf(16.1), null),
                lineItem(ecsCharge, "Customs Duty Outbound", "PAY-1", BigDecimal.valueOf(16.0), null),
                lineItem(ecsCharge, "Other Charges", "PAY-1", BigDecimal.valueOf(22.54), null),
                lineItem(ecsCharge, "Other Customs Charges", "PAY-1", BigDecimal.valueOf(21.33), null),
                lineItem(ecsCharge, "Transit Charges", "PAY-1", BigDecimal.valueOf(24.0), null),
                lineItem(ecsCharge, "VAT", null, BigDecimal.valueOf(8.0), null));

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "ECS",
                Set.of("ECS"),
                List.of(ecsCharge),
                lineItemsByChargeId(ecsCharge, ecsLines),
                Map.of(),
                Map.of(),
                Map.of());

        ChargeTypeDetailsDTO ecs = chargeDetail(response, "ECS");
        ChargeLineItemRowDTO vat = findLine(ecs, "VAT");

        assertThat(vat.getEstimateAmount()).isEqualByComparingTo("8");
        assertThat(vat.getActualOne()).isNull();
        assertThat(vat.getEligibleCost()).isEqualByComparingTo("0");
        assertThat(vat.getOutstanding()).isEqualByComparingTo("0");
        assertThat(ecs.getTotals().getTotalActuals()).isEqualByComparingTo("99.97");
        assertThat(ecs.getTotals().getEligibleCost()).isEqualByComparingTo("99.97");
        assertThat(ecs.getTotals().getOutstanding()).isEqualByComparingTo("99.97");
        assertThat(ecs.getOutstandingAmount()).isEqualByComparingTo("99.97");
    }

    @Test
    void toResponse_shouldUseApAccountNumberOnCostLineItems() {
        Shipment shipment = shipment("AWB-AP-LINE", "DDP");
        Charge ecdCharge = charge("ECD", "JED", shipment);

        ChargeLineItem ecdLine = lineItem(ecdCharge, "SC01", null, BigDecimal.TEN, null);
        ecdLine.setApAccountNumber("AP-LINE-001");

        APAccount apAccount = new APAccount();
        apAccount.setAccountNumber("AP-LINE-001");
        apAccount.setAccountName("AP Line Master Name");

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "ECD",
                Set.of("ECD"),
                List.of(ecdCharge),
                lineItemsByChargeId(ecdCharge, List.of(ecdLine)),
                Map.of(),
                Map.of(),
                Map.of(ShipmentChargeDetailsMapper.apAccountKey("JED", "AP-LINE-001"), apAccount));

        ChargeLineItemRowDTO line = chargeDetail(response, "ECD").getLineItems().get(0);
        assertThat(line.getAccountNumber()).isEqualTo("AP-LINE-001");
        assertThat(line.getAccountName()).isEqualTo("AP Line Master Name");
    }

    @Test
    void toResponse_shouldUseApMasterNameOnEcsLineItems() {
        Shipment shipment = shipment("AWB-ECS-AP", "DDP");
        Charge ecsCharge = charge("ECS", "RUH", shipment);
        ecsCharge.setAccountType("ACCOUNT");

        ChargeLineItem ecsLine = lineItem(ecsCharge, "SC01", null, BigDecimal.TEN, null);
        ecsLine.setApAccountNumber("ECS-AP-001");

        APAccount apAccount = new APAccount();
        apAccount.setAccountNumber("ECS-AP-001");
        apAccount.setAccountName("ECS AP Vendor");

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "ECS",
                Set.of("ECS"),
                List.of(ecsCharge),
                lineItemsByChargeId(ecsCharge, List.of(ecsLine)),
                Map.of(),
                Map.of(),
                Map.of(ShipmentChargeDetailsMapper.apAccountKey("RUH", "ECS-AP-001"), apAccount));

        ChargeTypeDetailsDTO ecs = chargeDetail(response, "ECS");
        assertThat(ecs.getAccountInformation().getAccountNumber()).isEqualTo("ECS-AP-001");
        assertThat(ecs.getAccountInformation().getAccountName()).isEqualTo("ECS AP Vendor");
        assertThat(ecs.getAccountInformation().getAccountType()).isEqualTo("ACCOUNT");
        ChargeLineItemRowDTO line = ecs.getLineItems().get(0);
        assertThat(line.getAccountNumber()).isEqualTo("ECS-AP-001");
        assertThat(line.getAccountName()).isEqualTo("ECS AP Vendor");
    }

    @Test
    void toResponse_shouldUseLatestApAccountNumberOnEc3AccountInformation() {
        Shipment shipment = shipment("AWB-EC3-AP", "DDP");
        Charge ec3Charge = charge("EC3", "JED", shipment);
        ec3Charge.setAccountType("ACCOUNT");

        ChargeLineItem firstLine = lineItem(ec3Charge, "SC00", null, BigDecimal.TEN, null);
        firstLine.setApAccountNumber(" EC3-AP-001 ");
        ReflectionTestUtils.setField(firstLine, "createdTime", Instant.parse("2024-01-01T10:00:00Z"));
        ChargeLineItem secondLine = lineItem(ec3Charge, "SC01", null, BigDecimal.valueOf(20), null);
        secondLine.setApAccountNumber("EC3-AP-002");
        ReflectionTestUtils.setField(secondLine, "createdTime", Instant.parse("2024-01-01T10:01:00Z"));

        APAccount apAccount = new APAccount();
        apAccount.setAccountNumber("EC3-AP-002");
        apAccount.setAccountName("EC3 AP Vendor Latest");

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "EC3",
                Set.of("EC3"),
                List.of(ec3Charge),
                lineItemsByChargeId(ec3Charge, List.of(firstLine, secondLine)),
                Map.of(),
                Map.of(),
                Map.of(ShipmentChargeDetailsMapper.apAccountKey("JED", "EC3-AP-002"), apAccount));

        assertThat(chargeDetail(response, "EC3").getAccountInformation().getAccountNumber()).isEqualTo("EC3-AP-002");
        assertThat(chargeDetail(response, "EC3").getAccountInformation().getAccountName()).isEqualTo("EC3 AP Vendor Latest");
    }

    @Test
    void toResponse_shouldKeepArAccountNumberOnRevenueLineItems() {
        Shipment shipment = shipment("AWB-AR-LINE", "DDU");
        shipment.setConsigneeNumber("CONS-99");
        Charge dtcCharge = charge("DTC", "DXB", shipment);

        Account arAccount = new Account();
        arAccount.setAccountNature(AccountNature.AR);
        ARAccount arMaster = new ARAccount();
        arMaster.setAccountNumber("AR-LINE-88");
        arAccount.setArAccount(arMaster);

        ChargeLineItem dtcLine = lineItem(dtcCharge, "SC01", "PAY-1", BigDecimal.TEN, null);
        dtcLine.setAccount(arAccount);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTC",
                Set.of("DTC"),
                List.of(dtcCharge),
                lineItemsByChargeId(dtcCharge, List.of(dtcLine)),
                Map.of(),
                Map.of(),
                Map.of());

        assertThat(chargeDetail(response, "DTC").getLineItems().get(0).getAccountNumber())
                .isEqualTo("AR-LINE-88");
    }

    @Test
    void toResponse_shouldUseDtcArMasterWhenChargeAccountIsCompleteAndLineAccountIsEmpty() {
        Shipment shipment = shipment("AWB-DTC-AR-FALLBACK", "DDU");
        shipment.setConsigneeNumber("60502729");
        shipment.setConsigneeName("Clara Hair");
        Charge dtcCharge = charge("DTC", "RUH", shipment);

        ChargeLineItem dtcLine = lineItem(dtcCharge, "VAT", null, BigDecimal.TEN, null);

        ARAccount arMaster = new ARAccount();
        arMaster.setAccountNumber("60502729");
        arMaster.setAccountName("Clara Hair");
        arMaster.setEntityCode("RUH");

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTC",
                Set.of("DTC"),
                List.of(dtcCharge),
                lineItemsByChargeId(dtcCharge, List.of(dtcLine)),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(ShipmentChargeDetailsMapper.arAccountKey("RUH", "60502729"), arMaster));

        ChargeLineItemRowDTO line = chargeDetail(response, "DTC").getLineItems().get(0);
        assertThat(line.getAccountNumber()).isEqualTo("60502729");
        assertThat(line.getAccountName()).isEqualTo("Clara Hair");
    }

    @Test
    void toResponse_shouldNotUseDtcArMasterWhenChargeAccountNameIsMissing() {
        Shipment shipment = shipment("AWB-DTC-AR-INCOMPLETE", "DDU");
        shipment.setConsigneeNumber("60502729");
        shipment.setConsigneeName(null);
        Charge dtcCharge = charge("DTC", "RUH", shipment);

        ChargeLineItem dtcLine = lineItem(dtcCharge, "VAT", null, BigDecimal.TEN, null);

        ARAccount arMaster = new ARAccount();
        arMaster.setAccountNumber("60502729");
        arMaster.setAccountName("Clara Hair");
        arMaster.setEntityCode("RUH");

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTC",
                Set.of("DTC"),
                List.of(dtcCharge),
                lineItemsByChargeId(dtcCharge, List.of(dtcLine)),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(ShipmentChargeDetailsMapper.arAccountKey("RUH", "60502729"), arMaster));

        ChargeLineItemRowDTO line = chargeDetail(response, "DTC").getLineItems().get(0);
        assertThat(line.getAccountNumber()).isNull();
        assertThat(line.getAccountName()).isNull();
    }

    @Test
    void toResponse_shouldKeepExistingDtcLineAccountInsteadOfArFallback() {
        Shipment shipment = shipment("AWB-DTC-AR-EXISTING", "DDU");
        shipment.setConsigneeNumber("60502729");
        shipment.setConsigneeName("Clara Hair");
        Charge dtcCharge = charge("DTC", "RUH", shipment);

        Account lineAccount = new Account();
        lineAccount.setAccountNature(AccountNature.AR);
        ARAccount lineArMaster = new ARAccount();
        lineArMaster.setAccountNumber("LINE-001");
        lineArMaster.setAccountName("Existing Line Account");
        lineAccount.setArAccount(lineArMaster);

        ChargeLineItem dtcLine = lineItem(dtcCharge, "VAT", null, BigDecimal.TEN, null);
        dtcLine.setAccount(lineAccount);

        ARAccount fallbackArMaster = new ARAccount();
        fallbackArMaster.setAccountNumber("60502729");
        fallbackArMaster.setAccountName("Clara Hair");
        fallbackArMaster.setEntityCode("RUH");

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTC",
                Set.of("DTC"),
                List.of(dtcCharge),
                lineItemsByChargeId(dtcCharge, List.of(dtcLine)),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(ShipmentChargeDetailsMapper.arAccountKey("RUH", "60502729"), fallbackArMaster));

        ChargeLineItemRowDTO line = chargeDetail(response, "DTC").getLineItems().get(0);
        assertThat(line.getAccountNumber()).isEqualTo("LINE-001");
        assertThat(line.getAccountName()).isEqualTo("Existing Line Account");
    }

    @Test
    void toResponse_shouldUseFirstLineItemApAccountNumberForCostChargeTypes() {
        Shipment shipment = shipment("AWB-AP", "DDP");
        Charge ecdCharge = charge("ECD", "JED", shipment);
        ecdCharge.setAccountType("ACCOUNT");

        ChargeLineItem ecdLine = lineItem(ecdCharge, "SC01", null, BigDecimal.TEN, null);
        ecdLine.setApAccountNumber("AP-001");

        APAccount apAccount = new APAccount();
        apAccount.setAccountNumber("AP-001");
        apAccount.setAccountName("JED AP Account");

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "ECD",
                Set.of("ECD"),
                List.of(ecdCharge),
                lineItemsByChargeId(ecdCharge, List.of(ecdLine)),
                Map.of(),
                Map.of(),
                Map.of(ShipmentChargeDetailsMapper.apAccountKey("JED", "AP-001"), apAccount));

        assertThat(chargeDetail(response, "ECD").getAccountInformation().getAccountNumber()).isEqualTo("AP-001");
        assertThat(chargeDetail(response, "ECD").getAccountInformation().getAccountName()).isEqualTo("JED AP Account");
        assertThat(chargeDetail(response, "ECD").getAccountInformation().getAccountType()).isEqualTo("ACCOUNT");
    }

    @Test
    void toResponse_shouldUseFirstDtaLineItemAccountAndPreserveEachLineAccount() {
        Shipment shipment = shipment("AWB-DTA-AP", "DDP");
        shipment.setConsigneeNumber("CONS-99");
        shipment.setConsigneeName("Consignee Ltd");
        Charge ecdCharge = charge("ECD", "JED", shipment);
        Charge dtaCharge = charge("DTA", "JED", shipment);
        dtaCharge.setAccountType("ACCOUNT");

        Account entitySettings = new Account();
        entitySettings.setChargeType("ECD");
        entitySettings.setDefaultAccountNumber("AP-001");
        Map<String, Account> entitySettingsAccounts = Map.of(
                ShipmentChargeDetailsMapper.accountKey("JED", "ECD"),
                entitySettings);

        APAccount apAccount = new APAccount();
        apAccount.setAccountNumber("AP-001");
        apAccount.setAccountName("JED AP Account");

        Map<String, APAccount> apAccounts = Map.of(
                ShipmentChargeDetailsMapper.apAccountKey("JED", "AP-001"),
                apAccount);

        ChargeLineItem ecdLine = lineItem(ecdCharge, "SC01", null, BigDecimal.TEN, null);
        ecdLine.setApAccountNumber("AP-LINE-001");
        Account apLineAccount = new Account();
        apLineAccount.setAccountNature(AccountNature.AP);
        APAccount apLineMaster = new APAccount();
        apLineMaster.setAccountNumber("AP-LINE-001");
        apLineMaster.setAccountName("AP Line Account Name");
        apLineAccount.setApAccount(apLineMaster);
        ecdLine.setAccount(apLineAccount);

        Account arAccount = new Account();
        arAccount.setAccountNature(AccountNature.AR);
        ARAccount arMaster = new ARAccount();
        arMaster.setAccountNumber("AR-DTA-88");
        arMaster.setAccountName("AR DTA Name");
        arAccount.setArAccount(arMaster);

        Account secondArAccount = new Account();
        secondArAccount.setAccountNature(AccountNature.AR);
        ARAccount secondArMaster = new ARAccount();
        secondArMaster.setAccountNumber("AR-DTA-99");
        secondArMaster.setAccountName("Second AR DTA Name");
        secondArAccount.setArAccount(secondArMaster);

        ChargeLineItem dtaLineWithoutAccount =
                lineItem(dtaCharge, "SC00", "PAY-1", BigDecimal.TEN, null);
        ChargeLineItem firstDtaLineWithAccount =
                lineItem(dtaCharge, "SC01", "PAY-2", BigDecimal.valueOf(20), null);
        firstDtaLineWithAccount.setAccount(arAccount);
        ChargeLineItem secondDtaLineWithAccount =
                lineItem(dtaCharge, "SC02", "PAY-3", BigDecimal.valueOf(30), null);
        secondDtaLineWithAccount.setAccount(secondArAccount);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTA",
                Set.of("ECD", "DTA"),
                List.of(ecdCharge, dtaCharge),
                lineItemsByChargeId(
                        ecdCharge,
                        List.of(ecdLine),
                        dtaCharge,
                        List.of(dtaLineWithoutAccount, firstDtaLineWithAccount, secondDtaLineWithAccount)),
                Map.of(),
                entitySettingsAccounts,
                apAccounts);

        ChargeTypeDetailsDTO dta = chargeDetail(response, "DTA");
        assertThat(dta.getAccountInformation().getAccountNumber()).isEqualTo("AR-DTA-88");
        assertThat(dta.getAccountInformation().getAccountName()).isEqualTo("AR DTA Name");
        assertThat(dta.getAccountInformation().getAccountType()).isEqualTo("ACCOUNT");
        assertThat(dta.getLineItems().get(0).getAccountNumber()).isNull();
        assertThat(dta.getLineItems().get(0).getAccountName()).isNull();
        assertThat(dta.getLineItems().get(1).getAccountNumber()).isEqualTo("AR-DTA-88");
        assertThat(dta.getLineItems().get(1).getAccountName()).isEqualTo("AR DTA Name");
        assertThat(dta.getLineItems().get(2).getAccountNumber()).isEqualTo("AR-DTA-99");
        assertThat(dta.getLineItems().get(2).getAccountName()).isEqualTo("Second AR DTA Name");
    }

    @Test
    void toResponse_shouldNotCopyEcdAccountOntoDtc() {
        Shipment shipment = shipment("AWB-DTC-KEEP", "DDU");
        shipment.setConsigneeNumber("CONS-99");
        shipment.setConsigneeName("Consignee Ltd");
        Charge ecdCharge = charge("ECD", "DXB", shipment);
        Charge dtcCharge = charge("DTC", "DXB", shipment);
        dtcCharge.setAccountType("ACCOUNT");

        Account entitySettings = new Account();
        entitySettings.setChargeType("ECD");
        entitySettings.setDefaultAccountNumber("AP-001");
        Map<String, Account> entitySettingsAccounts = Map.of(
                ShipmentChargeDetailsMapper.accountKey("DXB", "ECD"),
                entitySettings);

        APAccount apAccount = new APAccount();
        apAccount.setAccountNumber("AP-001");
        apAccount.setAccountName("DXB AP Account");

        Map<String, APAccount> apAccounts = Map.of(
                ShipmentChargeDetailsMapper.apAccountKey("DXB", "AP-001"),
                apAccount);

        Account arAccount = new Account();
        arAccount.setAccountNature(AccountNature.AR);
        ARAccount arMaster = new ARAccount();
        arMaster.setAccountNumber("AR-DTC-88");
        arMaster.setAccountName("AR DTC Name");
        arAccount.setArAccount(arMaster);

        ChargeLineItem dtcLine = lineItem(dtcCharge, "SC01", "PAY-1", BigDecimal.TEN, null);
        dtcLine.setAccount(arAccount);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTC",
                Set.of("ECD", "DTC"),
                List.of(ecdCharge, dtcCharge),
                lineItemsByChargeId(ecdCharge, List.of(), dtcCharge, List.of(dtcLine)),
                Map.of(),
                entitySettingsAccounts,
                apAccounts);

        ChargeTypeDetailsDTO dtc = chargeDetail(response, "DTC");
        assertThat(dtc.getAccountInformation().getAccountNumber()).isEqualTo("AR-DTC-88");
        assertThat(dtc.getAccountInformation().getAccountName()).isEqualTo("AR DTC Name");
        assertThat(dtc.getLineItems().get(0).getAccountNumber()).isEqualTo("AR-DTC-88");
        assertThat(dtc.getLineItems().get(0).getAccountName()).isEqualTo("AR DTC Name");
    }

    @Test
    void toResponse_shouldMapShipmentBusinessTypeFromShipmentDomain() {
        Shipment shipment = shipment("AWB-BT", "DDU");
        shipment.setShipmentBusinessType("B2C");
        Charge dtcCharge = charge("DTC", "DXB", shipment);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTC",
                Set.of("DTC"),
                List.of(dtcCharge),
                lineItemsByChargeId(dtcCharge, List.of()),
                Map.of(),
                Map.of(),
                Map.of());

        assertThat(chargeDetail(response, "DTC").getShipment().getCustomsAndGoods().getShipmentBusinessType())
                .isEqualTo("B2C");
    }

    @Test
    void toResponse_shouldUseChargeAccountTypeAndFirstLineItemAccount() {
        Shipment shipment = shipment("AWB-AR", "DDU");
        shipment.setConsigneeNumber("CONS-99");
        shipment.setConsigneeName("Consignee Ltd");
        Charge dtcCharge = charge("DTC", "DXB", shipment);
        dtcCharge.setAccountType("ACCOUNT");

        Account arAccount = new Account();
        arAccount.setAccountNature(AccountNature.AR);
        ARAccount arMaster = new ARAccount();
        arMaster.setAccountNumber("CONS-99");
        arMaster.setAccountName("Consignee Ltd");
        arAccount.setArAccount(arMaster);

        ChargeLineItem dtcLine = lineItem(dtcCharge, "SC01", null, BigDecimal.TEN, null);
        dtcLine.setAccount(arAccount);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTC",
                Set.of("DTC"),
                List.of(dtcCharge),
                lineItemsByChargeId(dtcCharge, List.of(dtcLine)),
                Map.of(),
                Map.of(),
                Map.of());

        assertThat(chargeDetail(response, "DTC").getAccountInformation().getAccountNumber()).isEqualTo("CONS-99");
        assertThat(chargeDetail(response, "DTC").getAccountInformation().getAccountName()).isEqualTo("Consignee Ltd");
        assertThat(chargeDetail(response, "DTC").getAccountInformation().getAccountType()).isEqualTo("ACCOUNT");
    }

    @Test
    void toResponse_shouldUseLineItemAccountForDts() {
        Shipment shipment = shipment("AWB-DTS", "DDP");
        shipment.setShipperNumber("SHIP-11");
        shipment.setShipperName("Shipper Co");
        shipment.setConsigneeNumber("CONS-99");
        shipment.setConsigneeName("Consignee Ltd");
        Charge dtsCharge = charge("DTS", "RUH", shipment);
        dtsCharge.setAccountType("ACCOUNT");

        Account dtsAccount = new Account();
        dtsAccount.setAccountNature(AccountNature.AR);
        ARAccount dtsMaster = new ARAccount();
        dtsMaster.setAccountNumber(" DTS-ACC-11 ");
        dtsMaster.setAccountName(" DTS Account Co ");
        dtsAccount.setArAccount(dtsMaster);

        ChargeLineItem dtsLine = lineItem(dtsCharge, "SC01", "PAY-1", BigDecimal.TEN, null);
        dtsLine.setAccount(dtsAccount);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTS",
                Set.of("DTS"),
                List.of(dtsCharge),
                lineItemsByChargeId(dtsCharge, List.of(dtsLine)),
                Map.of(),
                Map.of(),
                Map.of());

        ChargeTypeDetailsDTO dts = chargeDetail(response, "DTS");
        assertThat(dts.getAccountInformation().getAccountNumber()).isEqualTo("DTS-ACC-11");
        assertThat(dts.getAccountInformation().getAccountName()).isEqualTo("DTS Account Co");
        assertThat(dts.getLineItems().get(0).getAccountNumber()).isEqualTo("DTS-ACC-11");
        assertThat(dts.getLineItems().get(0).getAccountName()).isEqualTo("DTS Account Co");
    }

    @Test
    void toResponse_shouldUseLineItemAccountForDt3() {
        Shipment shipment = shipment("AWB-DT3", "DDP");
        shipment.setThirdPartyNumber("TP-77");
        shipment.setThirdPartyName("Third Party Inc");
        shipment.setConsigneeNumber("CONS-99");
        shipment.setConsigneeName("Consignee Ltd");
        Charge dt3Charge = charge("DT3", "JED", shipment);
        dt3Charge.setAccountType("ACCOUNT");

        Account dt3Account = new Account();
        dt3Account.setAccountNature(AccountNature.AR);
        ARAccount dt3Master = new ARAccount();
        dt3Master.setAccountNumber("DT3-ACC-77");
        dt3Master.setAccountName("DT3 Account Inc");
        dt3Account.setArAccount(dt3Master);

        ChargeLineItem dt3Line = lineItem(dt3Charge, "SC01", "PAY-1", BigDecimal.TEN, null);
        dt3Line.setAccount(dt3Account);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DT3",
                Set.of("DT3"),
                List.of(dt3Charge),
                lineItemsByChargeId(dt3Charge, List.of(dt3Line)),
                Map.of(),
                Map.of(),
                Map.of());

        ChargeTypeDetailsDTO dt3 = chargeDetail(response, "DT3");
        assertThat(dt3.getAccountInformation().getAccountNumber()).isEqualTo("DT3-ACC-77");
        assertThat(dt3.getAccountInformation().getAccountName()).isEqualTo("DT3 Account Inc");
        assertThat(dt3.getLineItems().get(0).getAccountNumber()).isEqualTo("DT3-ACC-77");
        assertThat(dt3.getLineItems().get(0).getAccountName()).isEqualTo("DT3 Account Inc");
    }

    @Test
    void toResponse_shouldKeepDtsAndDt3AccountInfoNullWhenLineItemAccountMissing() {
        Shipment shipment = shipment("AWB-DTS-DT3-NULL", "DDP");
        shipment.setShipperNumber("SHIP-11");
        shipment.setShipperName("Shipper Co");
        shipment.setThirdPartyNumber("TP-77");
        shipment.setThirdPartyName("Third Party Inc");

        Charge dtsCharge = charge("DTS", "RUH", shipment);
        dtsCharge.setAccountType("CASH");
        Charge dt3Charge = charge("DT3", "JED", shipment);
        dt3Charge.setAccountType("CASH");
        ChargeLineItem dtsLine = lineItem(dtsCharge, "SC01", "PAY-1", BigDecimal.TEN, null);
        ChargeLineItem dt3Line = lineItem(dt3Charge, "SC01", "PAY-1", BigDecimal.TEN, null);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTS",
                Set.of("DTS", "DT3"),
                List.of(dtsCharge, dt3Charge),
                lineItemsByChargeId(dtsCharge, List.of(dtsLine), dt3Charge, List.of(dt3Line)),
                Map.of(),
                Map.of(),
                Map.of());

        ChargeTypeDetailsDTO dts = chargeDetail(response, "DTS");
        ChargeTypeDetailsDTO dt3 = chargeDetail(response, "DT3");
        assertThat(dts.getAccountInformation().getAccountNumber()).isNull();
        assertThat(dts.getAccountInformation().getAccountName()).isNull();
        assertThat(dts.getAccountInformation().getAccountType()).isEqualTo("CASH");
        assertThat(dts.getLineItems().get(0).getAccountNumber()).isNull();
        assertThat(dts.getLineItems().get(0).getAccountName()).isNull();
        assertThat(dt3.getAccountInformation().getAccountNumber()).isNull();
        assertThat(dt3.getAccountInformation().getAccountName()).isNull();
        assertThat(dt3.getAccountInformation().getAccountType()).isEqualTo("CASH");
        assertThat(dt3.getLineItems().get(0).getAccountNumber()).isNull();
        assertThat(dt3.getLineItems().get(0).getAccountName()).isNull();
    }

    @Test
    void toResponse_shouldUseChargeAccountTypeWhenDtcHasNoLineItemAccount() {
        Shipment shipment = shipment("AWB-DTC-CASH", "DDU");
        shipment.setConsigneeName("Consignee Without Number");
        Charge dtcCharge = charge("DTC", "DXB", shipment);
        dtcCharge.setAccountType("CASH");

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTC",
                Set.of("DTC"),
                List.of(dtcCharge),
                lineItemsByChargeId(dtcCharge, List.of()),
                Map.of(),
                Map.of(),
                Map.of());

        assertThat(chargeDetail(response, "DTC").getAccountInformation().getAccountNumber()).isNull();
        assertThat(chargeDetail(response, "DTC").getAccountInformation().getAccountName()).isNull();
        assertThat(chargeDetail(response, "DTC").getAccountInformation().getAccountType()).isEqualTo("CASH");
    }

    @Test
    void toResponse_shouldUseChargeAccountTypeFromChargeEntity() {
        Shipment shipment = shipment("AWB-CASH", "DDU");
        shipment.setCashValue(100.0);
        shipment.setConsigneeNumber("CONS-1");
        shipment.setConsigneeName("Cash Consignee");
        Charge dtcCharge = charge("DTC", "DXB", shipment);
        dtcCharge.setAccountType("CASH");

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTC",
                Set.of("DTC"),
                List.of(dtcCharge),
                lineItemsByChargeId(dtcCharge, List.of()),
                Map.of(),
                Map.of(),
                Map.of());

        assertThat(chargeDetail(response, "DTC").getAccountInformation().getAccountType()).isEqualTo("CASH");
    }

    @Test
    void toResponse_shouldBlankAccountNumberAndNameWhenFirstLineItemAccountIsEntitySettings() {
        Shipment shipment = shipment("AWB-ENTITY", "DDP");
        Charge dtcCharge = charge("DTC", "DXB", shipment);
        dtcCharge.setAccountType("ACCOUNT");

        Account entitySettingsAccount = new Account();
        entitySettingsAccount.setAccountNature(AccountNature.ENTITY_SETTINGS);

        ChargeLineItem dtcLine = lineItem(dtcCharge, "SC01", null, BigDecimal.TEN, null);
        dtcLine.setAccount(entitySettingsAccount);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTC",
                Set.of("DTC"),
                List.of(dtcCharge),
                lineItemsByChargeId(dtcCharge, List.of(dtcLine)),
                Map.of(),
                Map.of(),
                Map.of());

        assertThat(chargeDetail(response, "DTC").getAccountInformation().getAccountType()).isEqualTo("ACCOUNT");
        assertThat(chargeDetail(response, "DTC").getAccountInformation().getAccountNumber()).isNull();
        assertThat(chargeDetail(response, "DTC").getAccountInformation().getAccountName()).isNull();
    }

    @Test
    void toResponse_shouldUseChargeCifValueAndCurrencyInCustomsAndGoods() {
        Shipment shipment = shipment("AWB-CIF", "DDU");
        shipment.setCostOfGoods(100.0);
        shipment.setCustomsCurrency("EUR");
        shipment.setCifValue(25.0);
        shipment.setCifValueCurrency("EUR");
        shipment.setBusinessType("B2B");
        shipment.setInsuranceValue(99.0);

        Charge dtcCharge = charge("DTC", "DXB", shipment);
        dtcCharge.setCifValue(150.0);
        dtcCharge.setCurrency("USD");
        dtcCharge.setCifCurrency("AED");
        dtcCharge.setShippingCharges(25.0);

        ShipmentChargeDetailsResponse response = mapper.toResponse(
                shipment,
                "DTC",
                Set.of("DTC"),
                List.of(dtcCharge),
                lineItemsByChargeId(dtcCharge, List.of()),
                Map.of(),
                Map.of(),
                Map.of());

        var customsAndGoods = chargeDetail(response, "DTC").getShipment().getCustomsAndGoods();
        assertThat(customsAndGoods.getGoodsValue()).isEqualByComparingTo("100");
        assertThat(customsAndGoods.getGoodsValueCurrency()).isEqualTo("EUR");
        assertThat(customsAndGoods.getCifValue()).isEqualByComparingTo("150");
        assertThat(customsAndGoods.getCifValueCurrency()).isEqualTo("AED");
        assertThat(customsAndGoods.getShippingCharges()).isEqualByComparingTo("25");
        assertThat(customsAndGoods.getShipmentChargeCurrency()).isEqualTo("AED");
        assertThat(customsAndGoods.getAccountBusinessType()).isEqualTo("B2B");
        assertThat(customsAndGoods.getInsuranceValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private static Map<UUID, List<ChargeLineItem>> lineItemsByChargeId(
            Charge charge,
            List<ChargeLineItem> items) {

        Map<UUID, List<ChargeLineItem>> map = new LinkedHashMap<>();
        map.put((UUID) ReflectionTestUtils.getField(charge, "id"), items);
        return map;
    }

    private static Map<UUID, List<ChargeLineItem>> lineItemsByChargeId(
            Charge firstCharge,
            List<ChargeLineItem> firstItems,
            Charge secondCharge,
            List<ChargeLineItem> secondItems) {

        Map<UUID, List<ChargeLineItem>> map = new LinkedHashMap<>();
        map.put((UUID) ReflectionTestUtils.getField(firstCharge, "id"), firstItems);
        map.put((UUID) ReflectionTestUtils.getField(secondCharge, "id"), secondItems);
        return map;
    }

    private static ChargeTypeDetailsDTO chargeDetail(ShipmentChargeDetailsResponse response, String chargeType) {
        return response.getCharges().stream()
                .filter(charge -> chargeType.equals(charge.getChargeType()))
                .findFirst()
                .orElseThrow();
    }
}
