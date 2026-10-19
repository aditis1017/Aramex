package com.customcharge.dashboard.mapper;

import com.customcharge.dashboard.constants.ChargeTypeConstants;
import com.customcharge.dashboard.dto.charge.*;
import com.customcharge.dashboard.util.RelatedChargeTypeResolver;
import com.customcharge.persistencelayer.entities.APAccount;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.util.ArAccountMasterSupport;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import static com.customcharge.dashboard.constants.AccountConstants.ACCOUNT_SERVICES;

@Component
public class ShipmentChargeDetailsMapper {

    public ShipmentChargeDetailsResponse toResponse(
            Shipment shipment,
            String requestedChargeType,
            Set<String> allShipmentChargeCodes,
            List<Charge> charges,
            Map<UUID, List<ChargeLineItem>> lineItemsByChargeId,
            Map<String, ServiceCodes> serviceCodesByKey,
            Map<String, Account> entitySettingsAccountsByKey,
            Map<String, APAccount> apAccountsByKey) {

        return toResponse(
                shipment,
                requestedChargeType,
                allShipmentChargeCodes,
                charges,
                lineItemsByChargeId,
                serviceCodesByKey,
                entitySettingsAccountsByKey,
                apAccountsByKey,
                Map.of(),
                null);
    }

    public ShipmentChargeDetailsResponse toResponse(
            Shipment shipment,
            String requestedChargeType,
            Set<String> allShipmentChargeCodes,
            List<Charge> charges,
            Map<UUID, List<ChargeLineItem>> lineItemsByChargeId,
            Map<String, ServiceCodes> serviceCodesByKey,
            Map<String, Account> entitySettingsAccountsByKey,
            Map<String, APAccount> apAccountsByKey,
            Map<String, ARAccount> dtcArAccountsByKey) {

        return toResponse(
                shipment,
                requestedChargeType,
                allShipmentChargeCodes,
                charges,
                lineItemsByChargeId,
                serviceCodesByKey,
                entitySettingsAccountsByKey,
                apAccountsByKey,
                dtcArAccountsByKey,
                null);
    }

    public ShipmentChargeDetailsResponse toResponse(
            Shipment shipment,
            String requestedChargeType,
            Set<String> allShipmentChargeCodes,
            List<Charge> charges,
            Map<UUID, List<ChargeLineItem>> lineItemsByChargeId,
            Map<String, ServiceCodes> serviceCodesByKey,
            Map<String, Account> entitySettingsAccountsByKey,
            Map<String, APAccount> apAccountsByKey,
            Map<String, ARAccount> dtcArAccountsByKey,
            ARAccount sellerArAccount) {

        List<ChargeTypeDetailsDTO> chargeDetails = charges.stream()
                .map(charge -> toChargeTypeDetails(
                        shipment,
                        allShipmentChargeCodes,
                        charge,
                        lineItemsByChargeId.getOrDefault(charge.getId(), List.of()),
                        serviceCodesByKey,
                        entitySettingsAccountsByKey,
                        apAccountsByKey,
                        dtcArAccountsByKey,
                        sellerArAccount))
                .toList();

        Map<String, ChargeTypeDetailsDTO> detailsByChargeType = chargeDetails.stream()
                .collect(Collectors.toMap(
                        ChargeTypeDetailsDTO::getChargeType,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));

        Map<String, List<ChargeLineItem>> lineItemsByChargeType = new LinkedHashMap<>();
        for (Charge charge : charges) {
            String chargeType = RelatedChargeTypeResolver.normalize(charge.getChargeCode());
            lineItemsByChargeType.put(
                    chargeType,
                    lineItemsByChargeId.getOrDefault(charge.getId(), List.of()));
        }

        List<ChargeTypeDetailsDTO> enrichedCharges = chargeDetails.stream()
                .map(detail -> enrichCostChargeWithPairedRevenue(
                        detail,
                        detailsByChargeType,
                        shipment,
                        lineItemsByChargeType))
                .toList();

        return ShipmentChargeDetailsResponse.builder()
                .hawbNumber(shipment.getHawbNumber())
                .requestedChargeType(requestedChargeType)
                .charges(enrichedCharges)
                .build();
    }

    /**
     * Copies actual amounts from the paired revenue charge onto matching cost charge rows for display only.
     * For DTA/DTS/DT3, uses {@code preConvertedAmount}; for DTC, copies {@code amount} as-is.
     * Revenue-only service codes are not added to the cost charge.
     */
    private ChargeTypeDetailsDTO enrichCostChargeWithPairedRevenue(
            ChargeTypeDetailsDTO costDetail,
            Map<String, ChargeTypeDetailsDTO> detailsByChargeType,
            Shipment shipment,
            Map<String, List<ChargeLineItem>> lineItemsByChargeType) {

        if (!ChargeTypeConstants.isCostAp(costDetail.getChargeType())) {
            return costDetail;
        }

        Optional<String> pairedRevenueType = ChargeTypeConstants.pairedRevenueChargeType(
                costDetail.getChargeType(),
                shipment.getShipmentType());
        if (pairedRevenueType.isEmpty()) {
            return costDetail;
        }

        ChargeTypeDetailsDTO revenueDetail = detailsByChargeType.get(pairedRevenueType.get());
        if (revenueDetail == null) {
            return costDetail;
        }

        List<ChargeLineItem> revenueLineItems = lineItemsByChargeType.getOrDefault(
                pairedRevenueType.get(),
                List.of());
        boolean usePreConvertedAmount = ChargeTypeConstants.requiresPreConvertedAmountWhenMirroringActuals(
                pairedRevenueType.get());
        RevenuePreConvertedAmountIndex preConvertedAmountIndex = usePreConvertedAmount
                ? new RevenuePreConvertedAmountIndex(revenueLineItems)
                : null;

        List<ChargeLineItemRowDTO> mergedRows = mergeCostRowsWithRevenueActuals(
                costDetail.getLineItems(),
                revenueDetail.getLineItems(),
                costDetail.getCurrency(),
                usePreConvertedAmount,
                preConvertedAmountIndex);

        return withLineItems(costDetail, mergedRows);
    }

