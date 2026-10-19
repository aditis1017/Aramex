package com.customcharge.shipment.controller;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.CategoryValue;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.CategoryValueRepository;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import com.customcharge.persistencelayer.repository.ShipmentRuleAssignmentRepository;
import com.customcharge.shipment.dto.MigrationRunRequest;
import com.customcharge.shipment.service.ShipmentMessageProcessor;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.customcharge.shipment.constants.ShipmentConstants.PAYMENT_TYPE_THIRD_PARTY;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_TYPE_DDP;

@RestController
@RequestMapping("/migration")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
public class MigrationController {

    private static final String MIGRATION_STATUS_CODE = "MIGRATION_STATUS";

    ShipmentRepository shipmentRepository;
    ChargeRepository chargeRepository;
    ChargeLineItemRepository chargeLineItemRepository;
    ShipmentRuleAssignmentRepository shipmentRuleAssignmentRepository;
    ShipmentMessageProcessor shipmentMessageProcessor;
    CategoryValueRepository categoryValueRepository;

    @PostMapping("/run")
    @Transactional
    public Map<String, Object> runMigration(@RequestBody(required = false) MigrationRunRequest request) {
        CategoryValue migrationStatus = categoryValueRepository
                .findByCode(MIGRATION_STATUS_CODE)
                .orElse(null);
        if (migrationStatus == null || !"true".equalsIgnoreCase(migrationStatus.getName())) {
            Map<String, Object> skipped = new HashMap<>();
            skipped.put("status", "SKIPPED");
            skipped.put("message", "Migration is disabled (MIGRATION_STATUS is not true)");
            return skipped;
        }

        Map<String, Object> result = hasHawbNumbers(request)
                ? runHawbRecalculation(normalizeHawbNumbers(request.getHawbNumbers()))
                : runDdpThirdPartyMigration();

        migrationStatus.setName("false");
        categoryValueRepository.save(migrationStatus);
        return result;
    }

    private static boolean hasHawbNumbers(MigrationRunRequest request) {
        return request != null
                && request.getHawbNumbers() != null
                && !normalizeHawbNumbers(request.getHawbNumbers()).isEmpty();
    }

