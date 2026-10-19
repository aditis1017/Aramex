package com.customcharge.shipment.service.mapper;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Shipment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Maps persisted {@link Shipment} entities into {@link ShipmentDto} for rule evaluation.
 * Category type is intentionally omitted so Drools can resolve it fresh.
 */
@Component
public class ShipmentToDtoMapper {

    public ShipmentDto mapForCategoryRules(Shipment shipment) {
        if (shipment == null) {
            return new ShipmentDto();
        }

        ShipmentDto dto = new ShipmentDto();
        dto.setHawb(shipment.getHawbNumber());
        dto.setHawbId(shipment.getHAWBID());
        dto.setProductType(shipment.getProductType());
        dto.setProductGroup(shipment.getProductGroup());
        dto.setPaymentType(shipment.getPaymentType());
        dto.setHawbAlertedDate(formatDateTime(shipment.getHAWBAlertedDate()));
        dto.setAttnOf(shipment.getAttentionOf());
        dto.setMawbConsole(shipment.getMAWBConsole());
        dto.setMawbId(shipment.getMawbId());

        dto.setShipperAccountNumber(shipment.getShipperNumber());
        dto.setConsigneeAccountNumber(shipment.getConsigneeNumber());
        dto.setThirdPartyAccountNumber(shipment.getThirdPartyNumber());
        dto.setThirdPartyName(shipment.getThirdPartyName());
        dto.setThirdPartyNumber(shipment.getThirdPartyNumber());
        dto.setThirdPartyReference(shipment.getThirdPartyReference());
        dto.setThirdPartyReference2(shipment.getThirdPartyReference2());
        dto.setThirdPartyState(shipment.getThirdPartyState());
        dto.setConsigneeID(shipment.getConsigneeID());
        dto.setShipperID(shipment.getShipperID());
        dto.setThirdPartyID(shipment.getThirdPartyID());
        dto.setShipperStateCode(shipment.getShipperStateCode());
        dto.setShipperZipCode(shipment.getShipperZipCode());
        dto.setShipperReference(shipment.getShipperReference());
        dto.setShipperReference2(shipment.getShipperReference2());

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

        dto.setCustomsAmount(shipment.getCostOfGoods());
        dto.setCustomsCurrencyCode(shipment.getCustomsCurrency());

        dto.setCommodityCountryCode(shipment.getCommodityCountryCode());
        dto.setCommodityDescription(shipment.getCommodityDescription());

        dto.setShipperName(shipment.getShipperName());
        dto.setShipperCity(shipment.getShipperCity());
        dto.setShipperCountryCode(shipment.getShipperCountryCode());

        dto.setConsigneeName(shipment.getConsigneeName());
        dto.setConsigneeCity(shipment.getConsigneeCity());
        dto.setConsigneeCountryCode(shipment.getConsigneeCountryCode());
        dto.setConsigneeStateCode(shipment.getConsigneeStateCode());
        dto.setConsigneeZipCode(shipment.getConsigneeZipCode());

        dto.setCodValue(shipment.getCodValue());
        dto.setCodCurrencyCode(shipment.getCodCurrency());
        dto.setCashValue(shipment.getCashValue());
        dto.setInsuranceValue(shipment.getInsuranceValue());
        dto.setInsuranceCurrency(shipment.getInsuranceCurrency());
        dto.setCashCurrency(shipment.getCashCurrency());

        dto.setOriginLocationCode(shipment.getOriginLocationCode());
        dto.setDestinationLocationCode(shipment.getDestinationLocationCode());
        dto.setCollectionReference(shipment.getCollectionReference());
        dto.setForeignHAWBNumber(shipment.getForeignHAWBNumber());
        dto.setHawbDimensions(shipment.getHawbDimensions());

        dto.setHawbOriginEntity(shipment.getOriginEntity());
        dto.setPiNumber(shipment.getPiNumber());
        dto.setCostOfGoods(shipment.getCostOfGoods());
        dto.setCifValue(shipment.getCifValue());
        dto.setCifValueCurrency(shipment.getCifValueCurrency());
        dto.setSkeleton(shipment.getSkeleton());
        dto.setConsigneeReference(shipment.getConsigneeReference());
        dto.setConsigneeReference2(shipment.getConsigneeReference2());

        dto.setShipmentType(shipment.getShipmentType());
        dto.setBusinessType(shipment.getBusinessType());
        dto.setAccountBusinessType(shipment.getAccountBusinessType());
        return dto;
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