    private List<ChargeLineItemRowDTO> mergeCostRowsWithRevenueActuals(
            List<ChargeLineItemRowDTO> costRows,
            List<ChargeLineItemRowDTO> revenueRows,
            String costCurrency,
            boolean usePreConvertedAmount,
            RevenuePreConvertedAmountIndex preConvertedAmountIndex) {

        if (revenueRows == null || revenueRows.isEmpty()) {
            return costRows;
        }

        Map<String, ChargeLineItemRowDTO> revenueByCode = indexRowsByServiceCode(revenueRows);

        List<ChargeLineItemRowDTO> merged = new ArrayList<>();
        for (ChargeLineItemRowDTO costRow : costRows) {
            String serviceCode = normalizeServiceCode(costRow.getServiceCode());
            ChargeLineItemRowDTO revenueRow = revenueByCode.get(serviceCode);
            if (revenueRow != null && !hasActualAmounts(costRow) && hasActualAmounts(revenueRow)) {
                merged.add(copyActualAmountsFromRevenue(
                        costRow,
                        revenueRow,
                        costCurrency,
                        serviceCode,
                        usePreConvertedAmount,
                        preConvertedAmountIndex));
            } else {
                merged.add(costRow);
            }
        }

        merged.sort(Comparator.comparing(ChargeLineItemRowDTO::getServiceCode, Comparator.nullsLast(String::compareTo)));
        return merged;
    }

    private static Map<String, ChargeLineItemRowDTO> indexRowsByServiceCode(List<ChargeLineItemRowDTO> rows) {
        Map<String, ChargeLineItemRowDTO> indexed = new LinkedHashMap<>();
        for (ChargeLineItemRowDTO row : rows) {
            String serviceCode = normalizeServiceCode(row.getServiceCode());
            if (!serviceCode.isEmpty()) {
                indexed.putIfAbsent(serviceCode, row);
            }
        }
        return indexed;
    }

    private ChargeLineItemRowDTO copyActualAmountsFromRevenue(
            ChargeLineItemRowDTO costRow,
            ChargeLineItemRowDTO revenueRow,
            String costCurrency,
            String serviceCode,
            boolean usePreConvertedAmount,
            RevenuePreConvertedAmountIndex preConvertedAmountIndex) {

        MirroredActualAmounts mirrored = mirrorActualAmountsForCostDisplay(
                revenueRow,
                serviceCode,
                usePreConvertedAmount,
                preConvertedAmountIndex,
                costRow.getEstimateAmount());

        return ChargeLineItemRowDTO.builder()
                .serviceCategory(costRow.getServiceCategory())
                .serviceCode(costRow.getServiceCode())
                .description(costRow.getDescription())
                .accountNumber(costRow.getAccountNumber())
                .accountName(costRow.getAccountName())
                .currency(resolveCostRowCurrency(costCurrency, costRow))
                .estimateAmount(costRow.getEstimateAmount())
                .actualOne(mirrored.actualOne())
                .actualTwo(mirrored.actualTwo())
                .actualThree(mirrored.actualThree())
                .actualFour(mirrored.actualFour())
                .totalActuals(mirrored.totalActuals())
                .eligibleCost(mirrored.eligibleCost())
                .invoiceOne(costRow.getInvoiceOne())
                .invoiceTwo(costRow.getInvoiceTwo())
                .invoiceThree(costRow.getInvoiceThree())
                .invoiceFour(costRow.getInvoiceFour())
                .outstanding(resolveOutstanding(
                        costRow.getEstimateAmount(),
                        mirrored.actualOne(),
                        mirrored.actualTwo(),
                        mirrored.actualThree(),
                        mirrored.actualFour(),
                        mirrored.totalActuals()))
                .totalInvoices(costRow.getTotalInvoices())
                .contractedRate(costRow.getContractedRate())
                .build();
    }

    private MirroredActualAmounts mirrorActualAmountsForCostDisplay(
            ChargeLineItemRowDTO revenueRow,
            String serviceCode,
            boolean usePreConvertedAmount,
            RevenuePreConvertedAmountIndex preConvertedAmountIndex,
            BigDecimal costEstimateFallback) {

        BigDecimal actualOne = mirrorActualAmount(
                revenueRow.getActualOne(), serviceCode, 0, usePreConvertedAmount, preConvertedAmountIndex);
        BigDecimal actualTwo = mirrorActualAmount(
                revenueRow.getActualTwo(), serviceCode, 1, usePreConvertedAmount, preConvertedAmountIndex);
        BigDecimal actualThree = mirrorActualAmount(
                revenueRow.getActualThree(), serviceCode, 2, usePreConvertedAmount, preConvertedAmountIndex);
        BigDecimal actualFour = mirrorActualAmount(
                revenueRow.getActualFour(), serviceCode, 3, usePreConvertedAmount, preConvertedAmountIndex);

        ActualAmounts mirrored = new ActualAmounts(actualOne, actualTwo, actualThree, actualFour, null, null, null, null);
        BigDecimal totalActuals = mirrored.totalActuals();
        BigDecimal eligibleCost = totalActuals != null ? totalActuals : costEstimateFallback;
        return new MirroredActualAmounts(actualOne, actualTwo, actualThree, actualFour, totalActuals, eligibleCost);
    }

    private BigDecimal mirrorActualAmount(
            BigDecimal revenueAmount,
            String serviceCode,
            int actualIndex,
            boolean usePreConvertedAmount,
            RevenuePreConvertedAmountIndex preConvertedAmountIndex) {

        if (revenueAmount == null) {
            return null;
        }
        if (!usePreConvertedAmount || preConvertedAmountIndex == null) {
            return revenueAmount;
        }
        BigDecimal preConvertedAmount = preConvertedAmountIndex.resolveAmount(serviceCode, actualIndex);
        return preConvertedAmount != null ? preConvertedAmount : revenueAmount;
    }

    private record MirroredActualAmounts(
            BigDecimal actualOne,
            BigDecimal actualTwo,
            BigDecimal actualThree,
            BigDecimal actualFour,
            BigDecimal totalActuals,
            BigDecimal eligibleCost) {
    }

    private static final class RevenuePreConvertedAmountIndex {

        private final Map<String, Map<String, BigDecimal>> amountsByServiceCode = new LinkedHashMap<>();

        RevenuePreConvertedAmountIndex(List<ChargeLineItem> revenueLineItems) {
            Map<String, List<ChargeLineItem>> grouped = revenueLineItems.stream()
                    .collect(Collectors.groupingBy(
                            item -> normalizeServiceCode(item.getServiceCode()),
                            LinkedHashMap::new,
                            Collectors.toList()));
            grouped.forEach((serviceCode, items) ->
                    amountsByServiceCode.put(serviceCode, buildAmounts(items)));
        }

        BigDecimal resolveAmount(String serviceCode, int actualIndex) {
            Map<String, BigDecimal> amounts = amountsByServiceCode.get(serviceCode);
            if (amounts == null) {
                return null;
            }
            String bucketKey = actualBucketKey(actualIndex);
            if (amounts.containsKey(bucketKey)) {
                return amounts.get(bucketKey);
            }
            return amounts.get("seq:" + actualIndex);
        }

