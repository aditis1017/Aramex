package com.customcharge.shipment.mapper;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.shipment.enums.PICode;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static com.customcharge.shipment.constants.ShipmentXmlFields.*;

@Component
@RequiredArgsConstructor
public class XmlShipmentDtoMapper {

    private final EntityRepository entityRepository;

    public ShipmentDto map(JsonNode node) {
        JsonNode hawbUpdate = resolveHawbUpdateNode(node);
        JsonNode hawbDetails = hawbUpdate.path(HAWB_DETAILS);
        JsonNode mawbDetails = hawbUpdate.path(MAWB_DETAILS);
        JsonNode item = hawbUpdate.path(ITEMS).path(ITEM);

        ShipmentDto dto = new ShipmentDto();
        dto.setHawb(hawbUpdate.path(HAWB_NUMBER).asText());
        dto.setProductType(hawbDetails.path(PRODUCT_TYPE).asText().toUpperCase());
        dto.setProductGroup(hawbDetails.path(HAWB_PRODUCT_GROUP).asText().toUpperCase());
        dto.setPaymentType(hawbDetails.path(PAYMENT_TYPE).asText().toUpperCase());
        dto.setHawbAlertedDate(hawbDetails.path(HAWB_ALERTED_DATE).asText());
        dto.setAttnOf(hawbDetails.path(ATTENTION_OF).asText());
        dto.setShipperAccountNumber(hawbDetails.path(SHIPPER_NUMBER).asText());
        dto.setConsigneeAccountNumber(hawbDetails.path(CONSIGNEE_NUMBER).asText());
        dto.setThirdPartyAccountNumber(hawbDetails.path(THIRD_PARTY_NUMBER).asText());

        dto.setThirdPartyName(hawbDetails.path(THIRD_PARTY_NAME).asText());
        dto.setThirdPartyReference(hawbDetails.path(THIRD_PARTY_REFERENCE).asText());
        dto.setThirdPartyReference2(hawbDetails.path(THIRD_PARTY_REFERENCE_2).asText());
        dto.setThirdPartyState(hawbDetails.path(THIRD_PARTY_STATE).asText());

        dto.setShipperEntity(hawbDetails.path(HAWB_ORIGIN_ENTITY).asText());
        dto.setDestinationEntity(hawbDetails.path(HAWB_DESTINATION_ENTITY).asText());
        dto.setThirdPartyEntity(hawbDetails.path(THIRD_PARTY_ENTITY).asText().toUpperCase());
        dto.setThirdPartyCity(hawbDetails.path(THIRD_PARTY_CITY).asText());
        dto.setThirdPartyCountry(hawbDetails.path(THIRD_PARTY_COUNTRY).asText());
        dto.setPieces(hawbUpdate.path(PIECES).asInt());
        dto.setServices(hawbDetails.path(SERVICES).asText());
        dto.setPickupDate(hawbDetails.path(PICKUP_DATE).asText());
        dto.setHawbWeight(hawbDetails.path(HAWB_WEIGHT).asDouble());
        dto.setChargeableWeight(hawbDetails.path(CHARGEABLE_WEIGHT).asDouble());
        dto.setHawbWeightUnit(hawbDetails.path(HAWB_WEIGHT_UNIT).asText());
        dto.setCollectAmount(hawbDetails.path(COLLECT_AMOUNT).asDouble());
        dto.setCollectCurrencyCode(hawbDetails.path(COLLECT_CURRENCY_CODE).asText());
        dto.setCustomsCurrencyCode(hawbDetails.path(CUSTOMS_CURRENCY_CODE).asText());
        dto.setCommodityCountryCode(hawbDetails.path(COMMODITY_COUNTRY_CODE).asText());
        dto.setCommodityDescription(hawbDetails.path(COMMODITY_DESCRIPTION).asText());
        dto.setShipperName(hawbDetails.path(SHIPPER_NAME).asText());
        dto.setShipperCity(hawbDetails.path(SHIPPER_CITY).asText());
        dto.setShipperCountryCode(hawbDetails.path(SHIPPER_COUNTRY_CODE).asText());
        dto.setConsigneeName(hawbDetails.path(CONSIGNEE_NAME).asText());
        dto.setConsigneeCity(hawbDetails.path(CONSIGNEE_CITY).asText());
        dto.setConsigneeCountryCode(hawbDetails.path(CONSIGNEE_COUNTRY_CODE).asText().toUpperCase());
        dto.setCodValue(hawbDetails.path(COD_VALUE).asDouble());
        dto.setCodCurrencyCode(hawbDetails.path(COD_CURRENCY_CODE).asText());
        dto.setCashValue(hawbDetails.path(CASH_VALUE).asDouble());
        dto.setInsuranceValue(hawbDetails.path(INSURANCE_VALUE).asDouble());
        dto.setOriginLocationCode(hawbDetails.path(ORIGIN_LOCATION_CODE).asText());
        dto.setDestinationLocationCode(hawbDetails.path(DESTINATION_LOCATION_CODE).asText());
        dto.setCollectionReference(hawbDetails.path(COLLECTION_REFERENCE).asText());
        dto.setCountryOfOrigin(item.path(COUNTRY_OF_ORIGIN).asText().toUpperCase());
        dto.setHawbOriginEntity(hawbUpdate.path(HAWB_ORIGIN_ENTITY).asText().toUpperCase());
        dto.setCostOfGoods(hawbDetails.path(CUSTOMS_AMOUNT).asDouble());
        dto.setPiNumber(hawbUpdate.path(PI_NUMBER).asText());
        dto.setComment1(hawbUpdate.path(COMMENT1).asText());
        dto.setComment2(hawbUpdate.path(COMMENT2).asText());

        dto.setSkeleton(hawbDetails.path(SKELETON).asBoolean());
        JsonNode shipmentProfile = hawbDetails
                .path(ADDITIONAL_PROPERTIES)
                .path(SHIPMENT_PROFILE);

        dto.setBusinessType(shipmentProfile.path(BUSINESS_TYPE).asText());
        dto.setHawbId(hawbUpdate.path(HAWB_ID).asLong());
        dto.setConsigneeID(hawbDetails.path(CONSIGNEE_ID).asLong());
        dto.setShipperID(hawbDetails.path(SHIPPER_ID).asLong());
        dto.setThirdPartyID(hawbDetails.path(THIRD_PARTY_ID).asLong());
        dto.setConsigneeStateCode(hawbDetails.path(CONSIGNEE_STATE_CODE).asText());
        dto.setConsigneeZipCode(hawbDetails.path(CONSIGNEE_ZIP_CODE).asText());
        String mawbNumber = mawbDetails.path(MAWB_NUMBER).asText(null);
        if (mawbNumber != null && !mawbNumber.isBlank()) {
            dto.setMawbConsole(mawbNumber.trim());
        }
        dto.setMawbId(mawbDetails.path(MAWB_ID).asLong());
        dto.setShipperStateCode(hawbDetails.path(SHIPPER_STATE_CODE).asText());
        dto.setShipperZipCode(hawbDetails.path(SHIPPER_ZIP_CODE).asText());
        dto.setCashCurrency(hawbDetails.path(CASH_CURRENCY).asText());
        dto.setShipperReference2(hawbDetails.path(SHIPPER_REFERENCE_2).asText());
        dto.setShipperReference(hawbDetails.path(SHIPPER_REFERENCE).asText());
        dto.setInsuranceCurrency(hawbDetails.path(INSURANCE_CURRENCY).asText());
        dto.setForeignHAWBNumber(hawbDetails.path(FOREIGN_HAWB_NUMBER).asText());
        dto.setHawbDimensions(hawbDetails.path(HAWB_DIMENSIONS).asText());
        dto.setConsigneeReference2(hawbDetails.path(CONSIGNEE_REFERENCE_2).asText());
        dto.setConsigneeReference(hawbDetails.path(CONSIGNEE_REFERENCE).asText());

        applySh022EntityOverrides(dto, hawbUpdate);
        return dto;
    }

