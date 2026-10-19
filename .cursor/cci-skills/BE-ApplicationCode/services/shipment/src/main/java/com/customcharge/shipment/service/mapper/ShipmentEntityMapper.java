package com.customcharge.shipment.service.mapper;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Shipment;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentEntityMapper {


    public void map(ShipmentDto dto, Shipment shipment) {
        shipment.setHawbNumber(dto.getHawb());
        shipment.setProductType(dto.getProductType());
        shipment.setPaymentType(dto.getPaymentType());
        shipment.setProductGroup(dto.getProductGroup());
        shipment.setBusinessType(dto.getBusinessType());
        shipment.setAccountBusinessType(dto.getAccountBusinessType());
        shipment.setShipmentType(dto.getShipmentType());
        shipment.setCategoryType(dto.getCategoryType());
        shipment.setOriginEntity(dto.getHawbOriginEntity());
        shipment.setHAWBDestinationEntity(dto.getDestinationEntity());
        shipment.setThirdPartyEntity(dto.getThirdPartyEntity());
        shipment.setThirdPartyCity(dto.getThirdPartyCity());
        shipment.setThirdPartyCountry(dto.getThirdPartyCountry());
        shipment.setShipperNumber(dto.getShipperAccountNumber());
        shipment.setShipperID(dto.getShipperID());
        shipment.setConsigneeNumber(dto.getConsigneeAccountNumber());
        shipment.setThirdPartyNumber(dto.getThirdPartyAccountNumber());
        shipment.setPieces(dto.getPieces());
        shipment.setChargeableWeight(dto.getChargeableWeight());
        shipment.setWeight(dto.getHawbWeight());
        shipment.setUnit(dto.getHawbWeightUnit());
        shipment.setCollectAmount(dto.getCollectAmount());
        shipment.setCollectCurrency(dto.getCollectCurrencyCode());
        shipment.setCostOfGoods(dto.getCustomsAmount());
        shipment.setCustomsCurrency(dto.getCustomsCurrencyCode());
        shipment.setCodValue(dto.getCodValue());
        shipment.setCodCurrency(dto.getCodCurrencyCode());
        shipment.setCashValue(dto.getCashValue());
        shipment.setCashCurrency(dto.getCashCurrency());
        shipment.setInsuranceValue(dto.getInsuranceValue());
        shipment.setCostOfGoods(dto.getCostOfGoods());
        shipment.setCommodityCountryCode(dto.getCommodityCountryCode());
        shipment.setCommodityDescription(dto.getCommodityDescription());
        shipment.setOriginLocationCode(dto.getOriginLocationCode());
        shipment.setDestinationLocationCode(dto.getDestinationLocationCode());
        shipment.setShipperCity(dto.getShipperCity());
        shipment.setConsigneeCity(dto.getConsigneeCity());
        shipment.setShipperCountryCode(dto.getShipperCountryCode());
        shipment.setConsigneeCountryCode(dto.getConsigneeCountryCode());
        shipment.setShipperName(dto.getShipperName());
        shipment.setConsigneeName(dto.getConsigneeName());
        shipment.setServices(dto.getServices());
        shipment.setCollectionReference(dto.getCollectionReference());
        shipment.setAttentionOf(dto.getAttnOf());
        shipment.setSkeleton(dto.getSkeleton());
        shipment.setHAWBID(dto.getHawbId());
        shipment.setConsigneeID(dto.getConsigneeID());
        shipment.setThirdPartyID(dto.getThirdPartyID());
        shipment.setConsigneeStateCode(dto.getConsigneeStateCode());
        shipment.setConsigneeZipCode(dto.getConsigneeZipCode());
        shipment.setMAWBConsole(dto.getMawbConsole());
        shipment.setMawbId(dto.getMawbId());
        shipment.setShipperStateCode(dto.getShipperStateCode());
        shipment.setShipperZipCode(dto.getShipperZipCode());
        shipment.setShipperReference(dto.getShipperReference());
        shipment.setShipperReference2(dto.getShipperReference2());
        shipment.setThirdPartyName(dto.getThirdPartyName());
        shipment.setThirdPartyReference(dto.getThirdPartyReference());
        shipment.setThirdPartyReference2(dto.getThirdPartyReference2());
        shipment.setThirdPartyState(dto.getThirdPartyState());
        shipment.setPiNumber(dto.getPiNumber());
        shipment.setInsuranceCurrency(dto.getInsuranceCurrency());
        shipment.setConsigneeReference(dto.getConsigneeReference());
        shipment.setConsigneeReference2(dto.getConsigneeReference2());

        shipment.setHawbDimensions(dto.getHawbDimensions());
        shipment.setForeignHAWBNumber(dto.getForeignHAWBNumber());

        if (hasText(dto.getHawbAlertedDate())) {
            shipment.setHAWBAlertedDate(LocalDateTime.parse(dto.getHawbAlertedDate()));
        }
        if (hasText(dto.getPickupDate())) {
            shipment.setPickupDate(LocalDateTime.parse(dto.getPickupDate()));
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }
}