    private static List<String> normalizeHawbNumbers(List<String> hawbNumbers) {
        if (hawbNumbers == null || hawbNumbers.isEmpty()) {
            return List.of();
        }
        Set<String> unique = new HashSet<>();
        List<String> normalized = new ArrayList<>();
        for (String hawb : hawbNumbers) {
            if (!StringUtils.hasText(hawb)) {
                continue;
            }
            String trimmed = hawb.trim();
            if (unique.add(trimmed)) {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }

    private Map<String, Object> runHawbRecalculation(List<String> requestedHawbs) {
        List<Shipment> shipments = shipmentRepository.findByHawbNumberIn(requestedHawbs);
        int processed = migrateAll(shipments);

        Set<String> foundHawbs = shipments.stream()
                .map(Shipment::getHawbNumber)
                .collect(Collectors.toSet());
        List<String> notFound = requestedHawbs.stream()
                .filter(hawb -> !foundHawbs.contains(hawb))
                .toList();

        log.info("HAWB migration completed: processed {} of {} requested shipments", processed, requestedHawbs.size());
        Map<String, Object> response = new HashMap<>();
        response.put("mode", "HAWB");
        response.put("status", "COMPLETED");
        response.put("requested", requestedHawbs.size());
        response.put("total", shipments.size());
        response.put("processed", processed);
        response.put("notFound", notFound);
        response.put("message", "Recalculation completed for requested HAWBs present in shipment domain");
        return response;
    }

    private Map<String, Object> runDdpThirdPartyMigration() {
        List<Shipment> shipments = shipmentRepository.findByPaymentTypeAndShipmentType(
                PAYMENT_TYPE_THIRD_PARTY, SHIPMENT_TYPE_DDP);
        int processed = migrateAll(shipments);

        log.info("DDP shipment migration completed: processed {} of {} shipments", processed, shipments.size());
        Map<String, Object> response = new HashMap<>();
        response.put("mode", "DDP_THIRD_PARTY");
        response.put("status", "COMPLETED");
        response.put("total", shipments.size());
        response.put("processed", processed);
        response.put("message", "Migration completed successfully");
        return response;
    }

    private int migrateAll(List<Shipment> shipments) {
        int processed = 0;
        for (Shipment shipment : shipments) {
            migrateShipment(shipment);
            processed++;
        }
        return processed;
    }

    private void migrateShipment(Shipment shipment) {
        Instant createdTime = shipment.getCreatedTime();
        Instant updatedTime = shipment.getUpdatedTime();
        ShipmentDto dto = toShipmentDto(shipment);
        String hawb = shipment.getHawbNumber();
        deleteChargesForShipment(shipment.getId());
        shipmentRuleAssignmentRepository.deleteByHawbNumber(hawb);
        shipmentRepository.delete(shipment);
        shipmentMessageProcessor.processShipmentDto(dto, ShipmentMessageProcessor.IngestChannel.SQS);
        restoreAuditTimestamps(hawb, createdTime, updatedTime);
    }

    private void restoreAuditTimestamps(String hawb, Instant createdTime, Instant updatedTime) {
        if (createdTime == null || updatedTime == null) {
            return;
        }
        shipmentRepository.updateAuditTimesByHawbNumber(hawb, createdTime, updatedTime);
        chargeRepository.updateAuditTimesByShipmentHawbNumber(hawb, createdTime, updatedTime);
        chargeLineItemRepository.updateAuditTimesByShipmentHawbNumber(hawb, createdTime, updatedTime);
    }

    private void deleteChargesForShipment(UUID shipmentId) {
        List<Charge> charges = chargeRepository.findByShipment_Id(shipmentId);
        if (charges.isEmpty()) {
            return;
        }
        List<UUID> chargeIds = charges.stream().map(Charge::getId).toList();
        chargeLineItemRepository.deleteByCharge_IdIn(chargeIds);
        chargeRepository.deleteByShipment_Id(shipmentId);
    }

    private ShipmentDto toShipmentDto(Shipment shipment) {
        ShipmentDto dto = new ShipmentDto();
        dto.setCreatedTime(shipment.getCreatedTime());
        dto.setUpdatedTime(shipment.getUpdatedTime());
        dto.setIsShipmentFull(true);

        dto.setHawb(shipment.getHawbNumber());
        dto.setProductType(shipment.getProductType());
        dto.setProductGroup(shipment.getProductGroup());
        dto.setPaymentType(shipment.getPaymentType());
        dto.setHawbAlertedDate(formatDateTime(shipment.getHAWBAlertedDate()));
        dto.setAttnOf(shipment.getAttentionOf());
        dto.setShipperAccountNumber(shipment.getShipperNumber());
        dto.setConsigneeAccountNumber(shipment.getConsigneeNumber());
        dto.setThirdPartyAccountNumber(shipment.getThirdPartyNumber());
        dto.setThirdPartyName(shipment.getThirdPartyName());
        dto.setThirdPartyReference(shipment.getThirdPartyReference());
        dto.setThirdPartyReference2(shipment.getThirdPartyReference2());
        dto.setThirdPartyState(shipment.getThirdPartyState());
        dto.setShipperEntity(shipment.getOriginEntity());
        dto.setDestinationEntity(shipment.getHAWBDestinationEntity());
        dto.setThirdPartyEntity(shipment.getThirdPartyEntity());
        dto.setThirdPartyCity(shipment.getThirdPartyCity());
        dto.setThirdPartyCountry(shipment.getThirdPartyCountry());
        dto.setPieces(shipment.getPieces());
        dto.setServices(shipment.getServices());
        dto.setPickupDate(formatDateTime(shipment.getPickupDate()));
        dto.setHawbWeight(shipment.getWeight());
        dto.setChargeableWeight(shipment.getChargeableWeight());
        dto.setHawbWeightUnit(shipment.getUnit());
        dto.setCollectAmount(shipment.getCollectAmount());
        dto.setCollectCurrencyCode(shipment.getCollectCurrency());
        dto.setCustomsCurrencyCode(shipment.getCustomsCurrency());
        dto.setCommodityCountryCode(shipment.getCommodityCountryCode());
        dto.setCommodityDescription(shipment.getCommodityDescription());
        dto.setShipperName(shipment.getShipperName());
        dto.setShipperCity(shipment.getShipperCity());
        dto.setShipperCountryCode(shipment.getShipperCountryCode());
        dto.setConsigneeName(shipment.getConsigneeName());
        dto.setConsigneeCity(shipment.getConsigneeCity());
        dto.setConsigneeCountryCode(shipment.getConsigneeCountryCode());
        dto.setCodValue(shipment.getCodValue());
        dto.setCodCurrencyCode(shipment.getCodCurrency());
        dto.setCashValue(shipment.getCashValue());
        dto.setInsuranceValue(shipment.getInsuranceValue());
        dto.setOriginLocationCode(shipment.getOriginLocationCode());
        dto.setDestinationLocationCode(shipment.getDestinationLocationCode());
        dto.setCollectionReference(shipment.getCollectionReference());
        dto.setHawbOriginEntity(shipment.getOriginEntity());
        dto.setCostOfGoods(shipment.getCostOfGoods());
        dto.setPiNumber(shipment.getPiNumber());
        dto.setSkeleton(shipment.getSkeleton());
        dto.setBusinessType(shipment.getBusinessType());
        dto.setHawbId(shipment.getHAWBID());
        dto.setConsigneeID(shipment.getConsigneeID());
        dto.setShipperID(shipment.getShipperID());
        dto.setThirdPartyID(shipment.getThirdPartyID());
        dto.setConsigneeStateCode(shipment.getConsigneeStateCode());
        dto.setConsigneeZipCode(shipment.getConsigneeZipCode());
        dto.setMawbConsole(shipment.getMAWBConsole());
        dto.setMawbId(shipment.getMawbId());
        dto.setShipperStateCode(shipment.getShipperStateCode());
        dto.setShipperZipCode(shipment.getShipperZipCode());
        dto.setCashCurrency(shipment.getCashCurrency());
        dto.setShipperReference2(shipment.getShipperReference2());
        dto.setShipperReference(shipment.getShipperReference());
        dto.setInsuranceCurrency(shipment.getInsuranceCurrency());
        dto.setForeignHAWBNumber(shipment.getForeignHAWBNumber());
        dto.setHawbDimensions(shipment.getHawbDimensions());
        dto.setConsigneeReference2(shipment.getConsigneeReference2());
        dto.setConsigneeReference(shipment.getConsigneeReference());
        return dto;
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