        private static Map<String, BigDecimal> buildAmounts(List<ChargeLineItem> items) {
            List<ChargeLineItem> sorted = items.stream()
                    .sorted(Comparator.comparing(
                            ChargeLineItem::getCreatedTime,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();

            Map<String, BigDecimal> amounts = new LinkedHashMap<>();
            int sequenceIndex = 0;
            for (ChargeLineItem item : sorted) {
                if (isFileBasedEstimateLine(item)) {
                    continue;
                }
                amounts.putIfAbsent(actualBucketKey(sequenceIndex), item.getPreConvertedAmount());
                amounts.put("seq:" + sequenceIndex, item.getPreConvertedAmount());
                sequenceIndex++;
            }
            return amounts;
        }

        private static String actualBucketKey(int index) {
            return switch (index) {
                case 0 -> "1";
                case 1 -> "2";
                case 2 -> "3";
                case 3 -> "4";
                default -> "seq:" + index;
            };
        }
    }

    private static String resolveCostRowCurrency(String costCurrency, ChargeLineItemRowDTO row) {
        if (costCurrency != null && !costCurrency.isBlank()) {
            return costCurrency;
        }
        return row.getCurrency();
    }

    private static boolean hasActualAmounts(ChargeLineItemRowDTO row) {
        return row.getActualOne() != null
                || row.getActualTwo() != null
                || row.getActualThree() != null
                || row.getActualFour() != null;
    }

    /**
     * When at least one line has actuals, estimate-only lines must not contribute
     * eligible cost or outstanding. Estimate amount is preserved for display.
     */
    private static List<ChargeLineItemRowDTO> zeroCostForEstimateOnlyWhenMixed(List<ChargeLineItemRowDTO> rows) {
        boolean anyActuals = rows.stream().anyMatch(ShipmentChargeDetailsMapper::hasActualAmounts);
        if (!anyActuals) {
            return rows;
        }

        List<ChargeLineItemRowDTO> adjusted = new ArrayList<>();
        BigDecimal zero = BigDecimal.ZERO;
        for (ChargeLineItemRowDTO row : rows) {
            if (hasActualAmounts(row)) {
                adjusted.add(row);
            } else {
                adjusted.add(rebuildRow(row, zero, zero));
            }
        }
        return adjusted;
    }

    private static ChargeLineItemRowDTO rebuildRow(
            ChargeLineItemRowDTO row,
            BigDecimal eligibleCost,
            BigDecimal outstanding) {

        return ChargeLineItemRowDTO.builder()
                .serviceCategory(row.getServiceCategory())
                .serviceCode(row.getServiceCode())
                .description(row.getDescription())
                .accountNumber(row.getAccountNumber())
                .accountName(row.getAccountName())
                .currency(row.getCurrency())
                .estimateAmount(row.getEstimateAmount())
                .actualOne(row.getActualOne())
                .actualTwo(row.getActualTwo())
                .actualThree(row.getActualThree())
                .actualFour(row.getActualFour())
                .totalActuals(row.getTotalActuals())
                .eligibleCost(eligibleCost)
                .invoiceOne(row.getInvoiceOne())
                .invoiceTwo(row.getInvoiceTwo())
                .invoiceThree(row.getInvoiceThree())
                .invoiceFour(row.getInvoiceFour())
                .outstanding(outstanding)
                .totalInvoices(row.getTotalInvoices())
                .contractedRate(row.getContractedRate())
                .build();
    }

    private static String normalizeServiceCode(String serviceCode) {
        if (serviceCode == null || serviceCode.isBlank()) {
            return "";
        }
        return serviceCode.trim().toUpperCase(Locale.ROOT);
    }

    private static ChargeTypeDetailsDTO withLineItems(
            ChargeTypeDetailsDTO detail,
            List<ChargeLineItemRowDTO> lineItems) {

        List<ChargeLineItemRowDTO> normalizedLineItems = zeroCostForEstimateOnlyWhenMixed(lineItems);
        ChargeLineItemTotalsDTO totals = sumLineItems(normalizedLineItems);

        return ChargeTypeDetailsDTO.builder()
                .chargeType(detail.getChargeType())
                .isEstimated(detail.isEstimated())
                .availableChargeTypes(detail.getAvailableChargeTypes())
                .shipment(detail.getShipment())
                .chargeGroup(detail.getChargeGroup())
                .billingEntity(detail.getBillingEntity())
                .billingCountry(detail.getBillingCountry())
                .destinationCostStatus(detail.getDestinationCostStatus())
                .status(detail.getStatus())
                .estimateAmount(detail.getEstimateAmount())
                .actualAmount(detail.getActualAmount())
                .outstandingAmount(totals.getOutstanding())
                .currency(detail.getCurrency())
                .boeList(detail.getBoeList())
                .paymentReferenceList(detail.getPaymentReferenceList())
                .accountInformation(detail.getAccountInformation())
                .lineItems(normalizedLineItems)
                .totals(totals)
                .build();
    }

    private ShipmentChargeSummaryDTO buildShipmentSummary(
            Shipment shipment,
            Charge charge,
            Account entitySettingsAccount,
            ARAccount sellerArAccount) {
        String billingEntity = charge.getEntity() != null ? charge.getEntity().getCode() : null;
        String billingCountry = charge.getEntity() != null && charge.getEntity().getCountry() != null
                ? charge.getEntity().getCountry().getCode()
                : null;

        return ShipmentChargeSummaryDTO.builder()
                .trackingNumber(shipment.getHawbNumber())
                .billingEntity(billingEntity)
                .billingCountry(billingCountry)
                .destinationCostStatus(EntityChargeGridRowMapper.formatStatus(charge.getStatus()))
                .mawbConsole(shipment.getMAWBConsole())
                .statusBadges(buildStatusBadges(shipment, charge, entitySettingsAccount))
                .customsAndGoods(buildCustomsAndGoods(shipment, charge, sellerArAccount))
                .deliveryInformation(buildDeliveryInfo(shipment))
                .additionalDetails(buildAdditionalDetails(shipment, charge, entitySettingsAccount, sellerArAccount))
                .build();
    }

    private ShipmentStatusBadgesDTO buildStatusBadges(Shipment shipment, Charge charge, Account entitySettingsAccount) {
//        String handler = entitySettingsAccount != null && entitySettingsAccount.getBusinessType() != null
//                ? entitySettingsAccount.getBusinessType()
//                : shipment.getBusinessType();
        String handler  = "INTER COMPANY";

        return ShipmentStatusBadgesDTO.builder()
                .operation(null)
                .charge(EntityChargeGridRowMapper.formatStatus(charge.getStatus()))
                .category(formatLabel(shipment.getCategoryType()))
                .handler(formatLabel(handler))
                .build();
    }

    private ShipmentCustomsGoodsDTO buildCustomsAndGoods(
            Shipment shipment,
            Charge charge,
            ARAccount sellerArAccount) {
        BigDecimal goodsValue = toBigDecimal(shipment.getCostOfGoods());
        String customsType = shipment.getShipmentType();

        return ShipmentCustomsGoodsDTO.builder()
                .customsType(customsType)
                .source(ACCOUNT_SERVICES)
                .shipmentBusinessType(shipment.getShipmentBusinessType())
                .accountBusinessType(shipment.getAccountBusinessType())
                .goodsValue(goodsValue)
                .goodsValueCurrency(shipment.getCustomsCurrency())
                .cifValue(toBigDecimal(charge.getCifValue()))
                .cifValueCurrency(charge.getCifCurrency())
                .shippingCharges(toBigDecimal(charge.getShippingCharges()))
                .shipmentChargeCurrency(charge.getCifCurrency())
                .numberOfPieces(shipment.getPieces())
                .productGroup(shipment.getProductGroup())
                .insuranceValue(BigDecimal.ZERO)
                .build();
    }

    private ShipmentDeliveryInfoDTO buildDeliveryInfo(Shipment shipment) {
        return ShipmentDeliveryInfoDTO.builder()
                .createdDate(toLocalDate(shipment.getCreatedTime()))
                .pickupDate(shipment.getPickupDate() != null
                        ? shipment.getPickupDate().toLocalDate()
                        : null)
                .deliveryType(shipment.getProductType())
                .paymentType(shipment.getPaymentType())
                .build();
    }

    private ShipmentAdditionalDetailsDTO buildAdditionalDetails(
            Shipment shipment,
            Charge charge,
            Account entitySettingsAccount,
            ARAccount sellerArAccount) {
        return ShipmentAdditionalDetailsDTO.builder()
                .goodsSummary(ShipmentGoodsSummaryDTO.builder()
                        .description(shipment.getCommodityDescription())
                        .collectedValue(toBigDecimal(shipment.getCollectAmount()))
                        .collectedValueCurrency(shipment.getCollectCurrency())
                        .deliveryType(shipment.getProductType())
                        .ioss("")
                        .services(shipment.getServices())
                        .build())
                .origin(ShipmentLocationDetailsDTO.builder()
                        .country(shipment.getShipperCountryCode())
                        .state(shipment.getShipperStateCode())
                        .city(shipment.getShipperCity())
                        .entity(shipment.getOriginEntity())
                        .partyName(shipment.getShipperName())
                        .accountNumber(shipment.getShipperNumber())
                        .build())
                .destination(ShipmentLocationDetailsDTO.builder()
                        .country(shipment.getConsigneeCountryCode())
                        .state(shipment.getConsigneeStateCode())
                        .city(shipment.getConsigneeCity())
                        .entity(shipment.getHAWBDestinationEntity())
                        .partyName(shipment.getConsigneeName())
                        .accountNumber(shipment.getConsigneeNumber())
                        .build())
                .thirdParty(ShipmentLocationDetailsDTO.builder()
                        .country(shipment.getThirdPartyCountry())
                        .state(shipment.getThirdPartyState())
                        .city(shipment.getThirdPartyCity())
                        .entity(shipment.getThirdPartyEntity())
                        .partyName(shipment.getThirdPartyName())
                        .accountNumber(shipment.getThirdPartyNumber())
                        .build())
                .sellerInformation(buildSellerInformation(shipment, sellerArAccount))
                .build();
    }

    private ShipmentSellerInfoDTO buildSellerInformation(Shipment shipment, ARAccount sellerArAccount) {
        return ShipmentSellerInfoDTO.builder()
                .country(shipment.getShipperCountryCode())
                .entity(shipment.getOriginEntity())
                .accountNumber(shipment.getShipperNumber())
                .accountName(resolveSellerAccountName(shipment, sellerArAccount))
                .collectionReference(shipment.getCollectionReference())
                .build();
    }

    private static String resolveSellerAccountName(Shipment shipment, ARAccount sellerArAccount) {
        if (sellerArAccount != null && StringUtils.hasText(sellerArAccount.getAccountName())) {
            return sellerArAccount.getAccountName().trim();
        }
        return shipment != null ? shipment.getShipperName() : null;
    }

    private ChargeTypeDetailsDTO toChargeTypeDetails(
            Shipment shipment,
            Set<String> allShipmentChargeCodes,
            Charge charge,
            List<ChargeLineItem> lineItems,
            Map<String, ServiceCodes> serviceCodesByKey,
            Map<String, Account> entitySettingsAccountsByKey,
            Map<String, APAccount> apAccountsByKey,
            Map<String, ARAccount> dtcArAccountsByKey,
            ARAccount sellerArAccount) {

        String chargeType = RelatedChargeTypeResolver.normalize(charge.getChargeCode());
        String entityCode = charge.getEntity() != null ? charge.getEntity().getCode() : null;
        Account entitySettingsAccount = entitySettingsAccountsByKey.get(accountKey(entityCode, chargeType));

        List<String> availableChargeTypes = allShipmentChargeCodes.stream()
                .sorted(Comparator.comparingInt(ChargeTypeConstants::chargeDetailsDisplayIndex))
                .toList();

        ShipmentAccountInfoDTO accountInformation =
                buildAccountInformation(charge, lineItems, apAccountsByKey);
        ARAccount dtcArAccount = resolveDtcArAccount(charge, dtcArAccountsByKey);
        List<ChargeLineItemRowDTO> rows = zeroCostForEstimateOnlyWhenMixed(
                buildLineItemRows(lineItems, charge, serviceCodesByKey, dtcArAccount, apAccountsByKey));
        ChargeLineItemTotalsDTO totals = sumLineItems(rows);

        return ChargeTypeDetailsDTO.builder()
                .chargeType(chargeType)
                .isEstimated(ChargeTypeConstants.isCostAp(chargeType))
                .availableChargeTypes(availableChargeTypes)
                .shipment(buildShipmentSummary(shipment, charge, entitySettingsAccount, sellerArAccount))
                .chargeGroup(EntityChargeGridRowMapper.resolveChargeGroup(chargeType))
                .billingEntity(entityCode)
                .billingCountry(charge.getEntity() != null && charge.getEntity().getCountry() != null
                        ? charge.getEntity().getCountry().getCode()
                        : null)
                .destinationCostStatus(EntityChargeGridRowMapper.formatStatus(charge.getStatus()))
                .status(EntityChargeGridRowMapper.formatStatus(charge.getStatus()))
                .estimateAmount(charge.getEstimateAmount())
                .actualAmount(charge.getActualAmount())
                .outstandingAmount(totals.getOutstanding())
                .currency(charge.getCurrency())
                .boeList(buildBoeList(lineItems))
                .paymentReferenceList(buildPaymentReferenceList(lineItems))
                .accountInformation(accountInformation)
                .lineItems(rows)
                .totals(totals)
                .build();
    }

    private ShipmentAccountInfoDTO buildAccountInformation(
            Charge charge,
            List<ChargeLineItem> lineItems,
            Map<String, APAccount> apAccountsByKey) {

        Shipment shipment = charge.getShipment();
        String businessType = shipment != null ? shipment.getBusinessType() : null;
        String chargeType = RelatedChargeTypeResolver.normalize(charge.getChargeCode());
        String accountNumber = null;
        String accountName = null;

        if (ChargeTypeConstants.isCostAp(chargeType)) {
            String apAccountNumber = firstLineItemApAccountNumber(lineItems);
            if (StringUtils.hasText(apAccountNumber)) {
                accountNumber = normalizeAccountValue(apAccountNumber);
                APAccount apAccount = resolveApAccount(charge, apAccountNumber, apAccountsByKey);
                if (apAccount != null) {
                    accountNumber = normalizeAccountValue(apAccount.getAccountNumber());
                    accountName = normalizeAccountValue(apAccount.getAccountName());
                }
            }
        } else {
            Account firstLineItemAccount = firstLineItemAccount(lineItems);
            if (firstLineItemAccount != null
                    && !AccountNature.ENTITY_SETTINGS.equals(firstLineItemAccount.getAccountNature())) {
                accountNumber = normalizeAccountValue(firstLineItemAccount.getAccountNumber());
                accountName = normalizeAccountValue(firstLineItemAccount.getAccountName());
            }
        }

        return ShipmentAccountInfoDTO.builder()
                .accountType(charge.getAccountType())
                .accountNumber(accountNumber)
                .accountName(accountName)
                .businessType(businessType)
                .build();
    }

    private List<ChargeLineItemRowDTO> buildLineItemRows(
            List<ChargeLineItem> lineItems,
            Charge charge,
            Map<String, ServiceCodes> serviceCodesByKey,
            ARAccount dtcArAccount,
            Map<String, APAccount> apAccountsByKey) {

        Map<String, List<ChargeLineItem>> groups = lineItems.stream()
                .collect(Collectors.groupingBy(
                        ShipmentChargeDetailsMapper::lineItemGroupKey,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<ChargeLineItemRowDTO> rows = new ArrayList<>();
        for (List<ChargeLineItem> group : groups.values()) {
            if (shouldMergeLineItems(group)) {
                rows.add(toGroupedFileLineItemRow(group, charge, serviceCodesByKey, dtcArAccount, apAccountsByKey));
            } else {
                rows.add(toLineItemRow(group.get(0), charge, serviceCodesByKey, dtcArAccount, apAccountsByKey));
            }
        }

        rows.sort(Comparator.comparing(ChargeLineItemRowDTO::getServiceCode, Comparator.nullsLast(String::compareTo)));
        return rows;
    }

    /**
     * Merges rows when multiple uploads exist for the same service (e.g. estimate without file id
     * plus actuals with file ids). Single legacy lines without a file reference stay one-to-one.
     */
    private static boolean shouldMergeLineItems(List<ChargeLineItem> group) {
        if (group.size() > 1) {
            return true;
        }
        return group.stream().anyMatch(item -> item.getFileReferenceId() != null);
    }

    private static String lineItemGroupKey(ChargeLineItem item) {
        String serviceCode = item.getServiceCode() != null ? item.getServiceCode().trim().toUpperCase(Locale.ROOT) : "";
        String accountId = item.getAccount() != null && item.getAccount().getId() != null
                ? item.getAccount().getId().toString()
                : "";
        return serviceCode + "|" + accountId;
    }

    private ChargeLineItemRowDTO toGroupedFileLineItemRow(
            List<ChargeLineItem> items,
            Charge charge,
            Map<String, ServiceCodes> serviceCodesByKey,
            ARAccount dtcArAccount,
            Map<String, APAccount> apAccountsByKey) {

        List<ChargeLineItem> sorted = items.stream()
                .sorted(Comparator.comparing(
                        ChargeLineItem::getCreatedTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        ChargeLineItem representative = sorted.get(0);
        String entityCode = charge.getEntity() != null ? charge.getEntity().getCode() : null;
        String chargeType = RelatedChargeTypeResolver.normalize(charge.getChargeCode());
        ServiceCodes master = serviceCodesByKey.get(
                serviceCodeKey(entityCode, chargeType, representative.getServiceCode()));

        String serviceCategory = master != null ? master.getType() : null;
        String description = master != null && master.getServiceDescription() != null
                ? master.getServiceDescription()
                : representative.getServiceCode();

        Account account = representative.getAccount();
        LineItemAccount lineItemAccount =
                resolveLineItemAccount(charge, representative, account, dtcArAccount, apAccountsByKey);

        BigDecimal estimateAmount = null;
        BigDecimal contractedRateTotal = null;
        List<BigDecimal> actualSequence = new ArrayList<>();
        ActualAmounts invoiceTotals = ActualAmounts.empty();

        for (ChargeLineItem item : sorted) {
            ActualAmounts itemInvoices = resolveInvoiceAmounts(item);
            invoiceTotals = invoiceTotals.merge(itemInvoices);
            contractedRateTotal = addAmount(contractedRateTotal, item.getContractedRate());

            if (isFileBasedEstimateLine(item)) {
                estimateAmount = addAmount(estimateAmount, item.getAmount());
            } else {
                actualSequence.add(lineItemTotal(item));
            }
        }

        ActualAmounts actuals = ActualAmounts.fromActualSequence(actualSequence);
        BigDecimal estimateCostBase = addAmount(estimateAmount, contractedRateTotal);
        BigDecimal displayEstimateAmount = estimateAmount;
        BigDecimal outstanding;
        if (ChargeTypeConstants.isRevenueAr(chargeType)) {
            BigDecimal revenueContractedRate = null;
            for (ChargeLineItem item : sorted) {
                if (isFileBasedEstimateLine(item)) {
                    revenueContractedRate = addAmount(revenueContractedRate, item.getContractedRate());
                }
            }
            if (displayEstimateAmount == null) {
                for (ChargeLineItem item : sorted) {
                    displayEstimateAmount = addAmount(displayEstimateAmount, item.getAmount());
                }
            }
            outstanding = resolveRevenueOutstanding(displayEstimateAmount, revenueContractedRate);
        } else {
            outstanding = resolveOutstanding(
                    estimateCostBase,
                    actuals.actualOne(),
                    actuals.actualTwo(),
                    actuals.actualThree(),
                    actuals.actualFour(),
                    actuals.totalActuals());
        }

        return ChargeLineItemRowDTO.builder()
                .serviceCategory(serviceCategory)
                .serviceCode(representative.getServiceCode())
                .description(description)
                .accountNumber(lineItemAccount.accountNumber())
                .accountName(lineItemAccount.accountName())
                .currency(resolveCurrency(charge, account))
                .estimateAmount(displayEstimateAmount)
                .actualOne(actuals.actualOne())
                .actualTwo(actuals.actualTwo())
                .actualThree(actuals.actualThree())
                .actualFour(actuals.actualFour())
                .totalActuals(actuals.totalActuals())
                .eligibleCost(actuals.totalActuals() != null ? actuals.totalActuals() : estimateCostBase)
                .invoiceOne(invoiceTotals.invoiceOne())
                .invoiceTwo(invoiceTotals.invoiceTwo())
                .invoiceThree(invoiceTotals.invoiceThree())
                .invoiceFour(invoiceTotals.invoiceFour())
                .outstanding(outstanding)
                .totalInvoices(invoiceTotals.totalInvoices())
                .contractedRate(contractedRateTotal)
                .build();
    }

    private static boolean isFileBasedEstimateLine(ChargeLineItem item) {
        if (resolveActualAmounts(item).hasActualBuckets()) {
            return false;
        }
        return !hasPaymentReference(item);
    }

    private static boolean hasPaymentReference(ChargeLineItem item) {
        return item.getPaymentReference() != null && !item.getPaymentReference().isBlank();
    }

    private static List<String> buildBoeList(List<ChargeLineItem> lineItems) {
        if (lineItems == null || lineItems.isEmpty()) {
            return List.of();
        }

        List<String> boeList = new ArrayList<>();
        Set<String> seenPaymentReferenceAndDeclaration = new HashSet<>();

        lineItems.stream()
                .filter(ShipmentChargeDetailsMapper::hasPaymentReference)
                .sorted(Comparator.comparing(
                        ChargeLineItem::getCreatedTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(item -> {
                    if (seenPaymentReferenceAndDeclaration.add(boeListEntryKey(item))) {
                        boeList.add(item.getDeclarationNumber());
                    }
                });

        return boeList;
    }

    private static List<String> buildPaymentReferenceList(List<ChargeLineItem> lineItems) {
        if (lineItems == null || lineItems.isEmpty()) {
            return List.of();
        }

        List<String> paymentReferenceList = new ArrayList<>();
        Set<String> seenPaymentReferenceAndDeclaration = new HashSet<>();

        lineItems.stream()
                .filter(ShipmentChargeDetailsMapper::hasPaymentReference)
                .sorted(Comparator.comparing(
                        ChargeLineItem::getCreatedTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(item -> {
                    if (seenPaymentReferenceAndDeclaration.add(boeListEntryKey(item))) {
                        paymentReferenceList.add(item.getPaymentReference());
                    }
                });

        return paymentReferenceList;
    }

    private static String boeListEntryKey(ChargeLineItem item) {
        String paymentReference = item.getPaymentReference() == null
                ? ""
                : item.getPaymentReference().strip().toUpperCase(Locale.ROOT);
        String declarationNumber = item.getDeclarationNumber();
        String normalizedDeclaration = declarationNumber == null
                ? ""
                : declarationNumber.strip().toUpperCase(Locale.ROOT);
        return paymentReference + "|" + normalizedDeclaration;
    }

    private static BigDecimal addAmount(BigDecimal total, BigDecimal amount) {
        if (amount == null) {
            return total;
        }
        return total == null ? amount : total.add(amount);
    }

    private ChargeLineItemRowDTO toLineItemRow(
            ChargeLineItem item,
            Charge charge,
            Map<String, ServiceCodes> serviceCodesByKey,
            ARAccount dtcArAccount,
            Map<String, APAccount> apAccountsByKey) {

        String entityCode = charge.getEntity() != null ? charge.getEntity().getCode() : null;
        String chargeType = RelatedChargeTypeResolver.normalize(charge.getChargeCode());
        ServiceCodes master = serviceCodesByKey.get(serviceCodeKey(entityCode, chargeType, item.getServiceCode()));

        String serviceCategory = master != null ? master.getType() : null;
        String description = master != null && master.getServiceDescription() != null
                ? master.getServiceDescription()
                : item.getServiceCode();

        Account account = item.getAccount();
        LineItemAccount lineItemAccount =
                resolveLineItemAccount(charge, item, account, dtcArAccount, apAccountsByKey);
        ActualAmounts actuals = resolveActualAmounts(item).merge(resolveInvoiceAmounts(item));
        BigDecimal estimateAmount = item.getAmount();
        BigDecimal estimateCostBase = lineItemTotal(item);
        BigDecimal outstanding;
        if (ChargeTypeConstants.isRevenueAr(chargeType)) {
            outstanding = resolveRevenueOutstanding(estimateAmount, item.getContractedRate());
        } else {
            BigDecimal displayEstimate = actuals.hasActualBuckets() ? null : estimateAmount;
            estimateCostBase = actuals.hasActualBuckets() ? null : estimateCostBase;
            estimateAmount = displayEstimate;
            outstanding = resolveOutstanding(
                    estimateCostBase,
                    actuals.actualOne(),
                    actuals.actualTwo(),
                    actuals.actualThree(),
                    actuals.actualFour(),
                    actuals.totalActuals());
        }

        return ChargeLineItemRowDTO.builder()
                .serviceCategory(serviceCategory)
                .serviceCode(item.getServiceCode())
                .description(description)
                .accountNumber(lineItemAccount.accountNumber())
                .accountName(lineItemAccount.accountName())
                .currency(resolveCurrency(charge, account))
                .estimateAmount(estimateAmount)
                .actualOne(actuals.actualOne())
                .actualTwo(actuals.actualTwo())
                .actualThree(actuals.actualThree())
                .actualFour(actuals.actualFour())
                .totalActuals(actuals.totalActuals())
                .eligibleCost(actuals.totalActuals() != null ? actuals.totalActuals() : estimateCostBase)
                .invoiceOne(actuals.invoiceOne())
                .invoiceTwo(actuals.invoiceTwo())
                .invoiceThree(actuals.invoiceThree())
                .invoiceFour(actuals.invoiceFour())
                .outstanding(outstanding)
                .totalInvoices(actuals.totalInvoices())
                .contractedRate(item.getContractedRate())
                .build();
    }

    private static String resolveLineItemAccountNumber(Charge charge, ChargeLineItem item, Account account) {
        String chargeType = RelatedChargeTypeResolver.normalize(charge.getChargeCode());
        if (ChargeTypeConstants.isCostAp(chargeType)) {
            return normalizeAccountValue(item.getApAccountNumber());
        }
        return normalizeAccountValue(account != null ? account.getAccountNumber() : null);
    }

    private static String resolveLineItemAccountName(Charge charge, Account account) {
        return normalizeAccountValue(account != null ? account.getAccountName() : null);
    }

    private static LineItemAccount resolveLineItemAccount(
            Charge charge,
            ChargeLineItem item,
            Account account,
            ARAccount dtcArAccount,
            Map<String, APAccount> apAccountsByKey) {

        String accountNumber = resolveLineItemAccountNumber(charge, item, account);
        String accountName = resolveLineItemAccountName(charge, account);
        String chargeType = RelatedChargeTypeResolver.normalize(charge.getChargeCode());

        if (ChargeTypeConstants.isCostAp(chargeType)
                && StringUtils.hasText(accountNumber)
                && charge.getEntity() != null
                && StringUtils.hasText(charge.getEntity().getCode())
                && apAccountsByKey != null) {
            APAccount apAccount = apAccountsByKey.get(
                    apAccountKey(charge.getEntity().getCode().trim(), accountNumber.trim()));
            if (apAccount != null && StringUtils.hasText(apAccount.getAccountName())) {
                accountName = apAccount.getAccountName().trim();
            }
        }

        if (ChargeTypeConstants.DTC.equals(chargeType)
                && !StringUtils.hasText(accountNumber)
                && !StringUtils.hasText(accountName)
                && dtcArAccount != null) {
            accountNumber = dtcArAccount.getAccountNumber();
            accountName = dtcArAccount.getAccountName();
        }
        return sanitizeLineItemAccount(chargeType, accountNumber, accountName);
    }

    private static ARAccount resolveDtcArAccount(
            Charge charge,
            Map<String, ARAccount> dtcArAccountsByKey) {

        String chargeType = RelatedChargeTypeResolver.normalize(charge.getChargeCode());
        Shipment shipment = charge.getShipment();
        if (!ChargeTypeConstants.DTC.equals(chargeType)
                || shipment == null
                || !StringUtils.hasText(shipment.getConsigneeNumber())
                || !StringUtils.hasText(shipment.getConsigneeName())
                || charge.getEntity() == null
                || !StringUtils.hasText(charge.getEntity().getCode())) {
            return null;
        }
        return dtcArAccountsByKey.get(arAccountKey(
                charge.getEntity().getCode(),
                shipment.getConsigneeNumber().trim()));
    }

    private static ActualAmounts resolveActualAmounts(ChargeLineItem item) {
        if (!hasPaymentReference(item)) {
            return ActualAmounts.empty();
        }
        return ActualAmounts.actualOne(lineItemTotal(item));
    }

    private static BigDecimal lineItemTotal(ChargeLineItem item) {
        if (item == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
        BigDecimal contractedRate = item.getContractedRate() != null ? item.getContractedRate() : BigDecimal.ZERO;
        return amount.add(contractedRate);
    }

    private static ActualAmounts resolveInvoiceAmounts(ChargeLineItem item) {
        return ActualAmounts.empty();
    }

    /**
     * Revenue outstanding: contracted rate when present and non-zero, otherwise line amount.
     */
    private static BigDecimal resolveRevenueOutstanding(BigDecimal amount, BigDecimal contractedRate) {
        if (contractedRate != null && contractedRate.compareTo(BigDecimal.ZERO) != 0) {
            return contractedRate;
        }
        return amount;
    }

    /**
     * Outstanding follows actuals when any actual bucket is present; otherwise it matches the estimate.
     */
    private static BigDecimal resolveOutstanding(
            BigDecimal estimateAmount,
            BigDecimal actualOne,
            BigDecimal actualTwo,
            BigDecimal actualThree,
            BigDecimal actualFour,
            BigDecimal totalActuals) {

        if (actualOne != null || actualTwo != null || actualThree != null || actualFour != null) {
            return totalActuals;
        }
        return estimateAmount;
    }

    private static ChargeLineItemTotalsDTO sumLineItems(List<ChargeLineItemRowDTO> rows) {
        return ChargeLineItemTotalsDTO.builder()
                .estimateAmount(sum(rows, ChargeLineItemRowDTO::getEstimateAmount))
                .actualOne(sum(rows, ChargeLineItemRowDTO::getActualOne))
                .actualTwo(sum(rows, ChargeLineItemRowDTO::getActualTwo))
                .actualThree(sum(rows, ChargeLineItemRowDTO::getActualThree))
                .actualFour(sum(rows, ChargeLineItemRowDTO::getActualFour))
                .totalActuals(sum(rows, ChargeLineItemRowDTO::getTotalActuals))
                .eligibleCost(sum(rows, ChargeLineItemRowDTO::getEligibleCost))
                .invoiceOne(sum(rows, ChargeLineItemRowDTO::getInvoiceOne))
                .invoiceTwo(sum(rows, ChargeLineItemRowDTO::getInvoiceTwo))
                .invoiceThree(sum(rows, ChargeLineItemRowDTO::getInvoiceThree))
                .invoiceFour(sum(rows, ChargeLineItemRowDTO::getInvoiceFour))
                .outstanding(sum(rows, ChargeLineItemRowDTO::getOutstanding))
                .totalInvoices(sum(rows, ChargeLineItemRowDTO::getTotalInvoices))
                .contractRateTotal(sum(rows, ChargeLineItemRowDTO::getContractedRate))
                .build();
    }

    private static BigDecimal sum(List<ChargeLineItemRowDTO> rows, java.util.function.Function<ChargeLineItemRowDTO, BigDecimal> getter) {
        return rows.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Line-item currency follows the charge currency set by shipment service (origin for DTS, etc.).
     * Account billing/rate currency is only a fallback when the charge has no currency.
     */
    static String resolveCurrency(Charge charge, Account account) {
        if (charge != null && charge.getCurrency() != null && !charge.getCurrency().isBlank()) {
            return charge.getCurrency();
        }
        if (account != null && AccountNature.AR.equals(account.getAccountNature())) {
            String billingCurrency = ArAccountMasterSupport.resolveBillingCurrency(account.getArAccount());
            if (billingCurrency != null && !billingCurrency.isBlank()) {
                return billingCurrency;
            }
            String rateCurrency = ArAccountMasterSupport.resolveRateCurrency(account.getArAccount());
            if (rateCurrency != null && !rateCurrency.isBlank()) {
                return rateCurrency;
            }
        } else if (account != null && account.getBillingCurrency() != null && !account.getBillingCurrency().isBlank()) {
            return account.getBillingCurrency();
        } else if (account != null && account.getRateCurrency() != null && !account.getRateCurrency().isBlank()) {
            return account.getRateCurrency();
        }
        return charge != null ? charge.getCurrency() : null;
    }

    public static String accountKey(String entityCode, String chargeType) {
        return RelatedChargeTypeResolver.normalize(entityCode) + "|" + RelatedChargeTypeResolver.normalize(chargeType);
    }

    public static String entityCodeKey(String entityCode) {
        return RelatedChargeTypeResolver.normalize(entityCode);
    }

    public static String apAccountKey(String entityCode, String accountNumber) {
        return RelatedChargeTypeResolver.normalize(entityCode)
                + "|"
                + RelatedChargeTypeResolver.normalize(accountNumber);
    }

    public static String arAccountKey(String entityCode, String accountNumber) {
        return RelatedChargeTypeResolver.normalize(entityCode)
                + "|"
                + RelatedChargeTypeResolver.normalize(accountNumber);
    }

    public static String serviceCodeKey(String entityCode, String chargeType, String serviceCode) {
        return RelatedChargeTypeResolver.normalize(entityCode)
                + "|"
                + RelatedChargeTypeResolver.normalize(chargeType)
                + "|"
                + RelatedChargeTypeResolver.normalize(serviceCode);
    }

    private static LocalDate toLocalDate(Instant instant) {
        return instant == null ? null : instant.atZone(ZoneOffset.UTC).toLocalDate();
    }

    private static BigDecimal toBigDecimal(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private static String formatLabel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeAccountValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static LineItemAccount sanitizeLineItemAccount(
            String chargeType,
            String accountNumber,
            String accountName) {

        String normalizedNumber = normalizeAccountValue(accountNumber);
        if (usesCashWhenNoAccountNumber(chargeType) && !StringUtils.hasText(normalizedNumber)) {
            return new LineItemAccount(null, null);
        }
        return new LineItemAccount(normalizedNumber, normalizeAccountValue(accountName));
    }

    private static boolean usesCashWhenNoAccountNumber(String chargeType) {
        return ChargeTypeConstants.DTC.equals(chargeType)
                || ChargeTypeConstants.DTS.equals(chargeType)
                || ChargeTypeConstants.DT3.equals(chargeType);
    }

    private static Account firstLineItemAccount(List<ChargeLineItem> lineItems) {
        if (lineItems == null) {
            return null;
        }
        for (ChargeLineItem lineItem : lineItems) {
            if (lineItem != null && lineItem.getAccount() != null) {
                return lineItem.getAccount();
            }
        }
        return null;
    }

    private static String firstLineItemApAccountNumber(List<ChargeLineItem> lineItems) {
        if (lineItems == null) {
            return null;
        }

        return lineItems.stream()
                .filter(lineItem -> lineItem != null
                        && StringUtils.hasText(lineItem.getApAccountNumber()))
                .min(Comparator.comparing(
                        ChargeLineItem::getCreatedTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(lineItem -> lineItem.getApAccountNumber().trim())
                .orElse(null);
    }

    private static APAccount resolveApAccount(
            Charge charge,
            String apAccountNumber,
            Map<String, APAccount> apAccountsByKey) {

        if (!StringUtils.hasText(apAccountNumber)
                || apAccountsByKey == null
                || apAccountsByKey.isEmpty()
                || charge.getEntity() == null
                || !StringUtils.hasText(charge.getEntity().getCode())) {
            return null;
        }
        return apAccountsByKey.get(apAccountKey(charge.getEntity().getCode().trim(), apAccountNumber.trim()));
    }

    private record LineItemAccount(String accountNumber, String accountName) {}

    private record ActualAmounts(
            BigDecimal actualOne,
            BigDecimal actualTwo,
            BigDecimal actualThree,
            BigDecimal actualFour,
            BigDecimal invoiceOne,
            BigDecimal invoiceTwo,
            BigDecimal invoiceThree,
            BigDecimal invoiceFour) {

        static ActualAmounts empty() {
            return new ActualAmounts(null, null, null, null, null, null, null, null);
        }

        static ActualAmounts actualOne(BigDecimal amount) {
            return new ActualAmounts(amount, null, null, null, null, null, null, null);
        }

        static ActualAmounts actualTwo(BigDecimal amount) {
            return new ActualAmounts(null, amount, null, null, null, null, null, null);
        }

        static ActualAmounts actualThree(BigDecimal amount) {
            return new ActualAmounts(null, null, amount, null, null, null, null, null);
        }

        static ActualAmounts actualFour(BigDecimal amount) {
            return new ActualAmounts(null, null, null, amount, null, null, null, null);
        }

        static ActualAmounts invoiceOne(BigDecimal amount) {
            return new ActualAmounts(null, null, null, null, amount, null, null, null);
        }

        static ActualAmounts invoiceTwo(BigDecimal amount) {
            return new ActualAmounts(null, null, null, null, null, amount, null, null);
        }

        static ActualAmounts invoiceThree(BigDecimal amount) {
            return new ActualAmounts(null, null, null, null, null, null, amount, null);
        }

        static ActualAmounts invoiceFour(BigDecimal amount) {
            return new ActualAmounts(null, null, null, null, null, null, null, amount);
        }

        static ActualAmounts fromActualSequence(List<BigDecimal> amounts) {
            if (amounts == null || amounts.isEmpty()) {
                return empty();
            }
            return new ActualAmounts(
                    amountAt(amounts, 0),
                    amountAt(amounts, 1),
                    amountAt(amounts, 2),
                    amountAt(amounts, 3),
                    null,
                    null,
                    null,
                    null);
        }

        private static BigDecimal amountAt(List<BigDecimal> amounts, int index) {
            return index < amounts.size() ? amounts.get(index) : null;
        }

        ActualAmounts merge(ActualAmounts other) {
            if (other == null) {
                return this;
            }
            return new ActualAmounts(
                    sum(actualOne, other.actualOne),
                    sum(actualTwo, other.actualTwo),
                    sum(actualThree, other.actualThree),
                    sum(actualFour, other.actualFour),
                    sum(invoiceOne, other.invoiceOne),
                    sum(invoiceTwo, other.invoiceTwo),
                    sum(invoiceThree, other.invoiceThree),
                    sum(invoiceFour, other.invoiceFour));
        }

        boolean hasActualBuckets() {
            return actualOne != null || actualTwo != null || actualThree != null || actualFour != null;
        }

        BigDecimal totalActuals() {
            return sum(actualOne, actualTwo, actualThree, actualFour);
        }

        BigDecimal totalInvoices() {
            return sum(invoiceOne, invoiceTwo, invoiceThree, invoiceFour);
        }

        private static BigDecimal sum(BigDecimal... values) {
            BigDecimal total = BigDecimal.ZERO;
            boolean any = false;
            for (BigDecimal value : values) {
                if (value != null) {
                    total = total.add(value);
                    any = true;
                }
            }
            return any ? total : null;
        }
    }
}