    /**
     * SH022: origin from {@code UpdateEntity} (InfoAxis id on {@code entity}), destination from
     * {@code Comment1} first segment when that entity code exists.
     */
    private void applySh022EntityOverrides(ShipmentDto dto, JsonNode hawbUpdate) {
        if (!PICode.SH022.getName().equalsIgnoreCase(dto.getPiNumber())) {
            return;
        }

        String updateEntity = hawbUpdate.path(UPDATE_ENTITY).asText(null);
        if (StringUtils.hasText(updateEntity)) {
            entityRepository.findByInfoAxsId(updateEntity.trim())
                    .map(Entity::getCode)
                    .filter(StringUtils::hasText)
                    .ifPresent(code -> {
                        dto.setShipperEntity(code);
                        dto.setHawbOriginEntity(code.toUpperCase());
                    });
        }

        String comment1 = dto.getComment1();
        if (!StringUtils.hasText(comment1) || !comment1.contains(",")) {
            return;
        }
        String entityCodeFromComment = comment1.substring(0, comment1.indexOf(',')).trim();
        if (!StringUtils.hasText(entityCodeFromComment)) {
            return;
        }
        entityRepository.findByCodeIgnoreCase(entityCodeFromComment)
                .map(Entity::getCode)
                .ifPresent(dto::setDestinationEntity);
    }

    /**
     * Supports both wrapped documents ({@code InfoLinkDocument/HAWBUpdate}) and messages whose root
     * element is {@code HAWBUpdate}.
     */
    public JsonNode resolveHawbUpdateNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        JsonNode nested = node.path(HAWB_UPDATE);
        if (!nested.isMissingNode() && !nested.isNull()) {
            return nested;
        }
        return node;
    }
}
