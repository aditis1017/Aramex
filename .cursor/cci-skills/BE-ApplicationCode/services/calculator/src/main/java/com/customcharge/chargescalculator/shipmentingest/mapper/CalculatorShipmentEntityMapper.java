package com.customcharge.chargescalculator.shipmentingest.mapper;

import com.customcharge.chargescalculator.shipmentingest.masterdata.MasterDataCodeLookupService;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Shipment;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.customcharge.chargescalculator.shipmentingest.IngestConstants.BUSINESS_TYPE;
import static com.customcharge.chargescalculator.shipmentingest.IngestConstants.PAYMENT_TYPE;
import static com.customcharge.chargescalculator.shipmentingest.IngestConstants.PRODUCT_GROUP;

@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CalculatorShipmentEntityMapper {

    MasterDataCodeLookupService masterDataCodeLookupService;

    public void map(ShipmentDto dto, Shipment shipment) {
        shipment.setHawbNumber(dto.getHawb());
        shipment.setProductType(dto.getProductType());
        shipment.setPaymentType(resolveCode(PAYMENT_TYPE, dto.getPaymentType()).orElse(null));
        shipment.setProductGroup(resolveCode(PRODUCT_GROUP, dto.getProductGroup()).orElse(null));
        shipment.setBusinessType(resolveCode(BUSINESS_TYPE, dto.getBusinessType()).orElse(null));
        shipment.setShipmentType(dto.getShipmentType());
        shipment.setCategoryType(dto.getCategoryType());
        shipment.setOriginEntity(dto.getHawbOriginEntity());
        shipment.setHAWBDestinationEntity(dto.getDestinationEntity());
        shipment.setThirdPartyEntity(dto.getThirdPartyEntity());
        shipment.setShipperNumber(dto.getShipperAccountNumber());
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
        shipment.setHAWBID(dto.getHawbId());
        shipment.setSkeleton(dto.getSkeleton());

        if (hasText(dto.getHawbAlertedDate())) {
            shipment.setHAWBAlertedDate(LocalDateTime.parse(dto.getHawbAlertedDate()));
        }
        if (hasText(dto.getPickupDate())) {
            shipment.setPickupDate(LocalDateTime.parse(dto.getPickupDate()));
        }
    }

    private Optional<String> resolveCode(String categoryCode, String input) {
        return masterDataCodeLookupService.resolveCode(categoryCode, input);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }
}
